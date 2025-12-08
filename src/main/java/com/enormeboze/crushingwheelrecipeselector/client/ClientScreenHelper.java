package com.enormeboze.crushingwheelrecipeselector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientScreenHelper {

    /**
     * Opens the modern RecipeSelectorScreen.
     * This method can ONLY be called on the client.
     */
    public static void openRecipeSelectorScreen(BlockPos wheelPos) {
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().setScreen(new RecipeSelectorScreen(wheelPos));
        }
    }
}