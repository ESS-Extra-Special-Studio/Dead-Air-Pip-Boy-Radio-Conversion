package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.WalkAnimationState;
import net.minecraft.world.item.ItemStack;

import org.joml.Quaternionf;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;
import uk.co.extraspecialstudio.extraspecial.esc.ui.EscRect;

import org.jetbrains.annotations.Nullable;
import java.lang.reflect.Field;

/**
 * Phosphor-green walking player for Pip STAT GUI and idle wrist LCD.
 * <p>
 * STAT and LCD share the same InventoryScreen doll pose. Walk animation is applied
 * only for the draw call and then fully restored — it must not shuffle the real player.
 * LCD blit content-centers the opaque silhouette so the figure is not stuck to an edge.
 */
public final class PipBoySilhouetteRenderer {
    private static final int FBO_W = 128;
    private static final int FBO_H = 160;
    private static final int MIN_OPAQUE = 60;

    /**
     * Three-quarter facing screen-left (Fallout). Negative −4 over-rotated into
     * “looking away right”; positive turns the other way toward a left-facing walk.
     */
    private static final float PROFILE_ANGLE_X = 2.0f;
    private static final float PROFILE_ANGLE_Y = 0.08f;
    /** Independent display stride — not written into the live player permanently. */
    private static final float DISPLAY_WALK_SPEED = 0.42f;
    private static final float DISPLAY_WALK_STEP = 0.16f;
    private static final float STAT_SCALE_FRAC = 0.46f;

    /** Set while rendering the CRT/LCD doll so arms hang down but items (Pip) still render. */
    public static final ThreadLocal<Boolean> NEUTRAL_ARM_POSES = ThreadLocal.withInitial(() -> false);

    /**
     * While true, STAT/LCD doll should show armor + Pip only — hide guns and other held items
     * (TACZ also draws hotbar guns via {@code HumanoidOffhandRender}).
     */
    public static final ThreadLocal<Boolean> DOLL_PIP_AND_ARMOR_ONLY = ThreadLocal.withInitial(() -> false);

    /** How to flip the idle doll in logical LCD space before paint-rotate. */
    public enum LcdFlip {
        NONE,
        /** Horizontal flip in logical buffer. */
        MIRROR_X,
        /** Vertical flip in logical buffer (before paint-rotate). */
        MIRROR_Y
    }

    private static final Field WALK_SPEED = field(WalkAnimationState.class, "speed");
    private static final Field WALK_SPEED_OLD = field(WalkAnimationState.class, "speedOld");
    private static final Field WALK_POS = field(WalkAnimationState.class, "position");

    private static float displayWalkPhase;
    private static int lastCaptureTick = Integer.MIN_VALUE;
    @Nullable
    private static NativeImage lastSilhouette;
    @Nullable
    private static TextureTarget fbo;

    private PipBoySilhouetteRenderer() {}

    /** Walk phase for the CRT/LCD doll (driven each draw; used by HumanoidModelMixin). */
    public static float displayWalkPhase() {
        return displayWalkPhase;
    }

    /** Limb swing amount while {@link #NEUTRAL_ARM_POSES} is set. */
    public static float displayWalkAmount() {
        return DISPLAY_WALK_SPEED;
    }

    /**
     * Force a calm in-place walk: legs stride, arms hang with a tiny opposite swing,
     * head locked to torso (FollowsAngle uses a larger head yaw than body and looks away).
     */
    public static void applyCalmWalkLimbs(HumanoidModel<?> model) {
        float phase = displayWalkPhase;
        float amount = DISPLAY_WALK_SPEED;
        model.rightLeg.xRot = Mth.cos(phase * 0.6662F) * 1.4F * amount;
        model.leftLeg.xRot = Mth.cos(phase * 0.6662F + (float) Math.PI) * 1.4F * amount;
        model.rightLeg.yRot = 0f;
        model.leftLeg.yRot = 0f;
        model.rightLeg.zRot = 0f;
        model.leftLeg.zRot = 0f;

        // Tiny swing only — ITEM pose was ~horizontal; keep arms at sides.
        float arm = Mth.cos(phase * 0.6662F + (float) Math.PI) * amount * 0.15F;
        model.rightArm.xRot = arm;
        model.leftArm.xRot = -arm;
        model.rightArm.yRot = 0f;
        model.leftArm.yRot = 0f;
        model.rightArm.zRot = 0f;
        model.leftArm.zRot = 0f;

        // Line head up with torso (slight left-facing body); no looking into the CRT void.
        model.head.xRot = 0f;
        model.head.yRot = 0f;
        model.head.zRot = 0f;
        model.hat.copyFrom(model.head);

        if (model instanceof PlayerModel<?> playerModel) {
            playerModel.leftSleeve.copyFrom(model.leftArm);
            playerModel.rightSleeve.copyFrom(model.rightArm);
            playerModel.leftPants.copyFrom(model.leftLeg);
            playerModel.rightPants.copyFrom(model.rightLeg);
        }
    }

    /** Large centered walking player in the STAT content rect (same doll LCD uses). */
    public static void renderInGui(GuiGraphics graphics, EscRect rect, float partialTick) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || rect.width() < 16 || rect.height() < 16) {
            return;
        }

        int availW = Math.max(16, rect.width() - 16);
        int availH = Math.max(16, rect.height() - 16);
        int scale = Math.max(40, (int) (Math.min(availW, availH) * STAT_SCALE_FRAC));
        int cx = rect.x() + rect.width() / 2;
        int cy = rect.y() + (int) (rect.height() * 0.88f);

        graphics.enableScissor(rect.x(), rect.y(), rect.right(), rect.bottom());
        try {
            withDisplayPose(player, () -> {
                RenderSystem.setShaderColor(0.08f, 1.0f, 0.32f, 1.0f);
                renderDoll(graphics, cx, cy, scale, player);
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            });
        } finally {
            graphics.disableScissor();
        }

        // Same pose → LCD cache (GUI ortho is valid here).
        maybeCaptureForLcd(player);
    }

    /** GUI-pass capture while Pip is held (valid ortho matrices). */
    public static void captureFromGuiPass() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        maybeCaptureForLcd(player);
    }

    public static void tickCapture() {
        // no-op — bare ticks use world matrices and break LCD capture
    }

    /**
     * Paint the STAT doll (scaled + centered) into the LCD buffer.
     * @param flip orientation fix for empty vs wrist paint-rotate paths
     */
    public static boolean paintLcd(NativeImage buf, int lw, int lh, long nowMs, LcdFlip flip) {
        if (lw < 4 || lh < 4) {
            return true;
        }
        if (lastSilhouette != null) {
            blitSilhouetteCentered(buf, lw, lh, flip);
        }
        return true;
    }

    /** @deprecated use {@link #paintLcd(NativeImage, int, int, long, LcdFlip)} */
    public static boolean paintLcd(NativeImage buf, int lw, int lh, long nowMs) {
        return paintLcd(buf, lw, lh, nowMs, LcdFlip.NONE);
    }

    /** @deprecated use {@link #paintLcd(NativeImage, int, int, long, LcdFlip)} */
    public static boolean paintLcd(NativeImage buf, int lw, int lh, long nowMs, boolean mirrorX) {
        return paintLcd(buf, lw, lh, nowMs, mirrorX ? LcdFlip.MIRROR_X : LcdFlip.NONE);
    }

    /**
     * Scale the opaque silhouette to fit the LCD and center it — same idea as STAT,
     * just smaller and without chrome.
     */
    private static void blitSilhouetteCentered(NativeImage buf, int lw, int lh, LcdFlip flip) {
        int x0 = FBO_W, y0 = FBO_H, x1 = -1, y1 = -1;
        for (int y = 0; y < FBO_H; y++) {
            for (int x = 0; x < FBO_W; x++) {
                if (((lastSilhouette.getPixelRGBA(x, y) >>> 24) & 0xFF) > 20) {
                    if (x < x0) x0 = x;
                    if (y < y0) y0 = y;
                    if (x > x1) x1 = x;
                    if (y > y1) y1 = y;
                }
            }
        }
        if (x1 < x0 || y1 < y0) {
            return;
        }
        int srcW = x1 - x0 + 1;
        int srcH = y1 - y0 + 1;
        float pad = 0.90f;
        float scale = Math.min(lw / (float) srcW, lh / (float) srcH) * pad;
        int drawW = Math.max(1, Math.round(srcW * scale));
        int drawH = Math.max(1, Math.round(srcH * scale));
        int ox = (lw - drawW) / 2;
        int oy = (lh - drawH) / 2;

        for (int y = 0; y < drawH; y++) {
            int srcY = Math.min(srcH - 1, (y * srcH) / drawH);
            int sy = flip == LcdFlip.MIRROR_Y ? (y1 - srcY) : (y0 + srcY);
            for (int x = 0; x < drawW; x++) {
                int srcX = Math.min(srcW - 1, (x * srcW) / drawW);
                int sx = flip == LcdFlip.MIRROR_X ? (x1 - srcX) : (x0 + srcX);
                int src = lastSilhouette.getPixelRGBA(sx, sy);
                int a = (src >>> 24) & 0xFF;
                if (a < 20) {
                    continue;
                }
                int dx = ox + x;
                int dy = oy + y;
                if (dx < 0 || dy < 0 || dx >= lw || dy >= lh) {
                    continue;
                }
                buf.setPixelRGBA(dx, dy, src);
            }
        }
    }

    private static void maybeCaptureForLcd(LocalPlayer player) {
        if (player.tickCount == lastCaptureTick && lastSilhouette != null) {
            return;
        }
        lastCaptureTick = player.tickCount;
        captureForLcd(player);
    }

    private static void captureForLcd(LocalPlayer player) {
        Minecraft mc = Minecraft.getInstance();
        try {
            if (fbo == null || fbo.width != FBO_W || fbo.height != FBO_H) {
                if (fbo != null) {
                    fbo.destroyBuffers();
                }
                fbo = new TextureTarget(FBO_W, FBO_H, true, Minecraft.ON_OSX);
            }
            fbo.setClearColor(0f, 0f, 0f, 0f);
            fbo.clear(Minecraft.ON_OSX);
            fbo.bindWrite(true);
            RenderSystem.viewport(0, 0, FBO_W, FBO_H);

            // Same framing language as STAT: centered X, feet near bottom, profile angles.
            int scale = (int) (FBO_H * STAT_SCALE_FRAC);
            int cx = FBO_W / 2;
            int cy = (int) (FBO_H * 0.88f);

            withDisplayPose(player, () -> {
                RenderSystem.setShaderColor(0.08f, 1.0f, 0.32f, 1.0f);
                GuiGraphics gg = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
                renderDoll(gg, cx, cy, scale, player);
                gg.flush();
                RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
            });

            fbo.unbindWrite();
            mc.getMainRenderTarget().bindWrite(true);

            NativeImage shot = Screenshot.takeScreenshot(fbo);
            shot.flipY();
            // Keep green-tinted skin detail (same look as STAT) — do not flatten to a solid blob.
            NativeImage sil = toPhosphorDetail(shot);
            shot.close();

            if (countOpaque(sil) < MIN_OPAQUE) {
                sil.close();
                return;
            }
            if (lastSilhouette != null) {
                lastSilhouette.close();
            }
            lastSilhouette = sil;
        } catch (Throwable t) {
            Dead_air_pip_boy_radio_conversion.LOGGER.debug(
                "Pip LCD silhouette capture failed: {}", t.toString());
            try {
                mc.getMainRenderTarget().bindWrite(true);
            } catch (Exception ignored) {
            }
        } finally {
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        }
    }

    /**
     * Inventory doll with head locked to torso. Vanilla
     * {@link InventoryScreen#renderEntityInInventoryFollowsAngle} sets
     * {@code yHeadRot = 180 + angle*40} but {@code yBodyRot = 180 + angle*20}, so the head
     * turns farther than the body and looks into the CRT background.
     */
    private static void renderDoll(GuiGraphics graphics, int x, int y, int scale, LivingEntity entity) {
        float bodyYaw = 180.0f + PROFILE_ANGLE_X * 20.0f;
        float pitch = -PROFILE_ANGLE_Y * 20.0f;

        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera = Axis.XP.rotationDegrees(pitch);
        pose.mul(camera);

        float oldBody = entity.yBodyRot;
        float oldY = entity.getYRot();
        float oldX = entity.getXRot();
        float oldHead = entity.yHeadRot;
        float oldHeadO = entity.yHeadRotO;
        try {
            entity.yBodyRot = bodyYaw;
            entity.setYRot(bodyYaw);
            entity.setXRot(pitch);
            entity.yHeadRot = bodyYaw;
            entity.yHeadRotO = bodyYaw;
            InventoryScreen.renderEntityInInventory(
                graphics, (float) x, (float) y, (float) scale,
                new org.joml.Vector3f(), pose, camera, entity
            );
        } finally {
            entity.yBodyRot = oldBody;
            entity.setYRot(oldY);
            entity.setXRot(oldX);
            entity.yHeadRot = oldHead;
            entity.yHeadRotO = oldHeadO;
        }
    }

    /**
     * CRT/LCD doll pose: calm walk, level head, arms-down via {@link #NEUTRAL_ARM_POSES}.
     * Hands keep only {@link PipRadioItem} (armor stays); other held items / TACZ hotbar guns
     * are hidden for the draw.
     */
    private static void withDisplayPose(LocalPlayer player, Runnable draw) {
        WalkAnimationState anim = player.walkAnimation;
        float oldSpeed = anim.speed();
        float oldPos = anim.position();
        float oldSpeedOld = getFloat(WALK_SPEED_OLD, anim, oldSpeed);

        float xRot = player.getXRot();
        float xRotO = player.xRotO;
        float attack = player.attackAnim;
        float attackO = player.oAttackAnim;
        boolean swinging = player.swinging;
        int swingTime = player.swingTime;
        boolean wasUsing = player.isUsingItem();
        ItemStack mainHand = player.getMainHandItem().copy();
        ItemStack offHand = player.getOffhandItem().copy();
        boolean prevNeutral = NEUTRAL_ARM_POSES.get();
        boolean prevFilter = DOLL_PIP_AND_ARMOR_ONLY.get();

        try {
            displayWalkPhase += DISPLAY_WALK_STEP;
            anim.setSpeed(DISPLAY_WALK_SPEED);
            setFloat(WALK_SPEED, anim, DISPLAY_WALK_SPEED);
            setFloat(WALK_SPEED_OLD, anim, DISPLAY_WALK_SPEED);
            setFloat(WALK_POS, anim, displayWalkPhase);

            NEUTRAL_ARM_POSES.set(true);
            DOLL_PIP_AND_ARMOR_ONLY.set(true);
            if (wasUsing) {
                player.stopUsingItem();
            }
            // Armor untouched. Off-hand Pip stays for wrist layer only; never leave a Pip in main hand
            // (that doubles up with the wrist draw and looks like the doll is "holding" one).
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (!(offHand.getItem() instanceof PipRadioItem)) {
                player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, ItemStack.EMPTY);
            }
            player.setXRot(0f);
            player.xRotO = 0f;
            player.attackAnim = 0f;
            player.oAttackAnim = 0f;
            player.swinging = false;
            player.swingTime = 0;

            draw.run();
        } finally {
            NEUTRAL_ARM_POSES.set(prevNeutral);
            DOLL_PIP_AND_ARMOR_ONLY.set(prevFilter);
            player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, mainHand);
            player.setItemInHand(net.minecraft.world.InteractionHand.OFF_HAND, offHand);
            player.setXRot(xRot);
            player.xRotO = xRotO;
            player.attackAnim = attack;
            player.oAttackAnim = attackO;
            player.swinging = swinging;
            player.swingTime = swingTime;
            setFloat(WALK_SPEED, anim, oldSpeed);
            setFloat(WALK_SPEED_OLD, anim, oldSpeedOld);
            setFloat(WALK_POS, anim, oldPos);
            anim.setSpeed(oldSpeed);
        }
    }

    private static Field field(Class<?> owner, String name) {
        try {
            Field f = owner.getDeclaredField(name);
            f.setAccessible(true);
            return f;
        } catch (Exception e) {
            Dead_air_pip_boy_radio_conversion.LOGGER.warn(
                "Could not reflect WalkAnimationState.{} — display walk restore may fail: {}", name, e.toString());
            return null;
        }
    }

    private static float getFloat(Field f, Object o, float fallback) {
        if (f == null) {
            return fallback;
        }
        try {
            return f.getFloat(o);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static void setFloat(Field f, Object o, float v) {
        if (f == null) {
            return;
        }
        try {
            f.setFloat(o, v);
        } catch (Exception ignored) {
        }
    }

    private static int countOpaque(NativeImage img) {
        int n = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                if (((img.getPixelRGBA(x, y) >>> 24) & 0xFF) > 20) {
                    n++;
                }
            }
        }
        return n;
    }

    /**
     * Convert the green-tinted InventoryScreen capture into LCD pixels while keeping
     * luminance detail (hoodie / gear readable). Flat flood-fill made every skin identical.
     */
    private static NativeImage toPhosphorDetail(NativeImage src) {
        NativeImage out = new NativeImage(src.getWidth(), src.getHeight(), true);
        for (int y = 0; y < src.getHeight(); y++) {
            for (int x = 0; x < src.getWidth(); x++) {
                int p = src.getPixelRGBA(x, y);
                int a = (p >>> 24) & 0xFF;
                int r = p & 0xFF;
                int g = (p >> 8) & 0xFF;
                int b = (p >> 16) & 0xFF;
                int lum = (r + g * 2 + b) / 4;
                if (a < 8 || lum < 12) {
                    out.setPixelRGBA(x, y, 0);
                    continue;
                }
                // Boost toward phosphor green but keep relative brightness for shape/skin.
                int pg = Math.min(255, 40 + (int) (lum * 1.15f));
                int pr = Math.min(255, pg / 5);
                int pb = Math.min(255, pg / 3);
                int pa = Math.min(255, 160 + lum / 2);
                out.setPixelRGBA(x, y, abgr(pa, pb, pg, pr));
            }
        }
        return out;
    }

    private static int abgr(int a, int b, int g, int r) {
        return (a & 0xFF) << 24 | (b & 0xFF) << 16 | (g & 0xFF) << 8 | (r & 0xFF);
    }
}
