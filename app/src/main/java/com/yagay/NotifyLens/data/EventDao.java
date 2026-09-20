package com.yagay.NotifyLens.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long upsert(EventRecord record);

    @Update
    void update(EventRecord record);

    @Query("SELECT * FROM events ORDER BY posted_at DESC LIMIT 1000")
    LiveData<List<EventRecord>> observeAll();

    @Query("SELECT * FROM events WHERE event_type = :type ORDER BY posted_at DESC LIMIT 1000")
    LiveData<List<EventRecord>> observeType(String type);

    @Query("SELECT * FROM events WHERE package_name = :packageName ORDER BY posted_at DESC LIMIT 2000")
    LiveData<List<EventRecord>> observePackage(String packageName);

    @Query("SELECT * FROM events WHERE package_name = :packageName AND event_type = :type ORDER BY posted_at DESC LIMIT 2000")
    LiveData<List<EventRecord>> observePackageType(String packageName, String type);

    @Query("SELECT * FROM events WHERE (app_label LIKE '%' || :q || '%' OR package_name LIKE '%' || :q || '%' OR title LIKE '%' || :q || '%' OR text LIKE '%' || :q || '%' OR full_text LIKE '%' || :q || '%') ORDER BY posted_at DESC LIMIT 1000")
    LiveData<List<EventRecord>> search(String q);

    @Query("SELECT package_name, MAX(app_label) AS app_label, COUNT(*) AS event_count, MAX(posted_at) AS last_time FROM events GROUP BY package_name ORDER BY last_time DESC")
    LiveData<List<AppSummary>> observeApps();

    @Query("SELECT * FROM events WHERE id = :id LIMIT 1")
    EventRecord byId(long id);

    @Query("SELECT * FROM events WHERE notification_key = :key ORDER BY posted_at DESC LIMIT 1")
    EventRecord latestByNotificationKey(String key);

    @Query("SELECT * FROM events WHERE package_name = :packageName AND event_type = :type AND posted_at >= :cutoff AND (full_text = :text OR text = :text) ORDER BY posted_at DESC LIMIT 1")
    EventRecord recentEquivalent(String packageName, String type, String text, long cutoff);

    @Query("DELETE FROM events WHERE package_name = :packageName")
    void deletePackage(String packageName);

    @Query("DELETE FROM events")
    void deleteAll();

    @Query("DELETE FROM events WHERE posted_at < :cutoff")
    void deleteOlderThan(long cutoff);
}
