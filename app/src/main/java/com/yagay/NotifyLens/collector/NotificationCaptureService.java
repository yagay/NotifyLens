package com.yagay.NotifyLens.collector;

import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.service.notification.NotificationListenerService.Ranking;
import android.service.notification.NotificationListenerService.RankingMap;

import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.CapturePolicy;
import com.yagay.NotifyLens.data.EventStore;

public class NotificationCaptureService extends NotificationListenerService {
    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        EventStore.cleanup(this, getSharedPreferences("settings", MODE_PRIVATE).getInt("retention_days", 90));
        StatusBarNotification[] active = getActiveNotifications();
        if (active != null) {
            for (StatusBarNotification sbn : active) save(sbn);
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        save(sbn);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        if (sbn != null) EventStore.markRemoved(this, sbn.getKey(), System.currentTimeMillis());
    }

    private void save(StatusBarNotification sbn) {
        if (sbn == null || getPackageName().equals(sbn.getPackageName())) return;
        if (CapturePolicy.isIgnored(this, sbn.getPackageName())) return;
        try {
            EventRecord record = NotificationParser.parse(this, sbn);
            try {
                Ranking ranking = new Ranking();
                RankingMap map = getCurrentRanking();
                if (map != null && map.getRanking(sbn.getKey(), ranking)) {
                    record.importance = ranking.getImportance();
                    record.conversation = ranking.isConversation();
                    record.rankingCanBubble = ranking.canBubble();
                    record.rankingAmbient = ranking.isAmbient();
                    record.rankingSuspended = ranking.isSuspended();
                }
            } catch (Throwable ignored) {}
            if (CapturePolicy.isRedacted(this, record.packageName)) CapturePolicy.redact(record);
            EventStore.save(this, record);
        } catch (Throwable ignored) {}
    }
}
