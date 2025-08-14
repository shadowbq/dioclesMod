package com.shadowbq.diocles;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Random;

/**
 * Utility class for handling announcement quotes.
 * Loads and provides random quotes from the announce_quotes.json resource file.
 */
public class AnnouncementQuotes {

    /**
     * Load a random quote from the announce_quotes.json resource file
     * 
     * @return A formatted string with a random quote and its source
     */
    public static String getRandomQuote() {
        try {
            InputStream is = AnnouncementQuotes.class.getResourceAsStream("/announce_quotes.json");
            if (is == null) {
                return "No quotes available!";
            }

            try (InputStreamReader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
                JsonArray quotes = JsonParser.parseReader(reader).getAsJsonArray();
                if (quotes.size() == 0) {
                    return "No quotes available!";
                }

                Random random = new Random();
                JsonObject selectedQuote = quotes.get(random.nextInt(quotes.size())).getAsJsonObject();
                String quote = selectedQuote.get("quote").getAsString();
                String source = selectedQuote.get("source").getAsString();

                return "§6\"" + quote + "§6\" §7- " + source;
            }
        } catch (Exception e) {
            return "§cError loading quotes: " + e.getMessage();
        }
    }
}
