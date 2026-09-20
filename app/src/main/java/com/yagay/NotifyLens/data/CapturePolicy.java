package com.yagay.NotifyLens.data;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.HashSet;
import java.util.Set;

public final class CapturePolicy {
    private static final String PREFS = "capture_policy";
    private static final String IGNORED = "ignored_packages";
    private static final String REDACTED = "redacted_packages";

    private CapturePolicy() {}

    public static boolean isIgnored(Context context, String pkg) {
        return prefs(context).getStringSet(IGNORED, Set.of()).contains(pkg);
    }

    public static boolean isRedacted(Context context, String pkg) {
        return prefs(context).getStringSet(REDACTED, Set.of()).contains(pkg);
    }

    public static void setIgnored(Context context, String pkg, boolean value) {
        mutate(context, IGNORED, pkg, value);
    }

    public static void setRedacted(Context context, String pkg, boolean value) {
        mutate(context, REDACTED, pkg, value);
    }

    public static void redact(EventRecord r) {
        r.title = "[内容已隐藏]";
        r.text = null;
        r.fullText = null;
        r.subText = null;
        r.summaryText = null;
        r.rawExtras = null;
        r.messagesJson = null;
        r.actionsJson = null;
    }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    private static void mutate(Context context, String key, String pkg, boolean add) {
        SharedPreferences sp = prefs(context);
        Set<String> copy = new HashSet<>(sp.getStringSet(key, Set.of()));
        if (add) copy.add(pkg); else copy.remove(pkg);
        sp.edit().putStringSet(key, copy).apply();
    }
}
