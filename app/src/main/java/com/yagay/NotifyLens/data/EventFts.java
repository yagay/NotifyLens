package com.yagay.NotifyLens.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Fts4;
import androidx.room.PrimaryKey;

@Fts4
@Entity(tableName = "events_fts")
public class EventFts {
    @PrimaryKey
    @ColumnInfo(name = "rowid")
    public int rowId;

    public String title;
    public String text;

    @ColumnInfo(name = "full_text")
    public String fullText;

    @ColumnInfo(name = "app_label")
    public String appLabel;

    @ColumnInfo(name = "package_name")
    public String packageName;

    public static EventFts from(EventRecord r) {
        EventFts f = new EventFts();
        f.rowId = (int) r.id;
        f.title = r.title;
        f.text = r.text;
        f.fullText = r.fullText;
        f.appLabel = r.appLabel;
        f.packageName = r.packageName;
        return f;
    }
}
