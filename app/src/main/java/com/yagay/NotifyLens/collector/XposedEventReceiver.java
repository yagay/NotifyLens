package com.yagay.NotifyLens.collector;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.CapturePolicy;
import com.yagay.NotifyLens.data.EventStore;
import com.yagay.NotifyLens.util.AppInfoUtil;

import java.util.concurrent.ConcurrentHashMap;

public class XposedEventReceiver extends BroadcastReceiver {
    public static final String ACTION = "com.yagay.NotifyLens.XPOSED_EVENT";
    private static final ConcurrentHashMap<String, Long> RECENT = new ConcurrentHashMap<>();

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION.equals(intent.getAction())) return;
        String pkg = safe(intent.getStringExtra("package"));
        String type = safe(intent.getStringExtra("type"));
        String text = safe(intent.getStringExtra("text"));
        if (pkg.isEmpty() || type.isEmpty() || text.isEmpty() || context.getPackageName().equals(pkg) || CapturePolicy.isIgnored(context, pkg)) return;

        long now = System.currentTimeMillis();
        String key = pkg + "|" + type + "|" + text;
        Long previous = RECENT.put(key, now);
        if (previous != null && now - previous < 1200) return;
        if (RECENT.size() > 300) RECENT.entrySet().removeIf(e -> now - e.getValue() > 10_000);

        EventRecord r = new EventRecord();
        r.eventType = type;
        r.source = "lsposed";
        r.packageName = pkg;
        r.appLabel = AppInfoUtil.label(context, pkg);
        r.title = intent.getStringExtra("title");
        r.text = text;
        r.fullText = text;
        r.className = intent.getStringExtra("class");
        r.postedAt = intent.getLongExtra("time", now);
        r.updatedAt = now;
        r.eventKey = "xposed:" + pkg + ":" + type + ":" + r.postedAt + ":" + Integer.toHexString(text.hashCode());
        if (CapturePolicy.isRedacted(context, pkg)) CapturePolicy.redact(r);
        EventStore.save(context, r);
    }

    private static String safe(String s) { return s == null ? "" : s.trim(); }
}
