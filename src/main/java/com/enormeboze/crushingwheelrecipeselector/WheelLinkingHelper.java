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
 */
public class WheelLinkingHelper {

    public static final int LINK_DISTANCE = 2;
    private static final int INVALID_SEARCH_RADIUS = 3;

    public static Set<BlockPos> findValidLinkTargets(Level level, BlockPos wheelPos) {
        Set<BlockPos> validTargets = new HashSet<>(4);

        BlockState state = level.getBlockState(wheelPos);
        if (!(state.getBlock() instanceof CrushingWheelBlock)) {
            return validTargets;
        }

        Direction.Axis rotationAxis = state.getValue(BlockStateProperties.AXIS);

        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == rotationAxis) {
                continue;
            }

            BlockPos candidatePos = wheelPos.relative(direction, LINK_DISTANCE);
            BlockState candidateState = level.getBlockState(candidatePos);

            if (candidateState.getBlock() instanceof CrushingWheelBlock) {
                Direction.Axis candidateAxis = candidateState.getValue(BlockStateProperties.AXIS);
                if (candidateAxis == rotationAxis) {
                    validTargets.add(candidatePos);
                }
            }
        }

        return validTargets;
    }

    public static Set<BlockPos> findInvalidLinkTargets(Level level, BlockPos wheelPos, Set<BlockPos> validTargets) {
        Set<BlockPos> invalidTargets = new HashSet<>();

        BlockState state = level.getBlockState(wheelPos);
        if (!(state.getBlock() instanceof CrushingWheelBlock)) {
            return invalidTargets;
        }

        BlockPos.MutableBlockPos checkPos = new BlockPos.MutableBlockPos();

        for (int x = -INVALID_SEARCH_RADIUS; x <= INVALID_SEARCH_RADIUS; x++) {
            for (int y = -INVALID_SEARCH_RADIUS; y <= INVALID_SEARCH_RADIUS; y++) {
                for (int z = -INVALID_SEARCH_RADIUS; z <= INVALID_SEARCH_RADIUS; z++) {
                    if (x == 0 && y == 0 && z == 0) continue;

                    checkPos.set(wheelPos.getX() + x, wheelPos.getY() + y, wheelPos.getZ() + z);
                    BlockState checkState = level.getBlockState(checkPos);

                    if (checkState.getBlock() instanceof CrushingWheelBlock) {
                        BlockPos foundPos = checkPos.immutable();

                        if (validTargets.contains(foundPos)) {
                            continue;
                        }

                        invalidTargets.add(foundPos);
                    }
                }
            }
        }

        return invalidTargets;
    }

    public static boolean canLink(Level level, BlockPos wheel1, BlockPos wheel2) {
        return CrushingWheelPairHelper.isValidPair(level, wheel1, wheel2);
    }

    @Nullable
    public static String getLinkFailureReason(Level level, BlockPos wheel1, BlockPos wheel2) {
        if (CrushingWheelPairHelper.isValidPair(level, wheel1, wheel2)) {
            return null;
        }
        return CrushingWheelPairHelper.getInvalidPairReason(level, wheel1, wheel2);
    }
}
