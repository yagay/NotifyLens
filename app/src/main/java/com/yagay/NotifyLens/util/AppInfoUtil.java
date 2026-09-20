package com.yagay.NotifyLens.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.util.LruCache;

public final class AppInfoUtil {
    private static final LruCache<String, Drawable.ConstantState> ICONS = new LruCache<>(96);
    private static final LruCache<String, String> LABELS = new LruCache<>(256);

    private AppInfoUtil() {}

    public static String label(Context context, String pkg) {
        if (pkg == null || pkg.isEmpty()) return "未知应用";
        String cached = LABELS.get(pkg);
        if (cached != null) return cached;
        String result;
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(ai);
            result = label == null ? pkg : label.toString();
        } catch (Throwable ignored) {
            if ("android".equals(pkg)) result = "Android 系统";
            else if ("com.android.systemui".equals(pkg)) result = "System UI";
            else result = pkg;
        }
        LABELS.put(pkg, result);
        return result;
    }

    public static Drawable icon(Context context, String pkg) {
        try {
            Drawable.ConstantState state = ICONS.get(pkg);
            if (state != null) return state.newDrawable(context.getResources());
            Drawable drawable = context.getPackageManager().getApplicationIcon(pkg);
            if (drawable.getConstantState() != null) ICONS.put(pkg, drawable.getConstantState());
            return drawable;
        } catch (Throwable ignored) {
            return context.getApplicationInfo().loadIcon(context.getPackageManager());
        }
    }

    public static void clearCache() {
        ICONS.evictAll();
        LABELS.evictAll();
    }
}
