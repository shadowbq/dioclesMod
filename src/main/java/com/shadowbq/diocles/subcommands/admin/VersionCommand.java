package com.shadowbq.diocles.subcommands.admin;

import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import com.shadowbq.diocles.DioclesConstants;

/**
 * Admin command that displays the mod version
 */
public class VersionCommand {

    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        src.sendFeedback(() -> Text.literal("Diocles Mod Version: " + DioclesConstants.VERSION), false);
        return 1;
    }
}
