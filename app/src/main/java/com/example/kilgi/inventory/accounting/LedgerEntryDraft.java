package com.example.kilgi.inventory.accounting;

import com.example.kilgi.inventory.data.JournalEntryEntity;
import com.example.kilgi.inventory.data.JournalLineEntity;

import java.util.List;

public class LedgerEntryDraft {

    private final JournalEntryEntity entry;
    private final List<JournalLineEntity> lines;

    public LedgerEntryDraft(JournalEntryEntity entry, List<JournalLineEntity> lines) {
        this.entry = entry;
        this.lines = lines;
    }

    public JournalEntryEntity getEntry() {
        return entry;
    }

    public List<JournalLineEntity> getLines() {
        return lines;
    }
}

