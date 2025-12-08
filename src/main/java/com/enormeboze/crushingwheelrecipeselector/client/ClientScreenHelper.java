package com.enormeboze.crushingwheelrecipeselector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class ClientScreenHelper {

    /**
     * Opens the RecipeSelectorScreen.
     * This method can ONLY be called on the client.
     */
    public static void openRecipeSelectorScreen(BlockPos wheelPos) {
        // The server cannot load the class containing this code, so it will not crash.
        // It's safe to use Minecraft.getInstance() here.
        if (Minecraft.getInstance().level != null) {
            Minecraft.getInstance().setScreen(new RecipeSelectorScreen(wheelPos));
        }
    }
}