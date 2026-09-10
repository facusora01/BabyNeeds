package com.babyneeds;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.animal.Animal;

/**
 * The conversation between a hungry piglet and its mother.
 *
 * A sow does not decide to nurse because a timer said so. The piglet is the one who wants something,
 * so the piglet is the one who asks, and the sow answers - and she is allowed to say no. She has to
 * be carrying enough milk for the bout to be worth it, she will not go straight back down after the
 * last one, and even with both of those satisfied it is still a coin toss. That last part is the
 * point: a litter that gets fed on demand every time is a machine, and a litter that has to keep
 * asking is an animal.
 */
public final class NursingRequest {

    /** The answer a piglet gets back, so the caller knows whether to keep waiting or wander off. */
    public enum Answer {
        /** She is already down; there is nothing to ask for. */
        ALREADY_LYING,
        /** She agreed. The bout starts on her next AI tick. */
        ACCEPTED,
        /** No, for now - out of milk, just got up, or simply did not feel like it. */
        REFUSED
    }

    private NursingRequest() {
    }

    /**
     * A piglet asking its mother to lie down. Called from the baby's own goal once it has walked to
     * her side, and rate-limited by the baby's ask cooldown so it is a request rather than nagging.
     */
    public static Answer ask(Animal baby, Animal mother) {
        if (AnimalState.pose(mother).isOnSide()) return Answer.ALREADY_LYING;
        if (AnimalState.askCooldown(baby) > 0) return Answer.REFUSED;

        // Whatever the answer, the piglet waits before asking again. Set before the roll so a
        // refusal costs it the same wait as a yes.
        AnimalState.setAskCooldown(baby, Config.NURSE_ASK_COOLDOWN_TICKS.get());
        nudge(baby, mother);

        if (!accepts(mother)) return Answer.REFUSED;

        AnimalState.setLieRequest(mother, Config.LIE_REQUEST_TICKS.get());
        return Answer.ACCEPTED;
    }

    /**
     * Her side of it. The two hard conditions come first because they are not up for negotiation -
     * no milk is no milk - and only then does she actually get a say.
     */
    private static boolean accepts(Animal mother) {
        if (!AnimalState.isLactating(mother)) return false;
        if (AnimalState.milk(mother) < Config.LIE_DOWN_MIN_MILK.get()) return false;
        if (AnimalState.lieCooldown(mother) > 0) return false;
        return mother.getRandom().nextDouble() < Config.LIE_DOWN_ACCEPT_CHANCE.get();
    }

    /**
     * The asking itself, made visible. Without this the whole exchange is invisible and a refusal is
     * indistinguishable from the mod being broken - which is exactly the hole this feature fell into
     * the first time.
     */
    private static void nudge(Animal baby, Animal mother) {
        baby.getLookControl().setLookAt(mother, 30.0f, 30.0f);
        baby.playSound(SoundEvents.PIG_AMBIENT, 0.5f, 1.6f);

        if (baby.level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SMOKE,
                    baby.getX(), baby.getY(0.8), baby.getZ(), 2, 0.1, 0.1, 0.1, 0.0);
        }
    }
}
