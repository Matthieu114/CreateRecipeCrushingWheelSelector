package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

public class ModNetworking {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(ModNetworking::registerPayloads);
    }

    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar(CrushingWheelRecipeSelector.MOD_ID);

        // Client -> Server: Request to open GUI
        registrar.playToServer(
                RequestOpenGuiPacket.TYPE,
                RequestOpenGuiPacket.STREAM_CODEC,
                RequestOpenGuiPacket::handle
        );

        // Client -> Server: Select a recipe
        registrar.playToServer(
                SelectRecipePacket.TYPE,
                SelectRecipePacket.STREAM_CODEC,
                SelectRecipePacket::handle
        );

        // Client -> Server: Clear a recipe selection
        registrar.playToServer(
                ClearRecipePacket.TYPE,
                ClearRecipePacket.STREAM_CODEC,
                ClearRecipePacket::handle
        );

        // Server -> Client: Sync selections (and optionally open GUI)
        registrar.playToClient(
                SyncSelectionsPacket.TYPE,
                SyncSelectionsPacket.STREAM_CODEC,
                SyncSelectionsPacket::handleSyncSelections
        );

        // Server -> Client: Start linking (show highlights)
        registrar.playToClient(
                StartLinkingPacket.TYPE,
                StartLinkingPacket.STREAM_CODEC,
                StartLinkingPacket::handle
        );

        // Server -> Client: Cancel linking (clear highlights)
        registrar.playToClient(
                CancelLinkingPacket.TYPE,
                CancelLinkingPacket.STREAM_CODEC,
                CancelLinkingPacket::handle
        );

        // Server -> Client: Link result (success/error particles)
        registrar.playToClient(
                LinkResultPacket.TYPE,
                LinkResultPacket.STREAM_CODEC,
                LinkResultPacket::handle
        );

        CrushingWheelRecipeSelector.LOGGER.info("Network packets registered");
    }
}