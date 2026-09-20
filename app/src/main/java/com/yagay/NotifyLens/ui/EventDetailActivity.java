package com.yagay.NotifyLens.ui;

import android.os.Bundle;
import android.service.notification.NotificationListenerService;

import androidx.appcompat.app.AppCompatActivity;

import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.EventStore;
import com.yagay.NotifyLens.data.NotificationRevision;
import com.yagay.NotifyLens.data.NotifyDatabase;
import com.yagay.NotifyLens.databinding.ActivityEventDetailBinding;

import java.util.Collections;
import java.util.List;

public class EventDetailActivity extends AppCompatActivity {
    private ActivityEventDetailBinding b;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        b = ActivityEventDetailBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.toolbar.setNavigationOnClickListener(v -> finish());
        long id = getIntent().getLongExtra("id", -1);
        if (id < 0) { finish(); return; }

        EventStore.io().execute(() -> {
            NotifyDatabase db = NotifyDatabase.get(this);
            EventRecord r = db.eventDao().byId(id);
            List<NotificationRevision> revisions = r != null && r.instanceId != null
                    ? db.historyDao().revisions(r.instanceId)
                    : Collections.emptyList();
            runOnUiThread(() -> { if (r != null && !isFinishing()) bind(r, revisions); });
        });
    }

    private void bind(EventRecord r, List<NotificationRevision> revisions) {
        b.app.setText(r.appLabel + "\n" + r.packageName);
        StringBuilder m = new StringBuilder();
        line(m, "类型", r.eventType);
        line(m, "来源", r.source);
        line(m, "出现", TimeFormat.full(r.postedAt));
        line(m, "最后更新", TimeFormat.full(r.updatedAt));
        if (r.removedAt != null) {
            line(m, "移除", TimeFormat.full(r.removedAt));
            line(m, "移除原因", removalReason(r.removalReason));
        }
        line(m, "类", r.className);
        line(m, "Channel", r.channelId);
        line(m, "Channel 名称", r.channelName);
        line(m, "Channel 描述", r.channelDescription);
        if (r.channelImportance != 0) line(m, "Channel Importance", String.valueOf(r.channelImportance));
        line(m, "Notification ID", r.notificationId == 0 ? null : String.valueOf(r.notificationId));
        line(m, "Tag", r.notificationTag);
        line(m, "Key", r.notificationKey);
        line(m, "Group", r.groupKey);
        if (r.groupSummary) line(m, "Group Summary", "是");
        line(m, "Template", r.template);
        line(m, "Category", r.category);
        line(m, "通知子类型", r.notificationKind);
        if (r.importance != 0) line(m, "Ranking Importance", String.valueOf(r.importance));
        if (r.conversation) line(m, "Conversation", "是");
        if (r.rankingCanBubble) line(m, "Ranking canBubble", "是");
        if (r.rankingAmbient) line(m, "Ambient", "是");
        if (r.rankingSuspended) line(m, "Suspended", "是");
        line(m, "Flags", r.flags == 0 ? null : "0x" + Integer.toHexString(r.flags));
        if (r.progressMax > 0 || r.progressIndeterminate) line(m, "进度", r.progressIndeterminate ? "不确定" : r.progress + "/" + r.progressMax);
        if (r.ongoing) line(m, "持续通知", "是");
        if (r.foregroundService) line(m, "前台服务", "是");
        if (r.bubble) line(m, "气泡", "是");
        if (r.fullScreen) line(m, "全屏 Intent", "是");
        if (r.headsUp) line(m, "实际 Heads-up", "是");
        if (r.payloadSilent) line(m, "Payload 未请求声音/振动", "是");
        if (r.silent) line(m, "最终分类为静默", "是");
        if (r.revisionCount > 0) line(m, "版本数", String.valueOf(r.revisionCount));
        b.meta.setText(m.toString().trim());

        b.title.setText(n(r.title));
        b.fullText.setText(n(r.fullText != null ? r.fullText : r.text));
        b.revisionHistory.setText(formatRevisions(revisions));
        b.messages.setText(pretty(r.messagesJson));
        b.actions.setText(pretty(r.actionsJson));
        b.raw.setText(pretty(r.rawExtras));
    }

    private static String formatRevisions(List<NotificationRevision> revisions) {
        if (revisions == null || revisions.isEmpty()) return "—";
        StringBuilder out = new StringBuilder();
        for (NotificationRevision r : revisions) {
            if (out.length() > 0) out.append("\n\n");
            out.append("#").append(r.sequence)
                    .append("  ").append(TimeFormat.full(r.capturedAt));
            if (r.progressMax > 0) out.append("\n进度：").append(r.progress).append("/").append(r.progressMax);
            if (r.importance != 0) out.append("\nImportance：").append(r.importance);
            String body = r.fullText != null && !r.fullText.isBlank() ? r.fullText : r.text;
            if (r.title != null && !r.title.isBlank()) out.append("\n").append(r.title);
            if (body != null && !body.isBlank()) out.append("\n").append(body);
        }
        return out.toString();
    }

    private static String removalReason(int reason) {
        switch (reason) {
            case NotificationListenerService.REASON_CLICK: return "用户点击";
            case NotificationListenerService.REASON_CANCEL: return "用户划掉/取消";
            case NotificationListenerService.REASON_CANCEL_ALL: return "用户清除全部";
            case NotificationListenerService.REASON_APP_CANCEL: return "App 主动取消";
            case NotificationListenerService.REASON_APP_CANCEL_ALL: return "App 主动取消全部";
            case NotificationListenerService.REASON_TIMEOUT: return "通知超时";
            case NotificationListenerService.REASON_SNOOZED: return "通知被稍后提醒";
            case NotificationListenerService.REASON_CHANNEL_BANNED: return "通知 Channel 被禁用";
            case NotificationListenerService.REASON_PACKAGE_CHANGED: return "应用包状态变化";
            case NotificationListenerService.REASON_USER_STOPPED: return "用户/配置停止";
            case NotificationListenerService.REASON_ASSISTANT_CANCEL: return "通知助理取消";
            default: return reason == 0 ? "未知" : "系统原因 " + reason;
        }
    }

    private static void line(StringBuilder sb, String k, String v) {
        if (v != null && !v.isBlank()) sb.append(k).append(": ").append(v).append('\n');
    }
    private static String n(String s) { return s == null || s.isBlank() ? "—" : s; }
    private static String pretty(String s) {
        if (s == null || s.isBlank()) return "—";
        try {
            String t = s.trim();
            if (t.startsWith("{")) return new org.json.JSONObject(t).toString(2);
            if (t.startsWith("[")) return new org.json.JSONArray(t).toString(2);
        } catch (Throwable ignored) {}
        return s;
    }
}
