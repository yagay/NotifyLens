package com.yagay.NotifyLens.data;

import android.content.Context;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EventStore {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private EventStore() {}

    public static void save(Context context, EventRecord record) {
        IO.execute(() -> NotifyDatabase.get(context).eventDao().upsert(record));
    }

    public static void markRemoved(Context context, String notificationKey, long when) {
        IO.execute(() -> {
            EventDao dao = NotifyDatabase.get(context).eventDao();
            EventRecord r = dao.latestByNotificationKey(notificationKey);
            if (r != null) {
                r.removedAt = when;
                r.updatedAt = Math.max(r.updatedAt, when);
                dao.update(r);
            }
        });
    }

    public static void cleanup(Context context, int days) {
        if (days <= 0) return;
        long cutoff = System.currentTimeMillis() - days * 86_400_000L;
        IO.execute(() -> NotifyDatabase.get(context).eventDao().deleteOlderThan(cutoff));
    }

    public static ExecutorService io() { return IO; }
}
