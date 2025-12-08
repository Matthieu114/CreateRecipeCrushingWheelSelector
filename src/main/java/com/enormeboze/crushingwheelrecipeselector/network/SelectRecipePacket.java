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
 * Sent from client to server when player confirms a recipe selection
 */
public record SelectRecipePacket(
        BlockPos wheelPos,
        String inputItemId,
        ResourceLocation recipeId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectRecipePacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrushingWheelRecipeSelector.MOD_ID, "select_recipe"));

    public static final StreamCodec<FriendlyByteBuf, SelectRecipePacket> STREAM_CODEC =
            StreamCodec.of(SelectRecipePacket::write, SelectRecipePacket::read);

    public static void write(FriendlyByteBuf buf, SelectRecipePacket packet) {
        buf.writeBlockPos(packet.wheelPos);
        buf.writeUtf(packet.inputItemId);
        buf.writeResourceLocation(packet.recipeId);
    }

    public static SelectRecipePacket read(FriendlyByteBuf buf) {
        return new SelectRecipePacket(
                buf.readBlockPos(),
                buf.readUtf(),
                buf.readResourceLocation()
        );
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SelectRecipePacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                BlockPos wheelPos = packet.wheelPos();
                String inputItemId = packet.inputItemId();
                ResourceLocation recipeId = packet.recipeId();

                CrushingWheelRecipeSelector.LOGGER.debug("Player {} selected recipe {} for item {} at {}",
                        serverPlayer.getName().getString(), recipeId, inputItemId, wheelPos);

                // Save the selection to world data
                CrushingWheelSelections selections = CrushingWheelSelections.get(serverPlayer.serverLevel());
                if (selections != null) {
                    selections.setPreferredRecipe(wheelPos, inputItemId, recipeId);
                }
            }
        });
    }
}