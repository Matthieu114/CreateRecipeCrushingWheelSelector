package com.enormeboze.crushingwheelrecipeselector.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server when player selects a recipe for a crushing wheel
 */
public record SelectRecipePacket(
    BlockPos wheelPosition,
    String inputItemId,
    ResourceLocation recipeId
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SelectRecipePacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("crushingwheelrecipeselector", "select_recipe"));

    public static final StreamCodec<FriendlyByteBuf, SelectRecipePacket> STREAM_CODEC = StreamCodec.of(
        SelectRecipePacket::write,
        SelectRecipePacket::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buf, SelectRecipePacket packet) {
        buf.writeBlockPos(packet.wheelPosition);
        buf.writeUtf(packet.inputItemId);
        buf.writeResourceLocation(packet.recipeId);
    }

    private static SelectRecipePacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        String itemId = buf.readUtf();
        ResourceLocation recipeId = buf.readResourceLocation();
        return new SelectRecipePacket(pos, itemId, recipeId);
    }
}
