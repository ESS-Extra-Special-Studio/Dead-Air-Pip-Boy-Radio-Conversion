package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoySilhouetteRenderer;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipWristRender;

/**
 * Palm-space Pip is owned by {@code PipWristLayer}. During the STAT/LCD doll, hide every
 * held item that is not a Pip (guns clip badly on the CRT figure).
 */
@Mixin(ItemInHandLayer.class)
public abstract class ItemInHandLayerMixin {

    @Inject(method = "renderArmWithItem", at = @At("HEAD"), cancellable = true)
    private void deadAirPip$skipPalmPip(
        LivingEntity entity,
        ItemStack stack,
        ItemDisplayContext displayContext,
        HumanoidArm arm,
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        CallbackInfo ci
    ) {
        if (PipWristRender.isDrawing()) {
            return;
        }
        // STAT/LCD doll: wrist Pip only via PipWristLayer — never palm/hand items.
        if (Boolean.TRUE.equals(PipBoySilhouetteRenderer.DOLL_PIP_AND_ARMOR_ONLY.get())) {
            ci.cancel();
            return;
        }
        if (arm == HumanoidArm.LEFT && stack.getItem() instanceof PipRadioItem) {
            ci.cancel();
        }
    }
}
