package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.client.event.RenderHandEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.fml.common.EventBusSubscriber;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;

/**
 * First-person Pip:
 * <ul>
 *   <li>No gun — inject left arm under the off-hand Pip; vanilla item draw + locked display JSON.</li>
 *   <li>Gun — cancel off-hand item draw; Pip parents to TACZ support/reload arm via arm mixins.</li>
 *   <li>Walk bob — undo vanilla ItemInHandRenderer hand bob for the off-hand only while Pip is worn
 *       (right hand keeps normal sway).</li>
 * </ul>
 */
@EventBusSubscriber(modid = Dead_air_pip_boy_radio_conversion.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
public final class PipOffhandArmHandler {
    private PipOffhandArmHandler() {}

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onRenderHand(RenderHandEvent event) {
        if (event.getHand() != InteractionHand.OFF_HAND) {
            return;
        }
        if (!(event.getItemStack().getItem() instanceof PipRadioItem)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || player.isInvisible()) {
            return;
        }

        // Stabilize left arm + Pip against vanilla walk bob (shared stack before both hands).
        undoWalkBob(event.getPoseStack(), player, event.getPartialTick());

        if (isGunModItem(player.getMainHandItem())) {
            // Item path off; 3D Pip comes from support-arm mixin instead.
            event.setCanceled(true);
            return;
        }

        PoseStack pose = event.getPoseStack();
        pose.pushPose();
        applyEmptyArmPose(pose, HumanoidArm.LEFT, event.getEquipProgress(), event.getSwingProgress());
        renderLeftArm(pose, event.getMultiBufferSource(), event.getPackedLight(), player);
        pose.popPose();
        // Leave event uncanceled so the locked FIRSTPERSON_LEFTHAND Pip still draws.
    }

    /**
     * Inverse of the walk-bob applied in {@code ItemInHandRenderer.renderHandsWithItems}
     * (translate → ZP → XP). Call only for the off-hand Pip path.
     */
    private static void undoWalkBob(PoseStack pose, LocalPlayer player, float partialTick) {
        float distDelta = player.walkDist - player.walkDistO;
        float bobPhase = -(player.walkDist + distDelta * partialTick);
        float bob = Mth.lerp(partialTick, player.oBob, player.bob);
        float sin = Mth.sin(bobPhase * (float) Math.PI);
        float cos = Mth.cos(bobPhase * (float) Math.PI);
        float xpDeg = Math.abs(Mth.cos(bobPhase * (float) Math.PI - 0.2f) * bob) * 5.0f;
        float zpDeg = sin * bob * 3.0f;
        float tx = sin * bob * 0.5f;
        float ty = -Math.abs(cos * bob);

        pose.mulPose(Axis.XP.rotationDegrees(-xpDeg));
        pose.mulPose(Axis.ZP.rotationDegrees(-zpDeg));
        pose.translate(-tx, -ty, 0.0f);
    }

    public static boolean isGunModItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (id != null) {
            String ns = id.getNamespace();
            if (ns.equals("tacz") || ns.equals("cgm") || ns.equals("tac")) {
                return true;
            }
        }
        // Soft TACZ API — gun-pack items still implement IGun under the tacz item class.
        try {
            Class<?> iGun = Class.forName("com.tacz.guns.api.item.IGun");
            if (iGun.isInstance(stack.getItem())) {
                return true;
            }
            Object asGun = iGun.getMethod("getIGunOrNull", ItemStack.class).invoke(null, stack);
            return asGun != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    private static void applyEmptyArmPose(PoseStack pose, HumanoidArm side, float equippedProgress, float swingProgress) {
        boolean right = side != HumanoidArm.LEFT;
        float sign = right ? 1.0f : -1.0f;
        float swingSqrt = Mth.sqrt(swingProgress);
        float swingA = -0.3f * Mth.sin(swingSqrt * (float) Math.PI);
        float swingB = 0.4f * Mth.sin(swingSqrt * ((float) Math.PI * 2f));
        float swingC = -0.4f * Mth.sin(swingProgress * (float) Math.PI);

        pose.translate(
            sign * (swingA + 0.64000005f),
            swingB + -0.6f + equippedProgress * -0.6f,
            swingC + -0.71999997f
        );
        pose.mulPose(Axis.YP.rotationDegrees(sign * 45.0f));
        float swingD = Mth.sin(swingProgress * swingProgress * (float) Math.PI);
        float swingE = Mth.sin(swingSqrt * (float) Math.PI);
        pose.mulPose(Axis.YP.rotationDegrees(sign * swingE * 70.0f));
        pose.mulPose(Axis.ZP.rotationDegrees(sign * swingD * -20.0f));
        pose.translate(sign * -1.0f, 3.6f, 3.5f);
        pose.mulPose(Axis.ZP.rotationDegrees(sign * 120.0f));
        pose.mulPose(Axis.XP.rotationDegrees(200.0f));
        pose.mulPose(Axis.YP.rotationDegrees(sign * -135.0f));
        pose.translate(sign * 5.6f, 0.0f, 0.0f);
    }

    private static void renderLeftArm(
        PoseStack pose, MultiBufferSource buffer, int light, AbstractClientPlayer player
    ) {
        RenderSystem.setShaderTexture(0, player.getSkin().texture());
        PlayerRenderer renderer = (PlayerRenderer) Minecraft.getInstance()
            .getEntityRenderDispatcher()
            .getRenderer(player);
        renderer.renderLeftHand(pose, buffer, light, player);
    }
}
