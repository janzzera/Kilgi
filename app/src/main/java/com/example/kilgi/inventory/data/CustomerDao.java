package com.example.kilgi.inventory.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CustomerDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(CustomerEntity customer);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<CustomerEntity> customers);

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    LiveData<CustomerEntity> getById(String customerId);

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    CustomerEntity getByIdSync(String customerId);

    @Query("SELECT * FROM customers WHERE userId = :userId AND isActive = 1 ORDER BY displayName COLLATE NOCASE ASC")
    LiveData<List<CustomerEntity>> getActiveCustomersForUser(String userId);

    @Query("SELECT * FROM customers WHERE userId = :userId AND isActive = 1 ORDER BY displayName COLLATE NOCASE ASC")
    List<CustomerEntity> getActiveCustomersForUserSync(String userId);
}

