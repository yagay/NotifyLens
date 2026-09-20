package com.yagay.NotifyLens.data;

import android.content.Context;

import com.yagay.NotifyLens.util.ContentHasher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class EventStore {
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private EventStore() {}

    public static void save(Context context, EventRecord record) {
        IO.execute(() -> saveBlocking(context.getApplicationContext(), record));
    }

    public static void saveBlocking(Context context, EventRecord record) {
        NotifyDatabase db = NotifyDatabase.get(context);
        EventDao dao = db.eventDao();

        if (EventTypes.NOTIFICATION.equals(record.eventType)) {
            saveNotification(db, record);
            return;
        }

        String body = record.fullText != null ? record.fullText : record.text;
        if (body != null && !body.isBlank()) {
            EventRecord crossSource = dao.recentEquivalentFromOtherSource(
                    record.packageName, record.eventType, body, record.source, record.postedAt - 1_000L);
            if (crossSource != null) {
                crossSource.updatedAt = Math.max(crossSource.updatedAt, record.updatedAt);
                if (!crossSource.source.contains(record.source)) {
                    crossSource.source = crossSource.source + "+" + record.source;
                }
                if (record.className != null && !record.className.isBlank()) {
                    crossSource.className = record.className;
                }
                dao.update(crossSource);
                syncFts(db, crossSource);
                return;
            }
        }

        record.id = dao.insert(record);
        syncFts(db, record);
    }

    private static void saveNotification(NotifyDatabase db, EventRecord incoming) {
        EventDao dao = db.eventDao();
        HistoryDao history = db.historyDao();

        if (incoming.contentHash == null || incoming.contentHash.isBlank()) {
            incoming.contentHash = ContentHasher.hash(incoming);
        }

        NotificationInstance instance = history.instanceByKey(incoming.eventKey);
        if (instance == null) {
            instance = new NotificationInstance();
            instance.instanceKey = incoming.eventKey;
            instance.notificationKey = incoming.notificationKey;
            instance.packageName = incoming.packageName;
            instance.firstSeen = incoming.postedAt;
            instance.lastSeen = incoming.updatedAt;
            instance.channelId = incoming.channelId;
            instance.currentRevision = 0;
            instance.id = history.insertInstance(instance);
        } else {
            instance.lastSeen = Math.max(instance.lastSeen, incoming.updatedAt);
            instance.channelId = incoming.channelId;
        }

        NotificationRevision latest = history.latestRevision(instance.id);
        if (latest == null || !incoming.contentHash.equals(latest.contentHash)) {
            NotificationRevision revision = new NotificationRevision();
            revision.instanceId = instance.id;
            revision.sequence = instance.currentRevision + 1;
            revision.capturedAt = incoming.updatedAt;
            revision.contentHash = incoming.contentHash;
            revision.title = incoming.title;
            revision.text = incoming.text;
            revision.fullText = incoming.fullText;
            revision.messagesJson = incoming.messagesJson;
            revision.progress = incoming.progress;
            revision.progressMax = incoming.progressMax;
            revision.importance = incoming.importance;
            revision.source = incoming.source;
            long id = history.insertRevision(revision);
            if (id > 0) instance.currentRevision = revision.sequence;
        }
        history.updateInstance(instance);

        incoming.instanceId = instance.id;
        incoming.revisionCount = instance.currentRevision;

        EventRecord existing = dao.byEventKey(incoming.eventKey);
        if (existing == null) {
            incoming.id = dao.insert(incoming);
        } else {
            incoming.id = existing.id;
            incoming.removedAt = existing.removedAt;
            incoming.removalReason = existing.removalReason;
            incoming.headsUp = incoming.headsUp || existing.headsUp;
            dao.update(incoming);
        }
        syncFts(db, incoming);
    }

    public static void markRemoved(Context context, String notificationKey, long when, int reason) {
        IO.execute(() -> {
            NotifyDatabase db = NotifyDatabase.get(context);
            EventDao dao = db.eventDao();
            EventRecord r = dao.latestByNotificationKey(notificationKey);
            if (r != null) {
                r.removedAt = when;
                r.removalReason = reason;
                r.updatedAt = Math.max(r.updatedAt, when);
                dao.update(r);
                syncFts(db, r);
            }
            NotificationInstance instance = db.historyDao().latestInstanceByNotificationKey(notificationKey);
            if (instance != null) {
                instance.removedAt = when;
                instance.removalReason = reason;
                instance.lastSeen = Math.max(instance.lastSeen, when);
                db.historyDao().updateInstance(instance);
            }
        });
    }

    public static void markHeadsUp(Context context, String notificationKey, long when) {
        IO.execute(() -> {
            NotifyDatabase db = NotifyDatabase.get(context);
            EventRecord r = db.eventDao().latestByNotificationKey(notificationKey);
            if (r != null) {
                r.headsUp = true;
                r.updatedAt = Math.max(r.updatedAt, when);
                db.eventDao().update(r);
            }
        });
    }

    public static void recordGap(Context context, long disconnectedAt, long reconnectedAt, String reason) {
        if (disconnectedAt <= 0 || reconnectedAt <= disconnectedAt) return;
        IO.execute(() -> {
            CaptureGap gap = new CaptureGap();
            gap.disconnectedAt = disconnectedAt;
            gap.reconnectedAt = reconnectedAt;
            gap.durationMs = reconnectedAt - disconnectedAt;
            gap.reason = reason;
            NotifyDatabase.get(context).historyDao().insertGap(gap);
        });
    }

    public static void cleanup(Context context, int days) {
        if (days <= 0) return;
        long cutoff = System.currentTimeMillis() - days * 86_400_000L;
        IO.execute(() -> {
            NotifyDatabase db = NotifyDatabase.get(context);
            db.historyDao().deleteOldRevisions(cutoff);
            db.historyDao().deleteOldInstances(cutoff);
            db.eventDao().deleteOlderThan(cutoff);
            db.eventFtsDao().prune();
        });
    }

    public static void deletePackage(Context context, String packageName) {
        IO.execute(() -> {
            NotifyDatabase db = NotifyDatabase.get(context);
            db.historyDao().deleteRevisionsForPackage(packageName);
            db.historyDao().deleteInstancesForPackage(packageName);
            db.eventDao().deletePackage(packageName);
            db.eventFtsDao().prune();
        });
    }

    public static void clearAll(Context context) {
        IO.execute(() -> {
            NotifyDatabase db = NotifyDatabase.get(context);
            db.historyDao().deleteAllRevisions();
            db.historyDao().deleteAllInstances();
            db.historyDao().deleteAllGaps();
            db.eventDao().deleteAll();
            db.eventFtsDao().clear();
        });
    }

    private static void syncFts(NotifyDatabase db, EventRecord r) {
        if (r.id <= 0 || r.id > Integer.MAX_VALUE) return;
        try { db.eventFtsDao().upsert(EventFts.from(r)); } catch (Throwable ignored) {}
    }

    public static ExecutorService io() { return IO; }
}
