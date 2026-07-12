package com.example.kilgi.inventory.input;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses user-provided inventory form input into validated values.
 */
public final class InventoryInputParser {

    private InventoryInputParser() {
    }

    public static String requireText(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        return rawValue.trim();
    }

    public static double parseOptionalNonNegativeDouble(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            return 0;
        }

        try {
            double value = Double.parseDouble(rawValue.trim());
            if (value < 0) {
                throw new IllegalArgumentException(fieldName + " cannot be negative.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    public static double parseRequiredPositiveDouble(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        try {
            double value = Double.parseDouble(rawValue.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than zero.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid number.");
        }
    }

    public static int parseRequiredPositiveInt(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        try {
            int value = Integer.parseInt(rawValue.trim());
            if (value <= 0) {
                throw new IllegalArgumentException(fieldName + " must be greater than zero.");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " must be a valid whole number.");
        }
    }

    public static List<Double> parseMeasuredWeights(String rawValue, String fieldName) {
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        String[] tokens = rawValue.split("[,\n]");
        List<Double> weights = new ArrayList<>();

        for (String token : tokens) {
            String trimmedToken = token.trim();
            if (trimmedToken.isEmpty()) {
                continue;
            }

            try {
                double value = Double.parseDouble(trimmedToken);
                if (value <= 0) {
                    throw new IllegalArgumentException(fieldName + " entries must be greater than zero.");
                }
                weights.add(value);
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException(fieldName + " contains an invalid number: " + trimmedToken);
            }
        }

        if (weights.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must contain at least one weight.");
        }

        return weights;
    }
}

