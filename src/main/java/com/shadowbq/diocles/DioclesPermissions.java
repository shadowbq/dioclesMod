package com.shadowbq.diocles;

import net.minecraft.server.command.ServerCommandSource;

/**
 * Utility class for handling permissions in Diocles commands.
 * Centralizes permission logic and makes it easy to modify permission
 * requirements.
 */
public class DioclesPermissions {

    /**
     * Check if the command source has administrator permissions.
     * Currently requires operator level 2.
     * 
     * @param source The command source to check
     * @return true if the source has admin permissions
     */
    public static boolean hasAdminPermission(ServerCommandSource source) {
        return source.hasPermissionLevel(2);
    }

    /**
     * Check if the command source can use public commands.
     * Public commands are available to all players.
     * 
     * @param source The command source to check
     * @return true (always allows public access)
     */
    public static boolean hasPublicPermission(ServerCommandSource source) {
        return true;
    }
}
