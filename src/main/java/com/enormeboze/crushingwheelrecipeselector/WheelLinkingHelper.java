package com.enormeboze.crushingwheelrecipeselector;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;
import java.util.HashSet;
import java.util.Set;

/**
 * Helper class for wheel linking operations.
 *
 * PERFORMANCE OPTIMIZATION:
 * Instead of searching in a radius, we use axis-based detection.
 * A crushing wheel can only pair with another wheel that:
 * 1. Has the same rotation axis
 * 2. Is exactly 2 blocks away PERPENDICULAR to that axis
 *
 * This means we only check 4 positions max (the 4 perpendicular directions).
 */
public class WheelLinkingHelper {

    // Distance between paired wheels (1 block gap = 2 blocks apart)
    public static final int LINK_DISTANCE = 2;

    // Small search radius for finding nearby invalid wheels (for red highlight)
    // Keep this small for performance - just enough to show nearby misaligned wheels
    private static final int INVALID_SEARCH_RADIUS = 3;

    /**
     * Find valid link targets for a wheel.
     * Only returns wheels that can actually form a valid crushing pair.
     *
     * @param level The world
     * @param wheelPos The wheel being linked from
     * @return Set of valid target positions (0-4 positions max)
     */
    public static Set<BlockPos> findValidLinkTargets(Level level, BlockPos wheelPos) {
        Set<BlockPos> validTargets = new HashSet<>(4);

        BlockState state = level.getBlockState(wheelPos);
        if (!(state.getBlock() instanceof CrushingWheelBlock)) {
            return validTargets;
        }

        // Get the axis this wheel ROTATES around
        Direction.Axis rotationAxis = state.getValue(BlockStateProperties.AXIS);

        // Check all 6 directions, but only directions PERPENDICULAR to rotation axis are valid
        for (Direction direction : Direction.values()) {
            // Skip if this direction is along the rotation axis
            if (direction.getAxis() == rotationAxis) {
                continue;
            }

            BlockPos candidatePos = wheelPos.relative(direction, LINK_DISTANCE);
            BlockState candidateState = level.getBlockState(candidatePos);

            if (candidateState.getBlock() instanceof CrushingWheelBlock) {
                // Must have same rotation axis
                Direction.Axis candidateAxis = candidateState.getValue(BlockStateProperties.AXIS);
                if (candidateAxis == rotationAxis) {
                    validTargets.add(candidatePos);
                }
            }
        }

        return validTargets;
    }

    /**
     * Find invalid link targets - nearby wheels that CANNOT be linked.
     * These are shown with red highlight to help the player understand why they can't link.
     *
     * @param level The world
     * @param wheelPos The wheel being linked from
     * @param validTargets Already-found valid targets (to exclude from invalid set)
     * @return Set of invalid target positions
     */
    public static Set<BlockPos> findInvalidLinkTargets(Level level, BlockPos wheelPos, Set<BlockPos> validTargets) {
        Set<BlockPos> invalidTargets = new HashSet<>();

        BlockState state = level.getBlockState(wheelPos);
        if (!(state.getBlock() instanceof CrushingWheelBlock)) {
            return invalidTargets;
        }

        // Search in a small area for other crushing wheels
        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int x = -INVALID_SEARCH_RADIUS; x <= INVALID_SEARCH_RADIUS; x++) {
            for (int y = -INVALID_SEARCH_RADIUS; y <= INVALID_SEARCH_RADIUS; y++) {
                for (int z = -INVALID_SEARCH_RADIUS; z <= INVALID_SEARCH_RADIUS; z++) {
                    // Skip the wheel itself
                    if (x == 0 && y == 0 && z == 0) continue;

                    checkPos.set(wheelPos.getX() + x, wheelPos.getY() + y, wheelPos.getZ() + z);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (checkState.getBlock() instanceof CrushingWheelBlock) {
                        BlockPos foundPos = checkPos.immutable();

                        // Skip if it's already a valid target
                        if (validTargets.contains(foundPos)) {
                            continue;
                        }

                        // This is a nearby wheel that can't be linked - add to invalid set
                        invalidTargets.add(foundPos);
                    }
                }
            }
        }

        return invalidTargets;
    }

    /**
     * Check if two wheel positions can form a valid pair.
     * This is the validation used before completing a link.
     */
    public static boolean canLink(Level level, BlockPos wheel1, BlockPos wheel2) {
        return CrushingWheelPairHelper.isValidPair(level, wheel1, wheel2);
    }

    /**
     * Get the reason why two wheels cannot be linked.
     * Returns null if they can be linked.
     */
    @Nullable
    public static String getLinkFailureReason(Level level, BlockPos wheel1, BlockPos wheel2) {
        if (CrushingWheelPairHelper.isValidPair(level, wheel1, wheel2)) {
            return null;
        }
        return CrushingWheelPairHelper.getInvalidPairReason(level, wheel1, wheel2);
    }
}