package com.example.kilgi.inventory.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        entities = {
                UserEntity.class,
                ProviderEntity.class,
                CustomerEntity.class,
                LotEntity.class,
                BatchExpenseEntity.class,
                SpoilageLogEntity.class,
                JournalEntryEntity.class,
                JournalLineEntity.class,
                RetailSaleEntity.class,
                WholesaleInvoiceEntity.class,
                CustomerPaymentEntity.class,
                CustomerPaymentAllocationEntity.class,
                ProviderPaymentEntity.class,
                ProviderPaymentAllocationEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class KilgiDatabase extends RoomDatabase {

    private static volatile KilgiDatabase instance;

    public abstract UserDao userDao();

    public abstract ProviderDao providerDao();

    public abstract CustomerDao customerDao();

    public abstract LotDao lotDao();

    public abstract BatchExpenseDao batchExpenseDao();

    public abstract SpoilageLogDao spoilageLogDao();

    public abstract JournalDao journalDao();

    public abstract SalesDao salesDao();

    public static KilgiDatabase getInstance(Context context) {
        if (instance == null) {
            synchronized (KilgiDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    KilgiDatabase.class,
                                    "kilgi.db"
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return instance;
    }
}

