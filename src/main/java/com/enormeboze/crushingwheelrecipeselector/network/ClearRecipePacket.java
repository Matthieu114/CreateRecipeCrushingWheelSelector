package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to server when player clears a recipe selection
 */
public class ClearRecipePacket {

    private final BlockPos wheelPos;
    private final String inputItemId;

    public ClearRecipePacket(BlockPos wheelPos, String inputItemId) {
        this.wheelPos = wheelPos;
        this.inputItemId = inputItemId;
    }

    // Decoder constructor
    public ClearRecipePacket(FriendlyByteBuf buf) {
        this.wheelPos = buf.readBlockPos();
        this.inputItemId = buf.readUtf();
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(wheelPos);
        buf.writeUtf(inputItemId);
    }

    // Handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CrushingWheelRecipeSelector.LOGGER.debug("Player {} cleared recipe for item {} at {}",
                        player.getName().getString(), inputItemId, wheelPos);

                // Clear the selection from world data
                CrushingWheelSelections selections = CrushingWheelSelections.get(player.serverLevel());
                if (selections != null) {
                    selections.clearPreferredRecipe(wheelPos, inputItemId);
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
