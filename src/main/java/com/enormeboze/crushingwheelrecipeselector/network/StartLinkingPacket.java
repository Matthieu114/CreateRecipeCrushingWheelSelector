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
 * Sent from server to client when player starts linking process.
 * Contains the selected wheel position, valid targets (green), and invalid targets (red).
 */
public record StartLinkingPacket(
        BlockPos selectedWheel,
        Set<BlockPos> validTargets,
        Set<BlockPos> invalidTargets
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<StartLinkingPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrushingWheelRecipeSelector.MOD_ID, "start_linking"));

    public static final StreamCodec<FriendlyByteBuf, StartLinkingPacket> STREAM_CODEC =
            StreamCodec.of(StartLinkingPacket::write, StartLinkingPacket::read);

    public static void write(FriendlyByteBuf buf, StartLinkingPacket packet) {
        buf.writeBlockPos(packet.selectedWheel);

        // Write valid targets
        buf.writeInt(packet.validTargets.size());
        for (BlockPos pos : packet.validTargets) {
            buf.writeBlockPos(pos);
        }

        // Write invalid targets
        buf.writeInt(packet.invalidTargets.size());
        for (BlockPos pos : packet.invalidTargets) {
            buf.writeBlockPos(pos);
        }
    }

    public static StartLinkingPacket read(FriendlyByteBuf buf) {
        BlockPos selected = buf.readBlockPos();

        // Read valid targets
        int validCount = buf.readInt();
        Set<BlockPos> valid = new HashSet<>(validCount);
        for (int i = 0; i < validCount; i++) {
            valid.add(buf.readBlockPos());
        }

        // Read invalid targets
        int invalidCount = buf.readInt();
        Set<BlockPos> invalid = new HashSet<>(invalidCount);
        for (int i = 0; i < invalidCount; i++) {
            invalid.add(buf.readBlockPos());
        }

        return new StartLinkingPacket(selected, valid, invalid);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(StartLinkingPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Client-side: Start highlighting with both valid and invalid targets
            WheelHighlightRenderer.setSelectedWheel(
                    packet.selectedWheel(),
                    packet.validTargets(),
                    packet.invalidTargets()
            );
        });
    }
}