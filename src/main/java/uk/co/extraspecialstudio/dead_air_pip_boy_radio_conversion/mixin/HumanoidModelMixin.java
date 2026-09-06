package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.mixin;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoySilhouetteRenderer;

/**
 * After HumanoidModel.setupAnim — kill ITEM/use raises for the Pip STAT/LCD doll.
 * {@link PlayerModelMixin} re-applies after PlayerModel finishes (sleeves).
 */
@Mixin(HumanoidModel.class)
public abstract class HumanoidModelMixin<T extends LivingEntity> {

    @Inject(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At("RETURN"),
        require = 1
    )
    private void deadAirPip$calmWalkForDoll(
        T entity,
        float limbSwing,
        float limbSwingAmount,
        float ageInTicks,
        float netHeadYaw,
        float headPitch,
        CallbackInfo ci
    ) {
        if (!Boolean.TRUE.equals(PipBoySilhouetteRenderer.NEUTRAL_ARM_POSES.get())) {
            return;
        }
        // PlayerModel.setupAnim continues after this — final pass is PlayerModelMixin.
        // Still apply here for non-player humanoids / safety.
        PipBoySilhouetteRenderer.applyCalmWalkLimbs((HumanoidModel<?>) (Object) this);
    }
}
