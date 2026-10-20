package com.pvp_utils.client.modules.impl.Combat;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 击杀卡牌渲染器。
 * <p>
 * 在玩家击杀实体时，于屏幕中央展示一张卡牌，包含击杀者名称、武器名称与伤害数值，
 * 带渐入渐出动画，持续数秒后自动消失。风格与现有 HUD（命中标记等）保持一致。
 * </p>
 */
public class KillCardRenderer {
    private static final KillCardRenderer INSTANCE = new KillCardRenderer();

    /** 卡牌总展示时长（毫秒） */
    private static final long CARD_DURATION_MS = 3500L;
    /** 渐入时长（毫秒） */
    private static final long FADE_IN_MS = 220L;
    /** 渐出时长（毫秒） */
    private static final long FADE_OUT_MS = 520L;
    /** 等待实体血量同步的超时时间（毫秒），超时后使用估算伤害兜底展示 */
    private static final long PENDING_TIMEOUT_MS = 800L;

    /** 卡片外框颜色（击杀主题金橙） */
    private static final int BORDER_COLOR = 0xFFE2B24B;
    /** 卡片背景顶部颜色 */
    private static final int BG_TOP_COLOR = 0xE0182026;
    /** 卡片背景底部颜色 */
    private static final int BG_BOTTOM_COLOR = 0xD0101418;
    /** 击杀者名称颜色（金色） */
    private static final int NAME_COLOR = 0xFFFFE08A;
    /** 武器名称颜色（白色） */
    private static final int WEAPON_COLOR = 0xFFF2F4F8;
    /** 伤害数值颜色（红色，与伤害数字 HUD 一致） */
    private static final int DAMAGE_COLOR = 0xFFFF5555;
    /** 分隔符颜色 */
    private static final int SEPARATOR_COLOR = 0xFF9AA4B2;

    /** 待确认伤害的击杀请求：实体 ID -> 请求信息 */
    private final Map<Integer, PendingKill> pendingKills = new HashMap<>();
    /** 当前正在展示的卡牌 */
    private KillCard current = null;

    private KillCardRenderer() {
    }

    public static KillCardRenderer getInstance() {
        return INSTANCE;
    }

    /**
     * 击杀事件触发时调用（此时实体血量尚未同步，先暂存击杀请求）。
     * 若配置关闭或玩家状态缺失则直接忽略。
     *
     * @param target 被击杀的实体
     */
    public void onKillRequested(LivingEntity target) {
        if (!Config.killCard || target == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }
        String killerName = client.player.getDisplayName().getString();
        String weaponName = client.player.getMainHandItem().isEmpty()
                ? "徒手"
                : client.player.getMainHandItem().getHoverName().getString();
        pendingKills.put(target.getId(), new PendingKill(killerName, weaponName,
                Math.max(0.0f, target.getHealth()), System.currentTimeMillis()));
    }

    /**
     * 实体血量同步后调用，携带精确的致死伤害数值，弹出暂存的击杀请求并展示卡牌。
     *
     * @param entityId 实体 ID
     * @param damage   致死伤害数值
     */
    public void resolveKill(int entityId, float damage) {
        PendingKill pending = pendingKills.remove(entityId);
        if (pending != null) {
            showCard(pending.killerName, pending.weaponName, damage);
        }
    }

    /**
     * 每 tick 调用：处理超时未确认的击杀请求（兜底展示），并清理已过期的卡牌。
     *
     * @param client Minecraft 实例
     */
    public void tick(Minecraft client) {
        long now = System.currentTimeMillis();
        Iterator<Map.Entry<Integer, PendingKill>> iterator = pendingKills.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Integer, PendingKill> entry = iterator.next();
            PendingKill pending = entry.getValue();
            if (now - pending.requestedAtMs > PENDING_TIMEOUT_MS) {
                iterator.remove();
                showCard(pending.killerName, pending.weaponName, pending.fallbackDamage);
            }
        }
        if (current != null && now - current.startTimeMs > CARD_DURATION_MS) {
            current = null;
        }
    }

    private void showCard(String killerName, String weaponName, float damage) {
        Minecraft client = Minecraft.getInstance();
        if (!Config.killCard || client.player == null) {
            return;
        }
        current = new KillCard(killerName, weaponName, formatNumber(damage), System.currentTimeMillis());
    }

    /**
     * HUD 渲染入口：在屏幕中央绘制击杀卡牌。
     *
     * @param graphics GuiGraphics
     */
    public void render(GuiGraphics graphics) {
        if (!Config.killCard || current == null) {
            return;
        }
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        long now = System.currentTimeMillis();
        long elapsed = now - current.startTimeMs;
        if (elapsed < 0 || elapsed > CARD_DURATION_MS) {
            return;
        }

        float alpha = alphaAt(elapsed);
        if (alpha <= 0.001f) {
            return;
        }

        int guiW = client.getWindow().getGuiScaledWidth();
        int guiH = client.getWindow().getGuiScaledHeight();
        int centerX = guiW / 2;
        int centerY = guiH / 2 - 32;

        // 轻微上浮动画：出现的前 0.6 秒内从下方 8 像素浮起
        float lift = 0.0f;
        if (elapsed < 600L) {
            float t = elapsed / 600.0f;
            lift = (1.0f - easeOutCubic(t)) * 8.0f;
        }

        // 文本行
        String nameLine = current.killerName;
        String infoLine = current.weaponName + "  -  " + current.damageText;

        int titleWidth = client.font.width(nameLine);
        int infoWidth = client.font.width(infoLine);
        int cardWidth = Math.max(titleWidth, infoWidth) + 28;
        int cardHeight = 54;

        float x = centerX - cardWidth / 2.0f;
        float y = centerY - cardHeight / 2.0f + lift;

        int borderAlpha = Math.round(alpha * 255);
        int border = withAlpha(BORDER_COLOR, borderAlpha);
        int bgTop = withAlpha(BG_TOP_COLOR, borderAlpha);
        int bgBottom = withAlpha(BG_BOTTOM_COLOR, borderAlpha);

        // 外框 + 背景
        graphics.fill(Math.round(x) - 2, Math.round(y) - 2, Math.round(x) + cardWidth + 2,
                Math.round(y) + cardHeight + 2, border);
        graphics.fillGradient(Math.round(x), Math.round(y), Math.round(x) + cardWidth,
                Math.round(y) + cardHeight, bgTop, bgBottom);

        // 文本
        graphics.drawString(client.font, Component.literal(nameLine),
                Math.round(x + (cardWidth - titleWidth) / 2.0f), Math.round(y + 10),
                withAlpha(NAME_COLOR, borderAlpha), true);

        // 分隔线
        graphics.fill(Math.round(x) + 10, Math.round(y + 26), Math.round(x) + cardWidth - 10,
                Math.round(y + 27), withAlpha(SEPARATOR_COLOR, Math.round(alpha * 140)));

        graphics.drawString(client.font, Component.literal(infoLine),
                Math.round(x + (cardWidth - infoWidth) / 2.0f), Math.round(y + 34),
                withAlpha(WEAPON_COLOR, borderAlpha), true);
    }

    /** 渐入渐出透明度：先快速淡入，保持，再缓慢淡出 */
    private float alphaAt(long elapsed) {
        if (elapsed < FADE_IN_MS) {
            return smoothstep(0.0f, (float) FADE_IN_MS, (float) elapsed);
        }
        long remain = CARD_DURATION_MS - elapsed;
        if (remain < FADE_OUT_MS) {
            return smoothstep(0.0f, (float) FADE_OUT_MS, (float) remain);
        }
        return 1.0f;
    }

    private float easeOutCubic(float value) {
        float t = 1.0f - clamp(value, 0.0f, 1.0f);
        return 1.0f - t * t * t;
    }

    private float smoothstep(float edge0, float edge1, float value) {
        float t = clamp((value - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    private float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }

    private int withAlpha(int color, int alpha) {
        return (Math.max(0, Math.min(255, alpha)) << 24) | (color & 0x00FFFFFF);
    }

    private String formatNumber(float value) {
        if (value >= 10.0f || Math.abs(value - Math.round(value)) < 0.05f) {
            return String.valueOf(Math.round(value));
        }
        return String.format(java.util.Locale.ROOT, "%.1f", value);
    }

    /** 等待血量同步的击杀请求 */
    private static final class PendingKill {
        private final String killerName;
        private final String weaponName;
        private final float fallbackDamage;
        private final long requestedAtMs;

        private PendingKill(String killerName, String weaponName, float fallbackDamage, long requestedAtMs) {
            this.killerName = killerName;
            this.weaponName = weaponName;
            this.fallbackDamage = fallbackDamage;
            this.requestedAtMs = requestedAtMs;
        }
    }

    /** 正在展示的卡牌数据 */
    private static final class KillCard {
        private final String killerName;
        private final String weaponName;
        private final String damageText;
        private final long startTimeMs;

        private KillCard(String killerName, String weaponName, String damageText, long startTimeMs) {
            this.killerName = killerName;
            this.weaponName = weaponName;
            this.damageText = damageText;
            this.startTimeMs = startTimeMs;
        }
    }
}
