package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.renderer.other.HumanoidOffhandRender;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoySilhouetteRenderer;

/**
 * TACZ draws hotbar guns on the player model even when the selected slot is empty.
 * Suppress that on the Pip STAT/LCD doll so only armor + Pip remain.
 */
@Mixin(HumanoidOffhandRender.class)
public abstract class TaczHumanoidOffhandRenderMixin {

    @Inject(method = "renderGun", at = @At("HEAD"), cancellable = true, remap = false)
    private static void deadAirPip$hideGunsOnDoll(
        LivingEntity entity,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        CallbackInfo ci
    ) {
        if (Boolean.TRUE.equals(PipBoySilhouetteRenderer.DOLL_PIP_AND_ARMOR_ONLY.get())) {
            ci.cancel();
        }
    }
}
