package com.yagay.NotifyLens.data;

import android.content.Context;
import android.content.SharedPreferences;

public final class ListenerStateStore {
    private static final String PREFS = "listener_state";
    private ListenerStateStore() {}

    public static long markConnected(Context context) {
        SharedPreferences sp = prefs(context);
        long previousDisconnect = sp.getLong("last_disconnected", 0L);
        sp.edit()
                .putBoolean("connected", true)
                .putLong("last_connected", System.currentTimeMillis())
                .apply();
        return previousDisconnect;
    }

    public static void markDisconnected(Context context) {
        prefs(context).edit()
                .putBoolean("connected", false)
                .putLong("last_disconnected", System.currentTimeMillis())
                .apply();
    }

    public static void markEvent(Context context) {
        prefs(context).edit().putLong("last_event", System.currentTimeMillis()).apply();
    }

    public static boolean isConnected(Context context) {
        return prefs(context).getBoolean("connected", false);
    }

    public static long lastConnected(Context context) { return prefs(context).getLong("last_connected", 0L); }
    public static long lastDisconnected(Context context) { return prefs(context).getLong("last_disconnected", 0L); }
    public static long lastEvent(Context context) { return prefs(context).getLong("last_event", 0L); }

    private static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
