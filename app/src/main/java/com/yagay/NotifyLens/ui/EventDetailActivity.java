package com.yagay.NotifyLens.ui;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.EventStore;
import com.yagay.NotifyLens.data.NotifyDatabase;
import com.yagay.NotifyLens.databinding.ActivityEventDetailBinding;

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
            EventRecord r = NotifyDatabase.get(this).eventDao().byId(id);
            runOnUiThread(() -> { if (r != null && !isFinishing()) bind(r); });
        });
    }

    private void bind(EventRecord r) {
        b.app.setText(r.appLabel + "\n" + r.packageName);
        StringBuilder m = new StringBuilder();
        line(m, "类型", r.eventType);
        line(m, "来源", r.source);
        line(m, "时间", TimeFormat.full(r.postedAt));
        if (r.removedAt != null) line(m, "移除", TimeFormat.full(r.removedAt));
        line(m, "类", r.className);
        line(m, "Channel", r.channelId);
        line(m, "Notification ID", r.notificationId == 0 ? null : String.valueOf(r.notificationId));
        line(m, "Tag", r.notificationTag);
        line(m, "Key", r.notificationKey);
        line(m, "Group", r.groupKey);
        line(m, "Category", r.category);
        line(m, "通知子类型", r.notificationKind);
        if (r.importance != 0) line(m, "Importance", String.valueOf(r.importance));
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
        b.meta.setText(m.toString().trim());
        b.title.setText(n(r.title));
        b.fullText.setText(n(r.fullText != null ? r.fullText : r.text));
        b.messages.setText(pretty(r.messagesJson));
        b.actions.setText(pretty(r.actionsJson));
        b.raw.setText(pretty(r.rawExtras));
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
