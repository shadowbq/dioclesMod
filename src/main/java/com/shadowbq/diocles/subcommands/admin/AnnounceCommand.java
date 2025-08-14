package com.shadowbq.diocles.subcommands.admin;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import com.shadowbq.diocles.AnnouncementQuotes;

/**
 * Admin command that sends announcements to all players
 */
public class AnnounceCommand {

    public static int execute(CommandContext<ServerCommandSource> ctx) {
        MinecraftServer server = ctx.getSource().getServer();
        String message;

        // Try to get custom text argument, fall back to random quote if not provided
        try {
            message = StringArgumentType.getString(ctx, "text");
        } catch (Exception e) {
            // No text provided, use random quote
            message = AnnouncementQuotes.getRandomQuote();
        }

        // Send announcement to all players
        for (ServerPlayerEntity p : server.getPlayerManager().getPlayerList()) {
            p.sendMessage(Text.literal("§l§e[ANNOUNCEMENT] §r" + message), false);
        }

        // Send feedback to command source
        ctx.getSource().sendFeedback(() -> Text.literal("§aAnnouncement sent to all players"), false);
        return 1;
    }
}
