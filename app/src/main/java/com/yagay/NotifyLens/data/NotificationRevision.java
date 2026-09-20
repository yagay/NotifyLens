package com.yagay.NotifyLens.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "notification_revisions",
        indices = {
                @Index(value = {"instance_id"}),
                @Index(value = {"instance_id", "sequence"}, unique = true)
        }
)
public class NotificationRevision {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "instance_id")
    public long instanceId;

    public int sequence;

    @ColumnInfo(name = "captured_at")
    public long capturedAt;

    @NonNull
    @ColumnInfo(name = "content_hash")
    public String contentHash = "";

    public String title;
    public String text;

    @ColumnInfo(name = "full_text")
    public String fullText;

    @ColumnInfo(name = "messages_json")
    public String messagesJson;

    public int progress;

    @ColumnInfo(name = "progress_max")
    public int progressMax;

    public int importance;

    @NonNull
    public String source = "";
}
