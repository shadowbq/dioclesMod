package com.shadowbq.diocles.subcommands.debug;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.text.Text;
import com.shadowbq.diocles.DeathboardManager;

/**
 * Debug command that shows current scoreboard state
 */
public class DebugScoreboardCommand {

    public static int execute(CommandContext<ServerCommandSource> ctx) {
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
    }
}
