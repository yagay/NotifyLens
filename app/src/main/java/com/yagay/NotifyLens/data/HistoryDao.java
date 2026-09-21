package com.yagay.NotifyLens.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertInstance(NotificationInstance instance);

    @Update
    void updateInstance(NotificationInstance instance);

    @Query("SELECT * FROM notification_instances WHERE instance_key = :key LIMIT 1")
    NotificationInstance instanceByKey(String key);

    @Query("SELECT * FROM notification_instances WHERE notification_key = :key AND removed_at IS NULL ORDER BY last_seen DESC LIMIT 1")
    NotificationInstance activeInstanceByNotificationKey(String key);

    @Query("SELECT * FROM notification_instances WHERE notification_key = :key ORDER BY last_seen DESC LIMIT 1")
    NotificationInstance latestInstanceByNotificationKey(String key);

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    long insertRevision(NotificationRevision revision);

    @Query("SELECT * FROM notification_revisions WHERE instance_id = :instanceId ORDER BY sequence DESC LIMIT 1")
    NotificationRevision latestRevision(long instanceId);

    @Query("SELECT * FROM notification_revisions WHERE instance_id = :instanceId ORDER BY sequence ASC")
    List<NotificationRevision> revisions(long instanceId);

    @Insert
    long insertGap(CaptureGap gap);

    @Query("DELETE FROM notification_revisions WHERE instance_id IN (SELECT id FROM notification_instances WHERE package_name = :packageName)")
    void deleteRevisionsForPackage(String packageName);

    @Query("DELETE FROM notification_instances WHERE package_name = :packageName")
    void deleteInstancesForPackage(String packageName);

    @Query("DELETE FROM notification_revisions WHERE instance_id IN (SELECT id FROM notification_instances WHERE last_seen < :cutoff)")
    void deleteOldRevisions(long cutoff);

    @Query("DELETE FROM notification_instances WHERE last_seen < :cutoff")
    void deleteOldInstances(long cutoff);

    @Query("DELETE FROM notification_revisions")
    void deleteAllRevisions();

    @Query("DELETE FROM notification_instances")
    void deleteAllInstances();

    @Query("DELETE FROM capture_gaps")
    void deleteAllGaps();
}
