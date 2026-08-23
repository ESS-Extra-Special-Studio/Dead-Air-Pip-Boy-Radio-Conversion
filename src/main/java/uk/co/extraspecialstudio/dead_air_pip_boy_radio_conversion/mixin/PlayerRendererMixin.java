package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoySilhouetteRenderer;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipWristRender;

/** TACZ support/reload arm = {@code renderLeftHand} / {@code lefthand_pos}. */
@Mixin(PlayerRenderer.class)
public abstract class PlayerRendererMixin {

    /**
     * InventoryScreen doll: after vanilla ITEM poses from held Pip, force EMPTY so arms
     * hang while items / wrist Pip still render.
     */
    @Inject(method = "setModelProperties", at = @At("RETURN"))
    private void deadAirPip$neutralArmPoses(AbstractClientPlayer player, CallbackInfo ci) {
        if (!Boolean.TRUE.equals(PipBoySilhouetteRenderer.NEUTRAL_ARM_POSES.get())) {
            return;
        }
        PlayerModel<?> model = ((PlayerRenderer) (Object) this).getModel();
        model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
        model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
    }

    @Inject(method = "renderLeftHand", at = @At("RETURN"))
    private void deadAirPip$drawOnSupportArm(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        AbstractClientPlayer player,
        CallbackInfo ci
    ) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || player != mc.player) {
            return;
        }
        if (!(player instanceof LocalPlayer)) {
            return;
        }
        PipWristRender.tryDrawAfterFpArm(HumanoidArm.LEFT, poseStack, packedLight);
    }
}
