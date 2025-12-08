package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.client.LinkingParticles;
import com.enormeboze.crushingwheelrecipeselector.client.WheelHighlightRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Sent from server to client when linking succeeds or fails
 * Triggers appropriate particle effects
 */
public record LinkResultPacket(
        boolean success,
        BlockPos pos1,
        BlockPos pos2  // Only used if success=true
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<LinkResultPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrushingWheelRecipeSelector.MOD_ID, "link_result"));

    public static final StreamCodec<FriendlyByteBuf, LinkResultPacket> STREAM_CODEC =
            StreamCodec.of(LinkResultPacket::write, LinkResultPacket::read);

    public static void write(FriendlyByteBuf buf, LinkResultPacket packet) {
        buf.writeBoolean(packet.success);
        buf.writeBlockPos(packet.pos1);
        buf.writeBlockPos(packet.pos2);
    }

    public static LinkResultPacket read(FriendlyByteBuf buf) {
        boolean success = buf.readBoolean();
        BlockPos pos1 = buf.readBlockPos();
        BlockPos pos2 = buf.readBlockPos();
        return new LinkResultPacket(success, pos1, pos2);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(LinkResultPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Clear highlighting
            WheelHighlightRenderer.clearSelection();
            
            // Spawn appropriate particles
            if (packet.success()) {
                LinkingParticles.spawnLinkSuccessParticles(packet.pos1(), packet.pos2());
            } else {
                LinkingParticles.spawnLinkErrorParticles(packet.pos1());
            }
        });
    }
}
