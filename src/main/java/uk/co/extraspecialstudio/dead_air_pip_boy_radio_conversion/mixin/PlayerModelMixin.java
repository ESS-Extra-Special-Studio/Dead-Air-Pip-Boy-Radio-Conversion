package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.mixin;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoySilhouetteRenderer;

/**
 * PlayerModel.setupAnim runs after {@link net.minecraft.client.model.HumanoidModel#setupAnim}
 * and can leave sleeve/arm state matching ITEM (off-hand Pip). Force calm walk here too,
 * then copy sleeves from arms.
 */
@Mixin(PlayerModel.class)
public abstract class PlayerModelMixin {

    @Inject(
        method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
        at = @At("RETURN"),
        require = 1
    )
    private void deadAirPip$calmWalkSleeves(
        LivingEntity entity,
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
        PipBoySilhouetteRenderer.applyCalmWalkLimbs((PlayerModel<?>) (Object) this);
    }
}
