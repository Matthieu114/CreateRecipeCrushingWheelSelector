package com.enormeboze.crushingwheelrecipeselector;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import com.simibubi.create.content.kinetics.crusher.CrushingWheelControllerBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for finding paired crushing wheels
 * Crushing wheels have a 1-block gap between them
 */
public class CrushingWheelPairHelper {

    /**
     * Find the paired crushing wheel for a given wheel position
     * Crushing wheels are 2 blocks apart (with 1 block gap)
     * Returns null if no valid pair is found
     */
    public static BlockPos findPairedWheel(Level level, BlockPos wheelPos) {
        BlockState state = level.getBlockState(wheelPos);
        Block block = state.getBlock();

        // Only works for crushing wheel blocks
        if (!(block instanceof CrushingWheelBlock) && !(block instanceof CrushingWheelControllerBlock)) {
            return null;
        }

        // Check all horizontal directions for another crushing wheel
        // Crushing wheels are 2 blocks apart (with 1 block gap between them)
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            // Check position 2 blocks away
            BlockPos adjacentPos = wheelPos.relative(direction, 2);
            BlockState adjacentState = level.getBlockState(adjacentPos);
            Block adjacentBlock = adjacentState.getBlock();

            // Check if that block is also a crushing wheel
            if (adjacentBlock instanceof CrushingWheelBlock || adjacentBlock instanceof CrushingWheelControllerBlock) {
                // Found a paired wheel
                return adjacentPos;
            }
        }

        return null;
    }

    /**
     * Get all crushing wheel positions involved in this crushing setup
     * Returns list containing the original wheel and its pair (if found)
     */
    public static List<BlockPos> getAllWheelPositions(Level level, BlockPos wheelPos) {
        List<BlockPos> positions = new ArrayList<>();
        positions.add(wheelPos);

        BlockPos pairedWheel = findPairedWheel(level, wheelPos);
        if (pairedWheel != null) {
            positions.add(pairedWheel);
        }

        return positions;
    }

    /**
     * Check if two crushing wheels are paired (2 blocks apart)
     */
    public static boolean arePaired(BlockPos wheel1, BlockPos wheel2) {
        // Check if they're 2 blocks apart in any horizontal direction
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (wheel1.relative(direction, 2).equals(wheel2)) {
                return true;
            }
        }
        return false;
    }
}