package com.shadowbq.diocles;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;

public class DioclesMod implements ModInitializer {
    @Override
    public void onInitialize() {
        // Register commands
        CommandRegistrationCallback.EVENT
                .register((dispatcher, registryAccess, environment) -> DioclesCommand.register(dispatcher));

        // Initialize DeathboardManager when server starts
        ServerLifecycleEvents.SERVER_STARTED.register(server -> DeathboardManager.init(server));

        // Register tick event to check for server day change
        ServerTickEvents.END_SERVER_TICK.register(server -> DeathboardManager.checkDayAndSync(server));

        // Player death detection via Mixin (implemented in PlayerEntityMixin)
        // The mixin will call DeathboardManager.handleDeath() when a player dies
    }
}
