package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.OfflineSkinManager;
import com.pvp_utils.client.util.ChatUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 离线皮肤快速切换命令：
 * .skin list             列出全部皮肤
 * .skin next             切换到下一张
 * .skin use <文件名>      切换到指定皮肤
 * .skin import <路径>     从本地路径导入 PNG 皮肤
 * .skin folder           打开皮肤文件夹
 */
public class SkinCommand implements DotCommand {
    private static final List<String> SUB_COMMANDS = List.of("list", "next", "use", "import", "folder", "help");

    @Override
    public List<String> names() {
        return List.of("skin", "skins");
    }

    @Override
    public void execute(String args) {
        String arg = args == null ? "" : args.trim();
        if (arg.isEmpty() || "help".equalsIgnoreCase(arg)) {
            ChatUtils.send(Config.isChinese
                    ? "离线皮肤: .skin list / next / use <名称> / import <路径> / folder"
                    : "Offline skin: .skin list / next / use <name> / import <path> / folder");
            return;
        }
        String lower = arg.toLowerCase(Locale.ROOT);
        if ("list".equals(lower)) {
            List<String> files = OfflineSkinManager.listPngs();
            if (files.isEmpty()) {
                ChatUtils.warning(Config.isChinese ? "皮肤文件夹为空，请放入 64x64 的 PNG 皮肤文件" : "Skin folder is empty, drop a 64x64 PNG skin file");
            } else {
                ChatUtils.send(Config.isChinese
                        ? "可用皮肤 (" + files.size() + "): " + String.join(", ", files) + "，当前: " + OfflineSkinManager.currentName()
                        : "Skins (" + files.size() + "): " + String.join(", ", files) + ", current: " + OfflineSkinManager.currentName());
            }
            return;
        }
        if ("next".equals(lower)) {
            List<String> files = OfflineSkinManager.listPngs();
            if (files.isEmpty()) {
                ChatUtils.warning(Config.isChinese ? "皮肤文件夹为空，无法切换" : "Skin folder is empty, cannot switch");
                return;
            }
            OfflineSkinManager.cycleSkin();
            ChatUtils.success(Config.isChinese ? "已切换皮肤: " + OfflineSkinManager.currentName() : "Switched skin: " + OfflineSkinManager.currentName());
            return;
        }
        if ("folder".equals(lower)) {
            OfflineSkinManager.openFolder();
            ChatUtils.success(Config.isChinese ? "已打开皮肤文件夹" : "Opened skin folder");
            return;
        }
        if (lower.startsWith("use")) {
            String name = arg.length() > 4 ? arg.substring(4).trim() : "";
            if (name.isEmpty()) {
                ChatUtils.warning(Config.isChinese ? "用法: .skin use <文件名.png>" : "Usage: .skin use <file.png>");
                return;
            }
            if (OfflineSkinManager.select(name)) {
                ChatUtils.success(Config.isChinese ? "已切换皮肤: " + name : "Switched skin: " + name);
            } else {
                ChatUtils.failure(Config.isChinese ? "未找到该皮肤: " + name + "（可用 .skin list 查看）" : "Skin not found: " + name + " (use .skin list)");
            }
            return;
        }
        if (lower.startsWith("import")) {
            String path = arg.length() > 7 ? arg.substring(7).trim() : "";
            if (path.isEmpty()) {
                ChatUtils.warning(Config.isChinese ? "用法: .skin import <本地PNG路径>" : "Usage: .skin import <local png path>");
                return;
            }
            String name = OfflineSkinManager.importFrom(path);
            if (name == null) {
                ChatUtils.failure(Config.isChinese ? "导入失败，请检查路径是否为有效的 PNG 文件" : "Import failed, check the path is a valid PNG file");
            } else {
                OfflineSkinManager.select(name);
                ChatUtils.success(Config.isChinese ? "已导入并切换: " + name : "Imported and switched: " + name);
            }
            return;
        }
        ChatUtils.failure(Config.isChinese ? "未知子命令，输入 .skin help 查看用法" : "Unknown subcommand, type .skin help");
    }

    @Override
    public List<String> suggestions(String args) {
        if (args == null) return SUB_COMMANDS;
        String trimmed = args.trim();
        if (trimmed.startsWith("use ")) {
            String prefix = trimmed.substring(4).trim().toLowerCase(Locale.ROOT);
            List<String> result = new ArrayList<>();
            for (String file : OfflineSkinManager.listPngs()) {
                if (file.toLowerCase(Locale.ROOT).startsWith(prefix)) result.add(file);
            }
            return result;
        }
        String prefix = trimmed.toLowerCase(Locale.ROOT);
        return SUB_COMMANDS.stream().filter(sub -> sub.startsWith(prefix)).toList();
    }

    @Override
    public boolean acceptsFreeText() {
        return false;
    }
}
