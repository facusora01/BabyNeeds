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
public class PosedPigModel<T extends Pig> extends PigModel<T> {

    private final float restFrontZRot;
    private final float restHindZRot;
    private final float restBodyXRot;
    private final float restBodyY;
    private final float restBodyZ;
    private final float restHeadY;
    private final float restHeadZ;
    private final float restFrontY;
    private final float restFrontZ;
    private final float restHindY;
    private final float restHindZ;

    public PosedPigModel(ModelPart root) {
        super(root);
        // setupAnim rewrites rotations every frame but never offsets, so the resting ones have to be
        // remembered here and put back by hand.
        this.restFrontZRot = rightFrontLeg.zRot;
        this.restHindZRot = rightHindLeg.zRot;
        this.restBodyXRot = body.xRot;
        this.restBodyY = body.y;
        this.restBodyZ = body.z;
        this.restHeadY = head.y;
        this.restHeadZ = head.z;
        this.restFrontY = rightFrontLeg.y;
        this.restFrontZ = rightFrontLeg.z;
        this.restHindY = rightHindLeg.y;
        this.restHindZ = rightHindLeg.z;
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);
        restoreStandingPose();

        ClientStageCache.Entry entry = ClientStageCache.peek(entity.getId());
        if (entry == null) return;

        // Lying and sitting are different shapes, not two amounts of the same one, so whichever the
        // animal is doing wins outright rather than the two being mixed.
        float side = entry.sideProgress();
        if (side > 0.0f) {
            applyLying(side);
            return;
        }

        float sit = entry.sitProgress();
        if (sit > 0.0f) {
            applySit(sit);
        }
    }

    /**
     * Down on its side, legs stretched out in front of it and splayed a little - the shape a pig
     * makes in a wallow, and the one it keeps while it rocks from flank to flank.
     *
     * Only the legs are posed here. The roll itself is a rotation of the whole animal, applied by
     * the renderer, because a body rolling over is genuinely one rigid thing turning; what the whole
     * animal cannot do is stretch its own legs out, which is why this half lives in the model.
     */
    private void applyLying(float side) {
        float fold = Config.LIE_LEG_FOLD.get().floatValue() * side;
        float splay = Config.LIE_LEG_SPLAY.get().floatValue() * side;

        rightFrontLeg.xRot = fold;
        leftFrontLeg.xRot = fold;
        rightHindLeg.xRot = fold;
        leftHindLeg.xRot = fold;

        // Mirrored, so they open away from each other rather than leaning together.
        rightFrontLeg.zRot = restFrontZRot + splay;
        leftFrontLeg.zRot = restFrontZRot - splay;
        rightHindLeg.zRot = restHindZRot + splay;
        leftHindLeg.zRot = restHindZRot - splay;
    }

    /**
     * One model instance draws every pig on screen, so the standing pose has to be put back before
     * the next animal is posed - otherwise one pig sitting sits the whole herd, which is exactly
     * what happened while this method only put back the offsets.
     *
     * The rotations need it too, and only some of them: {@code setupAnim} rewrites the head and all
     * four legs every frame, so those clean themselves, but it never touches the body. The trunk's
     * ninety degrees is baked into the part when the model is built and never written again - so a
     * tilt left on it by the last pig drawn is inherited by every pig after it.
     */
    private void restoreStandingPose() {
        // setupAnim rewrites xRot on the head and all four legs, and nothing else - not the
        // body's rotation, not any zRot. Everything it leaves alone has to be put back here or one
        // animal's pose leaks into every animal drawn after it.
        body.xRot = restBodyXRot;
        rightFrontLeg.zRot = restFrontZRot;
        leftFrontLeg.zRot = restFrontZRot;
        rightHindLeg.zRot = restHindZRot;
        leftHindLeg.zRot = restHindZRot;
        body.y = restBodyY;
        body.z = restBodyZ;
        head.y = restHeadY;
        head.z = restHeadZ;
        rightFrontLeg.y = restFrontY;
        leftFrontLeg.y = restFrontY;
        rightFrontLeg.z = restFrontZ;
        leftFrontLeg.z = restFrontZ;
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

        body.xRot = restBodyXRot + tilt;
        body.y = restBodyY + (anchorY - carriedY);
        body.z = restBodyZ + (anchorZ - carriedZ);

        // The head is a sibling of the body rather than a child, so it holds still on its own and
        // goes on looking wherever the pig is looking. The pitch here is added to that rather than
        // replacing it, so a sitting pig still tracks whatever it is watching.
        head.y = restHeadY + Config.SIT_HEAD_DROP.get().floatValue() * sit;
        head.z = restHeadZ + Config.SIT_HEAD_SHIFT.get().floatValue() * sit;
        head.xRot += Config.SIT_HEAD_PITCH.get().floatValue() * sit;

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

        // The front pair defaults to exactly what vanilla put there - straight, planted, holding the
        // chest up - but it is posable too, because a pig sitting right back on its rump leans them.
        float frontFold = Config.SIT_FRONT_FOLD.get().floatValue() * sit;
        if (frontFold != 0.0f) {
            rightFrontLeg.xRot = frontFold;
            leftFrontLeg.xRot = frontFold;
        }
        rightFrontLeg.y = restFrontY + Config.SIT_FRONT_DROP.get().floatValue() * sit;
        leftFrontLeg.y = rightFrontLeg.y;
        rightFrontLeg.z = restFrontZ + Config.SIT_FRONT_SHIFT.get().floatValue() * sit;
        leftFrontLeg.z = rightFrontLeg.z;
    }

}
