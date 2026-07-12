package com.example.kilgi.inventory.data;

import androidx.room.Embedded;
import androidx.room.Relation;

import java.util.ArrayList;
import java.util.List;

public class JournalEntryWithLines {

    @Embedded
    public JournalEntryEntity entry;

    @Relation(parentColumn = "entryId", entityColumn = "entryId")
    public List<JournalLineEntity> lines = new ArrayList<>();
}

