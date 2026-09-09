package com.babyneeds;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
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

    /**
     * The trunk's resting rotation: 90 degrees is what lays it flat along the animal, and the sit is
     * measured from there.
     */
    private static final float FLAT = (float) Math.PI / 2.0f;

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

        // Every number below is read live from the config rather than compiled in. A pose is art:
        // it cannot be derived, only looked at, and working the tilt out on paper got its sign wrong
        // twice. This way the file can be edited with the game running and the pig changes shape
        // under you until it looks right.

        // A part rotates about its own pivot, and the body's pivot is in the middle of the animal,
        // so tilting the trunk on its own seesaws it: the rump sinks and the chest lifts clear of the
        // front legs. Both earlier attempts looked wrong for that reason rather than because of the
        // direction - one was a seesaw one way, one was a seesaw the other.
        //
        // Anchoring a point at the shoulders turns the seesaw into sitting: work out where the tilt
        // would carry that point, then slide the whole trunk back so it does not move at all. What is
        // left over is the rump going down by itself, which is the motion being drawn.
        float tilt = Config.SIT_BODY_TILT.get().floatValue() * sit;
        float anchor = Config.SIT_BODY_ANCHOR.get().floatValue();

        body.xRot = FLAT + tilt;
        // Minus, not plus. Rotating by tilt carries the anchor point to (-anchor*sin, -anchor*cos)
        // relative to where it was, so undoing that means moving the trunk the other way - getting
        // this backwards doubled the very error the anchor exists to cancel.
        body.y = restBodyY - anchor * Mth.sin(tilt)
                + Config.SIT_BODY_DROP.get().floatValue() * sit;
        body.z = restBodyZ - anchor * (1.0f - Mth.cos(tilt))
                + Config.SIT_BODY_SHIFT.get().floatValue() * sit;

        // The head keeps its own pitch - it is a sibling of the body, not a child, so it goes on
        // looking wherever the pig is looking instead of being swung skywards with the chest.
        head.y = restHeadY + Config.SIT_HEAD_DROP.get().floatValue() * sit;

        // Hind legs fold under the animal; the front pair stays planted exactly as it is.
        float fold = Config.SIT_HIND_FOLD.get().floatValue() * sit;
        float drop = Config.SIT_HIND_DROP.get().floatValue() * sit;
        float shift = Config.SIT_HIND_SHIFT.get().floatValue() * sit;
        rightHindLeg.xRot = fold;
        leftHindLeg.xRot = fold;
        rightHindLeg.y = restHindY + drop;
        leftHindLeg.y = restHindY + drop;
        rightHindLeg.z = restHindZ + shift;
        leftHindLeg.z = restHindZ + shift;
    }

    /** 0 standing, 1 fully sat, eased - the same value the rest of the mod's animation runs on. */
    private float sitProgress(T entity) {
        ClientStageCache.Entry entry = ClientStageCache.peek(entity.getId());
        return entry == null ? 0.0f : entry.sitProgress();
    }
}
