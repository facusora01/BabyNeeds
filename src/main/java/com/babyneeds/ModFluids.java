package com.babyneeds;

import java.util.function.Consumer;

import org.joml.Vector3f;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;

import javax.annotation.Nullable;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Muddy water: the wallow a pig actually wants. Water colour in Minecraft comes from the biome and
 * is resolved per position by the client, so a single brown puddle next to clean blue water is not
 * something a block can ask for - it has to be its own fluid. This one reuses vanilla's water
 * textures and simply tints them, so it costs no artwork and still reads as water.
 *
 * Every property below mirrors how Forge defines vanilla water, deliberately and one for one. A
 * fluid that looks like water but does not make infinite sources, put out fires or break your fall
 * is a trap for the player, not a feature: they will treat it as water, because it is water, just
 * dirty. The only differences are the tint, a slower flow, and the drag that {@link MuddyWaterBlock}
 * adds - wading through a wallow should feel like wading through a wallow.
 *
 * It is also in the {@code minecraft:water} fluid tag, so everything that asks "is this water?" by
 * tag - swimming, drowning, farmland hydration - says yes.
 */
public class ModFluids {

    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, BabyNeeds.MODID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(ForgeRegistries.FLUIDS, BabyNeeds.MODID);

    /** Silty brown, dark enough to read as a wallow but still recognisably water. */
    private static final int TINT = 0xFF6B5230;

    public static final RegistryObject<FluidType> MUDDY_WATER_TYPE = FLUID_TYPES.register("muddy_water",
            () -> new FluidType(FluidType.Properties.create()
                    .descriptionId("fluid.babyneeds.muddy_water")
                    .fallDistanceModifier(0.0f)
                    .canExtinguish(true)
                    .canConvertToSource(true)
                    .supportsBoating(true)
                    .canHydrate(true)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)
                    .sound(SoundActions.FLUID_VAPORIZE, SoundEvents.FIRE_EXTINGUISH)) {

                @Override
                @Nullable
                public BlockPathTypes getBlockPathType(FluidState state, BlockGetter level, BlockPos pos,
                                                       @Nullable Mob mob, boolean canFluidLog) {
                    // Exactly what Forge does for vanilla water: defer to vanilla pathfinding, so
                    // mobs wade and swim through a wallow the same way they do through a pond.
                    return canFluidLog ? super.getBlockPathType(state, level, pos, mob, true) : null;
                }

                @Override
                public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                    consumer.accept(new IClientFluidTypeExtensions() {
                        @Override
                        public ResourceLocation getStillTexture() {
                            return new ResourceLocation("block/water_still");
                        }

                        @Override
                        public ResourceLocation getFlowingTexture() {
                            return new ResourceLocation("block/water_flow");
                        }

                        @Override
                        public ResourceLocation getOverlayTexture() {
                            return new ResourceLocation("block/water_overlay");
                        }

                        @Override
                        public int getTintColor() {
                            return TINT;
                        }

                        @Override
                        public ResourceLocation getRenderOverlayTexture(Minecraft minecraft) {
                            // The same "you are underwater" overlay as water: being in a wallow
                            // should look like being in water, only filthier.
                            return new ResourceLocation("textures/misc/underwater.png");
                        }

                        @Override
                        public Vector3f modifyFogColor(Camera camera, float partialTick, ClientLevel level,
                                                       int renderDistance, float darkenWorldAmount,
                                                       Vector3f fluidFogColor) {
                            return new Vector3f(0.19f, 0.14f, 0.08f);
                        }

                        @Override
                        public void modifyFogRender(Camera camera, FogRenderer.FogMode mode,
                                                    float renderDistance, float partialTick,
                                                    float nearDistance, float farDistance,
                                                    FogShape shape) {
                            // Silt is opaque. Not "murky water" - you cannot see your own snout,
                            // which is the point of a wallow being somewhere a pig hides.
                            RenderSystem.setShaderFogStart(-1.0f);
                            RenderSystem.setShaderFogEnd(1.6f);
                        }
                    });
                }
            });

    public static final RegistryObject<FlowingFluid> MUDDY_WATER =
            FLUIDS.register("muddy_water", () -> new ForgeFlowingFluid.Source(ModFluids.properties()));
    public static final RegistryObject<FlowingFluid> FLOWING_MUDDY_WATER =
            FLUIDS.register("flowing_muddy_water", () -> new ForgeFlowingFluid.Flowing(ModFluids.properties()));

    private static ForgeFlowingFluid.Properties properties() {
        return new ForgeFlowingFluid.Properties(MUDDY_WATER_TYPE, MUDDY_WATER, FLOWING_MUDDY_WATER)
                .block(ModBlocks.MUDDY_WATER_BLOCK)
                .bucket(ModItems.MUDDY_WATER_BUCKET)
                .slopeFindDistance(4)
                .levelDecreasePerBlock(1)
                // Water ticks every 5; silt is heavier and creeps rather than runs.
                .tickRate(10);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
