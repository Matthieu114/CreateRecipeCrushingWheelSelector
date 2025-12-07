package com.enormeboze.crushingwheelrecipeselector.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;

/**
 * Packet sent from server to client to sync all selections for a specific wheel
 */
public record SyncSelectionsPacket(
    BlockPos wheelPosition,
    Map<String, ResourceLocation> selections
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSelectionsPacket> TYPE = 
        new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("crushingwheelrecipeselector", "sync_selections"));

    public static final StreamCodec<FriendlyByteBuf, SyncSelectionsPacket> STREAM_CODEC = StreamCodec.of(
        SyncSelectionsPacket::write,
        SyncSelectionsPacket::read
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private static void write(FriendlyByteBuf buf, SyncSelectionsPacket packet) {
        buf.writeBlockPos(packet.wheelPosition);
        
        // Write number of selections
        buf.writeInt(packet.selections.size());
        
        // Write each selection (itemId -> recipeId)
        for (Map.Entry<String, ResourceLocation> entry : packet.selections.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeResourceLocation(entry.getValue());
        }
    }

    private static SyncSelectionsPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        
        // Read number of selections
        int size = buf.readInt();
        
        // Read each selection
        Map<String, ResourceLocation> selections = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String itemId = buf.readUtf();
            ResourceLocation recipeId = buf.readResourceLocation();
            selections.put(itemId, recipeId);
        }
        
        return new SyncSelectionsPacket(pos, selections);
    }
}
