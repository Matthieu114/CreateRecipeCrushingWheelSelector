package com.enormeboze.crushingwheelrecipeselector;

import com.simibubi.create.content.kinetics.crusher.CrushingWheelBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import javax.annotation.Nullable;

/**
 * Utility class for finding paired crushing wheels using axis-based detection.
 * 
 * CRUSHING WHEEL MECHANICS:
 * - Wheels have an AXIS property indicating which axis they ROTATE around
 * - For crushing to work, two wheels must:
 *   1. Have the SAME rotation axis
 *   2. Be positioned PERPENDICULAR to that axis (so spinning edges face each other)
 *   3. Be exactly 2 blocks apart (1 block gap between them)
 */
public class CrushingWheelPairHelper {

    @Nullable
    public static BlockPos findPairedWheel(Level level, BlockPos wheelPos) {
        BlockState state = level.getBlockState(wheelPos);
        
        if (!(state.getBlock() instanceof CrushingWheelBlock)) {
            return null;
        }

        Direction.Axis rotationAxis = state.getValue(BlockStateProperties.AXIS);

        for (Direction direction : Direction.values()) {
            if (direction.getAxis() == rotationAxis) {
                continue;
            }

            BlockPos candidatePos = wheelPos.relative(direction, 2);
            BlockState candidateState = level.getBlockState(candidatePos);

            if (candidateState.getBlock() instanceof CrushingWheelBlock) {
                Direction.Axis candidateAxis = candidateState.getValue(BlockStateProperties.AXIS);
                if (candidateAxis == rotationAxis) {
                    return candidatePos;
                }
            }
        }

        return null;
    }

    public static BlockPos getControllerPosition(BlockPos wheel1, BlockPos wheel2) {
        return new BlockPos(
                (wheel1.getX() + wheel2.getX()) / 2,
                (wheel1.getY() + wheel2.getY()) / 2,
                (wheel1.getZ() + wheel2.getZ()) / 2
        );
    }

    public static boolean isValidPair(Level level, BlockPos wheel1, BlockPos wheel2) {
        BlockState state1 = level.getBlockState(wheel1);
        BlockState state2 = level.getBlockState(wheel2);

        if (!(state1.getBlock() instanceof CrushingWheelBlock) ||
            !(state2.getBlock() instanceof CrushingWheelBlock)) {
            return false;
        }

        Direction.Axis axis1 = state1.getValue(BlockStateProperties.AXIS);
        Direction.Axis axis2 = state2.getValue(BlockStateProperties.AXIS);
        if (axis1 != axis2) {
            return false;
        }

        int dx = Math.abs(wheel2.getX() - wheel1.getX());
        int dy = Math.abs(wheel2.getY() - wheel1.getY());
        int dz = Math.abs(wheel2.getZ() - wheel1.getZ());

        boolean alignedX = dx == 2 && dy == 0 && dz == 0;
        boolean alignedY = dx == 0 && dy == 2 && dz == 0;
        boolean alignedZ = dx == 0 && dy == 0 && dz == 2;

        if (!alignedX && !alignedY && !alignedZ) {
            return false;
        }

        Direction.Axis separationAxis;
        if (alignedX) separationAxis = Direction.Axis.X;
        else if (alignedY) separationAxis = Direction.Axis.Y;
        else separationAxis = Direction.Axis.Z;

        return separationAxis != axis1;
    }

    public static String getInvalidPairReason(Level level, BlockPos wheel1, BlockPos wheel2) {
        BlockState state1 = level.getBlockState(wheel1);
        BlockState state2 = level.getBlockState(wheel2);

        if (!(state1.getBlock() instanceof CrushingWheelBlock)) {
            return "First position is not a crushing wheel";
        }
        if (!(state2.getBlock() instanceof CrushingWheelBlock)) {
            return "Second position is not a crushing wheel";
        }

        Direction.Axis axis1 = state1.getValue(BlockStateProperties.AXIS);
        Direction.Axis axis2 = state2.getValue(BlockStateProperties.AXIS);

        if (axis1 != axis2) {
            return "Wheels have different rotation axes (" + axis1 + " vs " + axis2 + ")";
        }

        int dx = Math.abs(wheel2.getX() - wheel1.getX());
        int dy = Math.abs(wheel2.getY() - wheel1.getY());
        int dz = Math.abs(wheel2.getZ() - wheel1.getZ());

        boolean alignedX = dx == 2 && dy == 0 && dz == 0;
        boolean alignedY = dx == 0 && dy == 2 && dz == 0;
        boolean alignedZ = dx == 0 && dy == 0 && dz == 2;

        if (!alignedX && !alignedY && !alignedZ) {
            int totalDist = dx + dy + dz;
            if (totalDist != 2) {
                return "Wheels must be exactly 2 blocks apart (found " + totalDist + " blocks)";
            }
            return "Wheels are not aligned (must be in a straight line)";
        }

        Direction.Axis separationAxis;
        if (alignedX) separationAxis = Direction.Axis.X;
        else if (alignedY) separationAxis = Direction.Axis.Y;
        else separationAxis = Direction.Axis.Z;

        if (separationAxis == axis1) {
            return "Wheels are aligned along their rotation axis (" + axis1 + 
                   ") - must be positioned perpendicular to crush items";
        }

        return "Unknown reason";
    }
}
