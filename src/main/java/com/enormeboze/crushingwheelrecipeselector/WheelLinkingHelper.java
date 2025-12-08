package com.enormeboze.crushingwheelrecipeselector;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for wheel linking operations
 */
public class WheelLinkingHelper {

    // Maximum distance between wheels that can be linked (2 blocks = 1 block gap)
    public static final int MAX_LINK_DISTANCE = 2;
    
    // Search radius for finding nearby wheels
    public static final int SEARCH_RADIUS = 10;

    /**
     * Find all crushing wheel positions within search radius of a position
     */
    public static Set<BlockPos> findNearbyWheels(Level level, BlockPos center) {
        Set<BlockPos> wheels = new HashSet<>();
        
        for (int x = -SEARCH_RADIUS; x <= SEARCH_RADIUS; x++) {
            for (int y = -SEARCH_RADIUS; y <= SEARCH_RADIUS; y++) {
                for (int z = -SEARCH_RADIUS; z <= SEARCH_RADIUS; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    BlockState state = level.getBlockState(checkPos);
                    
                    if (state.getBlock() instanceof CrushingWheelBlock) {
                        wheels.add(checkPos);
                    }
                }
            }
        }
        
        return wheels;
    }
    
    /**
     * Check if two positions are within linking distance
     */
    public static boolean isWithinLinkDistance(BlockPos pos1, BlockPos pos2) {
        double distance = Math.sqrt(pos1.distSqr(pos2));
        return distance <= MAX_LINK_DISTANCE;
    }
    
    /**
     * Get a human-readable distance description
     */
    public static String getDistanceDescription(BlockPos pos1, BlockPos pos2) {
        double distance = Math.sqrt(pos1.distSqr(pos2));
        return String.format("%.1f blocks", distance);
    }
}
