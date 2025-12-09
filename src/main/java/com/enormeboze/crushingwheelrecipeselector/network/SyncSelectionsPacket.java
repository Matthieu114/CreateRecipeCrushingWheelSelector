package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Sent from server to client to sync selections and optionally open GUI
 */
public class SyncSelectionsPacket {

    private final BlockPos wheelPos;
    private final Map<String, ResourceLocation> selections;
    private final boolean openGui;

    public SyncSelectionsPacket(BlockPos wheelPos, Map<String, ResourceLocation> selections, boolean openGui) {
        this.wheelPos = wheelPos;
        this.selections = selections;
        this.openGui = openGui;
    }

    // Decoder constructor
    public SyncSelectionsPacket(FriendlyByteBuf buf) {
        this.wheelPos = buf.readBlockPos();
        int size = buf.readInt();
        this.selections = new HashMap<>();
        for (int i = 0; i < size; i++) {
            String key = buf.readUtf();
            ResourceLocation value = buf.readResourceLocation();
            selections.put(key, value);
        }
        this.openGui = buf.readBoolean();
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(wheelPos);
        buf.writeInt(selections.size());
        for (Map.Entry<String, ResourceLocation> entry : selections.entrySet()) {
            buf.writeUtf(entry.getKey());
            buf.writeResourceLocation(entry.getValue());
        }
        buf.writeBoolean(openGui);
    }

    // Handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Handle on client side
            ClientPacketHandler.handleSyncSelections(wheelPos, selections, openGui);
        });
        context.setPacketHandled(true);
        return true;
    }

    // Getters for client handler
    public BlockPos getWheelPos() { return wheelPos; }
    public Map<String, ResourceLocation> getSelections() { return selections; }
    public boolean shouldOpenGui() { return openGui; }
}
