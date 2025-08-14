package com.shadowbq.diocles;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DioclesConstants
 */
public class DioclesConstantsTest {

    @Test
    void testVersionConstantExists() {
        // Test that VERSION constant is not null and not empty
        assertNotNull(DioclesConstants.VERSION, "VERSION constant should not be null");
        assertFalse(DioclesConstants.VERSION.isEmpty(), "VERSION constant should not be empty");
        assertFalse(DioclesConstants.VERSION.isBlank(), "VERSION constant should not be blank");
    }

    @Test
    void testVersionFormat() {
        // Test that VERSION follows semantic versioning pattern (x.y.z)
        String version = DioclesConstants.VERSION;
        assertTrue(version.matches("\\d+\\.\\d+\\.\\d+"),
                "VERSION should follow semantic versioning format (x.y.z), but was: " + version);
    }
}
