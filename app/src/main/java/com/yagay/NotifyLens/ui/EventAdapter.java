package com.yagay.NotifyLens.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.EventTypes;
import com.yagay.NotifyLens.databinding.ItemEventBinding;
import com.yagay.NotifyLens.util.AppInfoUtil;

import java.util.ArrayList;
import java.util.List;

public class EventAdapter extends RecyclerView.Adapter<EventAdapter.Holder> {
    private final List<EventRecord> items = new ArrayList<>();

    public void submit(List<EventRecord> data) {
        items.clear();
        if (data != null) items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemEventBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        EventRecord r = items.get(position);
        Context c = h.itemView.getContext();
        h.b.appIcon.setImageDrawable(AppInfoUtil.icon(c, r.packageName));
        h.b.appName.setText(r.appLabel);
        h.b.type.setText(typeLabel(r));
        h.b.title.setText(nonEmpty(r.title, r.className, ""));
        h.b.title.setVisibility(h.b.title.getText().length() == 0 ? View.GONE : View.VISIBLE);
        h.b.text.setText(nonEmpty(r.fullText, r.text, ""));
        String life = r.removedAt == null ? "" : "  · 已移除 " + TimeFormat.shortTime(r.removedAt);
        h.b.time.setText(TimeFormat.full(r.postedAt) + "  · " + r.source + life);
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(c, EventDetailActivity.class);
            i.putExtra("id", r.id);
            c.startActivity(i);
        });
    }

    @Override public int getItemCount() { return items.size(); }

    private static String typeLabel(EventRecord r) {
        if (EventTypes.NOTIFICATION.equals(r.eventType)) {
            String k = r.notificationKind == null ? "standard" : r.notificationKind;
            switch (k) {
                case "full_screen": return "全屏通知";
                case "bubble": return "气泡";
                case "call": return "来电";
                case "alarm": return "闹钟";
                case "media": return "媒体";
                case "progress": return "进度";
                case "foreground_service": return "前台服务";
                case "message": return "消息";
                case "system": return "系统通知";
                case "ongoing": return "持续通知";
                case "silent": return "静默通知";
                default: return "通知";
            }
        }
        switch (r.eventType) {
            case EventTypes.TOAST: return "Toast";
            case EventTypes.DIALOG: return "Dialog";
            case EventTypes.SNACKBAR: return "Snackbar";
            case EventTypes.POPUP: return "Popup";
            case EventTypes.SYSTEM_UI: return "SystemUI";
            default: return "界面提示";
        }
    }

    private static String nonEmpty(String... s) {
        for (String x : s) if (x != null && !x.trim().isEmpty()) return x;
        return "";
    }

    static class Holder extends RecyclerView.ViewHolder {
        final ItemEventBinding b;
        Holder(ItemEventBinding binding) { super(binding.getRoot()); b = binding; }
    }
}
