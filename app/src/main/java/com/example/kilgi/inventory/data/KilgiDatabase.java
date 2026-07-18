package com.example.kilgi.inventory.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                LotEntity.class,
                BatchExpenseEntity.class,
                SpoilageLogEntity.class,
                JournalEntryEntity.class,
                JournalLineEntity.class
        },
        version = 2,
        exportSchema = false
)
public abstract class KilgiDatabase extends RoomDatabase {

    private static volatile KilgiDatabase instance;

    public abstract LotDao lotDao();

    public abstract BatchExpenseDao batchExpenseDao();

    public abstract SpoilageLogDao spoilageLogDao();

    public abstract JournalDao journalDao();

    public static KilgiDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (KilgiDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    KilgiDatabase.class,
                                    "kilgi-module-one.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}

