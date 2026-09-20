package com.yagay.NotifyLens.ui;

import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yagay.NotifyLens.R;
import com.yagay.NotifyLens.data.AppSummary;
import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.EventStore;
import com.yagay.NotifyLens.data.EventTypes;
import com.yagay.NotifyLens.data.NotifyDatabase;
import com.yagay.NotifyLens.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding b;
    private final EventAdapter eventAdapter = new EventAdapter();
    private final AppAdapter appAdapter = new AppAdapter();
    private LiveData<List<EventRecord>> eventSource;
    private LiveData<List<AppSummary>> appSource;
    private String selectedType = "all";
    private int mode = R.id.nav_timeline;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        b = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        b.list.setLayoutManager(new LinearLayoutManager(this));
        b.list.setAdapter(eventAdapter);

        b.btnNotificationAccess.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)));
        b.btnAccessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));

        b.bottomNav.setOnItemSelectedListener(item -> {
            mode = item.getItemId();
            renderMode();
            return true;
        });

        b.chipGroup.setOnCheckedStateChangeListener((group, ids) -> {
            if (ids.isEmpty()) return;
            selectedType = typeForChip(ids.get(0));
            observeTimeline();
        });

        b.searchEdit.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            public void onTextChanged(CharSequence s, int st, int before, int count) {
                if (mode == R.id.nav_timeline) observeTimeline();
                else if (mode == R.id.nav_apps) appAdapter.setQuery(s.toString());
            }
            public void afterTextChanged(Editable e) {}
        });

        setupRetention();
        b.btnClearAll.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("清空全部历史？")
                .setMessage("此操作不可撤销。")
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (d, w) -> EventStore.io().execute(() -> NotifyDatabase.get(this).eventDao().deleteAll()))
                .show());

        renderMode();
        observeTimeline();
    }

    @Override protected void onResume() {
        super.onResume();
        updatePermissionStatus();
    }

    private void updatePermissionStatus() {
        boolean nl = NotificationManagerCompat.getEnabledListenerPackages(this).contains(getPackageName());
        b.btnNotificationAccess.setText(nl ? "通知权限 ✓" : "开启通知权限");
        String enabled = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        boolean a11y = enabled != null && enabled.toLowerCase().contains(getPackageName().toLowerCase());
        b.btnAccessibility.setText(a11y ? "界面提示 ✓" : "开启界面提示");
        if (mode == R.id.nav_settings) b.captureStatus.setVisibility(View.VISIBLE);
        else if (mode == R.id.nav_timeline) b.captureStatus.setVisibility((nl && a11y) ? View.GONE : View.VISIBLE);
        else b.captureStatus.setVisibility(View.GONE);
    }

    private void renderMode() {
        boolean settings = mode == R.id.nav_settings;
        boolean apps = mode == R.id.nav_apps;
        b.settingsPanel.setVisibility(settings ? View.VISIBLE : View.GONE);
        b.list.setVisibility(settings ? View.GONE : View.VISIBLE);
        b.searchBox.setVisibility(settings ? View.GONE : View.VISIBLE);
        b.filterScroll.setVisibility(mode == R.id.nav_timeline ? View.VISIBLE : View.GONE);
        updatePermissionStatus();
        if (apps) {
            b.toolbar.setTitle("按应用查看");
            b.searchBox.setHint("搜索应用或包名");
            b.list.setAdapter(appAdapter);
            observeApps();
        } else if (!settings) {
            b.toolbar.setTitle("NotifyLens");
            b.searchBox.setHint("搜索应用、标题、内容或包名");
            b.list.setAdapter(eventAdapter);
            observeTimeline();
        } else {
            b.toolbar.setTitle("设置");
        }
    }

    private void observeTimeline() {
        if (mode != R.id.nav_timeline) return;
        if (eventSource != null) eventSource.removeObservers(this);
        String q = b.searchEdit.getText() == null ? "" : b.searchEdit.getText().toString().trim();
        if (!q.isEmpty()) eventSource = NotifyDatabase.get(this).eventDao().search(q);
        else if ("all".equals(selectedType)) eventSource = NotifyDatabase.get(this).eventDao().observeAll();
        else eventSource = NotifyDatabase.get(this).eventDao().observeType(selectedType);
        eventSource.observe(this, list -> {
            if (q.isEmpty() || "all".equals(selectedType)) eventAdapter.submit(list);
            else {
                List<EventRecord> filtered = new ArrayList<>();
                if (list != null) for (EventRecord r : list) if (selectedType.equals(r.eventType)) filtered.add(r);
                eventAdapter.submit(filtered);
            }
        });
    }

    private void observeApps() {
        if (appSource != null) return;
        appSource = NotifyDatabase.get(this).eventDao().observeApps();
        appSource.observe(this, list -> {
            appAdapter.submit(list);
            appAdapter.setQuery(b.searchEdit.getText() == null ? "" : b.searchEdit.getText().toString());
        });
    }

    private String typeForChip(int id) {
        if (id == R.id.chip_notification) return EventTypes.NOTIFICATION;
        if (id == R.id.chip_toast) return EventTypes.TOAST;
        if (id == R.id.chip_dialog) return EventTypes.DIALOG;
        if (id == R.id.chip_popup) return EventTypes.POPUP;
        if (id == R.id.chip_snackbar) return EventTypes.SNACKBAR;
        return "all";
    }

    private void setupRetention() {
        int days = getSharedPreferences("settings", MODE_PRIVATE).getInt("retention_days", 90);
        int check = days == 7 ? R.id.retention_7 : days == 30 ? R.id.retention_30 : days == 0 ? R.id.retention_forever : R.id.retention_90;
        b.retentionGroup.check(check);
        b.retentionGroup.setOnCheckedChangeListener((group, id) -> {
            int d = id == R.id.retention_7 ? 7 : id == R.id.retention_30 ? 30 : id == R.id.retention_forever ? 0 : 90;
            getSharedPreferences("settings", MODE_PRIVATE).edit().putInt("retention_days", d).apply();
            EventStore.cleanup(this, d);
        });
    }
}
