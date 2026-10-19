package com.pvp_utils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Config 的结构性元数据（从 Config 上帝类中拆分）。
 *
 * 仅承载与字段值无关的静态数据与映射工具方法：
 * - 模块规则表 MODULE_RULES（决定每个字段序列化到哪个 JSON 模块）
 * - HUD 组件表 HUD_COMPONENTS（决定 HUD 布局字段归入 Hud.cfg 哪个组件）
 * - 字段名 -> ConfigPath 的推导逻辑
 *
 * 拆分目的：降低 Config.java 单文件复杂度，便于后续按模块继续细拆。
 */
public final class ConfigMeta {

    private ConfigMeta() {
    }

    /** 依据字段名推导其在 JSON 中的模块与键名（kebab-case）。 */
    public static ConfigPath pathForField(String fieldName) {
        ModuleRule rule = moduleRuleFor(fieldName);
        if (rule == null) {
            return new ConfigPath("Global", toKebabCase(fieldName));
        }
        if (fieldName.equals(rule.toggleField())) {
            return new ConfigPath(rule.module(), "toggled");
        }
        String keySource = fieldName;
        if (!rule.prefix().isEmpty() && fieldName.startsWith(rule.prefix())) {
            keySource = fieldName.substring(rule.prefix().length());
            if (keySource.isEmpty()) {
                keySource = fieldName;
            } else {
                keySource = Character.toLowerCase(keySource.charAt(0)) + keySource.substring(1);
            }
        }
        return new ConfigPath(rule.module(), toKebabCase(keySource));
    }

    /** 查找字段所属的模块规则；无匹配返回 null（视为 Global）。 */
    public static ModuleRule moduleRuleFor(String fieldName) {
        for (ModuleRule rule : MODULE_RULES) {
            if (fieldName.equals(rule.toggleField()) || (!rule.prefix().isEmpty() && fieldName.startsWith(rule.prefix()))) {
                return rule;
            }
            for (String extra : rule.extraFields()) {
                if (fieldName.equals(extra)) {
                    return rule;
                }
            }
        }
        return null;
    }

    /** 判断字段是否为某个 HUD 组件的布局字段（归入 Hud.cfg）。 */
    public static boolean isHudField(String fieldName) {
        for (HudComponent component : HUD_COMPONENTS) {
            for (String hudField : component.fields()) {
                if (hudField.equals(fieldName)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 计算字段在 HUD 组件 JSON 对象内的键名（kebab-case）。 */
    public static String hudKeyForField(HudComponent component, String fieldName) {
        String key = fieldName;
        if (!component.prefix().isEmpty() && fieldName.startsWith(component.prefix())) {
            key = fieldName.substring(component.prefix().length());
            if (!key.isEmpty()) {
                key = Character.toLowerCase(key.charAt(0)) + key.substring(1);
            }
        }
        return toKebabCase(key);
    }

    /** 驼峰命名转 kebab-case，用于 JSON 键名。 */
    public static String toKebabCase(String value) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0) {
                    out.append('-');
                }
                out.append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static final ModuleRule[] MODULE_RULES = new ModuleRule[] {
            new ModuleRule("AutoMainHand", "mainHandAssist", "mainHandAssist"),
            new ModuleRule("ElytraImprovements", "elytra", "elytraAssist"),
            new ModuleRule("HitMarker", "hitMarker", "hitMarker"),
            new ModuleRule("LocalReplay", "localReplay", "localReplay", "localReplayAutoRecord", "localReplayBufferTicks", "localReplayIdleTicks"),
            new ModuleRule("HitSound", "hitSound", "hitSound"),
            new ModuleRule("Legacy17Animations", "legacy17", "legacy17Animations", "legacy17UseSwing", "legacy17FishingRod", "legacy17HeadRotation", "legacy17HurtTilt", "legacy17ItemPickup"),
            new ModuleRule("SwordBlockingAnimation", "swordBlock", "swordBlock", "offsetX", "offsetY", "offsetZ", "animSpeed", "animationMode"),
            new ModuleRule("AutoBlock", "autoMode", "autoMode", "range"),
            new ModuleRule("RemoveAttackCooldownAnimation", "noAttackCooldownAnimation", "noAttackCooldownAnimation"),
            new ModuleRule("SneakAnimationAdjustment", "noSneakAnimation", "noSneakAnimation", "sneakDropScale", "sneakAnimationSpeed"),
            new ModuleRule("AutoScreenshot", "autoScreenshot", "autoScreenshot"),
            new ModuleRule("AutoGG", "autoGG", "autoGG"),
            new ModuleRule("ServerAutoLogin", "serverAutoLogin", "serverAutoLogin", "serverAutoLoginRules", "serverAutoLoginMode"),
            new ModuleRule("FoodInfo", "foodInfo", "foodInfo"),
            new ModuleRule("LowHealthWarning", "lowHealthNotify", "lowHealthNotify"),
            new ModuleRule("DamageNumbers", "damageNumbers", "damageNumbers"),
            new ModuleRule("AttackReachDisplay", "attackReachDisplay", "attackReachDisplay"),
            new ModuleRule("FallDamagePrediction", "fallDamagePredict", "fallDamagePredict"),
            new ModuleRule("FireballLandingPrediction", "fireballLandingPredict", "fireballLandingPredict"),
            new ModuleRule("ProjectileTrajectoryPrediction", "projectileTrajectoryPredict", "projectileTrajectoryPredict", "projectileTrajectoryPredictBow", "projectileTrajectoryPredictSnowball", "projectileTrajectoryPredictEnderPearl", "projectileTrajectoryPredictBlock", "projectileTrajectoryPredictEntity", "projectileTrajectoryPredictEntityMovement", "projectileTrajectoryPredictOtherPlayers"),
            new ModuleRule("VictorySound", "victorySound", "victorySound"),
            new ModuleRule("GammaOverride", "gammaOverride", "gammaOverride", "gammaValue"),
            new ModuleRule("AutoSprint", "autoSprint", "autoSprint"),
            new ModuleRule("NoSwimming", "noSwimming", "noSwimming"),
            new ModuleRule("NickHider", "nickHider", "nickHider"),
            new ModuleRule("HeldItemPosition", "heldItem", "heldItemPosition"),
            new ModuleRule("FishingRodAssist", "fishingRodAssist", "fishingRodAssist"),
            new ModuleRule("BlockCountDisplay", "blockCountDisplay", "blockCountDisplay"),
            new ModuleRule("BetterPingDisplay", "betterPingDisplay", "betterPingDisplay"),
            new ModuleRule("LyricsDisplay", "lyricsDisplay", "lyricsDisplay"),
            new ModuleRule("MusicInfoHUD", "musicInfoHud", "musicInfoHud", "musicInfoHudCoverRounded"),
            new ModuleRule("DynamicIsland", "dynamicIsland", "dynamicIsland"),
            new ModuleRule("Arraylist", "arraylist", "arraylist"),
            new ModuleRule("ItemUseStatus", "itemUseStatus", "itemUseStatus"),
            new ModuleRule("ItemPhysics", "itemPhysics", "itemPhysics"),
            new ModuleRule("DroppedItem2DRender", "item2DRender", "item2DRender"),
            new ModuleRule("TimeChange", "timeChange", "timeChange", "clientTime"),
            new ModuleRule("WeatherChange", "weatherChange", "weatherChange", "weatherMode"),
            new ModuleRule("Zoom", "zoom", "zoom"),
            new ModuleRule("Freelook", "freelook", "freelook"),
            new ModuleRule("ArmorHUD", "armorHud", "armorHud"),
            new ModuleRule("ArmorTransparency", "armorTransparency", "armorTransparency", "armorTransparencyHead", "armorTransparencyChest", "armorTransparencyLegs", "armorTransparencyFeet", "armorTransparencyShowInCombat"),
            new ModuleRule("PotionStatus", "potionStatus", "potionStatus"),
            new ModuleRule("QuickDeposit", "autoChestDeposit", "autoChestDeposit"),
            new ModuleRule("RemoveContainerBackground", "removeContainerBackground", "removeContainerBackground"),
            new ModuleRule("Keystrokes", "keystrokes", "keystrokes"),
            new ModuleRule("InputMethodFix", "disableImeInGame", "disableImeInGame"),
            new ModuleRule("RenderControl", "hide", ""),
            new ModuleRule("AttackEffects", "attackEffects", ""),
            new ModuleRule("HitColor", "hitColor", "hitColor"),
            new ModuleRule("CustomBlockOutline", "customBlockOutline", "customBlockOutline", "customBlockOutlineFill", "customBlockOutlineAnimation", "customBlockOutlineRed", "customBlockOutlineGreen", "customBlockOutlineBlue", "customBlockOutlineAlpha", "customBlockOutlineFillRed", "customBlockOutlineFillGreen", "customBlockOutlineFillBlue", "customBlockOutlineFillAlpha", "customBlockOutlineWidth", "customBlockOutlineAnimationSpeed", "customBlockOutlineMoveSpeed"),
            new ModuleRule("RainbowEnchantmentGlint", "customEnchantmentGlint", "customEnchantmentGlint"),
            new ModuleRule("MotionCamera", "motionCamera", "motionCamera"),
            new ModuleRule("CustomCape", "customCape", "customCape", "customCapeImage"),
            new ModuleRule("BetterChat", "betterChat", "betterChat"),
            new ModuleRule("BetterScoreboard", "betterScoreboard", "betterScoreboard"),
            new ModuleRule("BetterItemSelector", "betterItemSelector", "betterItemSelector"),
            new ModuleRule("BetterMouseLogic", "betterMouseLogic", "betterMouseLogic"),
            new ModuleRule("SmoothHotbarScrolling", "smoothHotbar", "smoothHotbarScrolling", "hotbarRollover"),
            new ModuleRule("EntityOptimize", "entityOptimize", "entityOptimize", "entityRenderDistance", "itemRenderDistance", "xpOrbRenderDistance", "arrowRenderDistance"),
            new ModuleRule("MemoryOptimize", "cacheCleaner", "cacheCleaner", "cacheCleanerInterval"),
            new ModuleRule("MainUI", "mainUI", "useMainUI", "mainUIBackgroundImage", "mainUIGlslShader"),
            new ModuleRule("TargetHUD", "targetHud", "targetHud"),
            new ModuleRule("NameTags", "nameTag", "nameTag"),
            new ModuleRule("DynamicMotionBlur", "dynamicMotionBlur", "dynamicMotionBlur", "motionBlurAlgorithm"),
            new ModuleRule("HUDTheme", "hud", "", "skiaBlurStrength"),
            new ModuleRule("ClickGuiScale", "clickGui", "", "clickGuiScale", "clickGuiScrollSpeed"),
            new ModuleRule("Notification", "notification", ""),
            new ModuleRule("LiquidGlass", "liquidGlass", "", "liquidGlassBlur", "liquidGlassRefractionHeight", "liquidGlassRefractionAmount", "liquidGlassIoR", "liquidGlassDepthEffect", "liquidGlassDispersion", "liquidGlassRenderPrecision", "liquidGlassHighlight", "liquidGlassHighlightFollowView", "liquidGlassShadow"),
    };

    static final HudComponent[] HUD_COMPONENTS = new HudComponent[] {
            new HudComponent("TargetHUD", "targetHud", "targetHudX", "targetHudY", "targetHudZ", "targetHudScale"),
            new HudComponent("Keystrokes", "keystrokes", "keystrokesX", "keystrokesY", "keystrokesScale"),
            new HudComponent("PingHUD", "pingHud", "pingHudStyle", "pingHudBackground", "pingHudX", "pingHudY", "pingHudScale"),
            new HudComponent("TpsHUD", "tpsHud", "tpsHudStyle", "tpsHudBackground", "tpsHudX", "tpsHudY", "tpsHudScale"),
            new HudComponent("ClockHUD", "clockHud", "clockHudStyle", "clockHudBackground", "clockHudX", "clockHudY", "clockHudScale"),
            new HudComponent("DroppedItemRadar", "droppedItemRadar", "droppedItemRadarStyle", "droppedItemRadarBackground", "droppedItemRadarX", "droppedItemRadarY", "droppedItemRadarScale"),
            new HudComponent("BlockCountDisplay", "blockCountDisplay", "blockCountDisplayX", "blockCountDisplayY", "blockCountDisplayScale"),
            new HudComponent("ArmorHUD", "armorHud", "armorHudX", "armorHudY", "armorHudScale"),
            new HudComponent("ItemUseStatus", "itemUseStatus", "itemUseStatusX", "itemUseStatusY", "itemUseStatusScale"),
            new HudComponent("DynamicIsland", "dynamicIsland", "dynamicIslandX", "dynamicIslandY", "dynamicIslandScale"),
            new HudComponent("Notification", "notification", "notificationX", "notificationY", "notificationScale"),
            new HudComponent("PotionStatus", "potionStatus", "potionStatusX", "potionStatusY", "potionStatusScale"),
            new HudComponent("LyricsDisplay", "lyricsDisplay", "lyricsDisplayX", "lyricsDisplayY", "lyricsDisplayScale"),
            new HudComponent("MusicInfoHUD", "musicInfoHud", "musicInfoHudX", "musicInfoHudY", "musicInfoHudScale"),
            new HudComponent("BetterScoreboard", "betterScoreboard", "betterScoreboardX", "betterScoreboardY", "betterScoreboardScale"),
    };

    record ModuleRule(String module, String prefix, String toggleField, String... extraFields) {
    }

    record ConfigPath(String module, String key) {
    }

    record HudComponent(String name, String prefix, String... fields) {
    }
}
