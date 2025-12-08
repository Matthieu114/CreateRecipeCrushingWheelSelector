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
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import com.enormeboze.crushingwheelrecipeselector.CrushingWheelRecipeSelector;

import java.util.HashSet;
import java.util.Set;

/**
 * Renders visual highlights on crushing wheels during the linking process.
 * - Green outline: Selected wheel / valid link target
 * - Red outline: Invalid link target (too far)
 * - Particles: Connection effect between wheels
 */
@OnlyIn(Dist.CLIENT)
@EventBusSubscriber(modid = CrushingWheelRecipeSelector.MOD_ID, value = Dist.CLIENT)
public class WheelHighlightRenderer {

    // Currently selected wheel (first wheel in linking process)
    private static BlockPos selectedWheel = null;
    
    // Wheels that are valid link targets (within range)
    private static Set<BlockPos> validTargets = new HashSet<>();
    
    // Wheels that are invalid link targets (out of range)
    private static Set<BlockPos> invalidTargets = new HashSet<>();
    
    // Timestamp for animation
    private static long selectionStartTime = 0;
    
    // Maximum distance between wheels (2 blocks = 1 block gap between them)
    public static final int MAX_LINK_DISTANCE = 2;

    /**
     * Set the selected wheel and calculate valid/invalid targets
     */
    public static void setSelectedWheel(BlockPos pos, Set<BlockPos> allWheelPositions) {
        selectedWheel = pos;
        selectionStartTime = System.currentTimeMillis();
        validTargets.clear();
        invalidTargets.clear();
        
        if (pos != null && allWheelPositions != null) {
            for (BlockPos wheelPos : allWheelPositions) {
                if (!wheelPos.equals(pos)) {
                    if (isWithinLinkDistance(pos, wheelPos)) {
                        validTargets.add(wheelPos);
                    } else {
                        invalidTargets.add(wheelPos);
                    }
                }
            }
        }
    }
    
    /**
     * Clear all highlights
     */
    public static void clearSelection() {
        selectedWheel = null;
        validTargets.clear();
        invalidTargets.clear();
    }
    
    /**
     * Check if the selected wheel is set
     */
    public static boolean hasSelection() {
        return selectedWheel != null;
    }
    
    /**
     * Get the selected wheel position
     */
    public static BlockPos getSelectedWheel() {
        return selectedWheel;
    }
    
    /**
     * Check if two wheels are within linking distance
     */
    public static boolean isWithinLinkDistance(BlockPos pos1, BlockPos pos2) {
        // Calculate Manhattan distance (for block-based distance)
        // Or use actual distance for more accuracy
        double distance = Math.sqrt(pos1.distSqr(pos2));
        return distance <= MAX_LINK_DISTANCE;
    }
    
    /**
     * Check if a position is a valid link target
     */
    public static boolean isValidTarget(BlockPos pos) {
        return validTargets.contains(pos);
    }
    
    /**
     * Check if a position is an invalid target
     */
    public static boolean isInvalidTarget(BlockPos pos) {
        return invalidTargets.contains(pos);
    }

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
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
        Vec3 camera = event.getCamera().getPosition();
        
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        
        // Calculate pulse animation (0.0 to 1.0)
        float time = (System.currentTimeMillis() - selectionStartTime) / 1000f;
        float pulse = (float) (0.5 + 0.5 * Math.sin(time * 4)); // Pulsing effect
        
        // Render selected wheel (bright green)
        renderWheelHighlight(poseStack, bufferSource, camera, selectedWheel, 
            0.2f, 1.0f, 0.2f, 0.4f + 0.3f * pulse); // Green with pulse
        
        // Render valid targets (green)
        for (BlockPos validPos : validTargets) {
            renderWheelHighlight(poseStack, bufferSource, camera, validPos,
                0.3f, 0.9f, 0.3f, 0.3f + 0.2f * pulse); // Lighter green
            
            // Render connection line/particles
            renderConnectionEffect(poseStack, bufferSource, camera, selectedWheel, validPos, 
                0.3f, 1.0f, 0.3f, 0.5f, time);
        }
        
        // Render invalid targets (red)
        for (BlockPos invalidPos : invalidTargets) {
            renderWheelHighlight(poseStack, bufferSource, camera, invalidPos,
                1.0f, 0.2f, 0.2f, 0.3f + 0.2f * pulse); // Red
        }
        
        bufferSource.endBatch();
    }
    
    private static void renderWheelHighlight(PoseStack poseStack, MultiBufferSource bufferSource,
            Vec3 camera, BlockPos pos, float r, float g, float b, float a) {
        
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        
        // Create slightly expanded bounding box for the highlight
        AABB box = new AABB(pos).inflate(0.02);
        
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        
        // Render the outline box
        LevelRenderer.renderLineBox(poseStack, consumer, box, r, g, b, a);
        
        poseStack.popPose();
    }
    
    private static void renderConnectionEffect(PoseStack poseStack, MultiBufferSource bufferSource,
            Vec3 camera, BlockPos from, BlockPos to, float r, float g, float b, float a, float time) {
        
        poseStack.pushPose();
        poseStack.translate(-camera.x, -camera.y, -camera.z);
        
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());
        
        // Get center points of both blocks
        Vec3 fromCenter = Vec3.atCenterOf(from);
        Vec3 toCenter = Vec3.atCenterOf(to);
        
        // Draw animated dashed line between wheels
        int segments = 8;
        float segmentProgress = (time * 2) % 1.0f; // Animated dash movement
        
        PoseStack.Pose pose = poseStack.last();
        
        for (int i = 0; i < segments; i++) {
            float startT = (i + segmentProgress) / segments;
            float endT = (i + 0.5f + segmentProgress) / segments;
            
            if (startT > 1.0f) startT -= 1.0f;
            if (endT > 1.0f) endT -= 1.0f;
            if (startT > endT) continue; // Skip wrap-around segments
            
            Vec3 start = fromCenter.lerp(toCenter, startT);
            Vec3 end = fromCenter.lerp(toCenter, endT);
            
            // Calculate direction for normal
            Vec3 dir = end.subtract(start).normalize();
            
            consumer.addVertex(pose.pose(), (float) start.x, (float) start.y, (float) start.z)
                    .setColor(r, g, b, a)
                    .setNormal(pose, (float) dir.x, (float) dir.y, (float) dir.z);
            consumer.addVertex(pose.pose(), (float) end.x, (float) end.y, (float) end.z)
                    .setColor(r, g, b, a)
                    .setNormal(pose, (float) dir.x, (float) dir.y, (float) dir.z);
        }
        
        poseStack.popPose();
    }
}
