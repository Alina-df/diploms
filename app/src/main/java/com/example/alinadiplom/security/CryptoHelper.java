package com.example.alinadiplom.security;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public final class CryptoHelper {

    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "DualDormAESKey";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_SIZE = 12;    // рекомендованный размер IV для GCM
    private static final int TAG_SIZE = 128;  // 16 байт * 8

    // Получаем (или создаём) ключ
    private static SecretKey getSecretKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(ANDROID_KEY_STORE);
        ks.load(null);

        if (!ks.containsAlias(KEY_ALIAS)) {
            KeyGenerator kg = KeyGenerator.getInstance(
                    KeyProperties.KEY_ALGORITHM_AES,
                    ANDROID_KEY_STORE
            );
            KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT
            )
                    .setKeySize(256)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .build();
            kg.init(spec);
            kg.generateKey();
        }
        return ((SecretKey) ks.getKey(KEY_ALIAS, null));
    }

    /** Шифруем строку, возвращаем Base‑64 */
    public static String encrypt(String plain) throws Exception {
        if (plain == null) return null;
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());
        byte[] iv = cipher.getIV();                         // 12‑байтовый IV
        byte[] ciphertext = cipher.doFinal(
                plain.getBytes(StandardCharsets.UTF_8));

        // IV + шифр     → Base64
        ByteBuffer bb = ByteBuffer.allocate(iv.length + ciphertext.length);
        bb.put(iv);
        bb.put(ciphertext);
        return Base64.encodeToString(bb.array(), Base64.NO_WRAP);
    }

    /** Дешифруем Base‑64, возвращаем исходную строку */
    public static String decrypt(String encoded) throws Exception {
        if (encoded == null) return null;
        byte[] data = Base64.decode(encoded, Base64.NO_WRAP);
        ByteBuffer bb = ByteBuffer.wrap(data);

        byte[] iv = new byte[IV_SIZE];
        bb.get(iv);
        byte[] ciphertext = new byte[bb.remaining()];
        bb.get(ciphertext);

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        GCMParameterSpec spec = new GCMParameterSpec(TAG_SIZE, iv);
        cipher.init(Cipher.DECRYPT_MODE, getSecretKey(), spec);

        byte[] plainBytes = cipher.doFinal(ciphertext);
        return new String(plainBytes, StandardCharsets.UTF_8);
    }

    private CryptoHelper() {}  // утилитарный класс
}
