package com.pvp_utils.client.modules.impl.Combat.LocalReplay;

/**
 * 本地回放：单个状态采样帧。
 *
 * 轻量设计：仅记录位置、朝向、生命值等战斗复盘所需的核心状态，
 * 不包含完整实体数据。字段全部公开，与 Config 反射风格保持一致，
 * 便于 Gson 直接序列化 / 反序列化。
 */
public final class ReplayFrame {
    /** 录制开始后的累计 tick（游戏刻）。 */
    public int tick;
    public double x;
    public double y;
    public double z;
    public float yaw;
    public float pitch;
    public float health;
    public int hunger;
    /** 主手物品名（用于回放界面展示），可为空串。 */
    public String mainHand;
    public boolean sneaking;
    public boolean sprinting;
    public boolean flying;
    public boolean swimming;
    public boolean onGround;

    public ReplayFrame() {
    }

    public ReplayFrame(int tick, double x, double y, double z, float yaw, float pitch,
                       float health, int hunger, String mainHand,
                       boolean sneaking, boolean sprinting, boolean flying,
                       boolean swimming, boolean onGround) {
        this.tick = tick;
        this.x = x;
        this.y = y;
        this.z = z;
        this.yaw = yaw;
        this.pitch = pitch;
        this.health = health;
        this.hunger = hunger;
        this.mainHand = mainHand == null ? "" : mainHand;
        this.sneaking = sneaking;
        this.sprinting = sprinting;
        this.flying = flying;
        this.swimming = swimming;
        this.onGround = onGround;
    }
}
