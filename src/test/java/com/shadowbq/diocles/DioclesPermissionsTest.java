package com.shadowbq.diocles;

import net.minecraft.server.command.ServerCommandSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DioclesPermissions
 */
public class DioclesPermissionsTest {

    private ServerCommandSource mockSource;

    @BeforeEach
    void setUp() {
        mockSource = mock(ServerCommandSource.class);
    }

    @Test
    void testHasAdminPermission_WithLevel2_ReturnsTrue() {
        // Arrange
        when(mockSource.hasPermissionLevel(2)).thenReturn(true);

        // Act
        boolean result = DioclesPermissions.hasAdminPermission(mockSource);

        // Assert
        assertTrue(result, "Should return true when source has permission level 2");
        verify(mockSource).hasPermissionLevel(2);
    }

    @Test
    void testHasAdminPermission_WithoutLevel2_ReturnsFalse() {
        // Arrange
        when(mockSource.hasPermissionLevel(2)).thenReturn(false);

        // Act
        boolean result = DioclesPermissions.hasAdminPermission(mockSource);

        // Assert
        assertFalse(result, "Should return false when source doesn't have permission level 2");
        verify(mockSource).hasPermissionLevel(2);
    }

    @Test
    void testHasPublicPermission_AlwaysReturnsTrue() {
        // Act
        boolean result = DioclesPermissions.hasPublicPermission(mockSource);

        // Assert
        assertTrue(result, "Public permission should always return true");

        // Verify no interactions with the mock (since public permission doesn't check
        // anything)
        verifyNoInteractions(mockSource);
    }

    @Test
    void testHasPublicPermission_WithNullSource_ReturnsTrue() {
        // Act & Assert - should not throw exception and should return true
        assertTrue(DioclesPermissions.hasPublicPermission(null),
                "Public permission should return true even with null source");
    }

    @Test
    void testHasAdminPermission_WithNullSource_ThrowsException() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> {
            DioclesPermissions.hasAdminPermission(null);
        }, "Should throw NullPointerException when source is null");
    }
}
