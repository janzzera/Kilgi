package com.example.kilgi.inventory.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.List;

public class LotWithDetails {

    @Embedded
    public LotEntity lot;

    @Relation(parentColumn = "lotId", entityColumn = "lotId")
    public List<BatchExpenseEntity> expenses = new ArrayList<>();

    @Relation(parentColumn = "lotId", entityColumn = "lotId")
    public List<SpoilageLogEntity> spoilageLogs = new ArrayList<>();
}

