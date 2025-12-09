package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.client.ClientPacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Sent from server to client when player starts linking process.
 * Contains the selected wheel position, valid targets (green), and invalid targets (red).
 */
public class StartLinkingPacket {

    private final BlockPos selectedWheel;
    private final Set<BlockPos> validTargets;
    private final Set<BlockPos> invalidTargets;

    public StartLinkingPacket(BlockPos selectedWheel, Set<BlockPos> validTargets, Set<BlockPos> invalidTargets) {
        this.selectedWheel = selectedWheel;
        this.validTargets = validTargets;
        this.invalidTargets = invalidTargets;
    }

    // Decoder constructor
    public StartLinkingPacket(FriendlyByteBuf buf) {
        this.selectedWheel = buf.readBlockPos();

        // Read valid targets
        int validCount = buf.readInt();
        this.validTargets = new HashSet<>(validCount);
        for (int i = 0; i < validCount; i++) {
            validTargets.add(buf.readBlockPos());
        }

        // Read invalid targets
        int invalidCount = buf.readInt();
        this.invalidTargets = new HashSet<>(invalidCount);
        for (int i = 0; i < invalidCount; i++) {
            invalidTargets.add(buf.readBlockPos());
        }
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(selectedWheel);

        // Write valid targets
        buf.writeInt(validTargets.size());
        for (BlockPos pos : validTargets) {
            buf.writeBlockPos(pos);
        }

        // Write invalid targets
        buf.writeInt(invalidTargets.size());
        for (BlockPos pos : invalidTargets) {
            buf.writeBlockPos(pos);
        }
    }

    // Handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Handle on client side
            ClientPacketHandler.handleStartLinking(selectedWheel, validTargets, invalidTargets);
        });
        context.setPacketHandled(true);
        return true;
    }
}
