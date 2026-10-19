package com.pvp_utils.client.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 密码加密工具。
 *
 * <p>v2 格式（当前）：每个密文携带独立的 16 字节随机盐，密钥由
 * {@code SHA-256(salt || 用户指纹)} 派生，避免固定盐导致的密钥可预测问题；
 * 密文布局为 {@code salt(16) + iv(12) + AES-GCM 密文}。</p>
 *
 * <p>v1 格式（历史兼容）：固定盐 + 用户指纹派生密钥，仅用于解密存量数据，
 * 新写入一律使用 v2。</p>
 *
 * <p>加解密失败会记录日志并返回空串，调用方需按"空串视为无有效值"处理，
 * 不再静默吞掉异常。</p>
 */
public final class PasswordCipher {
    private static final Logger LOGGER = LoggerFactory.getLogger("PVPUtils.PasswordCipher");

    private static final String PREFIX_V2 = "enc:v2:";
    private static final String PREFIX_V1 = "enc:v1:";
    /** v1 遗留固定盐，仅用于解密旧数据。 */
    private static final byte[] LEGACY_SALT = "PVPUtils-ServerAutoLogin-Salt".getBytes(StandardCharsets.UTF_8);
    private static final int SALT_LENGTH = 16;
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordCipher() {
    }

    public static String encrypt(String plainText) {
        if (plainText == null || plainText.isEmpty()) {
            return "";
        }
        try {
            byte[] salt = new byte[SALT_LENGTH];
            RANDOM.nextBytes(salt);
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key(salt), new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[salt.length + iv.length + encrypted.length];
            System.arraycopy(salt, 0, combined, 0, salt.length);
            System.arraycopy(iv, 0, combined, salt.length, iv.length);
            System.arraycopy(encrypted, 0, combined, salt.length + iv.length, encrypted.length);
            return PREFIX_V2 + Base64.getEncoder().encodeToString(combined);
        } catch (Exception e) {
            LOGGER.error("Failed to encrypt password", e);
            return "";
        }
    }

    public static String decrypt(String stored) {
        if (stored == null || stored.isEmpty()) {
            return "";
        }
        try {
            if (stored.startsWith(PREFIX_V2)) {
                return decryptV2(stored);
            }
            if (stored.startsWith(PREFIX_V1)) {
                return decryptV1(stored);
            }
            LOGGER.warn("Unrecognized encrypted password prefix, value ignored");
            return "";
        } catch (Exception e) {
            LOGGER.error("Failed to decrypt password", e);
            return "";
        }
    }

    private static String decryptV2(String stored) throws Exception {
        byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX_V2.length()));
        if (combined.length <= SALT_LENGTH + IV_LENGTH) {
            LOGGER.warn("Truncated v2 cipher text ignored");
            return "";
        }
        byte[] salt = Arrays.copyOfRange(combined, 0, SALT_LENGTH);
        byte[] iv = Arrays.copyOfRange(combined, SALT_LENGTH, SALT_LENGTH + IV_LENGTH);
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, key(salt), new GCMParameterSpec(TAG_BITS, iv));
        byte[] decrypted = cipher.doFinal(combined, SALT_LENGTH + IV_LENGTH, combined.length - SALT_LENGTH - IV_LENGTH);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    /** 旧固定盐格式，仅兼容读取存量配置。 */
    private static String decryptV1(String stored) throws Exception {
        byte[] combined = Base64.getDecoder().decode(stored.substring(PREFIX_V1.length()));
        if (combined.length <= IV_LENGTH) {
            return "";
        }
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, keyLegacy(), new GCMParameterSpec(TAG_BITS, combined, 0, IV_LENGTH));
        byte[] decrypted = cipher.doFinal(combined, IV_LENGTH, combined.length - IV_LENGTH);
        return new String(decrypted, StandardCharsets.UTF_8);
    }

    public static boolean isEncrypted(String stored) {
        return stored != null && (stored.startsWith(PREFIX_V2) || stored.startsWith(PREFIX_V1));
    }

    /** v2：随机盐参与密钥派生，盐存于密文内部。 */
    private static SecretKeySpec key(byte[] salt) {
        return deriveKey(salt);
    }

    /** v1：固定盐派生，仅用于旧数据。 */
    private static SecretKeySpec keyLegacy() {
        return deriveKey(LEGACY_SALT);
    }

    private static SecretKeySpec deriveKey(byte[] salt) {
        try {
            String fingerprint = System.getProperty("user.name", "") + "|" + System.getProperty("user.home", "");
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            digest.update(fingerprint.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(Arrays.copyOf(digest.digest(), 16), "AES");
        } catch (Exception e) {
            throw new IllegalStateException("Password cipher key unavailable", e);
        }
    }
}
