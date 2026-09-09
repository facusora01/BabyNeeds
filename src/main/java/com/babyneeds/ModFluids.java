package com.babyneeds;

import java.util.function.Consumer;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
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
 * It is tagged as water (see the fluid tag in data/) so swimming, drowning, farmland hydration and
 * putting out fires all behave exactly as players expect.
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
                    .canSwim(true)
                    .canDrown(true)
                    .canPushEntity(true)
                    .canHydrate(true)
                    .supportsBoating(true)
                    .density(1100)
                    .viscosity(1100)
                    .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                    .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY)) {

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
                .tickRate(5);
    }

    public static void register(IEventBus modEventBus) {
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);
    }
}
