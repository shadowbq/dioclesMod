package com.shadowbq.diocles;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for DioclesMod
 * Note: These are basic tests that don't require Fabric/Minecraft
 * initialization
 */
public class DioclesModTest {

    @Test
    void testModInitializerInterface() {
        // Test that DioclesMod implements the required Fabric ModInitializer interface
        DioclesMod mod = new DioclesMod();
        assertNotNull(mod);

        // Test that we can call onInitialize without throwing an exception
        // Note: This will fail if actual Minecraft/Fabric context is required
        // In a real test environment, you'd mock these dependencies
        assertDoesNotThrow(() -> {
            // We can't actually call onInitialize without proper Fabric context
            // but we can verify the class structure
            assertTrue(mod instanceof net.fabricmc.api.ModInitializer);
        });
    }

    @Test
    void testModClassStructure() {
        // Basic structural tests
        DioclesMod mod = new DioclesMod();

        // Verify it's the expected class
        assertEquals("DioclesMod", mod.getClass().getSimpleName());
        assertEquals("com.shadowbq.diocles.DioclesMod", mod.getClass().getName());

        // Verify it has the required method
        assertDoesNotThrow(() -> {
            mod.getClass().getMethod("onInitialize");
        });
    }
}
