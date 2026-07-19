package com.example.kilgi.inventory.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CustomerDao {

    @Insert
    void insert(CustomerEntity customer);

    @Query("SELECT * FROM customers WHERE customerId = :customerId LIMIT 1")
    CustomerEntity getById(String customerId);

    @Query("SELECT * FROM customers WHERE userId = :userId AND isActive = 1 ORDER BY displayName COLLATE NOCASE ASC")
    List<CustomerEntity> getActiveCustomersForUser(String userId);
}

