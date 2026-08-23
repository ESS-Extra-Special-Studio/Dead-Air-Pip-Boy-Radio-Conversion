package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block.PipRadioDecorBlock;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block.PipRadioDecorBlockEntity;

/**
 * Desk Pip: blank CRT (no live LCD), screen toward {@link PipRadioDecorBlock#FACING} (toward placer).
 */
public final class PipRadioDecorRenderer implements BlockEntityRenderer<PipRadioDecorBlockEntity> {
    public PipRadioDecorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
        PipRadioDecorBlockEntity be,
        float partialTick,
        PoseStack pose,
        MultiBufferSource buffer,
        int packedLight,
        int packedOverlay
    ) {
        ItemStack stack = be.getStored();
        if (stack.isEmpty()) {
            return;
        }
        Direction facing = be.getBlockState().getValue(PipRadioDecorBlock.FACING);

        pose.pushPose();
        pose.translate(0.5, 0.20, 0.5);
        // Screen (geo −X) toward placer; XP −90 lays the wrist mesh flat on the ground.
        pose.mulPose(Axis.YP.rotationDegrees(-facing.toYRot() + 90f));
        pose.mulPose(Axis.XP.rotationDegrees(-90f));
        pose.scale(1.15f, 1.15f, 1.15f);

        PipRadioItemRenderer.beginDecorMode();
        try {
            // FIXED worked when the model was visible; NONE + wrist-centering made it vanish.
            Minecraft.getInstance().getItemRenderer().renderStatic(
                stack,
                ItemDisplayContext.FIXED,
                packedLight,
                packedOverlay,
                pose,
                buffer,
                be.getLevel(),
                0
            );
        } finally {
            PipRadioItemRenderer.endDecorMode();
        }
        pose.popPose();
    }
}
