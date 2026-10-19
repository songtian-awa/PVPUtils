package com.pvp_utils.client.optimize;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;

public final class GameOptimizer {
    private static final GameOptimizer INSTANCE = new GameOptimizer();
    private int tickCounter = 0;

    private GameOptimizer() {}

    public static GameOptimizer getInstance() {
        return INSTANCE;
    }

    public void onClientTick() {
        if (!Config.lowEndOptimization) {
            return;
        }
        tickCounter++;
        if (tickCounter >= Config.lowEndGcInterval) {
            tickCounter = 0;
            System.gc();
        }
    }
}
