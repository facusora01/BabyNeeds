package com.babyneeds;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * The mud a pig picks up from rolling in a wallow. Rather than shipping a second skin, this draws
 * the animal's own model again in a translucent brown wash whose opacity is how filthy it is, so a
 * pig fresh out of the mud is dark and caked and one that has been dry for a while is barely marked.
 *
 * It fades as the pig dries off and washes away in clean water, so the coating is a live readout of
 * what the animal has been doing rather than a permanent skin swap.
 */
public class MudLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final ResourceLocation PIG_TEXTURE =
            new ResourceLocation("textures/entity/pig/pig.png");
    /** Wet silt: dark, desaturated brown. */
    private static final float RED = 0.34f;
    private static final float GREEN = 0.26f;
    private static final float BLUE = 0.15f;
    /** Never fully opaque - some of the animal underneath should always show through. */
    private static final float MAX_ALPHA = 0.85f;

    public MudLayer(RenderLayerParent<T, M> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack pose, MultiBufferSource buffer, int packedLight, T entity,
                       float limbSwing, float limbSwingAmount, float partialTick,
                       float ageInTicks, float netHeadYaw, float headPitch) {
        if (entity.isInvisible()) return;

        ClientStageCache.Entry entry = ClientStageCache.peek(entity.getId());
        if (entry == null) return;

        float muddiness = entry.muddiness();
        if (muddiness <= 0.02f) return;

        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucent(PIG_TEXTURE));
        getParentModel().renderToBuffer(pose, consumer, packedLight, OverlayTexture.NO_OVERLAY,
                RED, GREEN, BLUE, muddiness * MAX_ALPHA);
    }
}
