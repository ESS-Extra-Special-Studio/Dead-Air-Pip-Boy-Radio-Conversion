package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;

/**
 * When any {@link PipBoyGui} is open, raise the off-hand Pip toward the camera (empty-hand FP path).
 * Gun FP raise is handled inside {@link PipWristRender}.
 */
@EventBusSubscriber(modid = Dead_air_pip_boy_radio_conversion.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class PipRaisePoseHandler {
    private PipRaisePoseHandler() {}

    @SubscribeEvent
    public static void onRenderHand(RenderHandEvent event) {
        if (!(Minecraft.getInstance().screen instanceof PipBoyGui)) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof PipRadioItem)) {
            return;
        }
        if (event.isCanceled()) {
            return;
        }
        float p = PipBoyRaise.raiseProgress();
        if (p <= 0.001f) {
            return;
        }
        float s = p * p * (3f - 2f * p);
        PoseStack pose = event.getPoseStack();
        boolean left = event.getHand() == InteractionHand.OFF_HAND;

        if (left) {
            pose.translate(Mth.lerp(s, 0f, 0.42f), Mth.lerp(s, 0f, 0.32f), Mth.lerp(s, 0f, -0.5f));
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, 0f, 50f)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, 0f, -40f)));
            pose.mulPose(Axis.ZP.rotationDegrees(Mth.lerp(s, 0f, -12f)));
            pose.scale(Mth.lerp(s, 1f, 1.2f), Mth.lerp(s, 1f, 1.2f), Mth.lerp(s, 1f, 1.2f));
        } else {
            pose.translate(Mth.lerp(s, 0f, -0.35f), Mth.lerp(s, 0f, 0.28f), Mth.lerp(s, 0f, -0.45f));
            pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, 0f, -55f)));
            pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, 0f, -35f)));
            pose.scale(Mth.lerp(s, 1f, 1.15f), Mth.lerp(s, 1f, 1.15f), Mth.lerp(s, 1f, 1.15f));
        }
    }
}
