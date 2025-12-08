package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from client to server when player clears a recipe selection
 */
public record ClearRecipePacket(
        BlockPos wheelPos,
        String inputItemId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClearRecipePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrushingWheelRecipeSelector.MOD_ID, "clear_recipe"));

    public static final StreamCodec<FriendlyByteBuf, ClearRecipePacket> STREAM_CODEC =
            StreamCodec.of(ClearRecipePacket::write, ClearRecipePacket::read);

    public static void write(FriendlyByteBuf buf, ClearRecipePacket packet) {
        buf.writeBlockPos(packet.wheelPos);
        buf.writeUtf(packet.inputItemId);
    }

    public static ClearRecipePacket read(FriendlyByteBuf buf) {
        return new ClearRecipePacket(
                buf.readBlockPos(),
                buf.readUtf()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(ClearRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                BlockPos wheelPos = packet.wheelPos();
                String inputItemId = packet.inputItemId();

                CrushingWheelRecipeSelector.LOGGER.debug("Player {} cleared recipe for item {} at {}",
                        serverPlayer.getName().getString(), inputItemId, wheelPos);

                // Clear the selection from world data
                CrushingWheelSelections selections = CrushingWheelSelections.get(serverPlayer.serverLevel());
                if (selections != null) {
                    selections.clearPreferredRecipe(wheelPos, inputItemId);
                }
            }
        });
    }
}