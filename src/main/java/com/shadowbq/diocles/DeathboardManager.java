package com.shadowbq.diocles;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.io.IOException;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * Deathboard manager: handles per-death posts and day-sync posts.
 */
public class DeathboardManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String OBJECTIVE_NAME = "diocles_deaths";

    private static String postUrl = null; // base URL, e.g. http://xhost:3000
    private static String authKey = null;
    private static volatile long lastServerDay = Long.MIN_VALUE;

    // in-memory last-death details (player -> map of details)
    private static final Map<String, Map<String, Object>> lastDeathDetails = new ConcurrentHashMap<>();

    private static final ExecutorService executor = Executors.newFixedThreadPool(2);

    public static void init(MinecraftServer server) {
        loadConfig(server);

        Scoreboard scoreboard = server.getScoreboard();
        ScoreboardObjective obj = scoreboard.getObjectives().stream()
                .filter(o -> o.getName().equals(OBJECTIVE_NAME))
                .findFirst().orElse(null);
        if (obj == null) {
            scoreboard.addObjective(
                    OBJECTIVE_NAME,
                    ScoreboardCriterion.DEATH_COUNT,
                    Text.literal("Deaths"),
                    ScoreboardCriterion.RenderType.INTEGER,
                    false,
                    null);
        }

        // initialize lastServerDay
        try {
            lastServerDay = server.getOverworld().getTimeOfDay() / 24000L;
        } catch (Exception e) {
            lastServerDay = Long.MIN_VALUE;
        }

        System.out.println("[Diocles] initialized. POST base=" + (postUrl != null ? postUrl : "none"));
    }

    private static void loadConfig(MinecraftServer server) {
        postUrl = System.getenv("DIOCLES_XHOST");
        authKey = System.getenv("DIOCLES_AUTHKEY");

        if (postUrl != null)
            return;

        try {
            java.nio.file.Path cfgPath = server.getSavePath(net.minecraft.util.WorldSavePath.ROOT)
                    .resolve("config").resolve("diocles.json");
            if (Files.exists(cfgPath)) {
                try (Reader r = Files.newBufferedReader(cfgPath, StandardCharsets.UTF_8)) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> cfg = GSON.fromJson(r, Map.class);
                    if (postUrl == null && cfg.containsKey("deathboard_uri"))
                        postUrl = cfg.get("deathboard_uri").toString();
                    if (authKey == null && cfg.containsKey("authkey"))
                        authKey = cfg.get("authkey").toString();
                }
            }
        } catch (Exception e) {
            System.err.println("[Diocles] Failed reading config: " + e.getMessage());
        }
    }

    /**
     * Called on player death callback.
     */
    public static void handleDeath(MinecraftServer server, ServerPlayerEntity player) {
        if (server == null || player == null)
            return;

        System.out.println("[Diocles] handleDeath called for player: " + player.getName().getString());

        Scoreboard scoreboard = server.getScoreboard();
        ScoreboardObjective obj = scoreboard.getObjectives().stream()
                .filter(o -> o.getName().equals(OBJECTIVE_NAME))
                .findFirst().orElse(null);
        if (obj == null) {
            scoreboard.addObjective(
                    OBJECTIVE_NAME,
                    ScoreboardCriterion.DEATH_COUNT,
                    Text.literal("Deaths"),
                    ScoreboardCriterion.RenderType.INTEGER,
                    false,
                    null);
            obj = scoreboard.getObjectives().stream()
                    .filter(o -> o.getName().equals(OBJECTIVE_NAME))
                    .findFirst().orElse(null);
        }

        // read authoritative death count from scoreboard (may have been incremented by
        // MC)
        int deaths = 0;
        try {
            deaths = scoreboard.getOrCreateScore(player, obj).getScore();
        } catch (Exception ignored) {
        }

        long day = server.getOverworld().getTimeOfDay() / 24000L;
        String time = Instant.now().toString();
        BlockPos pos = player.getBlockPos();

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("last_death_time", time);
        info.put("last_death_day", day);
        info.put("death_count", deaths);
        info.put("location", Map.of("x", pos.getX(), "y", pos.getY(), "z", pos.getZ()));
        info.put("world", player.getWorld().getRegistryKey().getValue().toString());

        // update in-memory map
        lastDeathDetails.put(player.getName().getString(), info);

        // build single-player payload
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put(player.getName().getString(), info);

        // post asynchronously to /api/deathboard
        if (postUrl != null && !postUrl.isBlank()) {
            String endpoint = "/api/deathboard";
            executor.submit(() -> sendPost(payload, endpoint));
        }
    }

    /**
     * Called each server tick to detect day change and sync full board once per day
     * change.
     */
    public static void checkDayAndSync(MinecraftServer server) {
        if (server == null)
            return;
        long currentDay;
        try {
            currentDay = server.getOverworld().getTimeOfDay() / 24000L;
        } catch (Exception e) {
            return;
        }
        if (lastServerDay == Long.MIN_VALUE) {
            lastServerDay = currentDay;
            return;
        }
        if (currentDay != lastServerDay) {
            lastServerDay = currentDay;
            // build full payload and post to /api/sync
            Map<String, Object> full = buildFullPayload(server);
            if (postUrl != null && !postUrl.isBlank()) {
                executor.submit(() -> sendPost(full, "/api/sync"));
            }
        }
    }

    /**
     * Build full scoreboard payload: merge scoreboard counts with lastDeathDetails
     * where available.
     */
    public static Map<String, Object> buildFullPayload(MinecraftServer server) {
        Map<String, Object> out = new LinkedHashMap<>();
        Scoreboard scoreboard = server.getScoreboard();
        ScoreboardObjective obj = scoreboard.getObjectives().stream()
                .filter(o -> o.getName().equals(OBJECTIVE_NAME))
                .findFirst().orElse(null);
        if (obj == null)
            return out;

        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            String name = player.getName().getString();
            Map<String, Object> info = new LinkedHashMap<>();
            int score = 0;
            try {
                score = scoreboard.getOrCreateScore(player, obj).getScore();
            } catch (Exception ignored) {
            }
            info.put("death_count", score);
            // prefer lastDeathDetails if available
            Map<String, Object> details = lastDeathDetails.get(name);
            if (details != null) {
                if (details.containsKey("last_death_time"))
                    info.put("last_death_time", details.get("last_death_time"));
                if (details.containsKey("last_death_day"))
                    info.put("last_death_day", details.get("last_death_day"));
                if (details.containsKey("location"))
                    info.put("location", details.get("location"));
                if (details.containsKey("world"))
                    info.put("world", details.get("world"));
            } else {
                // fallback: include last_death_day as current server day
                try {
                    info.put("last_death_day", server.getOverworld().getTimeOfDay() / 24000L);
                } catch (Exception ignored) {
                }
            }
            out.put(name, info);
        }
        return out;
    }

    private static void sendPost(Map<String, Object> payload, String endpoint) {
        String json = GSON.toJson(payload);
        HttpURLConnection conn = null;
        try {
            URI uri = URI.create(postUrl + endpoint);
            URL url = uri.toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(10000);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            if (authKey != null && !authKey.isBlank())
                conn.setRequestProperty("authkey", authKey);
            conn.setDoOutput(true);
            byte[] out = json.getBytes(StandardCharsets.UTF_8);
            conn.getOutputStream().write(out);
            int code = conn.getResponseCode();
            System.out.println("[Diocles] POST " + postUrl + endpoint + " -> HTTP " + code);
        } catch (IOException e) {
            System.err.println("[Diocles] Failed to POST to " + endpoint + ": " + e.getMessage());
        } finally {
            if (conn != null)
                conn.disconnect();
        }
    }
}
