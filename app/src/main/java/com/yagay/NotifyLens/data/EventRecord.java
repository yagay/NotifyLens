package com.yagay.NotifyLens.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
    tableName = "events",
    indices = {
        @Index(value = {"event_key"}, unique = true),
        @Index(value = {"package_name"}),
        @Index(value = {"event_type"}),
        @Index(value = {"posted_at"}),
        @Index(value = {"notification_key"})
    }
)
public class EventRecord {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull @ColumnInfo(name = "event_key")
    public String eventKey = "";

    @NonNull @ColumnInfo(name = "event_type")
    public String eventType = EventTypes.NOTIFICATION;

    @NonNull @ColumnInfo(name = "source")
    public String source = "normal";

    @NonNull @ColumnInfo(name = "package_name")
    public String packageName = "";

    @NonNull @ColumnInfo(name = "app_label")
    public String appLabel = "";

    public String title;
    public String text;

    @ColumnInfo(name = "full_text")
    public String fullText;

    @ColumnInfo(name = "sub_text")
    public String subText;

    @ColumnInfo(name = "summary_text")
    public String summaryText;

    @ColumnInfo(name = "raw_extras")
    public String rawExtras;

    @ColumnInfo(name = "messages_json")
    public String messagesJson;

    @ColumnInfo(name = "actions_json")
    public String actionsJson;

    @ColumnInfo(name = "posted_at")
    public long postedAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    @ColumnInfo(name = "removed_at")
    public Long removedAt;

    @ColumnInfo(name = "notification_key")
    public String notificationKey;

    @ColumnInfo(name = "notification_id")
    public int notificationId;

    @ColumnInfo(name = "notification_tag")
    public String notificationTag;

    @ColumnInfo(name = "channel_id")
    public String channelId;

    @ColumnInfo(name = "group_key")
    public String groupKey;

    public String category;

    @ColumnInfo(name = "notification_kind")
    public String notificationKind;

    public int importance;
    public boolean conversation;

    @ColumnInfo(name = "ranking_can_bubble")
    public boolean rankingCanBubble;

    @ColumnInfo(name = "ranking_ambient")
    public boolean rankingAmbient;

    @ColumnInfo(name = "ranking_suspended")
    public boolean rankingSuspended;
    public int flags;
    public boolean ongoing;

    @ColumnInfo(name = "foreground_service")
    public boolean foregroundService;

    public boolean clearable;
    public boolean bubble;

    @ColumnInfo(name = "full_screen")
    public boolean fullScreen;

    public boolean silent;
    public int progress;

    @ColumnInfo(name = "progress_max")
    public int progressMax;

    @ColumnInfo(name = "progress_indeterminate")
    public boolean progressIndeterminate;

    @ColumnInfo(name = "class_name")
    public String className;
}
