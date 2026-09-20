package com.yagay.NotifyLens.ui;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.yagay.NotifyLens.data.AppSummary;
import com.yagay.NotifyLens.databinding.ItemAppBinding;
import com.yagay.NotifyLens.util.AppInfoUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.Holder> {
    private final List<AppSummary> all = new ArrayList<>();
    private final List<AppSummary> shown = new ArrayList<>();
    private String query = "";

    public void submit(List<AppSummary> data) {
        all.clear();
        if (data != null) all.addAll(data);
        apply();
    }

    public void setQuery(String q) { query = q == null ? "" : q; apply(); }

    private void apply() {
        shown.clear();
        String q = query.trim().toLowerCase(Locale.ROOT);
        for (AppSummary s : all) {
            String label = s.appLabel == null ? "" : s.appLabel;
            String pkg = s.packageName == null ? "" : s.packageName;
            if (q.isEmpty() || label.toLowerCase(Locale.ROOT).contains(q) || pkg.toLowerCase(Locale.ROOT).contains(q)) shown.add(s);
        }
        notifyDataSetChanged();
    }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new Holder(ItemAppBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override public void onBindViewHolder(@NonNull Holder h, int position) {
        AppSummary s = shown.get(position);
        Context c = h.itemView.getContext();
        h.b.appIcon.setImageDrawable(AppInfoUtil.icon(c, s.packageName));
        h.b.appName.setText(s.appLabel);
        h.b.packageName.setText(s.packageName);
        h.b.count.setText(String.valueOf(s.eventCount));
        h.b.lastTime.setText("最后记录：" + TimeFormat.full(s.lastTime));
        h.itemView.setOnClickListener(v -> {
            Intent i = new Intent(c, AppHistoryActivity.class);
            i.putExtra("package", s.packageName);
            i.putExtra("label", s.appLabel);
            c.startActivity(i);
        });
    }

    @Override public int getItemCount() { return shown.size(); }
    static class Holder extends RecyclerView.ViewHolder {
        final ItemAppBinding b;
        Holder(ItemAppBinding binding) { super(binding.getRoot()); b = binding; }
    }
}
