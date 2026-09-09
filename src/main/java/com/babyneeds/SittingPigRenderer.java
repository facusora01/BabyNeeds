package com.babyneeds;

import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.PigRenderer;

/**
 * Vanilla's pig renderer with the model swapped for one that can sit. Everything else about it -
 * texture, shadow, the saddle layer - is inherited untouched, so this is the smallest change that
 * gets per-part posing: the model field is protected, which means a subclass can replace it without
 * an access transformer or a mixin.
 */
public class SittingPigRenderer extends PigRenderer {

    public SittingPigRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.model = new SittingPigModel<>(context.bakeLayer(ModelLayers.PIG));
    }
}
