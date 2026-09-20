package com.yagay.NotifyLens.data;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

public final class DatabaseMigrations {
    private DatabaseMigrations() {}

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {
            db.execSQL("ALTER TABLE events ADD COLUMN removal_reason INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE events ADD COLUMN channel_name TEXT");
            db.execSQL("ALTER TABLE events ADD COLUMN channel_description TEXT");
            db.execSQL("ALTER TABLE events ADD COLUMN channel_importance INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE events ADD COLUMN is_group_summary INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE events ADD COLUMN template TEXT");
            db.execSQL("ALTER TABLE events ADD COLUMN payload_silent INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE events ADD COLUMN heads_up INTEGER NOT NULL DEFAULT 0");
            db.execSQL("ALTER TABLE events ADD COLUMN content_hash TEXT");
            db.execSQL("ALTER TABLE events ADD COLUMN instance_id INTEGER");
            db.execSQL("ALTER TABLE events ADD COLUMN revision_count INTEGER NOT NULL DEFAULT 0");

            db.execSQL("CREATE TABLE IF NOT EXISTS notification_instances (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, instance_key TEXT NOT NULL, notification_key TEXT, package_name TEXT NOT NULL, first_seen INTEGER NOT NULL, last_seen INTEGER NOT NULL, removed_at INTEGER, removal_reason INTEGER NOT NULL, channel_id TEXT, current_revision INTEGER NOT NULL)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notification_instances_instance_key ON notification_instances(instance_key)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_instances_notification_key ON notification_instances(notification_key)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_instances_package_name ON notification_instances(package_name)");

            db.execSQL("CREATE TABLE IF NOT EXISTS notification_revisions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, instance_id INTEGER NOT NULL, sequence INTEGER NOT NULL, captured_at INTEGER NOT NULL, content_hash TEXT NOT NULL, title TEXT, text TEXT, full_text TEXT, messages_json TEXT, progress INTEGER NOT NULL, progress_max INTEGER NOT NULL, importance INTEGER NOT NULL, source TEXT NOT NULL)");
            db.execSQL("CREATE INDEX IF NOT EXISTS index_notification_revisions_instance_id ON notification_revisions(instance_id)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notification_revisions_instance_id_sequence ON notification_revisions(instance_id, sequence)");
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_notification_revisions_instance_id_content_hash ON notification_revisions(instance_id, content_hash)");

            db.execSQL("CREATE TABLE IF NOT EXISTS capture_gaps (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, disconnected_at INTEGER NOT NULL, reconnected_at INTEGER NOT NULL, duration_ms INTEGER NOT NULL, reason TEXT)");

            db.execSQL("CREATE VIRTUAL TABLE IF NOT EXISTS events_fts USING FTS4(title TEXT, text TEXT, full_text TEXT, app_label TEXT, package_name TEXT)");
            db.execSQL("INSERT INTO events_fts(rowid, title, text, full_text, app_label, package_name) SELECT id, title, text, full_text, app_label, package_name FROM events");
        }
    };
}
