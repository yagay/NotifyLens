package com.yagay.NotifyLens.data;

import androidx.room.ColumnInfo;

public class AppSummary {
    @ColumnInfo(name = "package_name")
    public String packageName;

    @ColumnInfo(name = "app_label")
    public String appLabel;

    @ColumnInfo(name = "event_count")
    public long eventCount;

    @ColumnInfo(name = "last_time")
    public long lastTime;
}
