package com.enormeboze.crushingwheelrecipeselector.client;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import java.util.Random;

/**
 * Spawns particles for visual feedback during wheel linking
 */
@OnlyIn(Dist.CLIENT)
public class LinkingParticles {

    private static final Random random = new Random();

    /**
     * Spawn success particles when two wheels are successfully linked
     */
    public static void spawnLinkSuccessParticles(BlockPos pos1, BlockPos pos2) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 center1 = Vec3.atCenterOf(pos1);
        Vec3 center2 = Vec3.atCenterOf(pos2);
        Vec3 midpoint = center1.add(center2).scale(0.5);

        // Spawn happy green particles at both wheels and midpoint
        for (int i = 0; i < 20; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 1.5;
            double offsetY = (random.nextDouble() - 0.5) * 1.5;
            double offsetZ = (random.nextDouble() - 0.5) * 1.5;

            // Particles at first wheel
            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    center1.x + offsetX, center1.y + offsetY, center1.z + offsetZ,
                    0, 0.1, 0);

            // Particles at second wheel
            mc.level.addParticle(ParticleTypes.HAPPY_VILLAGER,
                    center2.x + offsetX, center2.y + offsetY, center2.z + offsetZ,
                    0, 0.1, 0);
        }

        // Spawn a line of particles between the two wheels
        int steps = 10;
        for (int i = 0; i <= steps; i++) {
            double t = (double) i / steps;
            Vec3 point = center1.lerp(center2, t);
            
            mc.level.addParticle(ParticleTypes.END_ROD,
                    point.x, point.y, point.z,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1,
                    (random.nextDouble() - 0.5) * 0.1);
        }
    }

    /**
     * Spawn error particles when linking fails (too far)
     */
    public static void spawnLinkErrorParticles(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 center = Vec3.atCenterOf(pos);

        // Spawn angry red particles
        for (int i = 0; i < 15; i++) {
            double offsetX = (random.nextDouble() - 0.5) * 1.2;
            double offsetY = (random.nextDouble() - 0.5) * 1.2;
            double offsetZ = (random.nextDouble() - 0.5) * 1.2;

            mc.level.addParticle(ParticleTypes.ANGRY_VILLAGER,
                    center.x + offsetX, center.y + offsetY, center.z + offsetZ,
                    0, 0, 0);
        }
    }

    /**
     * Spawn selection particles when a wheel is selected for linking
     */
    public static void spawnSelectionParticles(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Vec3 center = Vec3.atCenterOf(pos);

        // Spawn glowing particles in a ring around the wheel
        for (int i = 0; i < 12; i++) {
            double angle = (2 * Math.PI * i) / 12;
            double radius = 0.8;
            double x = center.x + Math.cos(angle) * radius;
            double z = center.z + Math.sin(angle) * radius;

            mc.level.addParticle(ParticleTypes.GLOW,
                    x, center.y, z,
                    0, 0.05, 0);
        }
    }
}
