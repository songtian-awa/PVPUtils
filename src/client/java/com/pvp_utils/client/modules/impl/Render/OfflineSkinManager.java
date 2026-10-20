package com.pvp_utils.client.modules.impl.Render;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.GpuTexture;
import com.pvp_utils.Config;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.core.ClientAsset;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public final class OfflineSkinManager {
    private static final Path EXTERNAL_PATH = FabricLoader.getInstance().getGameDir().resolve("PVPUtils/skin");
    private static boolean initialized;
    private static DynamicTexture texture;
    private static Identifier loadedTextureId;
    private static String loaded = "";
    private static String lastLoggedTexture = "";
    private static String lastLoggedList = "";

    private OfflineSkinManager() {
    }

    public static void init() {
        if (initialized) return;
        initialized = true;
        try {
            Files.createDirectories(EXTERNAL_PATH);
        } catch (IOException e) {
            System.out.println("[PVPUtils] OfflineSkin init failed: " + e.getMessage());
        }
    }

    public static void openFolder() {
        init();
        Util.getPlatform().openPath(EXTERNAL_PATH);
    }

    public static List<String> listPngs() {
        init();
        List<String> files = new ArrayList<>();
        try (var stream = Files.list(EXTERNAL_PATH)) {
            stream.filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".png"))
                    .sorted(Comparator.naturalOrder())
                    .forEach(files::add);
        } catch (IOException e) {
            System.out.println("[PVPUtils] OfflineSkin list failed: " + e.getMessage());
        }
        if (!files.isEmpty() && !files.toString().equals(lastLoggedList)) {
            lastLoggedList = files.toString();
            System.out.println("[PVPUtils] OfflineSkin available pngs: " + lastLoggedList);
        }
        return files;
    }

    public static String currentName() {
        List<String> files = listPngs();
        if (files.isEmpty()) return Config.isChinese ? "无皮肤文件" : "No skin file";
        if (Config.offlineSkinImage == null || Config.offlineSkinImage.isBlank() || !files.contains(Config.offlineSkinImage)) {
            return files.get(0);
        }
        return Config.offlineSkinImage;
    }

    public static void cycleSkin() {
        List<String> files = listPngs();
        if (files.isEmpty()) {
            System.out.println("[PVPUtils] OfflineSkin folder is empty, put a 64x64 PNG in " + EXTERNAL_PATH.toAbsolutePath());
            return;
        }
        int index = files.indexOf(Config.offlineSkinImage);
        Config.offlineSkinImage = files.get((index + 1 + files.size()) % files.size());
        Config.save();
        destroy();
        System.out.println("[PVPUtils] OfflineSkin switched to " + Config.offlineSkinImage);
    }

    public static ClientAsset.Texture texture() {
        if (!Config.offlineSkin) return null;
        init();
        List<String> files = listPngs();
        if (files.isEmpty()) return null;
        String selected = Config.offlineSkinImage;
        if (selected == null || selected.isBlank() || !files.contains(selected)) {
            selected = files.get(0);
            Config.offlineSkinImage = selected;
            Config.save();
        }
        Path path = EXTERNAL_PATH.resolve(selected);
        if (!Files.exists(path)) return null;
        String key = selected.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_.-]", "_");
        Identifier texturePath = Identifier.fromNamespaceAndPath("pvp_utils", "offline_skin/" + key);
        if (texture == null || !selected.equals(loaded)) {
            destroy();
            try {
                NativeImage image = NativeImage.read(Files.newInputStream(path));
                texture = new DynamicTexture("pvp_utils:offline_skin/" + key, image.getWidth(), image.getHeight(), false);
                Minecraft.getInstance().getTextureManager().register(texturePath, texture);
                GpuTexture gpuTexture = texture.getTexture();
                RenderSystem.getDevice().createCommandEncoder().writeToTexture(gpuTexture, image);
                image.close();
                loadedTextureId = texturePath;
                loaded = selected;
            } catch (IOException e) {
                System.out.println("[PVPUtils] OfflineSkin texture upload failed: " + e.getMessage());
                return null;
            }
        }
        String textureLog = selected + " -> " + texturePath + " exists=" + Files.exists(path) + " registered=" + (loadedTextureId != null);
        if (!textureLog.equals(lastLoggedTexture)) {
            lastLoggedTexture = textureLog;
            System.out.println("[PVPUtils] OfflineSkin texture: " + textureLog);
        }
        return new ClientAsset.ResourceTexture(texturePath, texturePath);
    }

    /** 返回皮肤文件夹路径（用于展示或命令行提示） */
    public static Path folderPath() {
        init();
        return EXTERNAL_PATH;
    }

    /** 选择指定皮肤文件并立即生效，返回是否成功 */
    public static boolean select(String name) {
        if (name == null || name.isBlank()) return false;
        List<String> files = listPngs();
        if (!files.contains(name)) return false;
        if (!name.equals(Config.offlineSkinImage)) {
            Config.offlineSkinImage = name;
            Config.save();
        }
        destroy();
        return true;
    }

    /** 从外部路径导入 PNG 皮肤到皮肤文件夹（重名自动加序号），返回新文件名，失败返回 null */
    public static String importFrom(String sourcePath) {
        if (sourcePath == null || sourcePath.isBlank()) return null;
        Path source = Path.of(sourcePath.trim());
        if (!Files.isRegularFile(source)) return null;
        String lower = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (!lower.endsWith(".png")) return null;
        init();
        String targetName = source.getFileName().toString();
        Path target = EXTERNAL_PATH.resolve(targetName);
        int suffix = 1;
        while (Files.exists(target)) {
            String base = targetName.substring(0, targetName.length() - 4);
            target = EXTERNAL_PATH.resolve(base + "_" + suffix + ".png");
            suffix++;
        }
        try {
            Files.copy(source, target);
        } catch (IOException e) {
            System.out.println("[PVPUtils] OfflineSkin import failed: " + e.getMessage());
            return null;
        }
        return target.getFileName().toString();
    }

    private static void destroy() {
        if (loadedTextureId != null) {
            Minecraft.getInstance().getTextureManager().release(loadedTextureId);
        }
        texture = null;
        loadedTextureId = null;
        loaded = "";
    }
}
