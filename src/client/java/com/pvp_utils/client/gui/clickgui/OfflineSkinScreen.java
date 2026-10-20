package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.Config;
import com.pvp_utils.client.modules.impl.Render.OfflineSkinManager;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;

/**
 * 离线皮肤库管理界面：
 * 展示皮肤文件夹内全部 PNG，点击即可切换当前皮肤，
 * 支持 F 键快速打开皮肤文件夹，方便导入新的皮肤文件。
 */
public class OfflineSkinScreen extends Screen {
    private static final int ROW_HEIGHT = 16;
    private static final int LIST_TOP = 66;
    private static final int MAX_ROWS = 12;
    private List<String> skins;
    private String current = "";
    private String status = "";

    public OfflineSkinScreen() {
        super(Component.literal("OfflineSkin"));
        refresh();
    }

    /** 重新读取皮肤文件夹与当前选择 */
    private void refresh() {
        skins = OfflineSkinManager.listPngs();
        current = OfflineSkinManager.currentName();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fillGradient(0, 0, this.width, this.height, 0xCC101418, 0xCC101418);
        graphics.drawString(this.font, "离线皮肤库 / Offline Skin Library", 30, 24, 0xFFF2F4F8, false);
        String info = skins.isEmpty()
                ? (Config.isChinese ? "皮肤文件夹为空，请放入 64x64 的 PNG 皮肤文件" : "Skin folder is empty, drop a 64x64 PNG skin file")
                : (Config.isChinese ? "共 " + skins.size() + " 个皮肤，当前: " + current : skins.size() + " skins, current: " + current);
        graphics.drawString(this.font, info, 30, 42, 0xFF9AA4B2, false);
        if (!skins.isEmpty()) {
            int limit = Math.min(skins.size(), MAX_ROWS);
            for (int i = 0; i < limit; i++) {
                int y = LIST_TOP + i * ROW_HEIGHT;
                boolean selected = skins.get(i).equals(current);
                boolean hovered = mouseY >= y && mouseY < y + ROW_HEIGHT && mouseX >= 30 && mouseX < 360;
                int color = selected ? 0xFFFFD24D : hovered ? 0xFF9AA4B2 : 0xFFF2F4F8;
                String label = selected ? "> " + skins.get(i) : skins.get(i);
                graphics.drawString(this.font, label, 34, y + 4, color, false);
            }
            if (skins.size() > limit) {
                graphics.drawString(this.font,
                        Config.isChinese ? "还有 " + (skins.size() - limit) + " 个皮肤，可打开文件夹查看" : (skins.size() - limit) + " more skins, open folder to view",
                        34, LIST_TOP + limit * ROW_HEIGHT + 6, 0xFF9AA4B2, false);
            }
        }
        if (!status.isEmpty()) {
            graphics.drawString(this.font, status, 30, this.height - 52, 0xFF6BE86B, false);
        }
        graphics.drawString(this.font,
                Config.isChinese ? "左键选择皮肤 · F 打开文件夹 · ESC 返回" : "Click to select · F open folder · ESC back",
                30, this.height - 32, 0xFF9AA4B2, false);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean consumed) {
        if (event.button() == 0 && !skins.isEmpty()) {
            int index = (int) ((event.y() - LIST_TOP) / ROW_HEIGHT);
            if (event.x() >= 30 && event.x() < 360 && index >= 0 && index < Math.min(skins.size(), MAX_ROWS)) {
                String name = skins.get(index);
                if (OfflineSkinManager.select(name)) {
                    refresh();
                    status = Config.isChinese ? "已切换到: " + name : "Switched to: " + name;
                    return true;
                }
            }
        }
        return super.mouseClicked(event, consumed);
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_F) {
            OfflineSkinManager.openFolder();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
