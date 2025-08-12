package com.shadowbq.diocles;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import static com.github.tomakehurst.wiremock.client.WireMock.*;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Integration tests using WireMock to test HTTP API interactions
 * This simulates the external deathboard API that the mod communicates with
 */
public class HttpApiIntegrationTest {

    private WireMockServer mockServer;
    private String baseUrl;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @BeforeEach
    void setUp() {
        // Start WireMock server on a random available port
        mockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
        mockServer.start();

        // Configure WireMock
        WireMock.configureFor("localhost", mockServer.port());

        baseUrl = "http://localhost:" + mockServer.port();
        System.out.println("Mock server started on: " + baseUrl);
    }

    @AfterEach
    void tearDown() {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Test
    void testDeathboardApiEndpoint() throws IOException {
        // Setup: Mock the /api/deathboard endpoint
        stubFor(post(urlEqualTo("/api/deathboard"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("authkey", equalTo("test-auth-key"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\",\"message\":\"Death recorded\"}")));

        // Test: Send the same type of payload that DeathboardManager would send
        Map<String, Object> playerDeathData = createTestDeathPayload();
        String json = gson.toJson(playerDeathData);

        // Act: Make HTTP request (simulating DeathboardManager.sendPost)
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(baseUrl + "/api/deathboard");
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("authkey", "test-auth-key");
            conn.setDoOutput(true);

            // Send request body
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(out);

            // Get response
            int responseCode = conn.getResponseCode();
            assertEquals(200, responseCode);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // Verify: Check that WireMock received the expected request
        verify(postRequestedFor(urlEqualTo("/api/deathboard"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withHeader("authkey", equalTo("test-auth-key"))
                .withRequestBody(containing("testplayer"))
                .withRequestBody(containing("death_count"))
                .withRequestBody(containing("last_death_time")));
    }

    @Test
    void testSyncApiEndpoint() throws IOException {
        // Setup: Mock the /api/sync endpoint
        stubFor(post(urlEqualTo("/api/sync"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"success\",\"message\":\"Sync completed\"}")));

        // Test: Send full scoreboard sync payload
        Map<String, Object> fullScoreboardData = createTestSyncPayload();
        String json = gson.toJson(fullScoreboardData);

        // Act: Make HTTP request
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(baseUrl + "/api/sync");
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("authkey", "test-auth-key");
            conn.setDoOutput(true);

            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(out);

            int responseCode = conn.getResponseCode();
            assertEquals(200, responseCode);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // Verify the request was made correctly
        verify(postRequestedFor(urlEqualTo("/api/sync"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(containing("testplayer1"))
                .withRequestBody(containing("testplayer2")));
    }

    @Test
    void testApiErrorHandling() throws IOException {
        // Setup: Mock server returns error
        stubFor(post(urlEqualTo("/api/deathboard"))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withBody("Internal Server Error")));

        // Test: Verify error handling
        Map<String, Object> playerDeathData = createTestDeathPayload();
        String json = gson.toJson(playerDeathData);

        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(baseUrl + "/api/deathboard");
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(out);

            int responseCode = conn.getResponseCode();
            assertEquals(500, responseCode);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    @Test
    void testApiWithoutAuthKey() throws IOException {
        // Setup: Mock endpoint that doesn't require auth
        stubFor(post(urlEqualTo("/api/deathboard"))
                .withHeader("Content-Type", equalTo("application/json"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\":\"success\"}")));

        // Test: Request without auth header
        Map<String, Object> playerDeathData = createTestDeathPayload();
        String json = gson.toJson(playerDeathData);

        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(baseUrl + "/api/deathboard");
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(out);

            int responseCode = conn.getResponseCode();
            assertEquals(200, responseCode);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        // Verify request was made without auth header
        verify(postRequestedFor(urlEqualTo("/api/deathboard"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withoutHeader("authkey"));
    }

    @Test
    void testHealthCheckEndpoint() throws IOException {
        // Setup: Mock a health check endpoint for the debug-ping command
        stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"healthy\",\"service\":\"deathboard-api\"}")));

        // Test: Simple GET request (like debug-ping would do)
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(baseUrl + "/health");
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("authkey", "test-auth-key");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            assertEquals(200, responseCode);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        verify(getRequestedFor(urlEqualTo("/health"))
                .withHeader("authkey", equalTo("test-auth-key")));
    }

    @Test
    void testPingEndpointFallback() throws IOException {
        // Setup: Mock /ping endpoint (fallback when /health fails)
        stubFor(get(urlEqualTo("/ping"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"pong\"}")));

        // Test: GET request to ping endpoint
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(baseUrl + "/ping");
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("authkey", "test-auth-key");
            conn.setConnectTimeout(3000);
            conn.setReadTimeout(5000);

            int responseCode = conn.getResponseCode();
            assertEquals(200, responseCode);

        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        verify(getRequestedFor(urlEqualTo("/ping"))
                .withHeader("authkey", equalTo("test-auth-key")));
    }

    @Test
    void testHealthCheckFallbackChain() throws IOException {
        // Setup: Mock the fallback behavior - /health fails, /ping succeeds
        stubFor(get(urlEqualTo("/health"))
                .willReturn(aResponse()
                        .withStatus(404)));

        stubFor(get(urlEqualTo("/ping"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody("{\"status\":\"pong\"}")));

        // Test: Simulate the fallback logic that debug-ping uses
        String[] endpoints = { "/health", "/ping", "" };
        boolean success = false;
        String successEndpoint = "";

        for (String endpoint : endpoints) {
            HttpURLConnection conn = null;
            try {
                URI uri = URI.create(baseUrl + endpoint);
                URL url = uri.toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("authkey", "test-auth-key");
                conn.setConnectTimeout(3000);
                conn.setReadTimeout(5000);

                int responseCode = conn.getResponseCode();

                if (responseCode == 200) {
                    success = true;
                    successEndpoint = endpoint.isEmpty() ? "base URL" : endpoint;
                    break;
                }
            } catch (Exception e) {
                // Continue to next endpoint
            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        }

        assertTrue(success, "Health check fallback should succeed on /ping");
        assertEquals("/ping", successEndpoint);

        // Verify both endpoints were tried
        verify(getRequestedFor(urlEqualTo("/health")));
        verify(getRequestedFor(urlEqualTo("/ping")));
    }

    @Test
    void testConnectionTimeout() {
        // Setup: Mock with delay to test timeout handling
        stubFor(post(urlEqualTo("/api/deathboard"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(6000))); // 6 second delay

        // Test: Verify timeout behavior
        Map<String, Object> playerDeathData = createTestDeathPayload();
        String json = gson.toJson(playerDeathData);

        assertThrows(IOException.class, () -> {
            HttpURLConnection conn = null;
            try {
                URI uri = URI.create(baseUrl + "/api/deathboard");
                URL url = uri.toURL();
                conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setConnectTimeout(3000); // 3 second timeout
                conn.setReadTimeout(3000);
                conn.setDoOutput(true);

                byte[] out = json.getBytes(StandardCharsets.UTF_8);
                conn.getOutputStream().write(out);

                conn.getResponseCode(); // This should timeout

            } finally {
                if (conn != null) {
                    conn.disconnect();
                }
            }
        });
    }

    // Helper methods to create test data

    private Map<String, Object> createTestDeathPayload() {
        Map<String, Object> playerData = new HashMap<>();

        Map<String, Object> deathInfo = new HashMap<>();
        deathInfo.put("last_death_time", "2025-08-12T22:30:00.000Z");
        deathInfo.put("last_death_day", 150L);
        deathInfo.put("death_count", 5);

        Map<String, Integer> location = new HashMap<>();
        location.put("x", 100);
        location.put("y", 64);
        location.put("z", -200);
        deathInfo.put("location", location);

        deathInfo.put("world", "minecraft:overworld");

        playerData.put("testplayer", deathInfo);
        return playerData;
    }

    private Map<String, Object> createTestSyncPayload() {
        Map<String, Object> fullData = new HashMap<>();

        // Player 1
        Map<String, Object> player1Data = new HashMap<>();
        player1Data.put("death_count", 3);
        player1Data.put("last_death_time", "2025-08-12T20:15:00.000Z");
        player1Data.put("last_death_day", 149L);
        fullData.put("testplayer1", player1Data);

        // Player 2
        Map<String, Object> player2Data = new HashMap<>();
        player2Data.put("death_count", 7);
        player2Data.put("last_death_time", "2025-08-12T21:45:00.000Z");
        player2Data.put("last_death_day", 150L);
        fullData.put("testplayer2", player2Data);

        return fullData;
    }
}
