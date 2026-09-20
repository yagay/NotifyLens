package com.yagay.NotifyLens.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

public final class AppInfoUtil {
    private AppInfoUtil() {}

    public static String label(Context context, String pkg) {
        if (pkg == null || pkg.isEmpty()) return "未知应用";
        try {
            PackageManager pm = context.getPackageManager();
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            CharSequence label = pm.getApplicationLabel(ai);
            return label == null ? pkg : label.toString();
        } catch (Throwable ignored) {
            if ("android".equals(pkg)) return "Android 系统";
            if ("com.android.systemui".equals(pkg)) return "System UI";
            return pkg;
        }
    }

    public static Drawable icon(Context context, String pkg) {
        try {
            return context.getPackageManager().getApplicationIcon(pkg);
        } catch (Throwable ignored) {
            return context.getApplicationInfo().loadIcon(context.getPackageManager());
        }
    }
}
