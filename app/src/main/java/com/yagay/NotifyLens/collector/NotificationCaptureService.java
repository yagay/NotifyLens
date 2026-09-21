package com.yagay.NotifyLens.collector;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import com.yagay.NotifyLens.data.CapturePolicy;
import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.EventStore;
import com.yagay.NotifyLens.data.ListenerStateStore;
import com.yagay.NotifyLens.util.ContentHasher;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NotificationCaptureService extends NotificationListenerService {
    private static final ExecutorService CAPTURE = Executors.newSingleThreadExecutor();

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        long now = System.currentTimeMillis();
        long disconnectedAt = ListenerStateStore.markConnected(this);
        if (disconnectedAt > 0 && now - disconnectedAt >= 5_000L) {
            EventStore.recordGap(this, disconnectedAt, now, "NotificationListener disconnected");
        }
        EventStore.cleanup(this, getSharedPreferences("settings", MODE_PRIVATE).getInt("retention_days", 90));
        try {
            StatusBarNotification[] active = getActiveNotifications();
            RankingMap rankingMap = getCurrentRanking();
            if (active != null) for (StatusBarNotification sbn : active) scheduleSave(sbn, rankingMap);
        } catch (Throwable ignored) {}
    }

    @Override
    public void onListenerDisconnected() {
        ListenerStateStore.markDisconnected(this);
        super.onListenerDisconnected();
        try {
            requestRebind(new ComponentName(this, NotificationCaptureService.class));
        } catch (Throwable ignored) {}
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn, RankingMap rankingMap) {
        ListenerStateStore.markEvent(this);
        scheduleSave(sbn, rankingMap);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, RankingMap rankingMap, int reason) {
        ListenerStateStore.markEvent(this);
        if (sbn == null) return;
        long when = System.currentTimeMillis();
        String key = sbn.getKey();
        CAPTURE.execute(() -> EventStore.markRemovedBlocking(this, key, when, reason));
    }

    @Override
    public void onNotificationRankingUpdate(RankingMap rankingMap) {
        ListenerStateStore.markEvent(this);
        CAPTURE.execute(() -> {
            try {
                StatusBarNotification[] active = getActiveNotifications();
                if (active != null) for (StatusBarNotification sbn : active) saveBlocking(sbn, rankingMap);
            } catch (Throwable ignored) {}
        });
    }

    private void scheduleSave(StatusBarNotification sbn, RankingMap rankingMap) {
        if (sbn == null) return;
        CAPTURE.execute(() -> saveBlocking(sbn, rankingMap));
    }

    private void saveBlocking(StatusBarNotification sbn, RankingMap rankingMap) {
        if (sbn == null || getPackageName().equals(sbn.getPackageName())) return;
        if (CapturePolicy.isIgnored(this, sbn.getPackageName())) return;
        try {
            EventRecord record = NotificationParser.parse(this, sbn);
            applyRanking(record, sbn, rankingMap);
            record.contentHash = ContentHasher.hash(record);
            if (CapturePolicy.isRedacted(this, record.packageName)) CapturePolicy.redact(record);
            EventStore.saveBlocking(this, record);
        } catch (Throwable ignored) {}
    }

    private void applyRanking(EventRecord record, StatusBarNotification sbn, RankingMap rankingMap) {
        try {
            Ranking ranking = new Ranking();
            RankingMap map = rankingMap != null ? rankingMap : getCurrentRanking();
            if (map != null && map.getRanking(sbn.getKey(), ranking)) {
                record.importance = ranking.getImportance();
                record.conversation = ranking.isConversation();
                record.rankingCanBubble = ranking.canBubble();
                record.rankingAmbient = ranking.isAmbient();
                record.rankingSuspended = ranking.isSuspended();

                NotificationChannel channel = ranking.getChannel();
                if (channel != null) {
                    record.channelId = channel.getId();
                    CharSequence name = channel.getName();
                    record.channelName = name == null ? null : name.toString();
                    record.channelDescription = channel.getDescription();
                    record.channelImportance = channel.getImportance();
                }

                record.silent = record.payloadSilent
                        || ranking.isAmbient()
                        || ranking.getImportance() <= NotificationManager.IMPORTANCE_LOW;
                NotificationParser.reclassify(record);
            }
        } catch (Throwable ignored) {}
    }

    @Override
    public void onDestroy() {
        ListenerStateStore.markDisconnected(this);
        super.onDestroy();
    }
}
