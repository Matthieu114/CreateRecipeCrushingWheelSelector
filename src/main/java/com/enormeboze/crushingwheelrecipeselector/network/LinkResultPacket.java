package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from server to client when linking succeeds or fails
 * Triggers appropriate particle effects
 */
public class LinkResultPacket {

    private final boolean success;
    private final BlockPos pos1;
    private final BlockPos pos2;

    public LinkResultPacket(boolean success, BlockPos pos1, BlockPos pos2) {
        this.success = success;
        this.pos1 = pos1;
        this.pos2 = pos2;
    }

    // Decoder constructor
    public LinkResultPacket(FriendlyByteBuf buf) {
        this.success = buf.readBoolean();
        this.pos1 = buf.readBlockPos();
        this.pos2 = buf.readBlockPos();
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBoolean(success);
        buf.writeBlockPos(pos1);
        buf.writeBlockPos(pos2);
    }

    // Handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Handle on client side
            ClientPacketHandler.handleLinkResult(success, pos1, pos2);
        });
        context.setPacketHandled(true);
        return true;
    }
}
