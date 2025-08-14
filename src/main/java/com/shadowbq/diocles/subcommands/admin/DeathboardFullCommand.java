package com.shadowbq.diocles.subcommands.admin;

import com.google.gson.GsonBuilder;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import com.shadowbq.diocles.DeathboardManager;

/**
 * Admin command that prints full deathboard JSON to console
 */
public class DeathboardFullCommand {

    public static int execute(CommandContext<ServerCommandSource> ctx) {
        ServerCommandSource src = ctx.getSource();
        MinecraftServer server = src.getServer();
        var payload = DeathboardManager.buildFullPayload(server);
        System.out.println("=== Diocles Deathboard (Full) ===");
        System.out.println(new GsonBuilder().setPrettyPrinting().create().toJson(payload));
        return 1;
    }
}
