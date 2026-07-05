package com.example.kilgi.inventory.input;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class InventoryInputParserTest {

    @Test
    public void parseMeasuredWeights_supportsCommaAndNewlineSeparatedValues() {
        assertEquals(
                Arrays.asList(24.5, 25.0, 26.25),
                InventoryInputParser.parseMeasuredWeights("24.5, 25.0\n26.25", "Unit weights")
        );
    }

    @Test
    public void parseMeasuredWeights_rejectsInvalidNumbers() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> InventoryInputParser.parseMeasuredWeights("24.5, abc", "Unit weights")
        );

        assertEquals("Unit weights contains an invalid number: abc", exception.getMessage());
    }

    @Test
    public void parseMeasuredWeights_rejectsNonPositiveEntries() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> InventoryInputParser.parseMeasuredWeights("24.5, 0", "Unit weights")
        );

        assertEquals("Unit weights entries must be greater than zero.", exception.getMessage());
    }

    @Test
    public void parseOptionalNonNegativeDouble_returnsZeroForBlankInput() {
        assertEquals(0, InventoryInputParser.parseOptionalNonNegativeDouble("   ", "Shipping fees"), 0.0001);
    }

    @Test
    public void parseRequiredPositiveDouble_rejectsZero() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> InventoryInputParser.parseRequiredPositiveDouble("0", "Spoilage weight")
        );

        assertEquals("Spoilage weight must be greater than zero.", exception.getMessage());
    }
}

