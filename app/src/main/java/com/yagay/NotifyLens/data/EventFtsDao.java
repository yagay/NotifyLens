package com.yagay.NotifyLens.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface EventFtsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void upsert(EventFts value);

    @Query("DELETE FROM events_fts WHERE rowid = :rowId")
    void delete(int rowId);

    @Query("DELETE FROM events_fts")
    void clear();

    @Query("INSERT INTO events_fts(rowid, title, text, full_text, app_label, package_name) SELECT id, title, text, full_text, app_label, package_name FROM events WHERE id NOT IN (SELECT rowid FROM events_fts)")
    void backfillMissing();

    @Query("DELETE FROM events_fts WHERE rowid NOT IN (SELECT id FROM events)")
    void prune();

    @Query("SELECT events.* FROM events JOIN events_fts ON events.id = events_fts.rowid WHERE events_fts MATCH :query ORDER BY events.posted_at DESC LIMIT 1000")
    LiveData<List<EventRecord>> search(String query);
}
