package com.pvp_utils.client.gui.clickgui;

import com.pvp_utils.Config;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public class ChangelogScreen extends Screen {
    private static final String[] LINES_ZH = {
            "v1.8-beta.9",
            "新增: 全语言翻译（基于 Minecraft 标准语言文件的多语言支持）",
            "新增: 单场战绩结算（对局结束自动统计本场 K/D / 命中率）",
            "新增: 自定义死亡画面（死亡时显示存活时间 / 击杀者统计卡）",
            "新增: 击杀卡牌（击杀时在目标 HUD 下方展示击杀者 / 武器 / 伤害）",
            "新增: 本地回放（战斗轻量回放录制与慢放）",
            "新增: 离线皮肤库（皮肤管理界面与快捷切换）",
            "新增: 官方插件系统（JS 脚本插件加载 / 管理 / 独立插件页面）",
            "回退: 移除本地音乐库 MP3 功能",
            "回退: 移除 Fabric API 整合",
            "回退: 移除 Python 自动化控制台",
            "新增: 背包 HUD (不开背包看物品)",
            "新增: 物品剩余 HUD (珍珠/金苹果/水晶)",
            "新增: 战斗日志可拖动",
            "新增: 左边模块栏滚轮滑动",
            "优化: 液体玻璃性能",
            "修复: cosmetica-core 兼容",
            "v1.8-beta.12",
            "新增: 左边模块栏滚轮滑动",
            "修复: 物品剩余 HUD 数量显示",
            "新增: 战斗日志可拖动",
            "v1.8-beta.16",
            "新增: 背包 HUD (不开背包看物品)",
            "新增: 物品剩余 HUD (珍珠/金苹果/水晶)",
            "新增: 盔甲耐久百分比开关",
            "新增: 液体玻璃总开关",
            "优化: 液体玻璃性能 (FPS 提升约 2 倍)",
            "修复: 打开文件夹按钮",
            "v1.8-beta.15",
            "新增: 本地音乐库 (支持 MP3 拖入播放)",
            "新增: MP3 ID3 歌词提取",
            "新增: 音乐页面本地音乐 UI",
            "新增: 合并官方液体玻璃渲染器",
            "v1.8-beta.14",
            "新增: 战斗日志",
            "新增: 敌方护甲显示",
            "新增: Sodium 兼容检测",
            "新增: 左侧模块栏滚轮",
            "修复: 英文翻译",
            "v1.8-beta.12",
            "新增: PVPUtils 玩家频道聊天 (需服务端安装 PVPUtilsServer)",
            "修复: 掉落物雷达支持拖动移位",
            "v1.8-beta.11",
            "修复: 击杀闪电改为目标死亡时触发",
            "新增: 离线皮肤修复",
            "新增: 掉落物雷达 HUD",
            "新增: 配置预设: 默认 / PVP / 低配优化",
            "新增: 粒子优化",
            "新增: 自定义面板颜色",
            "新增: 独立聊天窗口",
            "新增: 更新日志页面",
            "整合: fabric-api 打包进模组",
            "修复: 实体渲染剔除注入点崩溃",
            "优化: 实体渲染距离剔除",
            "优化: 内存自动清理",
    };

    private static final String[] LINES_EN = {
            "v1.8-beta.9",
            "Added: Full language translation (multi-language support via Minecraft standard language files)",
            "Added: Match summary (auto-stat K/D and hit rate at round end)",
            "Added: Custom death screen (shows survival time / killer stats on death)",
            "Added: Kill card (shows killer / weapon / damage below the target HUD on kill)",
            "Added: Local replays (lightweight battle replay recording and slow motion)",
            "Added: Offline skin library (skin manager UI and quick switching)",
            "Added: Official plugin system (JS plugin loading / management / dedicated plugin page)",
            "Reverted: Removed local music library MP3 feature",
            "Reverted: Removed Fabric API integration",
            "Reverted: Removed Python automation console",
            "Added: Inventory HUD (view items without opening inventory)",
            "Added: Item count HUD (pearls / golden apples / crystals)",
            "Added: Draggable combat log",
            "Added: Left module bar wheel scrolling",
            "Optimized: Liquid glass performance",
            "Fixed: cosmetica-core compatibility",
            "v1.8-beta.12",
            "Added: Left module bar wheel scrolling",
            "Fixed: Item count HUD quantity display",
            "Added: Draggable combat log",
            "v1.8-beta.16",
            "Added: Inventory HUD (view items without opening inventory)",
            "Added: Item count HUD (pearls / golden apples / crystals)",
            "Added: Armor durability percentage toggle",
            "Added: Liquid glass master toggle",
            "Optimized: Liquid glass performance (~2x FPS improvement)",
            "Fixed: Open folder button",
            "v1.8-beta.15",
            "Added: Local music library (MP3 drag-and-drop playback)",
            "Added: MP3 ID3 lyrics extraction",
            "Added: Local music UI in music page",
            "Added: Merged official liquid glass renderer",
            "v1.8-beta.14",
            "Added: Combat log",
            "Added: Enemy armor display",
            "Added: Sodium compatibility check",
            "Added: Left module bar wheel scrolling",
            "Fixed: English translations",
            "v1.8-beta.12",
            "Added: PVPUtils player channel chat (requires PVPUtilsServer on the server)",
            "Fixed: Dropped item radar draggable",
            "v1.8-beta.11",
            "Fixed: Kill lightning triggers on target death",
            "Added: Offline skin fix",
            "Added: Dropped item radar HUD",
            "Added: Config presets: Default / PVP / Low-end optimization",
            "Added: Particle optimization",
            "Added: Custom panel colors",
            "Added: Standalone chat window",
            "Added: Changelog screen",
            "Integrated: fabric-api bundled into the mod",
            "Fixed: Entity render culling injection crash",
            "Optimized: Entity render distance culling",
            "Optimized: Automatic memory cleanup",
    };

    public ChangelogScreen() {
        super(Component.literal(UiText.key("pvp_utils.changelog.title", "更新日志", "Changelog")));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        super.render(graphics, mouseX, mouseY, partialTick);
        graphics.fillGradient(0, 0, this.width, this.height, 0xCC101418, 0xCC101418);
        String[] lines = Config.isChinese ? LINES_ZH : LINES_EN;
        int y = 40;
        for (String line : lines) {
            graphics.drawString(this.font, line, 30, y, 0xFFF2F4F8, false);
            y += 14;
        }
        graphics.drawString(this.font, UiText.key("pvp_utils.changelog.back", "按 ESC 返回", "Press ESC to go back"), 30, y + 10, 0xFF9AA4B2, false);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
