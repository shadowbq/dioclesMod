package com.shadowbq.diocles.subcommands.debug;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

/**
 * Debug command that tests API connectivity with health endpoint fallback
 */
public class DebugPingCommand {

    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        try {
            // Get config values - using environment variables like current code
            String postUrl = System.getenv("DIOCLES_XHOST");
            String authKey = System.getenv("DIOCLES_AUTHKEY");

            if (postUrl == null || postUrl.isBlank()) {
                src.sendFeedback(() -> Text.literal("§c[Diocles] No API URL configured"), false);
                return 1;
            }

            // Try health endpoints in order: /health, /ping, then base URL
            String[] endpoints = { "/health", "/ping", "" };
            boolean success = false;
            String successEndpoint = "";
            int finalCode = 0;
            String lastError = "";

            for (String endpoint : endpoints) {
                try {
                    URI baseUri = URI.create(postUrl);
                    URI fullUri = baseUri.resolve(endpoint);
                    URL url = fullUri.toURL();

                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    if (authKey != null && !authKey.isBlank()) {
                        conn.setRequestProperty("authkey", authKey);
                    }
                    conn.setConnectTimeout(3000);
                    conn.setReadTimeout(5000);
                    conn.connect();

                    finalCode = conn.getResponseCode();
                    conn.disconnect();

                    if (finalCode == 200) {
                        success = true;
                        successEndpoint = endpoint.isEmpty() ? "base URL" : endpoint;
                        break;
                    }
                } catch (Exception e) {
                    lastError = e.getMessage();
                    // Continue to next endpoint
                }
            }

            // Make variables effectively final for lambda
            final String finalSuccessEndpoint = successEndpoint;
            final int responseCode = finalCode;
            final String errorMessage = lastError;

            if (success) {
                src.sendFeedback(
                        () -> Text.literal("§a[Diocles] Pong from API ✅ (" + finalSuccessEndpoint + ")"),
                        false);
            } else {
                src.sendFeedback(() -> Text.literal("§c[Diocles] Ping failed ❌ (Status: " + responseCode
                        + ", Error: " + errorMessage + ")"), false);
            }
        } catch (Exception e) {
            src.sendFeedback(() -> Text.literal("§c[Diocles] Ping error: " + e.getMessage()), false);
        }
        return 1;
    }
}
