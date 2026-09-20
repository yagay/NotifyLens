package com.yagay.NotifyLens.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import net.zetetic.database.sqlcipher.SupportOpenHelperFactory;

@Database(entities = {EventRecord.class}, version = 1, exportSchema = false)
public abstract class NotifyDatabase extends RoomDatabase {
    private static volatile NotifyDatabase INSTANCE;
    public abstract EventDao eventDao();

    public static NotifyDatabase get(Context context) {
        if (INSTANCE == null) {
            synchronized (NotifyDatabase.class) {
                if (INSTANCE == null) {
                    Context app = context.getApplicationContext();
                    System.loadLibrary("sqlcipher");
                    byte[] passphrase = DbKeyManager.getOrCreate(app);
                    SupportOpenHelperFactory factory = new SupportOpenHelperFactory(passphrase);
                    INSTANCE = Room.databaseBuilder(
                                    app,
                                    NotifyDatabase.class,
                                    app.getDatabasePath("notifylens.db").getAbsolutePath())
                            .openHelperFactory(factory)
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
