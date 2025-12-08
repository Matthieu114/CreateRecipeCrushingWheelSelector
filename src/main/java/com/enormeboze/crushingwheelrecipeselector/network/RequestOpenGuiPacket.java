package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.Map;

/**
 * Sent from client to server when player wants to open the GUI
 */
public record RequestOpenGuiPacket(BlockPos wheelPos) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<RequestOpenGuiPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrushingWheelRecipeSelector.MOD_ID, "request_open_gui"));

    public static final StreamCodec<FriendlyByteBuf, RequestOpenGuiPacket> STREAM_CODEC =
            StreamCodec.of(RequestOpenGuiPacket::write, RequestOpenGuiPacket::read);

    public static void write(FriendlyByteBuf buf, RequestOpenGuiPacket packet) {
        buf.writeBlockPos(packet.wheelPos);
    }

    public static RequestOpenGuiPacket read(FriendlyByteBuf buf) {
        return new RequestOpenGuiPacket(buf.readBlockPos());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(RequestOpenGuiPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer serverPlayer) {
                BlockPos wheelPos = packet.wheelPos();

                // Get current selections for this wheel's group
                CrushingWheelSelections selections = CrushingWheelSelections.get(serverPlayer.serverLevel());
                Map<String, ResourceLocation> prefs = selections != null
                        ? selections.getAllPreferences(wheelPos)
                        : Map.of();

                // Send sync packet with openGui=true
                PacketDistributor.sendToPlayer(serverPlayer,
                        new SyncSelectionsPacket(wheelPos, prefs, true));
            }
        });
    }
}