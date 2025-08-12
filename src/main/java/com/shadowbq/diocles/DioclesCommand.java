package com.shadowbq.diocles;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;

import java.io.FileReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.*;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.literal;

public class DioclesCommand {
    public static class SubcommandEntry {
        public final String name;
        public final Command<ServerCommandSource> command;
        public final Predicate<ServerCommandSource> permissionCheck;

        public SubcommandEntry(String name, Command<ServerCommandSource> command,
                Predicate<ServerCommandSource> permissionCheck) {
            this.name = name;
            this.command = command;
            this.permissionCheck = permissionCheck;
        }
    }

    private static final Map<String, SubcommandEntry> subcommands = new LinkedHashMap<>();

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        // version (op)
        subcommands.put("version", new SubcommandEntry("version",
                ctx -> {
                    System.out.println("Diocles Mod Version: 1.0.0");
                    return 1;
                },
                src -> src.hasPermissionLevel(2)));

        // helloworld (op)
        subcommands.put("helloworld", new SubcommandEntry("helloworld",
                ctx -> {
                    MinecraftServer server = ctx.getSource().getServer();
                    for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                        p.sendMessage(Text.literal("§l§eHELLO WORLD!"), false);
                        // sendTitle is not available in 1.21.7, so we only send a chat message
                    }
                    return 1;
                },
                src -> src.hasPermissionLevel(2)));

        // deathboard (public) — top10 + your count in chat
        subcommands.put("deathboard", new SubcommandEntry("deathboard",
                ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    ServerPlayerEntity player;
                    try {
                        player = src.getPlayer();
                    } catch (Exception e) {
                        src.sendFeedback(() -> Text.literal("[Diocles] This command must be run in-game."), false);
                        return 1;
                    }
                    MinecraftServer server = src.getServer();
                    Scoreboard sb = server.getScoreboard();
                    ScoreboardObjective obj = sb.getObjectives().stream()
                            .filter(o -> o.getName().equals(DeathboardManager.OBJECTIVE_NAME))
                            .findFirst().orElse(null);
                    if (obj == null) {
                        player.sendMessage(Text.literal("§7[Diocles] Death objective not present."), false);
                        return 1;
                    }

                    List<Map.Entry<String, Integer>> scoreList = new ArrayList<>();
                    for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
                        String name = p.getName().getString();
                        int score = 0;
                        try {
                            score = sb.getOrCreateScore(p, obj).getScore();
                        } catch (Exception ignored) {
                        }
                        scoreList.add(Map.entry(name, score));
                    }
                    scoreList.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

                    int selfScore = 0;
                    try {
                        selfScore = sb.getOrCreateScore(player, obj).getScore();
                    } catch (Exception ignored) {
                    }
                    player.sendMessage(Text.literal("§6Your deaths: §c" + selfScore), false);
                    player.sendMessage(Text.literal("§eTop 10 Most Deaths:"), false);
                    for (int i = 0; i < Math.min(10, scoreList.size()); i++) {
                        var s = scoreList.get(i);
                        player.sendMessage(
                                Text.literal(
                                        "§7" + (i + 1) + ". §f" + s.getKey() + "§7 — §c" + s.getValue() + " death(s)"),
                                false);
                    }
                    return 1;
                },
                src -> true));

        // deathboard-full (op) — prints full scoreboard JSON to console
        subcommands.put("deathboard-full", new SubcommandEntry("deathboard-full",
                ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    MinecraftServer server = src.getServer();
                    var payload = DeathboardManager.buildFullPayload(server);
                    System.out.println("=== Diocles Deathboard (Full) ===");
                    System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(payload));
                    return 1;
                },
                src -> src.hasPermissionLevel(2)));

        // serverday (public) - show current server day
        subcommands.put("serverday", new SubcommandEntry("serverday",
                ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    long day = src.getServer().getOverworld().getTimeOfDay() / 24000L;
                    src.sendFeedback(() -> Text.literal("Server Day: " + day), false);
                    return 1;
                },
                src -> true));

        // debug ping (op) - Test API connectivity with health endpoint fallback
        subcommands.put("debug-ping", new SubcommandEntry("debug-ping",
                ctx -> {
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
                },
                src -> src.hasPermissionLevel(2)));

        // debug scoreboard (op) - Show current scoreboard state
        subcommands.put("debug-scoreboard", new SubcommandEntry("debug-scoreboard",
                ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    MinecraftServer server = src.getServer();
                    Scoreboard sb = server.getScoreboard();
                    ScoreboardObjective obj = sb.getObjectives().stream()
                            .filter(o -> o.getName().equals(DeathboardManager.OBJECTIVE_NAME))
                            .findFirst().orElse(null);

                    if (obj == null) {
                        src.sendFeedback(
                                () -> Text.literal(
                                        "§c[Diocles] No '" + DeathboardManager.OBJECTIVE_NAME + "' objective found."),
                                false);
                        return 1;
                    }

                    src.sendFeedback(() -> Text.literal("§6[Diocles] Current Deathboard:"), false);

                    for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                        String name = player.getName().getString();
                        int score = 0;
                        try {
                            score = sb.getOrCreateScore(player, obj).getScore();
                        } catch (Exception ignored) {
                        }

                        final String finalName = name;
                        final int finalScore = score;
                        src.sendFeedback(() -> Text.literal("§e" + finalName + " §7| §c" + finalScore), false);
                    }
                    return 1;
                },
                src -> src.hasPermissionLevel(2)));

        // debug fullstats (op) - Show full stats from memory
        subcommands.put("debug-fullstats", new SubcommandEntry("debug-fullstats",
                ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    MinecraftServer server = src.getServer();
                    var payload = DeathboardManager.buildFullPayload(server);

                    src.sendFeedback(() -> Text.literal("§6[Diocles] Full Deathboard Stats:"), false);

                    if (payload.isEmpty()) {
                        src.sendFeedback(() -> Text.literal("§7No death data available."), false);
                        return 1;
                    }

                    for (Map.Entry<String, Object> entry : payload.entrySet()) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> playerData = (Map<String, Object>) entry.getValue();
                        String playerName = entry.getKey();
                        Object deathCount = playerData.get("death_count");
                        Object lastDeathTime = playerData.get("last_death_time");

                        String line = String.format("§e%s §7| deaths: §c%s §7| last: §f%s",
                                playerName,
                                deathCount != null ? deathCount.toString() : "0",
                                lastDeathTime != null ? lastDeathTime.toString() : "never");
                        src.sendFeedback(() -> Text.literal(line), false);
                    }
                    return 1;
                },
                src -> src.hasPermissionLevel(2)));

        // register root and attach subcommands
        LiteralArgumentBuilder<ServerCommandSource> root = literal("diocles")
                .executes(ctx -> {
                    ServerCommandSource src = ctx.getSource();
                    src.sendFeedback(() -> Text.literal("§7[Diocles] Available subcommands:"), false);
                    for (var entry : subcommands.values()) {
                        if (entry.permissionCheck.test(src)) {
                            src.sendFeedback(() -> Text.literal(" - §a" + entry.name), false);
                        }
                    }
                    return 1;
                });

        for (var entry : subcommands.values()) {
            root.then(literal(entry.name).requires(entry.permissionCheck).executes(entry.command));
        }

        dispatcher.register(root);
    }
}
