package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import com.enormeboze.crushingwheelrecipeselector.client.ClientSelectionCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * Handles network packet registration and packet handling
 */
public class ModNetworking {

    /**
     * Register all network packets
     */
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");
        
        // Register select recipe packet (client -> server)
        registrar.playToServer(
            SelectRecipePacket.TYPE,
            SelectRecipePacket.STREAM_CODEC,
            ModNetworking::handleSelectRecipe
        );
        
        // Register clear recipe packet (client -> server)
        registrar.playToServer(
            ClearRecipePacket.TYPE,
            ClearRecipePacket.STREAM_CODEC,
            ModNetworking::handleClearRecipe
        );
        
        // Register sync selections packet (server -> client)
        registrar.playToClient(
            SyncSelectionsPacket.TYPE,
            SyncSelectionsPacket.STREAM_CODEC,
            ModNetworking::handleSyncSelections
        );
        
        CrushingWheelRecipeSelector.LOGGER.info("Registered network packets");
    }

    /**
     * Handle SelectRecipePacket on the server
     */
    private static void handleSelectRecipe(SelectRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ServerLevel level = serverPlayer.serverLevel();
                
                // Get or create the selections data
                CrushingWheelSelections selections = CrushingWheelSelections.get(level);
                
                // Save the selection
                selections.setPreferredRecipe(
                    packet.wheelPosition(),
                    packet.inputItemId(),
                    packet.recipeId()
                );
                
                CrushingWheelRecipeSelector.LOGGER.info(
                    "Player {} selected recipe {} for item {} at wheel {}",
                    serverPlayer.getName().getString(),
                    packet.recipeId(),
                    packet.inputItemId(),
                    packet.wheelPosition()
                );
            }
        });
    }

    /**
     * Handle ClearRecipePacket on the server
     */
    private static void handleClearRecipe(ClearRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                ServerLevel level = serverPlayer.serverLevel();
                
                // Get the selections data
                CrushingWheelSelections selections = CrushingWheelSelections.get(level);
                
                // Clear the selection for this item at this wheel
                selections.clearPreferredRecipe(packet.wheelPosition(), packet.inputItemId());
                
                CrushingWheelRecipeSelector.LOGGER.info(
                    "Player {} cleared recipe selection for item {} at wheel {}",
                    serverPlayer.getName().getString(),
                    packet.inputItemId(),
                    packet.wheelPosition()
                );
            }
        });
    }

    /**
     * Handle SyncSelectionsPacket on the client
     */
    private static void handleSyncSelections(SyncSelectionsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Update client-side cache with synced selections
            ClientSelectionCache.updateSelections(packet.wheelPosition(), packet.selections());
            
            CrushingWheelRecipeSelector.LOGGER.info(
                "Received {} selections for wheel at {}",
                packet.selections().size(),
                packet.wheelPosition()
            );
        });
    }
}
