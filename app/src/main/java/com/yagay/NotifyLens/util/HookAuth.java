package com.yagay.NotifyLens.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HookAuth {
    private static final String PREFS = "hook_auth";
    private static final String KEY_SECRET = "secret_v1";

    private HookAuth() {}

    public static String ensureLocalSecret(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String existing = sp.getString(KEY_SECRET, null);
        if (existing != null && !existing.isEmpty()) return existing;
        byte[] raw = new byte[32];
        new SecureRandom().nextBytes(raw);
        String value = Base64.encodeToString(raw, Base64.NO_WRAP);
        if (!sp.edit().putString(KEY_SECRET, value).commit()) {
            throw new IllegalStateException("Unable to persist hook authentication secret");
        }
        return value;
    }

    public static String localSecret(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_SECRET, null);
    }

    public static String sign(String secret, String pkg, String kind, String type,
                              String text, String className, String notificationKey,
                              long time, String nonce) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.decode(secret, Base64.NO_WRAP), "HmacSHA256"));
            byte[] result = mac.doFinal(canonical(pkg, kind, type, text, className, notificationKey, time, nonce)
                    .getBytes(StandardCharsets.UTF_8));
            return Base64.encodeToString(result, Base64.NO_WRAP);
        } catch (Throwable t) {
            return "";
        }
    }

    public static boolean verify(String secret, String signature, String pkg, String kind,
                                 String type, String text, String className,
                                 String notificationKey, long time, String nonce) {
        if (secret == null || signature == null || signature.isEmpty()) return false;
        String expected = sign(secret, pkg, kind, type, text, className, notificationKey, time, nonce);
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8),
                signature.getBytes(StandardCharsets.UTF_8));
    }

    private static String canonical(String pkg, String kind, String type, String text,
                                    String className, String notificationKey,
                                    long time, String nonce) {
        return safe(pkg) + "\n" + safe(kind) + "\n" + safe(type) + "\n" + safe(text)
                + "\n" + safe(className) + "\n" + safe(notificationKey)
                + "\n" + time + "\n" + safe(nonce);
    }

    private static String safe(String value) { return value == null ? "" : value; }
}
