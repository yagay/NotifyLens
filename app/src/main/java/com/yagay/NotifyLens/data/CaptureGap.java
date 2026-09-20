package com.yagay.NotifyLens.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "capture_gaps")
public class CaptureGap {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "disconnected_at")
    public long disconnectedAt;

    @ColumnInfo(name = "reconnected_at")
    public long reconnectedAt;

    @ColumnInfo(name = "duration_ms")
    public long durationMs;

    public String reason;
}
