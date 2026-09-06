package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipWristRender;

/**
 * TACZ draws FP arms through here after capturing {@code lefthand_pos} / {@code righthand_pos}.
 * Soft target — no TACZ compile dependency (plugin disables when mod absent).
 */
@Mixin(targets = "com.tacz.guns.util.RenderHelper", remap = false)
public abstract class TaczRenderHelperMixin {

    @Inject(method = "renderFirstPersonArm", at = @At("RETURN"), remap = false)
    private static void deadAirPip$afterTaczArm(
        LocalPlayer player,
        HumanoidArm hand,
        PoseStack matrixStack,
        int combinedLight,
        CallbackInfo ci
    ) {
        PipWristRender.tryDrawAfterFpArm(hand, matrixStack, combinedLight);
    }
}
