package com.example.kilgi.inventory.model;

/**
 * Represents a spoilage or waste event recorded against an active batch.
 */
public class SpoilageEntry {

    private final double lostWeightKg;
    private final String reason;
    private final long loggedAtMillis;

    public SpoilageEntry(double lostWeightKg, String reason) {
        this(lostWeightKg, reason, System.currentTimeMillis());
    }

    public SpoilageEntry(double lostWeightKg, String reason, long loggedAtMillis) {
        if (lostWeightKg <= 0) {
            throw new IllegalArgumentException("lostWeightKg must be greater than zero.");
        }
        if (loggedAtMillis < 0) {
            throw new IllegalArgumentException("loggedAtMillis cannot be negative.");
        }
        this.lostWeightKg = lostWeightKg;
        this.reason = normalizeReason(reason);
        this.loggedAtMillis = loggedAtMillis;
    }

    public double getLostWeightKg() {
        return lostWeightKg;
    }

    public String getReason() {
        return reason;
    }

    public long getLoggedAtMillis() {
        return loggedAtMillis;
    }

    private static String normalizeReason(String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            return "Unspecified spoilage";
        }
        return reason.trim();
    }
}

