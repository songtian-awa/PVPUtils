package com.pvp_utils.client.modules.impl.Combat;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

/**
 * 单场战绩结算 + 自定义死亡画面
 *
 * 对局（session）内自动累计击杀 / 死亡 / 命中 / 攻击尝试，
 * 死亡时以统计卡形式展示：存活时间 / 击杀者（条目119）+ K/D / 命中率（条目112）。
 * 复用项目既有单例渲染器范式：GuiMixin.onRender 调用 render，MinecraftMixin.tick 调用 tick。
 */
public final class MatchSummaryRenderer {
    private static final MatchSummaryRenderer INSTANCE = new MatchSummaryRenderer();

    /** 死亡统计卡总展示时长（毫秒） */
    private static final long DEATH_PANEL_MS = 8000L;
    /** 渐出时长（毫秒） */
    private static final long FADE_OUT_MS = 600L;

    private static final int BG_COLOR = 0xCC101418;
    private static final int BORDER_COLOR = 0xFF4B5563;
    private static final int TITLE_COLOR = 0xFFE2B24B;
    private static final int TEXT_COLOR = 0xFFF2F4F8;
    private static final int ACCENT_COLOR = 0xFF55FF77;

    private int kills;
    private int deaths;
    private int hits;
    private int attacks;
    private long totalAliveMs;
    private long roundStartMs = -1L;
    private String lastKillerName = null;
    private boolean wasDead = false;
    private long deathPanelShownAtMs = 0L;
    private long deathPanelUntilMs = 0L;
    private boolean sessionActive = false;

    private MatchSummaryRenderer() {
    }

    public static MatchSummaryRenderer getInstance() {
        return INSTANCE;
    }

    /** 每次攻击尝试（近战挥击，含挥空） */
    public void onAttack() {
        if (!sessionActive || !Config.matchSummary) {
            return;
        }
        attacks++;
    }

    /** 自己造成伤害事件 */
    public void onHit() {
        if (!sessionActive) {
            return;
        }
        hits++;
    }

    /** 自己击杀实体 */
    public void onKill() {
        if (!sessionActive) {
            return;
        }
        kills++;
    }

    /** 玩家死亡：累计存活时间、记录击杀者、激活统计卡 */
    public void onPlayerDeath(DamageSource source) {
        if (!sessionActive) {
            return;
        }
        deaths++;
        if (roundStartMs > 0) {
            totalAliveMs += System.currentTimeMillis() - roundStartMs;
        }
        roundStartMs = -1L;
        Entity killer = source == null ? null : source.getEntity();
        lastKillerName = killer == null ? null : killer.getDisplayName().getString();
        long now = System.currentTimeMillis();
        deathPanelShownAtMs = now;
        deathPanelUntilMs = now + DEATH_PANEL_MS;
        wasDead = true;
    }

    /** 重生 / 复活：重置单轮存活计时 */
    public void onRoundStart() {
        roundStartMs = System.currentTimeMillis();
    }

    /** 进入世界：开启新对局 */
    public void onSessionStart() {
        kills = 0;
        deaths = 0;
        hits = 0;
        attacks = 0;
        totalAliveMs = 0L;
        lastKillerName = null;
        wasDead = false;
        deathPanelUntilMs = 0L;
        roundStartMs = System.currentTimeMillis();
        sessionActive = true;
    }

    /** 离开世界：结束对局 */
    public void onSessionEnd() {
        sessionActive = false;
        roundStartMs = -1L;
        deathPanelUntilMs = 0L;
    }

    public void tick(Minecraft client) {
        boolean inWorld = client.level != null && client.player != null;
        if (inWorld != sessionActive) {
            if (inWorld) {
                onSessionStart();
            } else {
                onSessionEnd();
            }
        }
        if (!inWorld) {
            return;
        }
        Player player = client.player;
        boolean alive = !player.isDeadOrDying();
        if (wasDead && alive) {
            wasDead = false;
            onRoundStart();
        }
    }

    public void render(GuiGraphics gui) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        boolean summary = Config.matchSummary;
        boolean deathCard = Config.customDeathScreen;
        if (!summary && !deathCard) {
            return;
        }
        long now = System.currentTimeMillis();
        if (deathPanelUntilMs <= 0 || now > deathPanelUntilMs) {
            return;
        }
        float alpha = 1.0f;
        long remain = deathPanelUntilMs - now;
        if (remain < FADE_OUT_MS) {
            alpha = Math.max(0.0f, remain / (float) FADE_OUT_MS);
        }
        drawPanel(gui, alpha, summary, deathCard);
    }

    private void drawPanel(GuiGraphics gui, float alpha, boolean summary, boolean deathCard) {
        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenW = mc.getWindow().getGuiScaledWidth();
        int screenH = mc.getWindow().getGuiScaledHeight();

        boolean showSurvive = deathCard;
        boolean showKiller = deathCard;
        boolean showKillDeaths = summary;
        boolean showKd = summary;
        boolean showAccuracy = summary;

        int rowCount = 0;
        if (showSurvive) rowCount++;
        if (showKiller) rowCount++;
        if (showKillDeaths) rowCount++;
        if (showKd) rowCount++;
        if (showAccuracy) rowCount++;

        int panelW = 220;
        int panelH = 20 + 10 + rowCount * 13 + 14;
        int left = screenW / 2 - panelW / 2;
        int top = (int) (screenH * 0.32f) - panelH / 2;
        int right = left + panelW;
        int bottom = top + panelH;

        int bg = withAlpha(BG_COLOR, alpha);
        int border = withAlpha(BORDER_COLOR, alpha);

        gui.fill(left, top, right, bottom, bg);
        gui.fill(left, top, right, top + 1, border);
        gui.fill(left, bottom - 1, right, bottom, border);
        gui.fill(left, top, left + 1, bottom, border);
        gui.fill(right - 1, top, right, bottom, border);

        int cx = screenW / 2;
        int y = top + 10;

        drawCentered(gui, font, "对局结算 / Match Summary", cx, y, withAlpha(TITLE_COLOR, alpha));
        y += 14;

        if (showSurvive) {
            drawCentered(gui, font, "存活时间: " + formatTime(currentAliveMs()), cx, y, withAlpha(TEXT_COLOR, alpha));
            y += 13;
        }
        if (showKiller) {
            String killer = lastKillerName == null ? "无" : lastKillerName;
            drawCentered(gui, font, "击杀者: " + killer, cx, y, withAlpha(ACCENT_COLOR, alpha));
            y += 13;
        }
        if (showKillDeaths) {
            drawCentered(gui, font, "击杀 " + kills + " / 死亡 " + deaths, cx, y, withAlpha(TEXT_COLOR, alpha));
            y += 13;
        }
        if (showKd) {
            drawCentered(gui, font, "K/D: " + formatKd(), cx, y, withAlpha(TEXT_COLOR, alpha));
            y += 13;
        }
        if (showAccuracy) {
            drawCentered(gui, font, "命中率: " + formatAccuracy(), cx, y, withAlpha(TEXT_COLOR, alpha));
        }
    }

    private long currentAliveMs() {
        long now = System.currentTimeMillis();
        long alive = totalAliveMs;
        if (roundStartMs > 0) {
            alive += now - roundStartMs;
        }
        return alive;
    }

    private String formatKd() {
        if (deaths <= 0) {
            return kills + " / 0 (" + kills + ")";
        }
        return kills + " / " + deaths + " (" + String.format("%.2f", kills / (double) deaths) + ")";
    }

    private String formatAccuracy() {
        if (attacks <= 0) {
            return "--";
        }
        int percent = (int) Math.min(100, Math.round(hits * 100.0 / attacks));
        return percent + "% (" + hits + "/" + attacks + ")";
    }

    private static String formatTime(long ms) {
        long totalSec = ms / 1000;
        long min = totalSec / 60;
        long sec = totalSec % 60;
        return min + ":" + (sec < 10 ? "0" + sec : "" + sec);
    }

    private static void drawCentered(GuiGraphics gui, Font font, String text, int cx, int y, int color) {
        gui.drawString(font, text, cx - font.width(text) / 2, y, color, true);
    }

    private static int withAlpha(int argb, float alpha) {
        int a = (int) (((argb >>> 24) & 0xFF) * alpha);
        return (a << 24) | (argb & 0xFFFFFF);
    }
}
