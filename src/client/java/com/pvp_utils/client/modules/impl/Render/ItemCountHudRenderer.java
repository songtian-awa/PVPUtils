package com.pvp_utils.client.modules.impl.Render;

import com.pvp_utils.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ItemCountHudRenderer {
    private static final ItemCountHudRenderer INSTANCE = new ItemCountHudRenderer();
    private ItemCountHudRenderer() {}
    public static ItemCountHudRenderer getInstance() { return INSTANCE; }

    public void render(GuiGraphics graphics) {
        if (!Config.itemCountHud) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        int x = (int) Config.itemCountHudX;
        int y = (int) Config.itemCountHudY;

        int pearls = countItem(client, Items.ENDER_PEARL);
        int gapples = countItem(client, Items.ENCHANTED_GOLDEN_APPLE) + countItem(client, Items.GOLDEN_APPLE);
        int crystal = countItem(client, Items.END_CRYSTAL);

        graphics.fill(x - 2, y - 2, x + 110, y + 22, 0x80000000);

        graphics.renderItem(Items.ENDER_PEARL.getDefaultInstance(), x, y);
        graphics.drawString(client.font, "x" + pearls, x + 18, y + 4, 0xFFFFFF, true);

        graphics.renderItem(Items.ENCHANTED_GOLDEN_APPLE.getDefaultInstance(), x + 40, y);
        graphics.drawString(client.font, "x" + gapples, x + 58, y + 4, 0xFFFFFF, true);

        graphics.renderItem(Items.END_CRYSTAL.getDefaultInstance(), x + 80, y);
        graphics.drawString(client.font, "x" + crystal, x + 98, y + 4, 0xFFFFFF, true);
    }

    private int countItem(Minecraft client, net.minecraft.world.item.Item item) {
        int count = 0;
        var inv = client.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }
}
