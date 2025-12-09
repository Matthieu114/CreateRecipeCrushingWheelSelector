package com.enormeboze.crushingwheelrecipeselector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * Helper to open screens on the client side.
 * Separated to avoid classloading issues.
 */
@OnlyIn(Dist.CLIENT)
public class ClientScreenHelper {

    public static void openRecipeSelectorScreen(BlockPos wheelPos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            mc.setScreen(new RecipeSelectorScreen(wheelPos));
        }
    }
}
