package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipMods;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.compat.PipDeadAirLcd;

/**
 * Geo renderer + live on-model LCD.
 * Idle mini character GUI is always {@link PipLiveDisplay} (Pip-owned).
 * Dead Air only supplies the radio HUD texture while music is actually playing.
 */
public class PipRadioItemRenderer extends GeoItemRenderer<PipRadioItem> {
    private static final ThreadLocal<Boolean> WRIST_MODE = ThreadLocal.withInitial(() -> false);
    /** When {@link #WRIST_MODE}: true = gun FP LCD, false = TP LCD. */
    private static final ThreadLocal<Boolean> GUN_FP_LCD = ThreadLocal.withInitial(() -> false);
    /** Placed decor block — base atlas only (blank CRT, no live HUD). */
    private static final ThreadLocal<Boolean> DECOR_MODE = ThreadLocal.withInitial(() -> false);

    private static final float FP_LEFT_SLEEVE_ROLL_DEG = -90f;
    private static final float FP_LEFT_SCREEN_PLANE_FLIP_DEG = 180f;
    private static final float FP_LEFT_HAND_PEEK_Y = 0.12f;

    public PipRadioItemRenderer() {
        super(new PipRadioGeoModel());
    }

    static void beginWristMode(boolean gunFp) {
        WRIST_MODE.set(true);
        GUN_FP_LCD.set(gunFp);
    }

    static void endWristMode() {
        WRIST_MODE.set(false);
        GUN_FP_LCD.set(false);
    }

    static void beginDecorMode() {
        DECOR_MODE.set(true);
    }

    static void endDecorMode() {
        DECOR_MODE.set(false);
    }

    @Override
    public ResourceLocation getTextureLocation(PipRadioItem animatable) {
        if (Boolean.TRUE.equals(DECOR_MODE.get())) {
            return animatable != null
                ? animatable.getGeoTextureResource()
                : super.getTextureLocation(animatable);
        }
        if (animatable != null) {
            boolean wrist = Boolean.TRUE.equals(WRIST_MODE.get());
            boolean gunFp = Boolean.TRUE.equals(GUN_FP_LCD.get());
            ItemStack stack = getCurrentItemStack();
            if (PipMods.deadAirLoaded()) {
                ResourceLocation radio = PipDeadAirLcd.resolveRadioHudIfListening(
                    animatable, stack, wrist, gunFp);
                if (radio != null) {
                    return radio;
                }
            }
            ResourceLocation live = PipLiveDisplay.resolveTexture(animatable, stack, wrist, gunFp);
            if (live != null) {
                return live;
            }
        }
        return animatable != null
            ? animatable.getGeoTextureResource()
            : super.getTextureLocation(animatable);
    }

    @Override
    public void preRender(
        PoseStack poseStack,
        PipRadioItem animatable,
        BakedGeoModel model,
        MultiBufferSource bufferSource,
        VertexConsumer buffer,
        boolean isReRender,
        float partialTick,
        int packedLight,
        int packedOverlay,
        float red,
        float green,
        float blue,
        float alpha
    ) {
        super.preRender(
            poseStack, animatable, model, bufferSource, buffer,
            isReRender, partialTick, packedLight, packedOverlay, red, green, blue, alpha
        );
        if (Boolean.TRUE.equals(DECOR_MODE.get())) {
            return;
        }
        if (Boolean.TRUE.equals(WRIST_MODE.get())) {
            poseStack.translate(-0.5f, -0.51f, -0.5f);
            return;
        }
        applyLockedFpOrientation(poseStack);
    }

    private void applyLockedFpOrientation(PoseStack poseStack) {
        ItemDisplayContext ctx = this.renderPerspective;
        if (ctx == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {
            poseStack.mulPose(Axis.YP.rotationDegrees(FP_LEFT_SLEEVE_ROLL_DEG));
            poseStack.mulPose(Axis.ZP.rotationDegrees(FP_LEFT_SCREEN_PLANE_FLIP_DEG));
            poseStack.translate(0f, FP_LEFT_HAND_PEEK_Y, 0f);
        }
    }
}
