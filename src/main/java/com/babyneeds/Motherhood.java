package com.babyneeds;

import java.util.List;
import java.util.UUID;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * What happens between a baby and an adult that is not its mother.
 *
 * Vanilla already has orphans trailing after any adult of their species, which looks like a calf
 * looking for a mother because that is exactly what it is. Two things come out of that here: an
 * adult may take the orphan on - and comes into milk for it, having never given birth - or may
 * refuse, and a refusal from a large animal is a headbutt, not a shrug.
 *
 * And a mother whose baby is attacked goes after whoever did it. She is a cow, so she is not much
 * of a threat, but she is not going to stand and watch either.
 */
@Mod.EventBusSubscriber(modid = BabyNeeds.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class Motherhood {

    /** How close an orphan has to get before an adult decides about it. */
    private static final double ADOPTION_RANGE = 4.0;

    private Motherhood() {
    }

    // ------------------------------------------------------------------ adoption

    /**
     * An orphan close to a grown animal of its own kind, asking to be taken on. Called on the mod's
     * once-a-second budget, and only for babies that have no living mother left.
     */
    static void tickOrphan(Animal baby) {
        if (!Config.ALLOW_ADOPTION.get()) return;
        if (!GrowthCalculator.acceptsMilk(AnimalState.stage(baby))) return;
        if (AnimalState.findMother(baby) != null) return;

        int cooldown = AnimalState.adoptionCooldown(baby) - AnimalEvents.TICK_STEP;
        AnimalState.setAdoptionCooldown(baby, cooldown);
        if (cooldown > 0) return;

        Animal candidate = findFoster(baby);
        if (candidate == null) return;

        // One roll per adult, not per second: whichever way it goes, the answer stands for a while.
        AnimalState.setAdoptionCooldown(baby, Config.ADOPTION_RETRY_TICKS.get());
        AnimalState.setRejectedBy(baby, candidate.getUUID());

        if (baby.getRandom().nextDouble() < Config.ADOPTION_CHANCE.get()) {
            adopt(baby, candidate);
        } else {
            reject(baby, candidate);
        }
    }

    /** The nearest grown animal of the same species that has not already turned this baby away. */
    private static Animal findFoster(Animal baby) {
        Species species = AnimalState.speciesOf(baby);
        UUID rejectedBy = AnimalState.rejectedBy(baby);

        List<Animal> nearby = baby.level().getEntitiesOfClass(Animal.class,
                baby.getBoundingBox().inflate(ADOPTION_RANGE),
                other -> other != baby
                        && AnimalState.speciesOf(other) == species
                        && AnimalState.stage(other) == GrowthStage.ADULT
                        && !other.getUUID().equals(rejectedBy));

        Animal best = null;
        double bestDistance = Double.MAX_VALUE;
        for (Animal candidate : nearby) {
            double distance = baby.distanceToSqr(candidate);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = candidate;
            }
        }
        return best;
    }

    /** She takes it on, and comes into milk for it despite never having given birth. */
    private static void adopt(Animal baby, Animal foster) {
        AnimalState.setMother(baby, foster.getUUID());
        AnimalState.clearRejectedBy(baby);
        AnimalState.refreshLactation(foster);
        AnimalState.setDependents(foster, AnimalState.dependents(foster) + 1);
        AnimalState.setMilk(foster, Math.max(AnimalState.milk(foster), Config.SUCKLE_AMOUNT.get()));

        ModMessages.syncToTracking(foster);
        foster.playSound(SoundEvents.COW_AMBIENT, 0.8f, 1.1f);

        if (baby.level() instanceof ServerLevel server) {
            server.sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                    baby.getX(), baby.getY(1.0), baby.getZ(), 5, 0.3, 0.3, 0.3, 0.0);
        }
    }

    /**
     * She will not have it. A refusal from an animal that size is a headbutt: head down, snapped back
     * up, and the calf goes flying. It does not kill - it teaches.
     */
    private static void reject(Animal baby, Animal foster) {
        AnimalEvents.setPose(foster, PigPose.HEAD_DOWN);
        AnimalState.setHeadDownTicks(foster, Config.HEADBUTT_DIP_TICKS.get());
        foster.getLookControl().setLookAt(baby, 30.0f, 30.0f);

        baby.hurt(baby.damageSources().mobAttack(foster), Config.REJECTION_DAMAGE.get().floatValue());

        Vec3 away = baby.position().subtract(foster.position());
        if (away.lengthSqr() < 1.0E-4) {
            away = foster.getLookAngle();
        }
        away = away.normalize().scale(Config.REJECTION_KNOCKBACK.get());
        baby.push(away.x, 0.35, away.z);
        baby.hurtMarked = true;

        foster.playSound(SoundEvents.GOAT_RAM_IMPACT, 0.9f, 1.2f);
    }

    // ------------------------------------------------------------------ defence

    /**
     * Hurt a baby in front of its mother and she comes for you. She is a farm animal, so this is a
     * shove and a headbutt rather than a real threat, but standing there watching would be worse.
     */
    @SubscribeEvent
    static void onLivingHurt(LivingHurtEvent event) {
        if (!Config.MOTHERS_DEFEND_BABIES.get()) return;
        if (Config.UNINSTALL_MODE.get()) return;
        if (!(event.getEntity() instanceof Animal baby) || !AnimalState.isSupported(baby)) return;
        if (!AnimalState.isInitialized(baby)) return;
        if (AnimalState.stage(baby) == GrowthStage.ADULT) return;

        LivingEntity attacker = attackerOf(event.getSource());
        if (attacker == null || attacker == baby) return;

        Animal mother = AnimalState.findMother(baby);
        if (mother == null || mother == attacker) return;
        // A creeper blast that catches the calf as well is nobody's fault in particular.
        if (mother.distanceToSqr(baby) > Config.DEFEND_RADIUS.get() * Config.DEFEND_RADIUS.get()) return;

        mother.setTarget(attacker);
        AnimalState.setDefendTicks(mother, Config.DEFEND_TICKS.get());
        mother.playSound(SoundEvents.GOAT_LONG_JUMP, 1.0f, 0.9f);
    }

    private static LivingEntity attackerOf(DamageSource source) {
        Entity direct = source.getEntity();
        if (direct instanceof Player player) return player;
        return direct instanceof LivingEntity living ? living : null;
    }

    /**
     * Anger has a shelf life and a leash. She gives up when the timer runs out or when whoever it
     * was has put enough distance between them, because a cow that follows a player across the map
     * is a bug wearing a feature's clothes.
     */
    static void tickDefending(Animal mother) {
        int remaining = AnimalState.defendTicks(mother) - AnimalEvents.TICK_STEP;
        AnimalState.setDefendTicks(mother, remaining);

        LivingEntity target = mother.getTarget();
        if (target == null) return;

        double radius = Config.DEFEND_RADIUS.get();
        boolean tooFar = mother.distanceToSqr(target) > radius * radius;
        if (remaining <= 0 || tooFar || !target.isAlive()) {
            mother.setTarget(null);
        }
    }
}
