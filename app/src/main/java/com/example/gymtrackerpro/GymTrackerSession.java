package com.example.gymtrackerpro;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class GymTrackerSession {

    private static final String PREFS_NAME = "GymTrackerSession";
    private static final String TOKEN_KEY = "accessToken";
    private static final String EMAIL_KEY = "email";
    private static final String ACCOUNT_ID_KEY = "accountId";

    private static final String KEY_ALIAS = "GymTrackerTokenKey";
    private static final String ANDROID_KEYSTORE = "AndroidKeyStore";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final String ENCRYPTED_PREFIX = "ENC:";

    private static SecretKey getSecretKey() throws Exception {

        KeyStore keyStore = KeyStore.getInstance(ANDROID_KEYSTORE);
        keyStore.load(null);

        if (keyStore.containsAlias(KEY_ALIAS)) {
            return (SecretKey) keyStore.getKey(KEY_ALIAS, null);
        }

        KeyGenerator keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
        );

        KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT
                        | KeyProperties.PURPOSE_DECRYPT
        )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(
                        KeyProperties.ENCRYPTION_PADDING_NONE
                )
                .setRandomizedEncryptionRequired(true)
                .build();

        keyGenerator.init(spec);

        return keyGenerator.generateKey();
    }

    public static void saveSession(
            Context context,
            String accessToken,
            String email,
            String accountId
    ) throws Exception {

        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        cipher.init(Cipher.ENCRYPT_MODE, getSecretKey());

        byte[] encryptedBytes = cipher.doFinal(
                accessToken.getBytes(StandardCharsets.UTF_8)
        );

        byte[] iv = cipher.getIV();

        byte[] combined = new byte[iv.length + encryptedBytes.length];

        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(
                encryptedBytes,
                0,
                combined,
                iv.length,
                encryptedBytes.length
        );

        String encryptedToken = ENCRYPTED_PREFIX
                + Base64.encodeToString(combined, Base64.NO_WRAP);

        getPreferences(context).edit()
                .putString(TOKEN_KEY, encryptedToken)
                .putString(EMAIL_KEY, email)
                .putString(ACCOUNT_ID_KEY, accountId)
                .apply();
    }

    public static String getAccessToken(Context context) {

        String storedToken = getPreferences(context)
                .getString(TOKEN_KEY, null);

        if (storedToken == null || storedToken.trim().isEmpty()) {
            return null;
        }

        // Support migration from an older plaintext token.
        if (!storedToken.startsWith(ENCRYPTED_PREFIX)) {
            try {
                saveSession(
                        context,
                        storedToken,
                        getEmail(context),
                        getAccountId(context)
                );
            } catch (Exception e) {
                return null;
            }

            return storedToken;
        }

        try {
            String encoded = storedToken.substring(
                    ENCRYPTED_PREFIX.length()
            );

            byte[] combined = Base64.decode(encoded, Base64.NO_WRAP);

            int ivLength = 12;

            if (combined.length <= ivLength) {
                return null;
            }

            byte[] iv = new byte[ivLength];
            byte[] encryptedBytes =
                    new byte[combined.length - ivLength];

            System.arraycopy(combined, 0, iv, 0, ivLength);
            System.arraycopy(
                    combined,
                    ivLength,
                    encryptedBytes,
                    0,
                    encryptedBytes.length
            );

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);

            cipher.init(
                    Cipher.DECRYPT_MODE,
                    getSecretKey(),
                    new GCMParameterSpec(128, iv)
            );

            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(
                    decryptedBytes,
                    StandardCharsets.UTF_8
            );

        } catch (Exception e) {
            clearSession(context);
            return null;
        }
    }

    public static String getEmail(Context context) {
        return getPreferences(context).getString(EMAIL_KEY, null);
    }

    public static String getAccountId(Context context) {
        return getPreferences(context).getString(ACCOUNT_ID_KEY, null);
    }

    public static void clearSession(Context context) {

        getPreferences(context).edit()
                .remove(TOKEN_KEY)
                .remove(EMAIL_KEY)
                .remove(ACCOUNT_ID_KEY)
                .apply();
    }

    private static SharedPreferences getPreferences(Context context) {
        return context.getApplicationContext().getSharedPreferences(
                PREFS_NAME,
                Context.MODE_PRIVATE
        );
    }
}
