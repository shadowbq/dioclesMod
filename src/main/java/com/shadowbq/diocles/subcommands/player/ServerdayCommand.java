package com.shadowbq.diocles.subcommands.player;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

/**
 * Player command that shows the current server day
 */
public class ServerdayCommand {

    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        long day = src.getServer().getOverworld().getTimeOfDay() / 24000L;
        src.sendFeedback(() -> Text.literal("Server Day: " + day), false);
        return 1;
    }
}
