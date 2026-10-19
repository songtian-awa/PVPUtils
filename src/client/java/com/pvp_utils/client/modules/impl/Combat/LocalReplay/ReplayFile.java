package com.pvp_utils.client.modules.impl.Combat.LocalReplay;

import java.util.ArrayList;
import java.util.List;

/**
 * 本地回放：回放文件模型。
 *
 * 文件格式为 JSON（轻量、可读、无额外依赖），保存于
 * <gameDir>/PVPUtils/replays/ 目录。
 */
public final class ReplayFile {
    /** 数据格式版本，用于未来兼容迁移。 */
    public static final String FORMAT_VERSION = "1";

    public String version = FORMAT_VERSION;
    /** 录制完成时的时间戳（epoch 毫秒）。 */
    public long recordedAt;
    /** 录制时玩家名。 */
    public String playerName;
    /** 回放总时长（tick）。 */
    public int durationTicks;
    /** 状态帧序列。 */
    public List<ReplayFrame> frames = new ArrayList<>();
    /** 事件序列。 */
    public List<ReplayEvent> events = new ArrayList<>();

    public ReplayFile() {
    }
}
