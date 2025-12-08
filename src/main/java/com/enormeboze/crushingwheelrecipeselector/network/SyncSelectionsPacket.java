package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.client.ClientScreenHelper;
import com.enormeboze.crushingwheelrecipeselector.client.ClientSelectionCache;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.Map;

public record SyncSelectionsPacket(
        BlockPos wheelPos,
        Map<String, ResourceLocation> selections,
        boolean openGui  // Flag to control whether to open GUI after sync
) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<SyncSelectionsPacket> TYPE =
            new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(CrushingWheelRecipeSelector.MOD_ID, "sync_selections"));

    public static final StreamCodec<FriendlyByteBuf, SyncSelectionsPacket> STREAM_CODEC =
            StreamCodec.of(SyncSelectionsPacket::write, SyncSelectionsPacket::read);

    public static void write(FriendlyByteBuf buf, SyncSelectionsPacket packet) {
        buf.writeBlockPos(packet.wheelPos);
        buf.writeInt(packet.selections.size());
        for (Map.Entry<String, ResourceLocation> entry : packet.selections.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeResourceLocation(entry.getValue());
        }
        buf.writeBoolean(packet.openGui);
    }

    public static SyncSelectionsPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int size = buf.readInt();
        Map<String, ResourceLocation> selections = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            ResourceLocation value = buf.readResourceLocation();
            selections.put(key, value);
        }
        boolean openGui = buf.readBoolean();
        return new SyncSelectionsPacket(pos, selections, openGui);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handleSyncSelections(SyncSelectionsPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            // Always update cache
            ClientSelectionCache.updateSelections(packet.wheelPos(), packet.selections());

            // Only open GUI if requested
            if (packet.openGui()) {
                ClientScreenHelper.openRecipeSelectorScreen(packet.wheelPos());
            }
        });
    }
}