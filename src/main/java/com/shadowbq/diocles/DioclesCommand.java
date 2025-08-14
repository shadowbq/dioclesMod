package com.shadowbq.diocles;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

// Import subcommands
import com.shadowbq.diocles.subcommands.admin.AnnounceCommand;
import com.shadowbq.diocles.subcommands.admin.DeathboardFullCommand;
import com.shadowbq.diocles.subcommands.admin.VersionCommand;
import com.shadowbq.diocles.subcommands.debug.DebugFullstatsCommand;
import com.shadowbq.diocles.subcommands.debug.DebugPingCommand;
import com.shadowbq.diocles.subcommands.debug.DebugScoreboardCommand;
import com.shadowbq.diocles.subcommands.player.DeathboardCommand;
import com.shadowbq.diocles.subcommands.player.ServerdayCommand;

import java.util.*;
import java.util.function.Predicate;

import static net.minecraft.server.command.CommandManager.argument;
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
        // version (admin)
        subcommands.put("version", new SubcommandEntry("version",
                VersionCommand::execute,
                DioclesPermissions::hasAdminPermission));

        // announce (admin) - Send message to all players, with optional custom text
        subcommands.put("announce", new SubcommandEntry("announce",
                AnnounceCommand::execute,
                DioclesPermissions::hasAdminPermission));

        // deathboard (player) — top10 + your count in chat
        subcommands.put("deathboard", new SubcommandEntry("deathboard",
                DeathboardCommand::execute,
                DioclesPermissions::hasPublicPermission));

        // deathboard-full (admin) — prints full scoreboard JSON to console
        subcommands.put("deathboard-full", new SubcommandEntry("deathboard-full",
                DeathboardFullCommand::execute,
                DioclesPermissions::hasAdminPermission));

        // serverday (player) - show current server day
        subcommands.put("serverday", new SubcommandEntry("serverday",
                ServerdayCommand::execute,
                DioclesPermissions::hasPublicPermission));

        // debug ping (admin) - Test API connectivity with health endpoint fallback
        subcommands.put("debug-ping", new SubcommandEntry("debug-ping",
                DebugPingCommand::execute,
                DioclesPermissions::hasAdminPermission));

        // debug scoreboard (admin) - Show current scoreboard state
        subcommands.put("debug-scoreboard", new SubcommandEntry("debug-scoreboard",
                DebugScoreboardCommand::execute,
                DioclesPermissions::hasAdminPermission));

        // debug fullstats (admin) - Show full stats from memory
        subcommands.put("debug-fullstats", new SubcommandEntry("debug-fullstats",
                DebugFullstatsCommand::execute,
                DioclesPermissions::hasAdminPermission));

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
            if (entry.name.equals("announce")) {
                // Special handling for announce command with optional text argument
                root.then(literal(entry.name)
                    .requires(entry.permissionCheck)
                    .executes(entry.command) // No argument - uses random quote
                    .then(argument("text", StringArgumentType.greedyString())
                        .executes(entry.command))); // With argument - uses custom text
            } else {
                root.then(literal(entry.name).requires(entry.permissionCheck).executes(entry.command));
            }
        }

        dispatcher.register(root);
    }
}
