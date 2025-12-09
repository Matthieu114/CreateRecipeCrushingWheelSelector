package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Sent from client to server when player wants to open the GUI
 */
public class RequestOpenGuiPacket {

    private final BlockPos wheelPos;

    public RequestOpenGuiPacket(BlockPos wheelPos) {
        this.wheelPos = wheelPos;
    }

    // Decoder constructor
    public RequestOpenGuiPacket(FriendlyByteBuf buf) {
        this.wheelPos = buf.readBlockPos();
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(wheelPos);
    }

    // Handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                // Get current selections for this wheel's group
                CrushingWheelSelections selections = CrushingWheelSelections.get(player.serverLevel());
                Map<String, ResourceLocation> prefs = selections != null
                        ? selections.getAllPreferences(wheelPos)
                        : Map.of();

                // Send sync packet with openGui=true
                ModNetworking.sendToPlayer(player, new SyncSelectionsPacket(wheelPos, prefs, true));
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
