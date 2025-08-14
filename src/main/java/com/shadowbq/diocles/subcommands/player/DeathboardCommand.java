package com.shadowbq.diocles.subcommands.player;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import com.shadowbq.diocles.DeathboardManager;

import java.util.*;

/**
 * Player command that shows top 10 death leaderboard and player's own death
 * count
 */
public class DeathboardCommand {

    public static int execute(CommandContext<ServerCommandSource> ctx) {
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
    }
}
