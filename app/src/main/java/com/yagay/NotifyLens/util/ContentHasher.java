package com.yagay.NotifyLens.util;

import com.yagay.NotifyLens.data.EventRecord;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

public final class ContentHasher {
    private ContentHasher() {}

    public static String hash(EventRecord r) {
        try {
            String raw = s(r.title) + "\n" + s(r.text) + "\n" + s(r.fullText) + "\n"
                    + s(r.messagesJson) + "\n" + r.progress + "/" + r.progressMax + "\n"
                    + r.importance + "\n" + r.channelImportance + "\n" + r.flags;
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder out = new StringBuilder(digest.length * 2);
            for (byte b : digest) out.append(String.format("%02x", b & 0xff));
            return out.toString();
        } catch (Throwable t) {
            return Integer.toHexString((s(r.title) + s(r.fullText) + r.updatedAt).hashCode());
        }
    }

    private static String s(String value) { return value == null ? "" : value; }
}
