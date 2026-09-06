package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;

public class PipRadioGeoModel extends GeoModel<PipRadioItem> {
    @Override
    public ResourceLocation getModelResource(PipRadioItem animatable) {
        return animatable.getGeoModelResource();
    }

    @Override
    public ResourceLocation getTextureResource(PipRadioItem animatable) {
        return animatable.getGeoTextureResource();
    }

    @Override
    public ResourceLocation getAnimationResource(PipRadioItem animatable) {
        return animatable.getGeoAnimationResource();
    }
}
