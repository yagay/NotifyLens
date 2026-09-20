package com.yagay.NotifyLens.ui;

import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.yagay.NotifyLens.R;
import com.yagay.NotifyLens.data.EventRecord;
import com.yagay.NotifyLens.data.CapturePolicy;
import com.yagay.NotifyLens.data.EventStore;
import com.yagay.NotifyLens.data.EventTypes;
import com.yagay.NotifyLens.data.NotifyDatabase;
import com.yagay.NotifyLens.databinding.ActivityAppHistoryBinding;

import java.util.List;

public class AppHistoryActivity extends AppCompatActivity {
    private ActivityAppHistoryBinding b;
    private final EventAdapter adapter = new EventAdapter();
    private LiveData<List<EventRecord>> source;
    private String pkg;
    private String selectedType = "all";

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        b = ActivityAppHistoryBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());
        pkg = getIntent().getStringExtra("package");
        String label = getIntent().getStringExtra("label");
        if (pkg == null) { finish(); return; }
        b.toolbar.setTitle(label == null ? pkg : label);
        b.toolbar.setSubtitle(pkg);
        b.toolbar.setNavigationOnClickListener(v -> finish());
        b.switchIgnore.setChecked(CapturePolicy.isIgnored(this, pkg));
        b.switchRedact.setChecked(CapturePolicy.isRedacted(this, pkg));
        b.switchRedact.setEnabled(!b.switchIgnore.isChecked());
        b.switchIgnore.setOnCheckedChangeListener((button, checked) -> {
            CapturePolicy.setIgnored(this, pkg, checked);
            b.switchRedact.setEnabled(!checked);
        });
        b.switchRedact.setOnCheckedChangeListener((button, checked) -> CapturePolicy.setRedacted(this, pkg, checked));
        b.list.setLayoutManager(new LinearLayoutManager(this));
        b.list.setAdapter(adapter);
        b.chips.setOnCheckedStateChangeListener((group, ids) -> {
            if (!ids.isEmpty()) {
                selectedType = typeForChip(ids.get(0));
                observe();
            }
        });
        b.clearApp.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("清空此应用历史？")
                .setMessage(pkg)
                .setNegativeButton("取消", null)
                .setPositiveButton("清空", (d, w) -> EventStore.io().execute(() -> NotifyDatabase.get(this).eventDao().deletePackage(pkg)))
                .show());
        observe();
    }

    private void observe() {
        if (source != null) source.removeObservers(this);
        source = "all".equals(selectedType)
                ? NotifyDatabase.get(this).eventDao().observePackage(pkg)
                : NotifyDatabase.get(this).eventDao().observePackageType(pkg, selectedType);
        source.observe(this, adapter::submit);
    }

    private String typeForChip(int id) {
        if (id == R.id.chip_notification) return EventTypes.NOTIFICATION;
        if (id == R.id.chip_toast) return EventTypes.TOAST;
        if (id == R.id.chip_dialog) return EventTypes.DIALOG;
        if (id == R.id.chip_popup) return EventTypes.POPUP;
        if (id == R.id.chip_snackbar) return EventTypes.SNACKBAR;
        return "all";
    }
}
