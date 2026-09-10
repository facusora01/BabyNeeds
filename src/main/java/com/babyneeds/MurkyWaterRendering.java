package com.babyneeds;

import java.lang.reflect.Field;

import org.joml.Vector3f;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;

/**
 * Makes ordinary water look muddy near a wallow.
 *
 * <h2>Why it is done this way</h2>
 *
 * A wallow used to be its own fluid, tinted brown. It looked right and it was wrong underneath: two
 * distinct fluids never merge in Minecraft. Vanilla decides whether a neighbour counts towards
 * forming a source with {@code Fluid.isSame}, and {@code WaterFluid.isSame} compares by hand against
 * {@code Fluids.WATER} and {@code Fluids.FLOWING_WATER} - so clean water can never count a muddy
 * neighbour, whatever the mod does. Two clean sources make a source between them, two muddy ones do,
 * one of each never does. Same story for the surface mesh, which is why the edge showed a seam.
 *
 * So the water is now literally vanilla water everywhere, and only its <em>colour</em> changes. Every
 * mechanic is vanilla's because it is vanilla's fluid: sources form, it connects, it flows, buckets
 * work, lava reacts, farmland hydrates. Nothing is reimplemented and nothing can drift out of step.
 *
 * <h2>The catch</h2>
 *
 * Forge resolves a fluid's rendering through {@code FluidType.renderProperties}, a private field
 * written once when the type is registered, with no public way in afterwards. Vanilla water's type
 * belongs to Forge, so the only way to colour it is to reach into that field and put a wrapper
 * there - one that delegates every single call to whatever was already installed, and only answers
 * differently for the tint and the fog.
 *
 * That is a genuine hack and it is treated as one. It is client-only, it happens once at startup,
 * it keeps the original in hand and hands every unrelated question straight back to it, and if the
 * field ever moves or the reflection is refused, it logs and gives up - water goes back to looking
 * like water and everything else in the mod carries on. It is also the reason a second mod doing the
 * same thing to water would conflict: last one in wins.
 */
public final class MurkyWaterRendering {

    /** Silty brown, dark enough to read as a wallow but still recognisably water. */
    private static final int MUD_TINT = 0x6B5230;
    /** The colour the fog takes once your head is under a wallow. */
    private static final Vector3f MUD_FOG = new Vector3f(0.19f, 0.14f, 0.08f);
    /** How far you can see through silt, in blocks. Not "murky water" - you cannot see your snout. */
    private static final float MUD_FOG_END = 1.6f;

    private MurkyWaterRendering() {
    }

    /**
     * Wraps vanilla water's rendering so it can be tinted. Called once, on the client, during setup.
     */
    public static void install() {
        if (!Config.MUDDY_WATER.get()) return;

        try {
            FluidType water = ForgeMod.WATER_TYPE.get();
            Field field = FluidType.class.getDeclaredField("renderProperties");
            field.setAccessible(true);

            Object current = field.get(water);
            if (current instanceof Murky) return;  // already installed; never wrap ourselves twice

            IClientFluidTypeExtensions base = current instanceof IClientFluidTypeExtensions ext
                    ? ext
                    : IClientFluidTypeExtensions.DEFAULT;
            field.set(water, new Murky(base));
        } catch (ReflectiveOperationException | RuntimeException failure) {
            // Not fatal, and deliberately not rethrown: a wallow that is the wrong colour is a
            // cosmetic disappointment, and a crash on startup is not.
            BabyNeeds.LOGGER.warn("BabyNeeds could not tint water; wallows will look like clean water", failure);
        }
    }

    /**
     * Vanilla water's own rendering, with the tint and the fog answered from how much mud is around.
     * Everything else is handed straight back to whatever Forge had installed.
     */
    private record Murky(IClientFluidTypeExtensions base) implements IClientFluidTypeExtensions {

        // -------------------------------------------------------------- what this actually changes

        @Override
        public int getTintColor(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            double density = MuddyWater.mudDensity(getter, pos);
            if (density <= 0.0) return base.getTintColor(state, getter, pos);

            // Blended against the biome's own water colour rather than a generic blue, so the shift
            // into brown starts from exactly the colour of the water beside it and the pool shades
            // from its banks inwards.
            int clean = BiomeColors.getAverageWaterColor(getter, pos);
            double strength = MudDensityCalculator.strength(density, MuddyWater.MURK_SATURATION);
            return MudDensityCalculator.blend(clean, MUD_TINT, strength);
        }

        @Override
        public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                       int renderDistance, float darkenWorldAmount,
                                       Vector3f fluidFogColor) {
            double density = murkAtCamera(camera, level);
            if (density <= 0.0) {
                return base.modifyFogColor(camera, partialTick, level, renderDistance,
                        darkenWorldAmount, fluidFogColor);
            }

            Vector3f clean = base.modifyFogColor(camera, partialTick, level, renderDistance,
                    darkenWorldAmount, fluidFogColor);
            float towards = (float) MudDensityCalculator.strength(density, MuddyWater.MURK_SATURATION);
            return new Vector3f(
                    clean.x() + (MUD_FOG.x() - clean.x()) * towards,
                    clean.y() + (MUD_FOG.y() - clean.y()) * towards,
                    clean.z() + (MUD_FOG.z() - clean.z()) * towards);
        }

        @Override
        public void modifyFogRender(Camera camera, FogRenderer.FogMode mode, float renderDistance,
                                    float partialTick, float nearDistance, float farDistance,
                                    FogShape shape) {
            ClientLevel level = Minecraft.getInstance().level;
            double density = level == null ? 0.0 : murkAtCamera(camera, level);
            if (density <= 0.0) {
                base.modifyFogRender(camera, mode, renderDistance, partialTick, nearDistance,
                        farDistance, shape);
                return;
            }

            // Silt closes in as it thickens: the edge of a wallow is merely dirty, the middle of one
            // is somewhere a pig can hide.
            base.modifyFogRender(camera, mode, renderDistance, partialTick, nearDistance,
                    farDistance, shape);
            float eased = (float) MudDensityCalculator.strength(density, MuddyWater.MURK_SATURATION);
            RenderSystem.setShaderFogStart(-1.0f);
            RenderSystem.setShaderFogEnd(farDistance + (MUD_FOG_END - farDistance) * eased);
        }

        /** How muddy the water is right where the camera's eyes are, or 0 if they are not in any. */
        private double murkAtCamera(Camera camera, ClientLevel level) {
            BlockPos eye = BlockPos.containing(camera.getPosition());
            if (!level.getFluidState(eye).is(net.minecraft.tags.FluidTags.WATER)) return 0.0;
            return MuddyWater.mudDensity(level, eye);
        }

        // -------------------------------------------------------------- everything else, untouched

        @Override
        public ResourceLocation getStillTexture() {
            return base.getStillTexture();
        }

        @Override
        public ResourceLocation getFlowingTexture() {
            return base.getFlowingTexture();
        }

        @Override
        public ResourceLocation getOverlayTexture() {
            return base.getOverlayTexture();
        }

        @Override
        public ResourceLocation getStillTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return base.getStillTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getFlowingTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return base.getFlowingTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getOverlayTexture(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
            return base.getOverlayTexture(state, getter, pos);
        }

        @Override
        public ResourceLocation getStillTexture(FluidStack stack) {
            return base.getStillTexture(stack);
        }

        @Override
        public ResourceLocation getFlowingTexture(FluidStack stack) {
            return base.getFlowingTexture(stack);
        }

        @Override
        public ResourceLocation getOverlayTexture(FluidStack stack) {
            return base.getOverlayTexture(stack);
        }

        @Override
        public int getTintColor() {
            return base.getTintColor();
        }

        @Override
        public int getTintColor(FluidStack stack) {
            return base.getTintColor(stack);
        }

        @Override
        public ResourceLocation getRenderOverlayTexture(Minecraft minecraft) {
            return base.getRenderOverlayTexture(minecraft);
        }

        @Override
        public void renderOverlay(Minecraft minecraft, PoseStack poseStack) {
            base.renderOverlay(minecraft, poseStack);
        }
    }
}
