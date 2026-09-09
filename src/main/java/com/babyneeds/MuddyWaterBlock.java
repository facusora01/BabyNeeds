package com.babyneeds;

import java.util.function.Supplier;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

/**
 * The wallow you can stand in. Behaves as water in every respect except one: it drags. Mud is thick,
 * and a pool of it should cost something to cross - it is what makes a wallow read as a wallow
 * rather than as brown water, and it gives the pen a shape the player has to think about.
 *
 * The drag is applied the same way soul sand and honey do it, so it composes with everything that
 * already understands being stuck in a block.
 */
public class MuddyWaterBlock extends LiquidBlock {

    public MuddyWaterBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }

    @Override
    public void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);

        double drag = Config.MUDDY_WATER_DRAG.get();
        if (drag >= 1.0) return;

        // Horizontal movement is what the mud takes; vertical is left nearly alone so a player or a
        // piglet that falls in can still get itself out.
        entity.makeStuckInBlock(state, new Vec3(drag, 0.95, drag));
    }
}
