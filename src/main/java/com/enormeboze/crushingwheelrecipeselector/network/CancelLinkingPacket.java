package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.client.ClientPacketHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from server to client when linking is cancelled or completed
 * Clears all wheel highlighting
 */
public class CancelLinkingPacket {

    public CancelLinkingPacket() {
        // No data
    }

    // Decoder constructor
    public CancelLinkingPacket(FriendlyByteBuf buf) {
        // No data to read
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        // No data to write
    }

    // Handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Handle on client side
            ClientPacketHandler.handleCancelLinking();
        });
        context.setPacketHandled(true);
        return true;
    }
}
