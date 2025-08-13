package com.shadowbq.diocles;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.network.ServerPlayerEntity;

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

        // Register player death event using Fabric events instead of Mixin
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (entity instanceof ServerPlayerEntity player) {
                System.out.println("[Diocles] Player death detected via event: " + player.getName().getString());
                DeathboardManager.handleDeath(player.getServer(), player);
            }
        });
    }
}
