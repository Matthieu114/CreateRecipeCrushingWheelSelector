package com.enormeboze.crushingwheelrecipeselector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * Spawns particles for linking feedback.
 */
@OnlyIn(Dist.CLIENT)
public class LinkingParticles {

    private static final Random random = new Random();

    public static void spawnLinkSuccessParticles(BlockPos pos1, BlockPos pos2) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        // Spawn happy green particles at both wheels
        spawnSuccessParticlesAt(pos1);
        spawnSuccessParticlesAt(pos2);

        // Spawn particles along the connection line
        Vec3 start = Vec3.atCenterOf(pos1);
        Vec3 end = Vec3.atCenterOf(pos2);
        int steps = 10;

        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            double x = start.x + (end.x - start.x) * t;
            double y = start.y + (end.y - start.y) * t;
            double z = start.z + (end.z - start.z) * t;

            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    x + randomOffset(), y + randomOffset(), z + randomOffset(),
                    0, 0.05, 0);
        }
    }

    private static void spawnSuccessParticlesAt(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 center = Vec3.atCenterOf(pos);

        for (int i = 0; i < 15; i++) {
            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    center.x + randomOffset() * 0.8,
                    center.y + randomOffset() * 0.8,
                    center.z + randomOffset() * 0.8,
                    randomOffset() * 0.1,
                    0.1 + random.nextDouble() * 0.1,
                    randomOffset() * 0.1);
        }
    }

    public static void spawnLinkErrorParticles(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 center = Vec3.atCenterOf(pos);

        for (int i = 0; i < 10; i++) {
            mc.level.addParticle(ParticleTypes.SMOKE,
                    center.x + randomOffset() * 0.5,
                    center.y + randomOffset() * 0.5,
                    center.z + randomOffset() * 0.5,
                    randomOffset() * 0.02,
                    0.02,
                    randomOffset() * 0.02);
        }
    }

    private static double randomOffset() {
        return (random.nextDouble() - 0.5) * 2;
    }
}
