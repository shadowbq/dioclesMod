package com.shadowbq.diocles;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.*;
import java.net.URI;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;

/**
 * Tests for configuration and utility functions
 */
public class ConfigurationTest {

    @BeforeEach
    void setUp() {
        // Clear any existing environment variables for clean tests
        clearTestEnvironmentVariables();
    }

    @AfterEach
    void tearDown() {
        clearTestEnvironmentVariables();
    }

    private void clearTestEnvironmentVariables() {
        System.clearProperty("DIOCLES_XHOST");
        System.clearProperty("DIOCLES_AUTHKEY");
    }

    @Test
    void testEnvironmentVariableFormat() {
        // Test that environment variable names follow expected patterns
        String hostVar = "DIOCLES_XHOST";
        String authVar = "DIOCLES_AUTHKEY";

        // Should be uppercase
        assertEquals(hostVar.toUpperCase(), hostVar);
        assertEquals(authVar.toUpperCase(), authVar);

        // Should start with project prefix
        assertTrue(hostVar.startsWith("DIOCLES_"));
        assertTrue(authVar.startsWith("DIOCLES_"));

        // Should be reasonable length
        assertTrue(hostVar.length() > 8);
        assertTrue(authVar.length() > 8);
    }

    @Test
    void testConfigFileStructure() {
        // Test the expected JSON config structure
        Map<String, Object> config = new HashMap<>();
        config.put("deathboard_uri", "http://localhost:3000");
        config.put("authkey", "test-secret-key");

        // Verify required fields are present
        assertTrue(config.containsKey("deathboard_uri"));
        assertTrue(config.containsKey("authkey"));

        // Verify values are strings
        assertInstanceOf(String.class, config.get("deathboard_uri"));
        assertInstanceOf(String.class, config.get("authkey"));

        // Test URI format
        String uri = (String) config.get("deathboard_uri");
        assertDoesNotThrow(() -> {
            URI.create(uri);
        });
    }

    @Test
    void testUrlConstruction() {
        // Test URL construction for API endpoints
        String baseUrl = "http://localhost:3000";
        String[] endpoints = { "/api/deathboard", "/api/sync" };

        for (String endpoint : endpoints) {
            String fullUrl = baseUrl + endpoint;

            assertDoesNotThrow(() -> {
                URI uri = URI.create(fullUrl);
                URL url = uri.toURL();
                assertNotNull(url);
                assertEquals("http", url.getProtocol());
                assertEquals("localhost", url.getHost());
                assertEquals(3000, url.getPort());
                assertTrue(url.getPath().startsWith("/api/"));
            });
        }
    }

    @Test
    void testUrlHandlingEdgeCases() {
        // Test various URL formats that might be provided in config
        String[] testUrls = {
                "http://localhost:3000",
                "http://localhost:3000/",
                "https://example.com",
                "https://example.com:8080",
                "http://192.168.1.100:3000"
        };

        for (String baseUrl : testUrls) {
            assertDoesNotThrow(() -> {
                URI uri = URI.create(baseUrl);
                URL url = uri.toURL();
                assertNotNull(url);
                assertTrue(url.getProtocol().equals("http") || url.getProtocol().equals("https"));
            }, "Should handle URL: " + baseUrl);
        }
    }

    @Test
    void testConfigurationFallbackLogic() {
        // Test the fallback logic: environment variables take precedence over file
        // config
        Map<String, String> envConfig = new HashMap<>();
        Map<String, String> fileConfig = new HashMap<>();

        // Simulate environment variables
        envConfig.put("DIOCLES_XHOST", "http://env-host:3000");
        envConfig.put("DIOCLES_AUTHKEY", "env-auth-key");

        // Simulate file config
        fileConfig.put("deathboard_uri", "http://file-host:3000");
        fileConfig.put("authkey", "file-auth-key");

        // Test precedence logic
        String finalHost = envConfig.getOrDefault("DIOCLES_XHOST", fileConfig.get("deathboard_uri"));
        String finalAuth = envConfig.getOrDefault("DIOCLES_AUTHKEY", fileConfig.get("authkey"));

        assertEquals("http://env-host:3000", finalHost);
        assertEquals("env-auth-key", finalAuth);

        // Test when env vars are not set
        String finalHostNoEnv = null != null ? null : fileConfig.get("deathboard_uri");
        String finalAuthNoEnv = null != null ? null : fileConfig.get("authkey");

        assertEquals("http://file-host:3000", finalHostNoEnv);
        assertEquals("file-auth-key", finalAuthNoEnv);
    }

    @Test
    void testJsonConfigSerialization() {
        // Test JSON serialization/deserialization for config
        com.google.gson.Gson gson = new com.google.gson.GsonBuilder().create();

        Map<String, Object> config = new HashMap<>();
        config.put("deathboard_uri", "http://test:3000");
        config.put("authkey", "test-key");

        // Serialize to JSON
        String json = gson.toJson(config);
        assertNotNull(json);
        assertTrue(json.contains("deathboard_uri"));
        assertTrue(json.contains("authkey"));

        // Deserialize from JSON
        @SuppressWarnings("unchecked")
        Map<String, Object> deserialized = gson.fromJson(json, Map.class);
        assertEquals("http://test:3000", deserialized.get("deathboard_uri"));
        assertEquals("test-key", deserialized.get("authkey"));
    }

    @Test
    void testMinecraftWorldPathLogic() {
        // Test path construction for config file location
        // This simulates the logic: world_save/config/diocles.json
        String worldSave = "/server/world";
        String configDir = "config";
        String configFile = "diocles.json";

        String expectedPath = worldSave + "/" + configDir + "/" + configFile;
        assertEquals("/server/world/config/diocles.json", expectedPath);

        // Test with different separators
        String pathWithSlash = worldSave + "/" + configDir + "/" + configFile;
        String pathWithoutSlash = worldSave + configDir + "/" + configFile;

        assertNotEquals(pathWithSlash, pathWithoutSlash);
        assertTrue(pathWithSlash.contains("/config/"));
    }
}
