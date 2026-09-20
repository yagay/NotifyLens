package com.yagay.NotifyLens.collector;

import android.app.Notification;
import android.app.Person;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.RemoteViews;
import android.service.notification.StatusBarNotification;

import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.EventTypes;
import com.yagay.NotifyLens.util.AppInfoUtil;
import com.yagay.NotifyLens.util.TextUtil;

import org.json.JSONArray;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class NotificationParser {
    private NotificationParser() {}

    public static EventRecord parse(android.content.Context context, StatusBarNotification sbn) {
        Notification n = sbn.getNotification();
        Bundle e = n.extras == null ? Bundle.EMPTY : n.extras;
        EventRecord r = new EventRecord();
        r.eventType = EventTypes.NOTIFICATION;
        r.source = "notification_listener";
        r.packageName = sbn.getPackageName();
        r.appLabel = AppInfoUtil.label(context, r.packageName);
        r.notificationKey = sbn.getKey();
        r.notificationId = sbn.getId();
        r.notificationTag = sbn.getTag();
        r.postedAt = sbn.getPostTime();
        r.updatedAt = System.currentTimeMillis();
        r.eventKey = r.notificationKey + "@" + r.postedAt;

        r.title = cs(e.getCharSequence(Notification.EXTRA_TITLE));
        r.text = cs(e.getCharSequence(Notification.EXTRA_TEXT));
        r.subText = cs(e.getCharSequence(Notification.EXTRA_SUB_TEXT));
        r.summaryText = cs(e.getCharSequence(Notification.EXTRA_SUMMARY_TEXT));

        String bigText = cs(e.getCharSequence(Notification.EXTRA_BIG_TEXT));
        String lines = joinCharSequences(e.getCharSequenceArray(Notification.EXTRA_TEXT_LINES));
        r.messagesJson = messagesToJson(e);
        String messagesText = messagesToText(e);
        String remoteText = extractRemoteViewsText(context, r.packageName, n.contentView, n.bigContentView, n.headsUpContentView);
        r.fullText = mergeUseful(bigText, messagesText, lines, remoteText, r.text);
        r.rawExtras = bundleToJson(e).toString();
        r.actionsJson = actionsToJson(n).toString();

        r.channelId = n.getChannelId();
        r.groupKey = sbn.getGroupKey();
        r.category = n.category;
        r.flags = n.flags;
        r.ongoing = sbn.isOngoing();
        r.clearable = sbn.isClearable();
        r.foregroundService = (n.flags & Notification.FLAG_FOREGROUND_SERVICE) != 0;
        r.bubble = n.getBubbleMetadata() != null || (n.flags & Notification.FLAG_BUBBLE) != 0;
        r.fullScreen = n.fullScreenIntent != null;
        r.progress = e.getInt(Notification.EXTRA_PROGRESS, 0);
        r.progressMax = e.getInt(Notification.EXTRA_PROGRESS_MAX, 0);
        r.progressIndeterminate = e.getBoolean(Notification.EXTRA_PROGRESS_INDETERMINATE, false);
        r.silent = n.sound == null && n.vibrate == null && (n.defaults & (Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE)) == 0;
        r.notificationKind = classifyNotification(n, r);
        return r;
    }

    private static String classifyNotification(Notification n, EventRecord r) {
        if (r.fullScreen) return "full_screen";
        if (r.bubble) return "bubble";
        if (Notification.CATEGORY_CALL.equals(n.category)) return "call";
        if (Notification.CATEGORY_ALARM.equals(n.category)) return "alarm";
        if (Notification.CATEGORY_TRANSPORT.equals(n.category) || (n.extras != null && n.extras.containsKey(Notification.EXTRA_MEDIA_SESSION))) return "media";
        if (r.progressMax > 0 || r.progressIndeterminate) return "progress";
        if (r.foregroundService) return "foreground_service";
        if (Notification.CATEGORY_MESSAGE.equals(n.category)) return "message";
        if (Notification.CATEGORY_SYSTEM.equals(n.category) || Notification.CATEGORY_STATUS.equals(n.category) || Notification.CATEGORY_SERVICE.equals(n.category)) return "system";
        if (r.ongoing) return "ongoing";
        if (r.silent) return "silent";
        return "standard";
    }

    private static String cs(CharSequence cs) { return cs == null ? null : cs.toString(); }

    private static String firstNonEmpty(String... values) {
        for (String v : values) if (v != null && !v.trim().isEmpty()) return v;
        return null;
    }

    private static String mergeUseful(String... values) {
        java.util.LinkedHashSet<String> parts = new java.util.LinkedHashSet<>();
        for (String v : values) {
            if (v == null) continue;
            String t = v.trim();
            if (t.isEmpty()) continue;
            boolean covered = false;
            for (String existing : new java.util.ArrayList<>(parts)) {
                if (existing.contains(t)) { covered = true; break; }
                if (t.contains(existing)) parts.remove(existing);
            }
            if (!covered) parts.add(t);
        }
        return parts.isEmpty() ? null : String.join("\n", parts);
    }

    private static String extractRemoteViewsText(android.content.Context context, String pkg, RemoteViews... views) {
        java.util.LinkedHashSet<String> out = new java.util.LinkedHashSet<>();
        try {
            android.content.Context packageContext = context.createPackageContext(pkg, android.content.Context.CONTEXT_RESTRICTED);
            for (RemoteViews rv : views) {
                if (rv == null) continue;
                try {
                    FrameLayout parent = new FrameLayout(packageContext);
                    View v = rv.apply(packageContext, parent);
                    String text = TextUtil.collectText(v);
                    if (text != null && !text.isBlank()) out.add(text);
                } catch (Throwable ignored) {}
            }
        } catch (Throwable ignored) {}
        return out.isEmpty() ? null : String.join("\n", out);
    }

    private static String joinCharSequences(CharSequence[] lines) {
        if (lines == null || lines.length == 0) return null;
        List<String> out = new ArrayList<>();
        for (CharSequence c : lines) if (c != null && !c.toString().trim().isEmpty()) out.add(c.toString());
        return out.isEmpty() ? null : String.join("\n", out);
    }

    private static String messagesToText(Bundle extras) {
        try {
            Parcelable[] arr = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
            if (arr == null) return null;
            List<String> out = new ArrayList<>();
            for (Parcelable p : arr) {
                if (!(p instanceof Bundle)) continue;
                Bundle b = (Bundle) p;
                String text = cs(b.getCharSequence("text"));
                String sender = cs(b.getCharSequence("sender"));
                Person person = (Person) b.getParcelable("sender_person");
                if (person != null && person.getName() != null) sender = person.getName().toString();
                if (text != null) out.add((sender == null || sender.isEmpty()) ? text : sender + ": " + text);
            }
            return out.isEmpty() ? null : String.join("\n", out);
        } catch (Throwable ignored) { return null; }
    }

    private static String messagesToJson(Bundle extras) {
        JSONArray out = new JSONArray();
        try {
            Parcelable[] arr = extras.getParcelableArray(Notification.EXTRA_MESSAGES);
            if (arr == null) return out.toString();
            for (Parcelable p : arr) {
                if (!(p instanceof Bundle)) continue;
                Bundle b = (Bundle) p;
                JSONObject o = new JSONObject();
                o.put("text", cs(b.getCharSequence("text")));
                o.put("sender", cs(b.getCharSequence("sender")));
                o.put("time", b.getLong("time", 0L));
                Person person = (Person) b.getParcelable("sender_person");
                if (person != null) o.put("person", person.getName());
                out.put(o);
            }
        } catch (Throwable ignored) {}
        return out.toString();
    }

    private static JSONArray actionsToJson(Notification n) {
        JSONArray out = new JSONArray();
        if (n.actions == null) return out;
        for (Notification.Action a : n.actions) {
            try {
                JSONObject o = new JSONObject();
                o.put("title", a.title == null ? null : a.title.toString());
                o.put("semanticAction", a.getSemanticAction());
                out.put(o);
            } catch (Throwable ignored) {}
        }
        return out;
    }

    public static JSONObject bundleToJson(Bundle bundle) {
        return bundleToJson(bundle, 0);
    }

    private static JSONObject bundleToJson(Bundle bundle, int depth) {
        JSONObject out = new JSONObject();
        if (bundle == null || depth > 5) return out;
        try {
            Set<String> keys = bundle.keySet();
            for (String key : keys) {
                try { out.put(key, jsonValue(bundle.get(key), depth + 1)); }
                catch (Throwable t) { out.put(key, "<unreadable:" + t.getClass().getSimpleName() + ">"); }
            }
        } catch (Throwable ignored) {}
        return out;
    }

    private static Object jsonValue(Object v, int depth) {
        if (v == null) return JSONObject.NULL;
        if (v instanceof Bundle) return bundleToJson((Bundle) v, depth);
        if (v instanceof CharSequence || v instanceof Number || v instanceof Boolean) return String.valueOf(v);
        if (v.getClass().isArray()) {
            JSONArray a = new JSONArray();
            int len = Math.min(Array.getLength(v), 100);
            for (int i = 0; i < len; i++) a.put(jsonValue(Array.get(v, i), depth + 1));
            return a;
        }
        if (v instanceof Iterable<?>) {
            JSONArray a = new JSONArray();
            int n = 0;
            for (Object x : (Iterable<?>) v) { if (n++ >= 100) break; a.put(jsonValue(x, depth + 1)); }
            return a;
        }
        return String.valueOf(v);
    }
}
