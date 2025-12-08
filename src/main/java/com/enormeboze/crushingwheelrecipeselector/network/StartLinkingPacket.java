package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.client.WheelHighlightRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashSet;
import java.util.Set;

/**
 * Sent from server to client when player starts linking process
 * Contains the selected wheel position and all nearby wheel positions
 */
public record StartLinkingPacket(
        BlockPos selectedWheel,
        Set<BlockPos> nearbyWheels
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StartLinkingPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrushingWheelRecipeSelector.MOD_ID, "start_linking"));

    public static final StreamCodec<FriendlyByteBuf, StartLinkingPacket> STREAM_CODEC =
            StreamCodec.of(StartLinkingPacket::write, StartLinkingPacket::read);

    public static void write(FriendlyByteBuf buf, StartLinkingPacket packet) {
        buf.writeBlockPos(packet.selectedWheel);
        buf.writeInt(packet.nearbyWheels.size());
        for (BlockPos pos : packet.nearbyWheels) {
            buf.writeBlockPos(pos);
        }
    }

    public static StartLinkingPacket read(FriendlyByteBuf buf) {
        BlockPos selected = buf.readBlockPos();
        int count = buf.readInt();
        Set<BlockPos> nearby = new HashSet<>();
        for (int i = 0; i < count; i++) {
            nearby.add(buf.readBlockPos());
        }
        return new StartLinkingPacket(selected, nearby);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartLinkingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: Start highlighting
            WheelHighlightRenderer.setSelectedWheel(packet.selectedWheel(), packet.nearbyWheels());
        });
    }
}
