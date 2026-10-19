package com.pvp_utils.client.command.impl;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Combat.LocalReplay.LocalReplayManager;
import com.pvp_utils.client.util.ChatUtils;

import java.util.List;

/**
 * 本地回放命令。
 *
 * 用法：
 *   .replay                查看帮助
 *   .replay record         开始手动录制
 *   .replay stop [name]    停止录制并保存（可指定文件名）
 *   .replay play <name>    打开回放（慢放 / 复盘）
 *   .replay list           列出已保存的回放
 *   .replay delete <name>  删除指定回放
 *   .replay folder         打开回放文件所在目录
 */
public final class ReplayCommand implements DotCommand {
    @Override
    public List<String> names() {
        return List.of("replay");
    }

    @Override
    public boolean acceptsFreeText() {
        return true;
    }

    @Override
    public void execute(String args) {
        String text = args.trim();
        if (text.isEmpty()) {
            showHelp();
            return;
        }

        String lower = text.toLowerCase(java.util.Locale.ROOT);
        if (lower.equals("record") || lower.equals("start") || lower.equals("rec")) {
            LocalReplayManager.getInstance().startManual();
            return;
        }
        if (lower.equals("stop") || lower.startsWith("stop ")) {
            String name = lower.startsWith("stop ") ? text.substring(4).trim() : null;
            LocalReplayManager.getInstance().stopManual(name);
            return;
        }
        if (lower.equals("list") || lower.equals("ls")) {
            listReplays();
            return;
        }
        if (lower.equals("folder") || lower.equals("dir") || lower.equals("open")) {
            LocalReplayManager.getInstance().openFolder();
            return;
        }
        if (lower.startsWith("play ") || lower.startsWith("open ")) {
            String name = text.substring(text.indexOf(' ') + 1).trim();
            if (name.isEmpty()) {
                ChatUtils.warning(Config.isChinese ? "请指定回放文件名，如 .replay play replay_20261019_120000.json" : "Specify a replay file, e.g. .replay play replay_20261019_120000.json");
                return;
            }
            LocalReplayManager.getInstance().play(name);
            return;
        }
        if (lower.startsWith("delete ") || lower.startsWith("del ") || lower.startsWith("rm ")) {
            String name = text.substring(text.indexOf(' ') + 1).trim();
            if (name.isEmpty()) {
                ChatUtils.warning(Config.isChinese ? "请指定要删除的回放文件名。" : "Specify a replay file to delete.");
                return;
            }
            LocalReplayManager.getInstance().deleteReplay(name);
            return;
        }
        if (lower.equals("help") || lower.equals("?")) {
            showHelp();
            return;
        }
        ChatUtils.warning(Config.isChinese
                ? "未知的子命令：" + text + "，输入 .replay 查看帮助"
                : "Unknown sub-command: " + text + ". Type .replay for help.");
    }

    private void listReplays() {
        List<String> replays = LocalReplayManager.getInstance().listReplays();
        if (replays.isEmpty()) {
            ChatUtils.send(Config.isChinese ? "本地回放：暂无已保存的录像。" : "Local Replay: no replays saved.");
            return;
        }
        ChatUtils.send(Config.isChinese ? "本地回放：已保存 " + replays.size() + " 个录像：" : "Local Replay: " + replays.size() + " replays:");
        int shown = Math.min(replays.size(), 8);
        for (int i = 0; i < shown; i++) {
            ChatUtils.send("  " + replays.get(i));
        }
        if (replays.size() > shown) {
            ChatUtils.send(Config.isChinese ? "  …（其余 " + (replays.size() - shown) + " 个请查看回放目录）" : "  ... (" + (replays.size() - shown) + " more, check replay folder)");
        }
    }

    private void showHelp() {
        if (Config.isChinese) {
            ChatUtils.send("本地回放命令：");
            ChatUtils.send("  .replay record        开始手动录制");
            ChatUtils.send("  .replay stop [name]   停止录制并保存（可指定文件名）");
            ChatUtils.send("  .replay play <name>   打开回放（空格播放/暂停，←→快进快退，↑变速）");
            ChatUtils.send("  .replay list          列出已保存的录像");
            ChatUtils.send("  .replay delete <name> 删除录像");
            ChatUtils.send("  .replay folder        打开录像目录");
        } else {
            ChatUtils.send("Local Replay commands:");
            ChatUtils.send("  .replay record        start manual recording");
            ChatUtils.send("  .replay stop [name]   stop and save (optional name)");
            ChatUtils.send("  .replay play <name>   open replay (SPACE toggle, arrows seek, UP speed)");
            ChatUtils.send("  .replay list          list saved replays");
            ChatUtils.send("  .replay delete <name> delete a replay");
            ChatUtils.send("  .replay folder        open replay folder");
        }
    }

    @Override
    public List<String> suggestions(String args) {
        String lower = args.trim().toLowerCase(java.util.Locale.ROOT);
        List<String> subCommands = List.of("record", "stop", "play", "list", "delete", "folder", "help");
        if (lower.isEmpty()) {
            return subCommands;
        }
        return subCommands.stream().filter(s -> s.startsWith(lower)).collect(java.util.stream.Collectors.toList());
    }
}
