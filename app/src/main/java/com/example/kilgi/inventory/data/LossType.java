package com.example.kilgi.inventory.data;

/**
 * Defines how spoilage affects valuation and accounting.
 */
public enum LossType {
    NORMAL,
    ABNORMAL;

    public static LossType fromStoredValue(String value) {
        for (LossType type : values()) {
            if (type.name().equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported loss type: " + value);
    }
}

