package com.shadowbq.diocles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.RepeatedTest;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AnnouncementQuotes
 */
public class AnnouncementQuotesTest {

    @Test
    void testGetRandomQuote_ReturnsNonNullString() {
        // Act
        String quote = AnnouncementQuotes.getRandomQuote();

        // Assert
        assertNotNull(quote, "getRandomQuote should never return null");
        assertFalse(quote.isEmpty(), "getRandomQuote should never return empty string");
        assertFalse(quote.isBlank(), "getRandomQuote should never return blank string");
    }

    @Test
    void testGetRandomQuote_ReturnsFormattedQuote() {
        // Act
        String quote = AnnouncementQuotes.getRandomQuote();

        // Assert - should contain color codes and proper formatting
        // Format should be: §6"quote§6" §7- source
        assertTrue(quote.contains("§6"), "Quote should contain gold color code");
        assertTrue(quote.contains("§7"), "Quote should contain gray color code");
        assertTrue(quote.contains("\""), "Quote should contain quotation marks");
        assertTrue(quote.contains("§7- "), "Quote should contain separator between quote and source");
    }

    @RepeatedTest(10)
    void testGetRandomQuote_ReturnsValidQuotes() {
        // Act
        String quote = AnnouncementQuotes.getRandomQuote();

        // Assert - should not be an error message
        assertFalse(quote.startsWith("§cError"), "Should not return error message with valid resource");
        assertFalse(quote.equals("No quotes available!"), "Should not return 'no quotes' message with valid resource");

        // Should follow expected format: §6"...§6" §7- ...
        assertTrue(quote.matches("§6\".*§6\" §7- .*"),
                "Quote should match expected format: §6\"quote§6\" §7- source, but was: " + quote);
    }

    @Test
    void testGetRandomQuote_ConsistentFormat() {
        // Act - test multiple quotes for consistent formatting
        for (int i = 0; i < 10; i++) {
            String quote = AnnouncementQuotes.getRandomQuote();

            // Assert each quote follows the same format
            assertTrue(quote.startsWith("§6\""), "Quote should start with §6\"");
            assertTrue(quote.contains("§6\" §7- "), "Quote should contain the proper separator format");
            assertFalse(quote.endsWith(" "), "Quote should not end with whitespace");
        }
    }
}
