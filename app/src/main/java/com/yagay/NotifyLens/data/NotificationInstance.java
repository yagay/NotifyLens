package com.yagay.NotifyLens.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notification_instances",
        indices = {
                @Index(value = {"instance_key"}, unique = true),
                @Index(value = {"notification_key"}),
                @Index(value = {"package_name"})
        }
)
public class NotificationInstance {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    @ColumnInfo(name = "instance_key")
    public String instanceKey = "";

    @ColumnInfo(name = "notification_key")
    public String notificationKey;

    @NonNull
    @ColumnInfo(name = "package_name")
    public String packageName = "";

    @ColumnInfo(name = "first_seen")
    public long firstSeen;

    @ColumnInfo(name = "last_seen")
    public long lastSeen;

    @ColumnInfo(name = "removed_at")
    public Long removedAt;

    @ColumnInfo(name = "removal_reason")
    public int removalReason;

    @ColumnInfo(name = "channel_id")
    public String channelId;

    @ColumnInfo(name = "current_revision")
    public int currentRevision;
}
