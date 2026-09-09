package com.babyneeds;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.EatBlockGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.WrappedGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.Cow;
import net.minecraft.world.entity.animal.goat.Goat;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.BabyEntitySpawnEvent;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Where the mod meets the game. Everything here runs server-side on live animals: adopting them
 * into the stage system, installing the nursing and grazing goals, running the hunger and milk
 * economy on a 20-tick budget, and intercepting the player interactions vanilla would otherwise
 * resolve with its own age timer.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AnimalEvents {

    /** The systems run once a second rather than every tick; a herd is cheap this way. */
    static final int TICK_STEP = 20;
    /** Babies on a mother are recounted this often, not every tick - it costs an entity scan. */
    private static final int DEPENDENT_RECOUNT_TICKS = 100;
    /** Sanity ceiling so a pile of babies on one mother cannot scale her yield without bound. */
    private static final int MAX_DEPENDENTS = 8;

    // ------------------------------------------------------------------ lifecycle

    @SubscribeEvent
    static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Animal animal)) return;
        if (!AnimalState.isSupported(animal)) return;

        if (Config.UNINSTALL_MODE.get()) {
            stripModData(animal);
            return;
        }

        if (!AnimalState.isInitialized(animal)) {
            AnimalState.initializeExisting(animal);
        }

        GrowthStage stage = AnimalState.stage(animal);
        AnimalState.applyStageToAge(animal, stage);
        installGoals(animal);
        ModMessages.syncToTracking(animal);
    }

    /**
     * Binds the newborn to the animal that bore it. Vanilla has no sexes, so the parent that
     * initiated the breeding is treated as the mother - she is the one the calf will follow, and
     * the only one it can ever nurse from.
     */
    @SubscribeEvent
    static void onBabySpawn(BabyEntitySpawnEvent event) {
        if (Config.UNINSTALL_MODE.get()) return;
        AgeableMob child = event.getChild();
        if (child == null || child.level().isClientSide()) return;
        if (!AnimalState.isSupported(child)) return;

        Animal mother = motherOf(event.getParentA(), event.getParentB());
        AnimalState.initializeNewborn(child, mother);
        AnimalState.applyStageToAge(child, GrowthStage.BABY);

        if (mother != null) {
            int born = 1 + spawnLitter(mother, event.getParentB());
            // Giving birth is what puts her in milk, and she comes in with a first feed ready for
            // every baby she just had - a litter of four is not born into an empty udder.
            AnimalState.refreshLactation(mother);
            AnimalState.setDependents(mother, Math.max(AnimalState.dependents(mother), born));
            AnimalState.setMilk(mother,
                    Math.max(AnimalState.milk(mother), Config.SUCKLE_AMOUNT.get() * born));
            ModMessages.syncToTracking(mother);
        }
    }

    /**
     * A sow with a mud wallow sometimes farrows a whole litter instead of the single piglet vanilla
     * makes. Bigger litters are rarer, and every extra piglet is bound to her and competes for the
     * same udder, though her yield scales with the litter so none of them goes hungry for it.
     *
     * @return how many extra piglets were actually spawned
     */
    private static int spawnLitter(Animal mother, Mob otherParent) {
        if (AnimalState.speciesOf(mother) != Species.PIG) return 0;
        if (!(mother.level() instanceof ServerLevel server)) return 0;

        double chance = AnimalState.isInMud(mother)
                ? Config.MUD_LITTER_CHANCE.get()
                : Config.DRY_LITTER_CHANCE.get();
        int extra = LitterCalculator.extraBabies(
                mother.getRandom().nextDouble(), chance,
                mother.getRandom().nextDouble(),
                Config.MAX_LITTER_SIZE.get(), Config.LITTER_RARITY_EXPONENT.get());
        if (extra <= 0) return 0;

        AgeableMob partner = otherParent instanceof AgeableMob ageable ? ageable : mother;
        int spawned = 0;
        for (int i = 0; i < extra; i++) {
            AgeableMob piglet = mother.getBreedOffspring(server, partner);
            if (piglet == null) continue;
            piglet.setBaby(true);
            piglet.moveTo(mother.getX(), mother.getY(), mother.getZ(), mother.getYRot(), 0.0f);
            AnimalState.initializeNewborn(piglet, mother);
            AnimalState.applyStageToAge(piglet, GrowthStage.BABY);
            server.addFreshEntity(piglet);
            spawned++;
        }
        return spawned;
    }

    private static Animal motherOf(Mob parentA, Mob parentB) {
        if (parentA instanceof Animal a && AnimalState.isSupported(a)) return a;
        if (parentB instanceof Animal b && AnimalState.isSupported(b)) return b;
        return null;
    }

    @SubscribeEvent
    static void onStartTracking(PlayerEvent.StartTracking event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        Entity target = event.getTarget();
        if (!AnimalState.isSupported(target) || !AnimalState.isInitialized(target)) return;
        ModMessages.syncToPlayer(target, player);
    }

    // ------------------------------------------------------------------ the economy

    @SubscribeEvent
    static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide()) return;
        if (!(entity instanceof Animal animal) || !AnimalState.isSupported(animal)) return;
        if (Config.UNINSTALL_MODE.get()) return;
        if (!AnimalState.isInitialized(animal)) return;
        if (animal.tickCount % TICK_STEP != 0) return;

        Species species = AnimalState.speciesOf(animal);
        GrowthStage stage = AnimalState.stage(animal);

        tickCooldowns(animal);
        AnimalState.applyStageToAge(animal, stage);

        double nutrition = tickHunger(animal, species);
        nutrition = tickLactation(animal, species, nutrition);
        AnimalState.setNutrition(animal, nutrition);

        tickGrowth(animal, stage);
        tickStarvation(animal, nutrition);
        tickTrample(animal);
    }

    /**
     * One footfall on the ground the animal is standing on. Grass underfoot means the pasture
     * recovered and the block is forgotten; bare dirt accumulates trampling, and once it has been
     * bare well past the recovery window and worked hard enough, it can finally compact into coarse
     * dirt. Both branches are a single block lookup per animal per second.
     */
    private static void tickTrample(Animal animal) {
        if (!Config.PASTURE_DEGRADATION.get()) return;
        if (!animal.onGround()) return;
        if (!(animal.level() instanceof ServerLevel server)) return;

        BlockPos pos = animal.blockPosition().below();
        BlockState state = server.getBlockState(pos);
        PastureData pasture = PastureData.get(server);

        if (state.is(Blocks.GRASS_BLOCK)) {
            pasture.forget(pos);
            return;
        }
        if (!state.is(Blocks.DIRT)) return;

        long gameTime = server.getGameTime();
        int tramples = pasture.trample(pos, gameTime);
        double chance = PastureCalculator.compactChance(
                pasture.bareTicks(pos, gameTime),
                Config.PASTURE_RECOVERY_GRACE_TICKS.get(),
                tramples,
                Config.PASTURE_TRAMPLE_THRESHOLD.get(),
                PastureBlocks.grassRatioAround(server, pos, Config.PASTURE_SCAN_RADIUS.get()),
                Config.PASTURE_COMPACT_MAX_CHANCE.get());

        if (PastureCalculator.compact(chance, animal.getRandom().nextDouble())) {
            server.setBlockAndUpdate(pos, Blocks.COARSE_DIRT.defaultBlockState());
            pasture.forget(pos);
        }
    }

    private static void tickCooldowns(Animal animal) {
        AnimalState.setNurseCooldown(animal, AnimalState.nurseCooldown(animal) - TICK_STEP);
        AnimalState.setLieCooldown(animal, AnimalState.lieCooldown(animal) - TICK_STEP);
    }

    private static double tickHunger(Animal animal, Species species) {
        double perTick = HungerCalculator.decayPerTick(
                Config.HUNGER_DECAY_PER_MINUTE.get(), species.hungerRate());
        return HungerCalculator.drain(AnimalState.nutrition(animal), perTick * TICK_STEP);
    }

    /** Produces milk out of the mother's own nutrition, and dries her up when nothing demands it. */
    private static double tickLactation(Animal animal, Species species, double nutrition) {
        if (!AnimalState.isLactating(animal)) return nutrition;

        int dependents = countDependents(animal);
        int remaining = AnimalState.lactationTicks(animal) - TICK_STEP;
        AnimalState.setLactationTicks(animal, remaining);
        if (remaining <= 0) {
            AnimalState.setMilk(animal, 0.0);
            return nutrition;
        }

        if (!LactationCalculator.canProduce(nutrition, Config.LACTATION_MIN_NUTRITION.get())) {
            return nutrition;
        }

        // Yield and capacity are per mouth, so a sow feeding four makes four times the milk of a
        // cow feeding one. Without that, the litter would share a single calf's worth of udder and
        // the slowest piglets would starve no matter how fairly it was split.
        double perTick = LactationCalculator.productionPerTick(
                Config.MILK_PER_MINUTE.get(), species.milkRate(), dependents);
        double before = AnimalState.milk(animal);
        double after = LactationCalculator.produce(before, perTick * TICK_STEP,
                LactationCalculator.capacity(dependents));
        if (after <= before) return nutrition;

        AnimalState.setMilk(animal, after);
        double cost = LactationCalculator.nutritionCost(after - before, Config.MILK_NUTRITION_COST.get());
        return HungerCalculator.drain(nutrition, cost);
    }

    /**
     * How many babies are nursing from this mother. Recounted every few seconds rather than every
     * tick, and only for mothers actually in milk, so a herd costs almost nothing; between counts
     * the stored number is used. Births and deaths therefore take a moment to register, which no
     * one can feel.
     */
    private static int countDependents(Animal mother) {
        if (mother.tickCount % DEPENDENT_RECOUNT_TICKS >= TICK_STEP) {
            return AnimalState.dependents(mother);
        }

        double radius = Config.NURSE_SEARCH_RADIUS.get();
        List<Animal> babies = mother.level().getEntitiesOfClass(Animal.class,
                mother.getBoundingBox().inflate(radius),
                other -> other != mother
                        && mother.getUUID().equals(AnimalState.mother(other))
                        && GrowthCalculator.acceptsMilk(AnimalState.stage(other)));

        int count = Math.min(babies.size(), MAX_DEPENDENTS);
        AnimalState.setDependents(mother, count);
        return count;
    }

    private static void tickGrowth(Animal animal, GrowthStage stage) {
        if (stage == GrowthStage.ADULT) return;

        AnimalState.addStageTicks(animal, TICK_STEP);
        double speed = Config.GROWTH_SPEED.get();
        double required = GrowthCalculator.requiredIntake(stage, Config.BASE_STAGE_INTAKE.get(), speed);
        int requiredTicks = GrowthCalculator.requiredTicks(stage, Config.BASE_STAGE_TICKS.get(), speed);

        if (!GrowthCalculator.canAdvance(stage, AnimalState.progress(animal), required,
                AnimalState.stageTicks(animal), requiredTicks)) {
            return;
        }

        GrowthStage next = stage.next();
        AnimalState.setStage(animal, next);
        AnimalState.applyStageToAge(animal, next);
        ModMessages.syncToTracking(animal);

        if (animal.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    animal.getX(), animal.getY(0.7), animal.getZ(), 8, 0.4, 0.4, 0.4, 0.0);
        }
    }

    private static void tickStarvation(Animal animal, double nutrition) {
        if (nutrition > 0.0) {
            if (AnimalState.starvingTicks(animal) != 0) {
                AnimalState.setStarvingTicks(animal, 0);
            }
            return;
        }

        int starving = AnimalState.starvingTicks(animal) + TICK_STEP;
        AnimalState.setStarvingTicks(animal, starving);

        // Rounded down to the tick budget so the interval lands on a step we actually run.
        int interval = Math.max(TICK_STEP, (Config.STARVATION_DAMAGE_INTERVAL.get() / TICK_STEP) * TICK_STEP);
        int grace = (Config.STARVATION_GRACE_TICKS.get() / TICK_STEP) * TICK_STEP;
        if (HungerCalculator.shouldTakeStarvationDamage(nutrition, starving, grace, interval)) {
            animal.hurt(animal.damageSources().starve(), Config.STARVATION_DAMAGE.get().floatValue());
        }
    }

    /**
     * Body condition applied to the carcass. A starved animal always yields the worst possible
     * drop, and one butchered while underfed yields somewhere in between, so a neglected herd is
     * worth less meat and less leather even if the player kills it before hunger does.
     */
    @SubscribeEvent
    static void onLivingDrops(LivingDropsEvent event) {
        if (Config.UNINSTALL_MODE.get()) return;
        if (!Config.CONDITION_AFFECTS_DROPS.get()) return;
        if (!(event.getEntity() instanceof Animal animal) || !AnimalState.isSupported(animal)) return;
        if (!AnimalState.isInitialized(animal)) return;

        boolean starved = event.getSource().is(DamageTypes.STARVE);
        double multiplier = LootCalculator.dropMultiplier(
                AnimalState.nutrition(animal),
                starved,
                Config.FULL_CONDITION_NUTRITION.get(),
                Config.STARVED_DROP_MULTIPLIER.get());
        if (multiplier >= 1.0) return;

        Iterator<ItemEntity> drops = event.getDrops().iterator();
        while (drops.hasNext()) {
            ItemStack stack = drops.next().getItem();
            int scaled = LootCalculator.scaleCount(stack.getCount(), multiplier,
                    animal.getRandom().nextDouble());
            if (scaled <= 0) {
                drops.remove();
            } else {
                stack.setCount(scaled);
            }
        }
    }

    // ------------------------------------------------------------------ goals

    private static void installGoals(Animal animal) {
        Species species = AnimalState.speciesOf(animal);
        if (species == null) return;

        // Vanilla sheep already eat grass on their own timer; that goal is replaced so grazing,
        // nutrition and pasture wear all come from one system.
        List<Goal> vanillaEating = new ArrayList<>();
        for (WrappedGoal wrapped : animal.goalSelector.getAvailableGoals()) {
            Goal goal = wrapped.getGoal();
            if (goal instanceof EatBlockGoal) {
                vanillaEating.add(goal);
            } else if (goal instanceof GrazeGoal) {
                return; // already installed on this entity instance
            }
        }
        vanillaEating.forEach(animal.goalSelector::removeGoal);

        animal.goalSelector.addGoal(2, new NurseGoal(animal));
        if (species.nursesLyingDown()) {
            animal.goalSelector.addGoal(3, new SowNursingGoal(animal));
        }
        animal.goalSelector.addGoal(5, new GrazeGoal(animal, species));
    }

    // ------------------------------------------------------------------ player interaction

    @SubscribeEvent
    static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        if (Config.UNINSTALL_MODE.get()) return;
        if (!(event.getTarget() instanceof Animal animal) || !AnimalState.isSupported(animal)) return;
        if (!AnimalState.isInitialized(animal)) return;

        Player player = event.getEntity();
        ItemStack stack = event.getItemStack();
        GrowthStage stage = AnimalState.stage(animal);
        boolean serverSide = !player.level().isClientSide();

        if (Config.SHOW_STATUS_ON_SNEAK.get() && player.isShiftKeyDown() && stack.isEmpty()) {
            if (serverSide) sendStatus(player, animal);
            cancel(event);
            return;
        }

        if (stack.is(Items.MILK_BUCKET) && GrowthCalculator.acceptsMilk(stage)
                && Config.ALLOW_BOTTLE_FEEDING.get()) {
            if (serverSide) bottleFeed(player, animal, stack, stage);
            cancel(event);
            return;
        }

        if (stack.is(Items.BUCKET) && (animal instanceof Cow || animal instanceof Goat)) {
            if (!handleMilking(player, animal, stage, serverSide)) {
                cancel(event);
            }
            return;
        }

        if (stack.is(Items.SHEARS) && Config.RESTRICT_PRODUCE_TO_ADULTS.get() && stage != GrowthStage.ADULT) {
            if (serverSide) message(player, "babyneeds.msg.too_young_to_shear", ChatFormatting.RED);
            cancel(event);
            return;
        }

        if (animal.isFood(stack)) {
            handleFeeding(event, player, animal, stack, stage, serverSide);
        }
    }

    /** Hand feeding: a rescue for a hungry animal, never a shortcut past a stage. */
    private static void handleFeeding(PlayerInteractEvent.EntityInteract event, Player player,
                                      Animal animal, ItemStack stack, GrowthStage stage,
                                      boolean serverSide) {
        if (stage == GrowthStage.ADULT) {
            // Vanilla still handles love mode; nutrition only counts when the food is really eaten.
            if (serverSide && animal.canFallInLove()) {
                feedNutrition(animal, Config.FEED_ITEM_NUTRITION.get());
            }
            return;
        }

        // Below adult, vanilla would age the animal up on the spot - that is exactly what the mod
        // takes over, so the interaction is replaced instead of extended.
        cancel(event);
        if (!serverSide) return;

        if (!GrowthCalculator.acceptsSolids(stage)) {
            message(player, "babyneeds.msg.baby_only_milk", ChatFormatting.RED);
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        double food = Config.FEED_ITEM_NUTRITION.get();
        feedNutrition(animal, food);
        addGrowth(animal, food, stage);
        eatEffects(animal, ParticleTypes.HAPPY_VILLAGER);
    }

    /**
     * @return true when vanilla should continue and hand the player their milk bucket
     */
    private static boolean handleMilking(Player player, Animal animal, GrowthStage stage, boolean serverSide) {
        if (Config.RESTRICT_PRODUCE_TO_ADULTS.get() && stage != GrowthStage.ADULT) {
            if (serverSide) message(player, "babyneeds.msg.too_young_to_milk", ChatFormatting.RED);
            return false;
        }

        double milk = AnimalState.milk(animal);
        double cost = Config.PLAYER_BUCKET_MILK_COST.get();
        int dependents = AnimalState.isLactating(animal) ? AnimalState.dependents(animal) : 0;
        if (!LactationCalculator.canBeMilkedByPlayer(milk, cost, dependents,
                Config.MILK_RESERVE_FOR_CHILD.get())) {
            if (serverSide) message(player, "babyneeds.msg.udder_empty", ChatFormatting.YELLOW);
            return false;
        }

        if (serverSide) {
            AnimalState.setMilk(animal, milk - cost);
            // Being milked is demand: it keeps her in lactation the same way a calf does.
            AnimalState.refreshLactation(animal);
        }
        return true;
    }

    private static void bottleFeed(Player player, Animal animal, ItemStack stack, GrowthStage stage) {
        double amount = Config.SUCKLE_AMOUNT.get();
        feedMilk(animal, amount, stage);
        eatEffects(animal, ParticleTypes.HAPPY_VILLAGER);
        animal.playSound(SoundEvents.GENERIC_DRINK, 0.8f, 1.4f);

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
            if (!player.addItem(new ItemStack(Items.BUCKET))) {
                player.drop(new ItemStack(Items.BUCKET), false);
            }
        }
    }

    // ------------------------------------------------------------------ shared helpers

    static void feedNutrition(Animal animal, double food) {
        AnimalState.setNutrition(animal, HungerCalculator.feed(AnimalState.nutrition(animal), food));
    }

    /** Solid food counts towards growth for every stage that can digest it. */
    static void addGrowth(Animal animal, double amount, GrowthStage stage) {
        if (stage == GrowthStage.ADULT) return;
        double required = GrowthCalculator.requiredIntake(stage, Config.BASE_STAGE_INTAKE.get(),
                Config.GROWTH_SPEED.get());
        AnimalState.setProgress(animal,
                GrowthCalculator.addIntake(AnimalState.progress(animal), amount, required));
    }

    /** One nursing bout, from either a mother's udder or a player's bucket. */
    static void feedMilk(Animal baby, double milkAmount, GrowthStage stage) {
        feedNutrition(baby, milkAmount * Config.MILK_NUTRITION_VALUE.get());
        addGrowth(baby, milkAmount, stage);
        AnimalState.setNurseCooldown(baby, Config.SUCKLE_COOLDOWN_TICKS.get());
    }

    static void eatEffects(Animal animal, net.minecraft.core.particles.SimpleParticleType particle) {
        if (animal.level() instanceof ServerLevel server) {
            server.sendParticles(particle, animal.getX(), animal.getY(0.6), animal.getZ(),
                    5, 0.3, 0.3, 0.3, 0.0);
        }
        animal.playSound(SoundEvents.GENERIC_EAT, 0.6f, 1.2f);
    }

    static void sendStatus(Player player, Animal animal) {
        GrowthStage stage = AnimalState.stage(animal);
        Component stageName = Component.translatable("babyneeds.stage." + stage.key());
        Component line = Component.translatable("babyneeds.status.line",
                animal.getType().getDescription(),
                stageName,
                (int) Math.round(AnimalState.nutrition(animal)),
                (int) Math.round(AnimalState.milk(animal)));
        player.sendSystemMessage(line.copy().withStyle(ChatFormatting.AQUA));

        if (AnimalState.isLactating(animal)) {
            player.sendSystemMessage(Component.translatable("babyneeds.status.lactating")
                    .withStyle(ChatFormatting.GRAY));
        }
        if (stage != GrowthStage.ADULT) {
            double required = GrowthCalculator.requiredIntake(stage, Config.BASE_STAGE_INTAKE.get(),
                    Config.GROWTH_SPEED.get());
            int percent = required <= 0 ? 100
                    : (int) Math.round(100.0 * AnimalState.progress(animal) / required);
            player.sendSystemMessage(Component.translatable("babyneeds.status.growth", percent)
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    private static void message(Player player, String key, ChatFormatting color) {
        player.sendSystemMessage(Component.translatable(key).withStyle(color));
    }

    private static void cancel(PlayerInteractEvent.EntityInteract event) {
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
    }

    /** Returns an animal to vanilla behaviour: mod data gone, age consistent with its stage. */
    static void stripModData(Animal animal) {
        if (!AnimalState.isInitialized(animal)) return;
        GrowthStage stage = AnimalState.stage(animal);
        AnimalState.clear(animal);
        animal.setAge(stage == GrowthStage.ADULT ? 0 : -24000);
    }
}
