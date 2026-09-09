package com.babyneeds;

import java.util.EnumSet;
import java.util.List;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.ItemStack;

/**
 * Scraps on the ground. A pig will cross a pen for dropped food, which is how a player feeds a herd
 * without hand-feeding every animal: throw the harvest over the fence and they sort it out.
 *
 * Nutrition follows the food's own value to a player, so a thrown loaf of bread is worth several
 * potatoes and a rotten scrap is worth almost nothing.
 */
public class ScavengeGoal extends Goal {

    private static final double PICKUP_DISTANCE_SQ = 2.25;
    private static final int SEARCH_COOLDOWN_TICKS = 40;

    private final Animal animal;
    private ItemEntity target;
    private int searchCooldown;
    private int travelTicks;

    public ScavengeGoal(Animal animal) {
        this.animal = animal;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!Config.PIGS_EAT_SCRAPS.get()) return false;
        if (!GrowthCalculator.acceptsSolids(AnimalState.stage(animal))) return false;
        if (AnimalState.isLying(animal)) return false;
        if (AnimalState.nutrition(animal) >= HungerCalculator.MAX_NUTRITION) return false;
        if (searchCooldown-- > 0) return false;
        searchCooldown = SEARCH_COOLDOWN_TICKS;

        this.target = findScrap();
        return target != null;
    }

    @Override
    public boolean canContinueToUse() {
        return target != null && target.isAlive() && !target.getItem().isEmpty()
                && travelTicks < 100 && !AnimalState.isLying(animal);
    }

    @Override
    public void start() {
        travelTicks = 0;
        animal.getNavigation().moveTo(target, 1.15);
    }

    @Override
    public void stop() {
        target = null;
        travelTicks = 0;
        animal.getNavigation().stop();
    }

    @Override
    public void tick() {
        animal.getLookControl().setLookAt(target, 30.0f, 30.0f);
        travelTicks++;

        if (animal.distanceToSqr(target) > PICKUP_DISTANCE_SQ) {
            if (travelTicks % 20 == 0) {
                animal.getNavigation().moveTo(target, 1.15);
            }
            return;
        }
        eat();
    }

    private void eat() {
        ItemStack stack = target.getItem();
        double food = nutritionOf(stack);
        stack.shrink(1);
        if (stack.isEmpty()) {
            target.discard();
        }
        target = null;

        AnimalEvents.feedNutrition(animal, food);
        AnimalEvents.addGrowth(animal, food, AnimalState.stage(animal));
        AnimalEvents.eatEffects(animal, net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER);
        animal.playSound(SoundEvents.GENERIC_EAT, 0.7f, 1.1f);
    }

    private double nutritionOf(ItemStack stack) {
        FoodProperties food = stack.getFoodProperties(animal);
        int points = food == null ? 1 : food.getNutrition();
        return points * Config.SCRAP_NUTRITION_PER_POINT.get();
    }

    private ItemEntity findScrap() {
        double radius = Config.SCAVENGE_RADIUS.get();
        List<ItemEntity> scraps = animal.level().getEntitiesOfClass(ItemEntity.class,
                animal.getBoundingBox().inflate(radius),
                item -> item.isAlive() && item.getItem().isEdible());

        ItemEntity best = null;
        double bestDistance = Double.MAX_VALUE;
        for (ItemEntity scrap : scraps) {
            double distance = animal.distanceToSqr(scrap);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = scrap;
            }
        }
        return best;
    }
}
