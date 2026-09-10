package com.babyneeds;

import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, BabyNeeds.MODID);

    public static final RegistryObject<Item> MUDDY_WATER_BUCKET = ITEMS.register("muddy_water_bucket",
            () -> new BucketItem(ModFluids.MUDDY_WATER,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)));

    public static void register(IEventBus modEventBus) {
        ITEMS.register(modEventBus);
    }

    @SubscribeEvent
    static void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(MUDDY_WATER_BUCKET);
        }
    }
}
