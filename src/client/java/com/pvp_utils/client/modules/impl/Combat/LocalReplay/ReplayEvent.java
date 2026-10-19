package com.pvp_utils.client.modules.impl.Combat.LocalReplay;

/**
 * 本地回放：战斗事件。
 *
 * 事件与状态帧解耦，事件用于时间线复盘（命中 / 受击 / 击杀 / 死亡），
 * 状态帧用于轨迹与血量曲线绘制。
 */
public final class ReplayEvent {

    public enum Type {
        DAMAGE_DEALT,
        DAMAGE_TAKEN,
        KILL,
        DEATH
    }

    /** 录制开始后的累计 tick（游戏刻）。 */
    public int tick;
    /** 事件类型。 */
    public String type;
    /** 面向玩家的简要描述（已按当前语言生成）。 */
    public String message;

    public ReplayEvent() {
    }

    public ReplayEvent(int tick, Type type, String message) {
        this.tick = tick;
        this.type = type == null ? null : type.name();
        this.message = message == null ? "" : message;
    }

    public Type type() {
        if (type == null) {
            return null;
        }
        try {
            return Type.valueOf(type);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
