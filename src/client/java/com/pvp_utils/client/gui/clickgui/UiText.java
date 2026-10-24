package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.Config;
import net.minecraft.client.resources.language.I18n;

import java.util.Map;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;

public final class UiText {
    private static final Map<String, String> counterparts = new ConcurrentHashMap<>();

    private UiText() {}

    public static String t(String zh, String en) {
        counterparts.put(zh, en);
        counterparts.put(en, zh);
        return Config.isChinese ? zh : en;
    }

    /**
     * Minecraft 标准 i18n 桥：优先读取语言包（assets/pvp-utils/lang/&lt;locale&gt;.json），
     * 未收录该 key 时回退内置中英双语，避免出现翻译缺失乱码。
     */
    public static String key(String key, String zh, String en) {
        try {
            String localized = I18n.get(key);
            if (localized != null && !localized.equals(key)) {
                return localized;
            }
        } catch (Throwable ignored) {}
        return Config.isChinese ? zh : en;
    }

    /**
     * 多行文本 i18n 桥：语言文件条目以 {@code \n} 分隔多行，
     * 未收录该 key 时回退内置中英双语数组。
     */
    public static String[] lines(String key, String[] zh, String[] en) {
        try {
            String localized = I18n.get(key);
            if (localized != null && !localized.isEmpty() && !localized.equals(key)) {
                String[] parts = localized.split("\n", -1);
                if (parts.length > 0) {
                    return parts;
                }
            }
        } catch (Throwable ignored) {}
        return Config.isChinese ? zh : en;
    }

    public static boolean matchesSearch(String text, String query) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        String normalizedText = text.toLowerCase(Locale.ROOT);
        String compactQuery = query.replaceAll("\\s+", "");
        if (normalizedText.contains(query) || normalizedText.replaceAll("\\s+", "").contains(compactQuery)) {
            return true;
        }
        String counterpart = counterparts.get(text);
        if (counterpart == null) {
            return false;
        }
        String normalizedCounterpart = counterpart.toLowerCase(Locale.ROOT);
        return normalizedCounterpart.contains(query) || normalizedCounterpart.replaceAll("\\s+", "").contains(compactQuery);
    }
}
