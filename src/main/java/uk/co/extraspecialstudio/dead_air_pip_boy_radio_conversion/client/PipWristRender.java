package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import software.bernie.geckolib.animatable.client.GeoRenderProvider;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;

/**
 * TP: anatomical {@code leftArm}. Gun FP: TACZ {@code lefthand_pos} / LEFT (support+reload).
 * All POI transforms LOCKED perfect 2026-08-09 — see TRANSFORM_LOG.md (ESA canonical).
 */
@EventBusSubscriber(modid = Dead_air_pip_boy_radio_conversion.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class PipWristRender {
    private static final ThreadLocal<Boolean> DRAWING = ThreadLocal.withInitial(() -> false);

    private static final HumanoidArm GUN_FP_SUPPORT_ARM = HumanoidArm.LEFT;

    /**
     * TP — position LOCKED 2026-08-09; orientation corrected 2026-08-09:
     * model was upside-down, not the LCD. XP 180 = top↔bottom (not YP left↔right).
     * Axes after leftArm.translateAndRotate:
     * X+ outward; Y+ along arm toward hand; Z− forward / Z+ backward.
     */
    private static final float TP_SCALE = 1.02f;
    private static final float TP_ALONG_ARM = 0.34f;
    /** Tiny inward toward body from 0.045 (player ask 2026-08-09). */
    private static final float TP_OUTWARD = 0.030f;
    private static final float TP_THICKNESS = 0.010f;
    private static final float TP_WRIST_ROLL_DEG = 3.5f;
    /** Top-down flip — corrects upside-down cuff mount (XP). */
    private static final float TP_FLIP_TOP_DOWN_DEG = 180f;
    /** Left↔right spin after top-down (YP). */
    private static final float TP_FLIP_LEFT_RIGHT_DEG = 180f;

    /**
     * Gun FP — LOCKED perfect 2026-08-09 (player-confirmed). Do not nudge without explicit ask.
     * LCD paint is separate from TP (gun-FP liveId) — ZP 180 flips the atlas.
     * +OUTWARD = screen-left; +ALONG = toward hand; −ALONG = toward elbow;
     * +THICKNESS = down toward floor; −THICKNESS = up toward ceiling.
     * YP/XP 180 wrong; ZP 180 after translate.
     */
    private static final float GUN_FP_SCALE = 0.80f;
    private static final float GUN_FP_ALONG_ARM = 0.55f;
    private static final float GUN_FP_OUTWARD = 0.40f;
    private static final float GUN_FP_THICKNESS = -0.01f;
    private static final float GUN_FP_FLIP_DEG = 180f;

    private static boolean drawnThisFrame;

    private PipWristRender() {}

    public static boolean isDrawing() {
        return Boolean.TRUE.equals(DRAWING.get());
    }

    @SubscribeEvent
    public static void onRenderFrame(RenderFrameEvent.Pre event) {
        drawnThisFrame = false;
    }

    public static void drawOnLeftArm(
        PoseStack pose, MultiBufferSource buffer, int packedLight, ItemStack stack
    ) {
        draw(pose, buffer, packedLight, stack, false);
    }

    public static void tryDrawAfterFpArm(HumanoidArm arm, PoseStack pose, int packedLight) {
        if (arm != GUN_FP_SUPPORT_ARM) {
            return;
        }
        if (isDrawing() || drawnThisFrame) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || player.isInvisible()) {
            return;
        }
        ItemStack off = player.getOffhandItem();
        if (!(off.getItem() instanceof PipRadioItem)) {
            return;
        }
        if (!PipOffhandArmHandler.isGunModItem(player.getMainHandItem())) {
            return;
        }

        MultiBufferSource buffer = mc.renderBuffers().bufferSource();
        draw(pose, buffer, packedLight, off, true);
        if (buffer instanceof MultiBufferSource.BufferSource source) {
            source.endBatch();
        }
        drawnThisFrame = true;
    }

    private static void draw(
        PoseStack pose,
        MultiBufferSource buffer,
        int packedLight,
        ItemStack stack,
        boolean gunFp
    ) {
        if (stack.isEmpty() || !(stack.getItem() instanceof PipRadioItem)) {
            return;
        }
        if (isDrawing()) {
            return;
        }

        BlockEntityWithoutLevelRenderer custom = GeoRenderProvider.of(stack).getGeoItemRenderer();
        if (!(custom instanceof PipRadioItemRenderer renderer)) {
            return;
        }

        DRAWING.set(true);
        PipRadioItemRenderer.beginWristMode(gunFp);
        try {
            pose.pushPose();
            if (gunFp) {
                applyGunFpOffset(pose);
                applyRaiseIfOpen(pose);
            } else {
                applyTpOffset(pose);
            }
            renderer.renderByItem(
                stack,
                ItemDisplayContext.NONE,
                pose,
                buffer,
                packedLight,
                OverlayTexture.NO_OVERLAY
            );
            pose.popPose();
            if (!gunFp) {
                if (buffer instanceof MultiBufferSource.BufferSource source) {
                    source.endBatch();
                } else {
                    Minecraft.getInstance().renderBuffers().bufferSource().endBatch();
                }
            }
        } finally {
            PipRadioItemRenderer.endWristMode();
            DRAWING.set(false);
        }
    }

    private static void applyTpOffset(PoseStack pose) {
        pose.translate(TP_OUTWARD, TP_ALONG_ARM, TP_THICKNESS);
        pose.mulPose(Axis.XP.rotationDegrees(TP_FLIP_TOP_DOWN_DEG));
        pose.mulPose(Axis.YP.rotationDegrees(TP_FLIP_LEFT_RIGHT_DEG));
        pose.mulPose(Axis.YP.rotationDegrees(TP_WRIST_ROLL_DEG));
        pose.scale(TP_SCALE, TP_SCALE, TP_SCALE);
    }

    private static void applyGunFpOffset(PoseStack pose) {
        pose.translate(GUN_FP_OUTWARD, GUN_FP_ALONG_ARM, GUN_FP_THICKNESS);
        // YP and XP 180 were wrong axes; ZP flips the wrist-backwards case.
        pose.mulPose(Axis.ZP.rotationDegrees(GUN_FP_FLIP_DEG));
        pose.scale(GUN_FP_SCALE, GUN_FP_SCALE, GUN_FP_SCALE);
    }

    private static void applyRaiseIfOpen(PoseStack pose) {
        if (!(Minecraft.getInstance().screen instanceof PipBoyGui)) {
            return;
        }
        float p = PipBoyRaise.raiseProgress();
        if (p <= 0.001f) {
            return;
        }
        float s = p * p * (3f - 2f * p);
        pose.translate(Mth.lerp(s, 0f, 0.15f), Mth.lerp(s, 0f, -0.08f), Mth.lerp(s, 0f, -0.35f));
        pose.mulPose(Axis.XP.rotationDegrees(Mth.lerp(s, 0f, -55f)));
        pose.mulPose(Axis.YP.rotationDegrees(Mth.lerp(s, 0f, 25f)));
        pose.scale(Mth.lerp(s, 1f, 1.15f), Mth.lerp(s, 1f, 1.15f), Mth.lerp(s, 1f, 1.15f));
    }
}
