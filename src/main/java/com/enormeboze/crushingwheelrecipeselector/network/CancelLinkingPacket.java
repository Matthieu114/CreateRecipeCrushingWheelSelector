package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.client.WheelHighlightRenderer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server to client when linking is cancelled or completed
 * Clears all wheel highlighting
 */
public record CancelLinkingPacket() implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CancelLinkingPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrushingWheelRecipeSelector.MOD_ID, "cancel_linking"));

    public static final StreamCodec<FriendlyByteBuf, CancelLinkingPacket> STREAM_CODEC =
            StreamCodec.of(CancelLinkingPacket::write, CancelLinkingPacket::read);

    public static void write(FriendlyByteBuf buf, CancelLinkingPacket packet) {
        // No data to write
    }

    public static CancelLinkingPacket read(FriendlyByteBuf buf) {
        return new CancelLinkingPacket();
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(CancelLinkingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: Clear all highlighting
            WheelHighlightRenderer.clearSelection();
        });
    }
}
