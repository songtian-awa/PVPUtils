package com.pvp_utils.client.modules.impl.Combat.LocalReplay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.pvp_utils.Config;
import com.pvp_utils.client.util.ChatUtils;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 本地回放：录制与播放的核心管理器（单例）。
 *
 * 设计要点：
 * 1. 轻量录制：环形缓冲持续采样玩家状态帧（默认每 2 tick 一帧），不阻塞主线程；
 * 2. 触发时机：自动模式在首次战斗事件（命中 / 受击 / 击杀）时锁定战斗段，
 *    无战斗事件持续 idleTicks 后自动保存；手动模式通过命令 start/stop 控制；
 * 3. 保存格式：JSON，落在 <gameDir>/PVPUtils/replays/ 目录；
 * 4. 播放：加载 ReplayFile 后由 ReplayScreen 按时间轴慢放。
 */
public final class LocalReplayManager {
    private static final LocalReplayManager INSTANCE = new LocalReplayManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /** 自动录制时，战斗开始前额外保留的缓冲时长（tick）。 */
    private static final int RETAIN_BEFORE_TICKS = 80;
    /** 状态帧采样间隔（tick）。 */
    private static final int SAMPLE_INTERVAL_TICKS = 2;

    private final Deque<ReplayFrame> frames = new ArrayDeque<>();
    private final List<ReplayEvent> events = new ArrayList<>();

    private long currentTick;
    private boolean combatActive;
    private long combatStartTick = -1;
    private long lastCombatTick = -1;
    private boolean manualRecording;
    private boolean deathNotified;
    private ReplayFile activeReplay;

    private LocalReplayManager() {
    }

    public static LocalReplayManager getInstance() {
        return INSTANCE;
    }

    // ---------------------------------------------------------------- 主驱动

    public void tick(Minecraft client) {
        if (!Config.localReplay) {
            return;
        }
        if (client.player == null || client.level == null) {
            // 离开世界 / 死亡切场景：如有进行中的录制则落盘
            if (combatActive || manualRecording) {
                finishAndSave(true);
            }
            resetState();
            return;
        }

        if (manualRecording || combatActive) {
            sampleFrame(client);
            if (client.player.isDeadOrDying() && !deathNotified) {
                deathNotified = true;
                addEvent(ReplayEvent.Type.DEATH, Config.isChinese ? "你已死亡" : "You died");
                if (combatActive && Config.localReplayAutoRecord) {
                    finishAndSave(true);
                }
            }
        }

        if (combatActive && !manualRecording && Config.localReplayAutoRecord) {
            int idleTicks = Math.max(20, Config.localReplayIdleTicks);
            if (currentTick - lastCombatTick > idleTicks) {
                finishAndSave(true);
            }
        }

        currentTick++;
    }

    // ---------------------------------------------------------------- 战斗事件（由 Mixin 注入调用）

    public void onCombatHit(boolean isRanged, boolean isCrit, String targetName) {
        if (!Config.localReplay) {
            return;
        }
        markCombat();
        String detail = Config.isChinese
                ? "命中 " + targetName + (isCrit ? " (暴击)" : "") + (isRanged ? " (远程)" : "")
                : "Hit " + targetName + (isCrit ? " (crit)" : "") + (isRanged ? " (ranged)" : "");
        addEvent(ReplayEvent.Type.DAMAGE_DEALT, detail);
    }

    public void onCombatHurt(String sourceName) {
        if (!Config.localReplay) {
            return;
        }
        markCombat();
        String detail = Config.isChinese
                ? "受到攻击" + (sourceName == null || sourceName.isEmpty() ? "" : " (" + sourceName + ")")
                : "Damaged" + (sourceName == null || sourceName.isEmpty() ? "" : " by " + sourceName);
        addEvent(ReplayEvent.Type.DAMAGE_TAKEN, detail);
    }

    public void onKill(String targetName) {
        if (!Config.localReplay) {
            return;
        }
        markCombat();
        String detail = Config.isChinese
                ? "击杀 " + targetName
                : "Killed " + targetName;
        addEvent(ReplayEvent.Type.KILL, detail);
    }

    private void markCombat() {
        if (manualRecording) {
            return;
        }
        if (!combatActive) {
            combatActive = true;
            combatStartTick = currentTick;
        }
        lastCombatTick = currentTick;
    }

    private void addEvent(ReplayEvent.Type type, String message) {
        events.add(new ReplayEvent((int) currentTick, type, message));
    }

    // ---------------------------------------------------------------- 采样

    private void sampleFrame(Minecraft client) {
        if (currentTick % SAMPLE_INTERVAL_TICKS != 0) {
            return;
        }
        LocalPlayer player = client.player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getMainHandItem();
        String mainHand = stack.isEmpty() ? "" : stack.getHoverName().getString();
        ReplayFrame frame = new ReplayFrame(
                (int) currentTick,
                player.getX(), player.getY(), player.getZ(),
                player.getYRot(), player.getXRot(),
                player.getHealth(),
                player.getFoodData().getFoodLevel(),
                mainHand,
                player.isShiftKeyDown(),
                player.isSprinting(),
                player.isFallFlying(),
                player.isSwimming(),
                player.onGround()
        );
        frames.addLast(frame);
        int bufferTicks = Math.max(20, Config.localReplayBufferTicks);
        while (frames.size() > bufferTicks) {
            frames.removeFirst();
        }
    }

    // ---------------------------------------------------------------- 手动控制

    public void startManual() {
        if (!Config.localReplay) {
            ChatUtils.warning(Config.isChinese ? "请先在设置中开启本地回放。" : "Enable Local Replay in settings first.");
            return;
        }
        if (manualRecording) {
            ChatUtils.warning(Config.isChinese ? "已在录制中。" : "Already recording.");
            return;
        }
        frames.clear();
        events.clear();
        currentTick = 0;
        manualRecording = true;
        combatActive = false;
        combatStartTick = -1;
        lastCombatTick = -1;
        deathNotified = false;
        ChatUtils.success(Config.isChinese ? "本地回放：开始手动录制。" : "Local Replay: manual recording started.");
    }

    public void stopManual(String customName) {
        if (!manualRecording && !combatActive) {
            ChatUtils.warning(Config.isChinese ? "当前没有进行中的录制。" : "No active recording.");
            return;
        }
        finishAndSave(true);
        resetState();
        if (customName != null && !customName.isBlank()) {
            renameLastSaved(customName);
        }
    }

    // ---------------------------------------------------------------- 保存 / 加载

    private void finishAndSave(boolean resetCombat) {
        if (frames.isEmpty() && events.isEmpty()) {
            resetState();
            return;
        }
        ReplayFile file = new ReplayFile();
        file.recordedAt = System.currentTimeMillis();
        Minecraft client = Minecraft.getInstance();
        file.playerName = client.player == null ? "unknown"
                : client.player.getGameProfile().name();

        if (manualRecording) {
            file.frames = new ArrayList<>(frames);
            file.events = new ArrayList<>(events);
            file.durationTicks = (int) currentTick;
            if (file.durationTicks <= 0) {
                file.durationTicks = file.frames.isEmpty() ? 0 : file.frames.get(file.frames.size() - 1).tick;
            }
        } else {
            // 自动模式：裁剪出战斗段（含开战前 RETAIN_BEFORE_TICKS 缓冲）
            long startTick = Math.max(0, combatStartTick - RETAIN_BEFORE_TICKS);
            long endTick = currentTick;
            file.frames = frames.stream()
                    .filter(f -> f.tick >= startTick && f.tick <= endTick)
                    .collect(Collectors.toList());
            file.events = events.stream()
                    .filter(e -> e.tick >= startTick && e.tick <= endTick)
                    .collect(Collectors.toList());
            file.durationTicks = file.frames.isEmpty() ? 0 : file.frames.get(file.frames.size() - 1).tick;
        }

        if (file.frames.isEmpty() && file.events.isEmpty()) {
            resetState();
            return;
        }

        Path dir = replaysDir();
        try {
            Files.createDirectories(dir);
            Path target = dir.resolve(fileNameForNow());
            Files.writeString(target, GSON.toJson(file), StandardCharsets.UTF_8);
            if (manualRecording) {
                ChatUtils.success(Config.isChinese
                        ? "本地回放：已保存 " + target.getFileName()
                        : "Local Replay: saved " + target.getFileName());
            } else {
                ChatUtils.send(Config.isChinese
                        ? "本地回放：战斗录像已自动保存 " + target.getFileName()
                        : "Local Replay: combat replay auto-saved " + target.getFileName());
            }
            lastSavedPath = target;
        } catch (IOException e) {
            ChatUtils.error(Config.isChinese ? "本地回放：保存失败 " + e.getMessage() : "Local Replay: save failed " + e.getMessage());
        }

        if (resetCombat) {
            resetState();
        }
    }

    private static String fileNameForNow() {
        long now = System.currentTimeMillis();
        java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", Locale.ROOT);
        return "replay_" + fmt.format(now) + ".json";
    }

    public void renameLastSaved(String customName) {
        if (lastSavedPath == null || !Files.exists(lastSavedPath)) {
            return;
        }
        String safe = customName.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        if (safe.isEmpty()) {
            safe = "replay";
        }
        Path target = lastSavedPath.resolveSibling(safe + ".json");
        try {
            Files.move(lastSavedPath, target);
            lastSavedPath = target;
            ChatUtils.success(Config.isChinese ? "本地回放：已重命名为 " + target.getFileName() : "Local Replay: renamed to " + target.getFileName());
        } catch (IOException e) {
            ChatUtils.error(Config.isChinese ? "本地回放：重命名失败 " + e.getMessage() : "Local Replay: rename failed " + e.getMessage());
        }
    }

    public void play(String name) {
        Path dir = replaysDir();
        String fileName = name == null || name.isBlank() ? null
                : (name.toLowerCase(Locale.ROOT).endsWith(".json") ? name : name + ".json");
        if (fileName == null || !Files.exists(dir.resolve(fileName))) {
            ChatUtils.error(Config.isChinese ? "本地回放：未找到录像 " + (fileName == null ? "" : fileName)
                    : "Local Replay: replay not found " + (fileName == null ? "" : fileName));
            return;
        }
        try {
            ReplayFile file = GSON.fromJson(Files.readString(dir.resolve(fileName), StandardCharsets.UTF_8), ReplayFile.class);
            if (file == null || (file.frames == null && file.events == null)) {
                ChatUtils.error(Config.isChinese ? "本地回放：录像文件为空或已损坏。" : "Local Replay: replay file is empty or corrupted.");
                return;
            }
            if (file.frames == null) {
                file.frames = new ArrayList<>();
            }
            if (file.events == null) {
                file.events = new ArrayList<>();
            }
            file.frames.sort(Comparator.comparingInt(f -> f.tick));
            file.events.sort(Comparator.comparingInt(e -> e.tick));
            activeReplay = file;
            Minecraft client = Minecraft.getInstance();
            client.setScreen(new ReplayScreen(file, fileName));
        } catch (IOException | RuntimeException e) {
            ChatUtils.error(Config.isChinese ? "本地回放：加载失败 " + e.getMessage() : "Local Replay: load failed " + e.getMessage());
        }
    }

    public List<String> listReplays() {
        Path dir = replaysDir();
        if (!Files.isDirectory(dir)) {
            return List.of();
        }
        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(p -> p.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".json"))
                    .map(p -> p.getFileName().toString())
                    .sorted(Comparator.reverseOrder())
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return List.of();
        }
    }

    public void deleteReplay(String name) {
        Path dir = replaysDir();
        String fileName = name == null || name.isBlank() ? "" : name;
        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".json")) {
            fileName = fileName + ".json";
        }
        Path target = dir.resolve(fileName);
        if (!Files.exists(target)) {
            ChatUtils.error(Config.isChinese ? "本地回放：未找到录像 " + fileName : "Local Replay: replay not found " + fileName);
            return;
        }
        try {
            Files.delete(target);
            ChatUtils.success(Config.isChinese ? "本地回放：已删除 " + fileName : "Local Replay: deleted " + fileName);
        } catch (IOException e) {
            ChatUtils.error(Config.isChinese ? "本地回放：删除失败 " + e.getMessage() : "Local Replay: delete failed " + e.getMessage());
        }
    }

    public void openFolder() {
        Path dir = replaysDir();
        try {
            Files.createDirectories(dir);
            java.awt.Desktop.getDesktop().open(dir.toFile());
        } catch (IOException e) {
            ChatUtils.error(Config.isChinese ? "本地回放：无法打开目录 " + e.getMessage() : "Local Replay: cannot open folder " + e.getMessage());
        }
    }

    public static Path replaysDir() {
        return FabricLoader.getInstance().getGameDir().resolve("PVPUtils").resolve("replays");
    }

    // ---------------------------------------------------------------- 状态查询

    public boolean isRecording() {
        return manualRecording || combatActive;
    }

    public boolean isManualRecording() {
        return manualRecording;
    }

    public ReplayFile getActiveReplay() {
        return activeReplay;
    }

    public void setActiveReplay(ReplayFile replay) {
        this.activeReplay = replay;
    }

    private void resetState() {
        combatActive = false;
        manualRecording = false;
        combatStartTick = -1;
        lastCombatTick = -1;
        deathNotified = false;
        events.clear();
        frames.clear();
        currentTick = 0;
    }

    private Path lastSavedPath;
}
