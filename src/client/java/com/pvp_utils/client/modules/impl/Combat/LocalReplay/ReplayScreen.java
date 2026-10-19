package com.pvp_utils.client.modules.impl.Combat.LocalReplay;

import com.pvp_utils.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 本地回放：播放界面。
 *
 * 提供战斗回放的慢放与复盘：
 * - 俯视轨迹图（玩家 XY 移动轨迹 + 朝向箭头 + 击杀 / 死亡标记）
 * - 血量曲线（随时间变化）
 * - 事件时间线（命中 / 受击 / 击杀 / 死亡，可点击跳转）
 * - 进度条拖动 seek、播放 / 暂停、0.25x ~ 2x 变速慢放
 */
public final class ReplayScreen extends Screen {
    private static final int COLOR_BG = 0xCC101418;
    private static final int COLOR_TEXT = 0xFFF2F4F8;
    private static final int COLOR_MUTED = 0xFF9AA4B2;
    private static final int COLOR_ACCENT = 0xFF4AA3FF;
    private static final int COLOR_PLAY = 0xFF3EE06B;
    private static final int COLOR_STOP = 0xFFF87171;
    private static final int COLOR_DEALT = 0xFF4ADE80;
    private static final int COLOR_TAKEN = 0xFFF87171;
    private static final int COLOR_KILL = 0xFFFFB86B;
    private static final int COLOR_DEATH = 0xFFFF4D4D;

    private static final float[] SPEEDS = {0.25f, 0.5f, 1.0f, 2.0f};

    private final ReplayFile replay;
    private final String replayName;
    private final int durationTicks;

    private float playTick;
    private float speed = 1.0f;
    private boolean playing = true;
    private long lastFrameMs = System.currentTimeMillis();
    private int speedIndex = 2;

    public ReplayScreen(ReplayFile replay, String name) {
        super(Component.literal("Local Replay"));
        this.replay = replay;
        this.replayName = name;
        this.durationTicks = Math.max(1, replay.durationTicks);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fillGradient(0, 0, this.width, this.height, COLOR_BG, COLOR_BG);

        if (playing) {
            long now = System.currentTimeMillis();
            float delta = (now - lastFrameMs) / 1000.0f;
            lastFrameMs = now;
            playTick += delta * 20.0f * speed;
            if (playTick >= durationTicks) {
                playTick = 0;
            }
            if (playTick < 0) {
                playTick = 0;
            }
        } else {
            lastFrameMs = System.currentTimeMillis();
        }

        drawHeader(graphics);
        drawLeftPanel(graphics);
        drawEventList(graphics);
        drawControls(graphics);
    }

    private void drawHeader(GuiGraphics graphics) {
        graphics.drawString(this.font, Config.isChinese ? "本地回放 - " + replayName : "Local Replay - " + replayName,
                20, 16, COLOR_TEXT, false);
        String meta = Config.isChinese
                ? "玩家 " + replay.playerName + " | 时长 " + formatTime(durationTicks)
                : "Player " + replay.playerName + " | Duration " + formatTime(durationTicks);
        graphics.drawString(this.font, meta, 20, 30, COLOR_MUTED, false);
        String info = String.format("T %d / %d  (%.1fs / %.1fs)", (int) playTick, durationTicks,
                playTick / 20.0f, durationTicks / 20.0f);
        graphics.drawString(this.font, info, 20, 44, COLOR_ACCENT, false);
    }

    // ---------------------------------------------------------------- 左面板：轨迹图 + 血量曲线

    private void drawLeftPanel(GuiGraphics graphics) {
        int x0 = 20;
        int y0 = 60;
        int x1 = this.width - 240;
        int y1 = this.height - 100;

        graphics.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xFF222A36);
        int splitY = y0 + (y1 - y0) * 62 / 100;
        graphics.fill(x0, y0, x1, splitY, 0x10182026);
        graphics.fill(x0, splitY, x1, y1, 0x10182620);

        List<ReplayFrame> frames = replay.frames;
        if (frames != null && !frames.isEmpty()) {
            drawTrajectory(graphics, frames, x0, y0, x1, splitY);
            drawHealthCurve(graphics, frames, x0, splitY, x1, y1);
        } else {
            graphics.drawString(this.font, Config.isChinese ? "（无状态帧数据）" : "(no frame data)",
                    x0 + 10, y0 + 10, COLOR_MUTED, false);
        }
    }

    private void drawTrajectory(GuiGraphics graphics, List<ReplayFrame> frames, int x0, int y0, int x1, int y1) {
        double minX = Double.MAX_VALUE, maxX = -Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE, maxZ = -Double.MAX_VALUE;
        for (ReplayFrame f : frames) {
            minX = Math.min(minX, f.x);
            maxX = Math.max(maxX, f.x);
            minZ = Math.min(minZ, f.z);
            maxZ = Math.max(maxZ, f.z);
        }
        double rangeX = Math.max(maxX - minX, 0.01);
        double rangeZ = Math.max(maxZ - minZ, 0.01);

        graphics.drawString(this.font, Config.isChinese ? "俯视轨迹" : "Top-down track",
                x0 + 6, y0 + 4, COLOR_MUTED, false);

        // 轨迹折线（按时间顺序）
        int w = Math.max(2, (x1 - x0) / 300);
        int h = Math.max(2, (y1 - y0) / 300);
        for (int i = 1; i < frames.size(); i++) {
            ReplayFrame a = frames.get(i - 1);
            ReplayFrame b = frames.get(i);
            if (b.tick > playTick + 1) {
                break;
            }
            int ax = mapX(a.x, minX, rangeX, x0, x1);
            int az = mapZ(a.z, minZ, rangeZ, y0, y1);
            int bx = mapX(b.x, minX, rangeX, x0, x1);
            int bz = mapZ(b.z, minZ, rangeZ, y0, y1);
            int color = (i & 1) == 0 ? 0xAA4AA3FF : 0xAA3E8FDD;
            graphics.fill(Math.min(ax, bx), Math.min(az, bz),
                    Math.max(ax, bx) + w, Math.max(az, bz) + h, color);
        }

        // 事件位置标记
        if (replay.events != null) {
            for (ReplayEvent e : replay.events) {
                if (e.tick > playTick + 1) {
                    break;
                }
                ReplayFrame near = frameAtOrBefore(frames, e.tick);
                if (near == null) {
                    continue;
                }
                int px = mapX(near.x, minX, rangeX, x0, x1);
                int pz = mapZ(near.z, minZ, rangeZ, y0, y1);
                int color = eventColor(e);
                graphics.fill(px - 3, pz - 3, px + 3, pz + 3, color);
            }
        }

        // 当前帧位置 + 朝向箭头
        ReplayFrame cur = frameAtOrBefore(frames, (int) playTick);
        if (cur != null) {
            int px = mapX(cur.x, minX, rangeX, x0, x1);
            int pz = mapZ(cur.z, minZ, rangeZ, y0, y1);
            graphics.fill(px - 2, pz - 2, px + 2, pz + 2, COLOR_TEXT);
            float rad = (float) Math.toRadians(cur.yaw);
            int len = 9;
            int tx = px + (int) (-Math.sin(rad) * len);
            int tz = pz + (int) (-Math.cos(rad) * len);
            graphics.fill(Math.min(px, tx) - 1, Math.min(pz, tz) - 1,
                    Math.max(px, tx) + 1, Math.max(pz, tz) + 1, COLOR_PLAY);
        }

        graphics.fill(x0, y0, x1, y0 + 1, 0xFF333D4D);
        graphics.fill(x0, y1 - 1, x1, y1, 0xFF333D4D);
        graphics.fill(x0, y0, x0 + 1, y1, 0xFF333D4D);
        graphics.fill(x1 - 1, y0, x1, y1, 0xFF333D4D);
    }

    private void drawHealthCurve(GuiGraphics graphics, List<ReplayFrame> frames, int x0, int y0, int x1, int y1) {
        graphics.drawString(this.font, Config.isChinese ? "血量曲线" : "Health", x0 + 6, y0 + 4, COLOR_MUTED, false);

        float maxHealth = 20;
        for (ReplayFrame f : frames) {
            maxHealth = Math.max(maxHealth, f.health);
        }
        int plotTop = y0 + 16;
        int plotBottom = y1 - 6;

        for (int i = 1; i < frames.size(); i++) {
            ReplayFrame a = frames.get(i - 1);
            ReplayFrame b = frames.get(i);
            if (b.tick > playTick + 1) {
                break;
            }
            int ax = x0 + (int) ((a.tick / (float) durationTicks) * (x1 - x0));
            int bx = x0 + (int) ((b.tick / (float) durationTicks) * (x1 - x0));
            int ay = plotBottom - (int) ((a.health / maxHealth) * (plotBottom - plotTop));
            int by = plotBottom - (int) ((b.health / maxHealth) * (plotBottom - plotTop));
            int color = b.health < 6 ? 0xAAF87171 : 0xAA3EE06B;
            graphics.fill(Math.min(ax, bx), Math.min(ay, by),
                    Math.max(ax, bx) + 2, Math.max(ay, by) + 2, color);
        }

        // 当前时间竖线
        int cx = x0 + (int) ((playTick / durationTicks) * (x1 - x0));
        graphics.fill(cx, plotTop, cx + 1, plotBottom, 0xCCFFFFFF);
    }

    // ---------------------------------------------------------------- 右侧：事件列表

    private void drawEventList(GuiGraphics graphics) {
        int x0 = this.width - 230;
        int x1 = this.width - 20;
        int y0 = 60;
        int y1 = this.height - 100;

        graphics.fill(x0 - 2, y0 - 2, x1 + 2, y1 + 2, 0xFF222A36);
        graphics.fill(x0, y0, x1, y1, 0x10182026);
        graphics.drawString(this.font, Config.isChinese ? "事件时间线" : "Events", x0 + 6, y0 + 4, COLOR_MUTED, false);

        if (replay.events == null || replay.events.isEmpty()) {
            graphics.drawString(this.font, Config.isChinese ? "（无事件）" : "(no events)", x0 + 6, y0 + 20, COLOR_MUTED, false);
            return;
        }

        int lineY = y0 + 18;
        int lineH = 12;
        for (ReplayEvent e : replay.events) {
            if (lineY + lineH > y1) {
                break;
            }
            int color = eventColor(e);
            String label = String.format("t%+.1fs %s", e.tick / 20.0f, e.message);
            graphics.drawString(this.font, label, x0 + 6, lineY, color, false);
            lineY += lineH;
        }
        if (lineY + lineH > y1) {
            graphics.drawString(this.font, Config.isChinese ? "…（更多）" : "... (more)", x0 + 6, lineY, COLOR_MUTED, false);
        }
    }

    // ---------------------------------------------------------------- 底部控制

    private void drawControls(GuiGraphics graphics) {
        int barX0 = 20;
        int barX1 = this.width - 20;
        int barY = this.height - 78;

        // 进度条
        graphics.fill(barX0 - 1, barY - 1, barX1 + 1, barY + 13, 0xFF333D4D);
        graphics.fill(barX0, barY, barX1, barY + 11, 0x55333333);
        int fillEnd = barX0 + (int) ((playTick / (float) durationTicks) * (barX1 - barX0));
        graphics.fill(barX0, barY, fillEnd, barY + 11, 0xAA4AA3FF);
        graphics.fill(fillEnd - 2, barY - 2, fillEnd + 2, barY + 13, 0xFFFFFFFF);

        // 按钮行
        int btnY = this.height - 52;
        int btnH = 18;
        // 播放/暂停
        String playLabel = playing ? (Config.isChinese ? "暂停" : "Pause") : (Config.isChinese ? "播放" : "Play");
        int px = drawButton(graphics, playLabel, 20, btnY, 54, btnH, playing ? COLOR_PLAY : COLOR_ACCENT);
        // 速度
        String speedLabel = speed + "x";
        int sx = drawButton(graphics, speedLabel, px + 6, btnY, 46, btnH, COLOR_TEXT);
        // 重新播放
        int rx = drawButton(graphics, Config.isChinese ? "重播" : "Restart", sx + 6, btnY, 46, btnH, COLOR_MUTED);
        // 关闭
        int cx = drawButton(graphics, Config.isChinese ? "关闭" : "Close", barX1 - 40, btnY, 40, btnH, COLOR_STOP);

        String hint = Config.isChinese
                ? "空格 播放/暂停 | ←→ 快退/快进 | 点击事件跳转"
                : "SPACE toggle | ARROW seek | click event to jump";
        graphics.drawString(this.font, hint, 20, this.height - 26, COLOR_MUTED, false);
    }

    private int drawButton(GuiGraphics graphics, String label, int x, int y, int w, int h, int accent) {
        graphics.fill(x - 1, y - 1, x + w + 1, y + h + 1, 0xFF333D4D);
        graphics.fill(x, y, x + w, y + h, 0x66111827);
        int textColor = accent == COLOR_TEXT ? COLOR_TEXT : accent;
        int tw = this.font.width(label);
        graphics.drawString(this.font, label, x + (w - tw) / 2, y + (h - 8) / 2, textColor, false);
        return x + w;
    }

    // ---------------------------------------------------------------- 交互

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() != 0) {
            return super.mouseClicked(event, consumed);
        }
        double mouseX = event.x();
        double mouseY = event.y();
            // 进度条
            int barY = this.height - 78;
            if (mouseY >= barY - 2 && mouseY <= barY + 14 && mouseX >= 20 && mouseX <= this.width - 20) {
                seekTo(mouseX);
                return true;
            }
            int btnY = this.height - 52;
            if (mouseY >= btnY - 2 && mouseY <= btnY + 20) {
                if (mouseX >= 20 && mouseX <= 76) {
                    playing = !playing;
                    return true;
                }
                if (mouseX >= 82 && mouseX <= 128) {
                    cycleSpeed();
                    return true;
                }
                if (mouseX >= 134 && mouseX <= 180) {
                    playTick = 0;
                    playing = true;
                    return true;
                }
                if (mouseX >= this.width - 62 && mouseX <= this.width - 18) {
                    onClose();
                    return true;
                }
            }
            // 事件行点击跳转
            int listX0 = this.width - 230;
            int listY0 = 60;
            if (mouseX >= listX0 && mouseX <= this.width - 20 && mouseY >= listY0 && mouseY <= this.height - 100) {
                int index = (int) ((mouseY - (listY0 + 18)) / 12);
                if (index >= 0 && index < replay.events.size()) {
                    ReplayEvent e = replay.events.get(index);
                    playTick = Math.max(0, e.tick - 20);
                    playing = true;
                    return true;
                }
            }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
        if (event.button() != 0) {
            return super.mouseDragged(event, dragX, dragY);
        }
        double mouseX = event.x();
        double mouseY = event.y();
            int barY = this.height - 78;
            if (mouseY >= barY - 2 && mouseY <= barY + 14 && mouseX >= 20 && mouseX <= this.width - 20) {
                seekTo(mouseX);
                return true;
            }
        return super.mouseDragged(event, dragX, dragY);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_SPACE) {
            playing = !playing;
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_LEFT) {
            playTick = Math.max(0, playTick - 20);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_RIGHT) {
            playTick = Math.min(durationTicks, playTick + 20);
            return true;
        }
        if (event.key() == GLFW.GLFW_KEY_UP || event.key() == GLFW.GLFW_KEY_R) {
            cycleSpeed();
            return true;
        }
        return super.keyPressed(event);
    }

    private void seekTo(double mouseX) {
        int barX0 = 20;
        int barX1 = this.width - 20;
        float progress = (float) ((mouseX - barX0) / (barX1 - barX0));
        progress = Math.max(0, Math.min(1, progress));
        playTick = progress * durationTicks;
    }

    private void cycleSpeed() {
        speedIndex = (speedIndex + 1) % SPEEDS.length;
        speed = SPEEDS[speedIndex];
    }

    // ---------------------------------------------------------------- 工具

    private static String formatTime(int ticks) {
        int totalSeconds = Math.max(0, ticks / 20);
        return String.format("%d:%02d", totalSeconds / 60, totalSeconds % 60);
    }

    private static int mapX(double value, double min, double range, int x0, int x1) {
        return x0 + (int) ((value - min) / range * (x1 - x0));
    }

    private static int mapZ(double value, double min, double range, int y0, int y1) {
        return y0 + (int) ((value - min) / range * (y1 - y0));
    }

    private static ReplayFrame frameAtOrBefore(List<ReplayFrame> frames, int tick) {
        ReplayFrame result = frames.get(0);
        for (ReplayFrame f : frames) {
            if (f.tick > tick) {
                break;
            }
            result = f;
        }
        return result;
    }

    private static int eventColor(ReplayEvent e) {
        ReplayEvent.Type type = e.type();
        if (type == ReplayEvent.Type.DAMAGE_DEALT) {
            return COLOR_DEALT;
        }
        if (type == ReplayEvent.Type.DAMAGE_TAKEN) {
            return COLOR_TAKEN;
        }
        if (type == ReplayEvent.Type.KILL) {
            return COLOR_KILL;
        }
        return COLOR_DEATH;
    }
}
