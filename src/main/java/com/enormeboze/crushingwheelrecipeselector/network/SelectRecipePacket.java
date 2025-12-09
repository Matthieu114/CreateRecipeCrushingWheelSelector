package com.enormeboze.crushingwheelrecipeselector.network;

import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelSelections;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent from client to server when player confirms a recipe selection
 */
public class SelectRecipePacket {

    private final BlockPos wheelPos;
    private final String inputItemId;
    private final ResourceLocation recipeId;

    public SelectRecipePacket(BlockPos wheelPos, String inputItemId, ResourceLocation recipeId) {
        this.wheelPos = wheelPos;
        this.inputItemId = inputItemId;
        this.recipeId = recipeId;
    }

    // Decoder constructor
    public SelectRecipePacket(FriendlyByteBuf buf) {
        this.wheelPos = buf.readBlockPos();
        this.inputItemId = buf.readUtf();
        this.recipeId = buf.readResourceLocation();
    }

    // Encoder
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(wheelPos);
        buf.writeUtf(inputItemId);
        buf.writeResourceLocation(recipeId);
    }

    // Handler
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                CrushingWheelRecipeSelector.LOGGER.debug("Player {} selected recipe {} for item {} at {}",
                        player.getName().getString(), recipeId, inputItemId, wheelPos);

                // Save the selection to world data
                CrushingWheelSelections selections = CrushingWheelSelections.get(player.serverLevel());
                if (selections != null) {
                    selections.setPreferredRecipe(wheelPos, inputItemId, recipeId);
                }
            }
        });
        context.setPacketHandled(true);
        return true;
    }
}
