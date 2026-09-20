package com.yagay.NotifyLens.ui;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class TimeFormat {
    private static final ThreadLocal<SimpleDateFormat> FULL = ThreadLocal.withInitial(() -> new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()));
    private static final ThreadLocal<SimpleDateFormat> SHORT = ThreadLocal.withInitial(() -> new SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault()));
    static String full(long t) { return FULL.get().format(new Date(t)); }
    static String shortTime(long t) { return SHORT.get().format(new Date(t)); }
}
