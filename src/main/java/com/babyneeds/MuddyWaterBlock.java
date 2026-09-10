package com.babyneeds;

import java.util.function.Supplier;

import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.material.FlowingFluid;

/**
 * The wallow you can stand in. It behaves as water in every single respect - the only thing that
 * sets it apart is how it looks: brown, thickening towards the middle of the pool, and near-opaque
 * once your head is under it.
 *
 * It used to drag as well, on the theory that mud should cost something to cross. In practice that
 * is the part players felt as the mod fighting them rather than as mud being thick, so it is gone:
 * a wallow is somewhere you can see is filthy, not somewhere you get stuck. Everything else about
 * water - infinite sources, putting fires out, breaking your fall, hydrating farmland, carrying
 * boats - is inherited untouched, because a fluid that looks like water and does not behave like it
 * is a trap rather than a feature.
 *
 * The class stays even with nothing overridden: {@link LiquidBlock}'s constructor is protected, so a
 * subclass is how a mod registers one at all.
 */
public class MuddyWaterBlock extends LiquidBlock {

    public MuddyWaterBlock(Supplier<? extends FlowingFluid> fluid, Properties properties) {
        super(fluid, properties);
    }
}
