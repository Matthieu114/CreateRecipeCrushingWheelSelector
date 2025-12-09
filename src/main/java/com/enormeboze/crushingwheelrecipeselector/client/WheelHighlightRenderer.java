package com.enormeboze.crushingwheelrecipeselector.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;

/**
 * Renders highlight boxes around crushing wheels during the linking process.
 * Green = selected wheel + valid targets
 * Red = invalid nearby wheels (wrong axis, wrong distance)
 */
@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = "crushingwheelrecipeselector", value = Dist.CLIENT)
public class WheelHighlightRenderer {

    private static BlockPos selectedWheel = null;
    private static Set<BlockPos> validTargets = new HashSet<>();
    private static Set<BlockPos> invalidTargets = new HashSet<>();

    public static void setSelectedWheel(BlockPos wheel, Set<BlockPos> valid, Set<BlockPos> invalid) {
        selectedWheel = wheel;
        validTargets = new HashSet<>(valid);
        invalidTargets = new HashSet<>(invalid);
    }

    public static void clearSelection() {
        selectedWheel = null;
        validTargets.clear();
        invalidTargets.clear();
    }

    public static boolean hasSelection() {
        return selectedWheel != null;
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) {
            return;
        }

        if (selectedWheel == null) {
            return;
        }

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) {
            return;
        }

        PoseStack poseStack = event.getPoseStack();
        Vec3 cameraPos = event.getCamera().getPosition();

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer lineConsumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Render selected wheel (green)
        renderHighlightBox(poseStack, lineConsumer, selectedWheel, 0.0f, 1.0f, 0.0f, 1.0f);

        // Render valid targets (green with animated connection lines)
        float time = (System.currentTimeMillis() % 2000) / 2000.0f;
        float pulse = 0.6f + 0.4f * (float) Math.sin(time * Math.PI * 2);

        for (BlockPos target : validTargets) {
            renderHighlightBox(poseStack, lineConsumer, target, 0.0f, pulse, 0.0f, 0.8f);
            renderConnectionLine(poseStack, lineConsumer, selectedWheel, target, 0.0f, 1.0f, 0.0f, pulse);
        }

        // Render invalid targets (red)
        for (BlockPos invalid : invalidTargets) {
            renderHighlightBox(poseStack, lineConsumer, invalid, 1.0f, 0.0f, 0.0f, 0.6f);
        }

        // If no valid targets, render yellow warning on selected wheel
        if (validTargets.isEmpty()) {
            renderHighlightBox(poseStack, lineConsumer, selectedWheel, 1.0f, 1.0f, 0.0f, pulse);
        }

        poseStack.popPose();

        bufferSource.endBatch(RenderType.lines());
    }

    private static void renderHighlightBox(PoseStack poseStack, VertexConsumer consumer,
                                           BlockPos pos, float r, float g, float b, float a) {
        AABB box = new AABB(pos).inflate(0.002);
        LevelRenderer.renderLineBox(poseStack, consumer, box, r, g, b, a);
    }

    private static void renderConnectionLine(PoseStack poseStack, VertexConsumer consumer,
                                             BlockPos from, BlockPos to, float r, float g, float b, float a) {
        Vec3 start = Vec3.atCenterOf(from);
        Vec3 end = Vec3.atCenterOf(to);

        Vec3 dir = end.subtract(start).normalize();
        float nx = (float) dir.x;
        float ny = (float) dir.y;
        float nz = (float) dir.z;

        consumer.vertex(poseStack.last().pose(), (float) start.x, (float) start.y, (float) start.z)
                .color(r, g, b, a)
                .normal(poseStack.last().normal(), nx, ny, nz)
                .endVertex();

        consumer.vertex(poseStack.last().pose(), (float) end.x, (float) end.y, (float) end.z)
                .color(r, g, b, a)
                .normal(poseStack.last().normal(), nx, ny, nz)
                .endVertex();
    }
}
