package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.world.item.ItemStack;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;

/**
 * Third-person Pip cuff parented to {@code leftArm} (same arm-local offsets as FP).
 */
public final class PipWristLayer
    extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {

    public PipWristLayer(
        RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent
    ) {
        super(parent);
    }

    @Override
    public void render(
        PoseStack poseStack,
        MultiBufferSource buffer,
        int packedLight,
        AbstractClientPlayer player,
        float limbSwing,
        float limbSwingAmount,
        float partialTick,
        float ageInTicks,
        float netHeadYaw,
        float headPitch
    ) {
        ItemStack off = player.getOffhandItem();
        if (!(off.getItem() instanceof PipRadioItem)) {
            return;
        }
        if (player.isInvisible()) {
            return;
        }

        poseStack.pushPose();
        this.getParentModel().leftArm.translateAndRotate(poseStack);
        PipWristRender.drawOnLeftArm(poseStack, buffer, packedLight, off);
        poseStack.popPose();
    }
}
