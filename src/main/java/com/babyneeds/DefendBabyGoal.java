package com.babyneeds;

import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.phys.Vec3;

/**
 * A mother going after whoever hurt her baby. She only ever has a target because something attacked
 * her calf - there is no target goal to go with this one, so she never starts anything.
 *
 * A cow that trots over at walking pace is not a defence, it is an escort, so this runs at a real
 * charge. And a cow that connects sometimes puts her whole weight behind it: a headbutt that throws
 * whoever it hits well clear. That is the difference between an animal being annoying and an animal
 * being worth respecting.
 */
public class DefendBabyGoal extends MeleeAttackGoal {

    private final Animal defender;
    private final boolean canCharge;

    public DefendBabyGoal(Animal defender, boolean canCharge) {
        super(defender, Config.DEFEND_SPEED.get(), true);
        this.defender = defender;
        this.canCharge = canCharge;
    }

    @Override
    protected void checkAndPerformAttack(LivingEntity target, double distanceSq) {
        int before = getTicksUntilNextAttack();
        super.checkAndPerformAttack(target, distanceSq);

        // The cooldown only resets when a blow actually lands, which is the cheapest way to know one
        // did without reimplementing vanilla's reach and timing rules.
        boolean landed = getTicksUntilNextAttack() > before;
        if (landed && canCharge && defender.getRandom().nextDouble() < Config.CHARGE_CHANCE.get()) {
            charge(target);
        }
    }

    /** Head down, shoulder in, and whoever it was goes flying. */
    private void charge(LivingEntity target) {
        Vec3 away = target.position().subtract(defender.position());
        if (away.horizontalDistanceSqr() < 1.0E-4) {
            away = defender.getLookAngle();
        }
        away = away.normalize();

        target.knockback(Config.CHARGE_KNOCKBACK.get(), -away.x, -away.z);
        target.push(0.0, 0.42, 0.0);
        target.hurtMarked = true;

        AnimalEvents.setPose(defender, PigPose.HEAD_DOWN);
        AnimalState.setHeadDownTicks(defender, Config.HEADBUTT_DIP_TICKS.get());
        defender.playSound(SoundEvents.GOAT_RAM_IMPACT, 1.0f, 0.9f);
    }
}
