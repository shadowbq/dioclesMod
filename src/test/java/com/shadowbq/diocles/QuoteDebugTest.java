package com.shadowbq.diocles;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Debug test to see the actual format of quotes
 */
public class QuoteDebugTest {

    @Test
    void debugQuoteFormat() {
        String quote = AnnouncementQuotes.getRandomQuote();
        System.out.println("Actual quote format: '" + quote + "'");
        System.out.println("Quote length: " + quote.length());
        
        // Print each character with its code
        for (int i = 0; i < quote.length(); i++) {
            char c = quote.charAt(i);
            System.out.println("Index " + i + ": '" + c + "' (code: " + (int)c + ")");
        }
        
        // Always pass so we can see the output
        assertTrue(true);
    }
}
