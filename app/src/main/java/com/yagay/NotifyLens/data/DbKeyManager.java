package com.yagay.NotifyLens.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.security.KeyStore;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

final class DbKeyManager {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String ALIAS = "notifylens_db_master_v1";
    private static final String PREFS = "db_key";
    private static final String CIPHER_TEXT = "ciphertext";
    private static final String IV = "iv";

    private DbKeyManager() {}

    static byte[] getOrCreate(Context context) {
        try {
            SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
            SecretKey master = getOrCreateMasterKey();
            String ct = sp.getString(CIPHER_TEXT, null);
            String iv = sp.getString(IV, null);
            if (ct != null && iv != null) {
                Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                byte[] ivBytes = Base64.decode(iv, Base64.NO_WRAP);
                cipher.init(Cipher.DECRYPT_MODE, master, new GCMParameterSpec(128, ivBytes));
                return cipher.doFinal(Base64.decode(ct, Base64.NO_WRAP));
            }

            byte[] passphrase = new byte[32];
            new SecureRandom().nextBytes(passphrase);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, master);
            byte[] encrypted = cipher.doFinal(passphrase);
            sp.edit()
                    .putString(CIPHER_TEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
                    .putString(IV, Base64.encodeToString(cipher.getIV(), Base64.NO_WRAP))
                    .commit();
            return passphrase;
        } catch (Exception e) {
            throw new IllegalStateException("Unable to unlock NotifyLens encrypted database", e);
        }
    }

    private static SecretKey getOrCreateMasterKey() throws Exception {
        KeyStore ks = KeyStore.getInstance(KEYSTORE);
        ks.load(null);
        if (ks.containsAlias(ALIAS)) {
            KeyStore.SecretKeyEntry entry = (KeyStore.SecretKeyEntry) ks.getEntry(ALIAS, null);
            return entry.getSecretKey();
        }
        KeyGenerator generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(
                ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }
}
