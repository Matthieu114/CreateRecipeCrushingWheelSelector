package com.enormeboze.crushingwheelrecipeselector.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server when player clears a recipe selection
 */
public record ClearRecipePacket(
    BlockPos wheelPosition,
    String inputItemId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClearRecipePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("crushingwheelrecipeselector", "clear_recipe"));

    public static final StreamCodec<FriendlyByteBuf, ClearRecipePacket> STREAM_CODEC = StreamCodec.of(
        ClearRecipePacket::write,
        ClearRecipePacket::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buf, ClearRecipePacket packet) {
        buf.writeBlockPos(packet.wheelPosition);
        buf.writeUtf(packet.inputItemId);
    }

    private static ClearRecipePacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String itemId = buf.readUtf();
        return new ClearRecipePacket(pos, itemId);
    }
}
