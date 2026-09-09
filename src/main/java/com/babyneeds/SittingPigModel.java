package com.babyneeds;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.world.entity.animal.Pig;

/**
 * A pig that can actually sit. Vanilla has no sitting pose for a quadruped farm animal, and the
 * transform trick the mod used before - tipping the whole model back - produces the three faults you
 * would expect from it: the head ends up pointing at the sky, the hind legs sink through the floor
 * and the front legs come off it.
 *
 * A real sit has to move the parts separately, which means posing them after vanilla has: the render
 * event fires at the very start of {@code render()} and {@code setupAnim} overwrites everything a few
 * hundred bytecodes later, so the only place this can live is inside the model itself.
 *
 * The shape being aimed at: rump down on the ground, hind legs folded forward under it, front legs
 * planted and straight, head level and looking wherever the pig is looking. The parts of a
 * {@link net.minecraft.client.model.QuadrupedModel} are siblings rather than a chain, so dropping the
 * trunk leaves the head and legs behind - hence each one is moved on its own.
 */
public class SittingPigModel<T extends Pig> extends PigModel<T> {

    // The five tuning knobs. Model space has +y pointing DOWN and +z pointing backwards.
    /** How far the trunk sinks as the rump goes down. */
    private static final float BODY_DROP = 2.0f;
    /** How far back it slides while it does. */
    private static final float BODY_SHIFT = 1.0f;
    /** Tilt of the trunk: rear down, chest up. Flip the sign to tip it the other way. */
    private static final float BODY_TILT = 0.5f;
    /** How far the head follows the chest down. The chest barely moves, so neither does it. */
    private static final float HEAD_DROP = 1.0f;
    /** Hind legs folded forward under the body, and dropped to meet the ground. */
    private static final float HIND_LEG_FOLD = -1.35f;
    private static final float HIND_LEG_DROP = 3.0f;
    private static final float HIND_LEG_SHIFT = -1.0f;

    private final float restBodyY;
    private final float restBodyZ;
    private final float restHeadY;
    private final float restHindY;
    private final float restHindZ;

    public SittingPigModel(ModelPart root) {
        super(root);
        // setupAnim rewrites rotations every frame but never the offsets, so the resting ones have
        // to be remembered here and put back by hand.
        this.restBodyY = body.y;
        this.restBodyZ = body.z;
        this.restHeadY = head.y;
        this.restHindY = rightHindLeg.y;
        this.restHindZ = rightHindLeg.z;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        // One model instance draws every pig on screen, so the standing pose has to be restored
        // before the next animal is posed - otherwise one pig sitting sits the whole herd.
        body.y = restBodyY;
        body.z = restBodyZ;
        head.y = restHeadY;
        rightHindLeg.y = restHindY;
        leftHindLeg.y = restHindY;
        rightHindLeg.z = restHindZ;
        leftHindLeg.z = restHindZ;

        float sit = sitProgress(entity);
        if (sit <= 0.0f) return;

        // The rump goes down and the trunk tips back with it.
        body.y = restBodyY + BODY_DROP * sit;
        body.z = restBodyZ + BODY_SHIFT * sit;
        body.xRot += BODY_TILT * sit;

        // The head keeps its own pitch - it is a sibling of the body, not a child, so it goes on
        // looking wherever the pig is looking instead of being swung skywards with the chest.
        head.y = restHeadY + HEAD_DROP * sit;

        // Hind legs fold forward under the animal; the front pair stays planted exactly as it is.
        rightHindLeg.xRot = HIND_LEG_FOLD * sit;
        leftHindLeg.xRot = HIND_LEG_FOLD * sit;
        rightHindLeg.y = restHindY + HIND_LEG_DROP * sit;
        leftHindLeg.y = restHindY + HIND_LEG_DROP * sit;
        rightHindLeg.z = restHindZ + HIND_LEG_SHIFT * sit;
        leftHindLeg.z = restHindZ + HIND_LEG_SHIFT * sit;
    }

    /** 0 standing, 1 fully sat, eased - the same value the rest of the mod's animation runs on. */
    private float sitProgress(T entity) {
        ClientStageCache.Entry entry = ClientStageCache.peek(entity.getId());
        return entry == null ? 0.0f : entry.sitProgress();
    }
}
