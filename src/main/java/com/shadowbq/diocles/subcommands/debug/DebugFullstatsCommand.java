package com.shadowbq.diocles.subcommands.debug;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.shadowbq.diocles.DeathboardManager;

import java.util.Map;

/**
 * Debug command that shows full stats from memory
 */
public class DebugFullstatsCommand {

    public static int execute(CommandContext<ServerCommandSource> ctx) {
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
    }
}
