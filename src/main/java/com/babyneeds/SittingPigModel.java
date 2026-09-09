package com.babyneeds;

import net.minecraft.client.model.PigModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.Pig;

/**
 * A pig that can actually sit: rump on the ground, hind legs folded forward along it, front legs
 * straight and planted, head level.
 *
 * It has to be done inside the model because {@code setupAnim} runs a few hundred bytecodes after the
 * render event and overwrites anything posed from outside. And it has to be done part by part,
 * because tipping the whole animal backwards - the first attempt - buries the hind legs in the floor
 * and points the snout at the sky.
 *
 * <h2>The geometry, since guessing at it went badly</h2>
 *
 * A pig's parts, in model units, with +y pointing DOWN and +z pointing BACKWARDS: the body pivot sits
 * at y=11, z=2 with the trunk already rotated 90 degrees, which spreads it from z=-8 (chest) to z=+8
 * (rump) with its underside at y=18. The four leg pivots are also at y=18 and a leg is 6 long, so the
 * ground is y=24.
 *
 * A part rotates about its own pivot, and the body's pivot is up at the height of the neck. Rotating
 * the trunk there swings the animal about its own throat, which is what drove the snout into the dirt
 * in every earlier attempt. The point that must hold still when a pig sits is where its chest meets
 * its front legs: z=-8, y=18 - ten forward of the pivot and seven below it. So the tilt is applied,
 * the new position of that point is worked out, and the whole trunk is slid back by the difference so
 * the chest does not move at all.
 *
 * The angle then follows from the animal rather than from taste: the rump has to fall from y=18 to
 * the ground at y=24, six units, over the sixteen between the anchor and the tail. That is
 * asin(6/16) - about 22 degrees - and it is why the default tilt is what it is rather than a rounder
 * number.
 */
public class SittingPigModel<T extends Pig> extends PigModel<T> {

    /** The trunk's resting rotation. 90 degrees is what lays it flat along the animal. */
    private static final float FLAT = (float) Math.PI / 2.0f;

    private final float restBodyY;
    private final float restBodyZ;
    private final float restHeadY;
    private final float restHindY;
    private final float restHindZ;

    public SittingPigModel(ModelPart root) {
        super(root);
        // setupAnim rewrites rotations every frame but never offsets, so the resting ones have to be
        // remembered here and put back by hand.
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
        restoreStandingPose();

        float sit = sitProgress(entity);
        if (sit <= 0.0f) return;
        applySit(sit);
    }

    /**
     * One model instance draws every pig on screen, so the standing pose has to be put back before
     * the next animal is posed - otherwise one pig sitting sits the whole herd.
     */
    private void restoreStandingPose() {
        body.y = restBodyY;
        body.z = restBodyZ;
        head.y = restHeadY;
        rightHindLeg.y = restHindY;
        leftHindLeg.y = restHindY;
        rightHindLeg.z = restHindZ;
        leftHindLeg.z = restHindZ;
    }

    private void applySit(float sit) {
        float tilt = Config.SIT_TILT.get().floatValue() * sit;
        float sin = Mth.sin(tilt);
        float cos = Mth.cos(tilt);

        // The anchor, relative to the body's pivot: forwards is negative z, downwards is positive y.
        float anchorZ = -Config.SIT_ANCHOR_FORWARD.get().floatValue();
        float anchorY = Config.SIT_ANCHOR_BELOW.get().floatValue();

        // Where the rotation would carry that point, and therefore how far the trunk has to move to
        // leave it exactly where it was. Rotating about the chest instead of the throat is the fix.
        float carriedY = anchorY * cos - anchorZ * sin;
        float carriedZ = anchorY * sin + anchorZ * cos;

        body.xRot = FLAT + tilt;
        body.y = restBodyY + (anchorY - carriedY);
        body.z = restBodyZ + (anchorZ - carriedZ);

        // The head is a sibling of the body rather than a child, so it stays put and goes on looking
        // wherever the pig is looking - which is what a sitting animal does. With the chest anchored
        // the chest barely moves, so neither should it.
        head.y = restHeadY + Config.SIT_HEAD_DROP.get().floatValue() * sit;

        // Hind legs fold forward and lie along the ground. Folded flat, a leg no longer reaches the
        // six units below its pivot that it does standing, so the pivot drops to make up the
        // difference and the trotters stay on the floor instead of hanging in the air.
        float fold = Config.SIT_HIND_FOLD.get().floatValue() * sit;
        rightHindLeg.xRot = fold;
        leftHindLeg.xRot = fold;
        rightHindLeg.y = restHindY + Config.SIT_HIND_DROP.get().floatValue() * sit;
        leftHindLeg.y = rightHindLeg.y;
        rightHindLeg.z = restHindZ + Config.SIT_HIND_SHIFT.get().floatValue() * sit;
        leftHindLeg.z = rightHindLeg.z;

        // The front pair is left exactly as vanilla put it: straight, planted, holding the chest up.
    }

    /** 0 standing, 1 fully sat, eased - the same value the rest of the mod's animation runs on. */
    private float sitProgress(T entity) {
        ClientStageCache.Entry entry = ClientStageCache.peek(entity.getId());
        return entry == null ? 0.0f : entry.sitProgress();
    }
}
