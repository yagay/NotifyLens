package com.yagay.NotifyLens.collector;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import com.yagay.NotifyLens.data.CapturePolicy;
import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.EventStore;
import com.yagay.NotifyLens.data.EventTypes;
import com.yagay.NotifyLens.util.AppInfoUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class UiAccessibilityService extends AccessibilityService {
    private final ConcurrentHashMap<String, Long> recent = new ConcurrentHashMap<>();

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null) return;
        String pkg = event.getPackageName().toString();
        if (getPackageName().equals(pkg) || CapturePolicy.isIgnored(this, pkg)) return;

        String className = event.getClassName() == null ? "" : event.getClassName().toString();
        String sourceClass = "";
        try {
            AccessibilityNodeInfo src = event.getSource();
            if (src != null && src.getClassName() != null) sourceClass = src.getClassName().toString();
        } catch (Throwable ignored) {}

        String text = join(event.getText());
        if ((text == null || text.isBlank()) && event.getContentDescription() != null) {
            text = event.getContentDescription().toString();
        }
        if (text == null || text.isBlank()) return;

        String type = classify(event, className, sourceClass);
        if (type == null) return;

        long now = System.currentTimeMillis();
        String dedupe = pkg + "|" + event.getEventType() + "|" + type + "|" + text;
        Long last = recent.put(dedupe, now);
        if (last != null && now - last < 300L) return;
        if (recent.size() > 300) recent.entrySet().removeIf(e -> now - e.getValue() > 5_000L);

        EventRecord r = new EventRecord();
        r.eventType = type;
        r.source = "accessibility";
        r.packageName = pkg;
        r.appLabel = AppInfoUtil.label(this, pkg);
        r.text = text;
        r.fullText = text;
        r.className = !className.isBlank() ? className : sourceClass;
        r.postedAt = now;
        r.updatedAt = now;
        r.eventKey = "a11y:" + pkg + ":" + type + ":" + now + ":" + Integer.toHexString(text.hashCode());
        if (CapturePolicy.isRedacted(this, pkg)) CapturePolicy.redact(r);
        EventStore.save(this, r);
    }

    private String classify(AccessibilityEvent event, String className, String sourceClass) {
        String c = (className + " " + sourceClass).toLowerCase();
        int t = event.getEventType();
        if (t == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            if (event.getParcelableData() instanceof Notification) return null;
            return EventTypes.TOAST;
        }
        if (c.contains("snackbar")) return EventTypes.SNACKBAR;
        if (c.contains("toast")) return EventTypes.TOAST;
        if (c.contains("popup")) return EventTypes.POPUP;
        if (c.contains("dialog") || c.contains("alertdialog")) return EventTypes.DIALOG;
        if (t == AccessibilityEvent.TYPE_ANNOUNCEMENT) return EventTypes.OTHER_UI;
        return null;
    }

    private static String join(List<CharSequence> list) {
        if (list == null || list.isEmpty()) return null;
        StringBuilder sb = new StringBuilder();
        for (CharSequence c : list) {
            if (c == null || c.toString().isBlank()) continue;
            if (sb.length() > 0) sb.append("\n");
            sb.append(c);
        }
        return sb.length() == 0 ? null : sb.toString();
    }

    @Override public void onInterrupt() {}
}
