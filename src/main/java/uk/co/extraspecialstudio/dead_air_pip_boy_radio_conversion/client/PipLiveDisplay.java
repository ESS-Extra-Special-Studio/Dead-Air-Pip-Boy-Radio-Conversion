package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioSkin;

import javax.annotation.Nullable;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Pip-owned on-model CRT: the Fallout mini character GUI (STAT silhouette).
 * Formerly painted only via Dead Air {@code RadioLiveDisplay} idle painters — that path is gone.
 * Dead Air may still paint radio HUD while music plays ({@code PipDeadAirLcd}).
 */
public final class PipLiveDisplay {
    public record LcdRect(int x0, int y0, int x1, int y1) {
        public int width() {
            return x1 - x0;
        }

        public int height() {
            return y1 - y0;
        }
    }

    public record Spec(
        ResourceLocation baseTexture,
        ResourceLocation liveId,
        LcdRect rect,
        int expectedAtlasSize,
        int paintRotateDegrees
    ) {}

    public static final LcdRect LCD = new LcdRect(194, 194, 254, 260);

    private static final Map<ResourceLocation, PipLiveDisplay> BY_LIVE_ID = new ConcurrentHashMap<>();
    private static final int LCD_IDLE_BG = abgr(255, 0, 0, 0);
    private static final int PIXEL_LIME = abgr(255, 20, 90, 18);
    private static final int PIXEL_DIM = abgr(255, 70, 76, 28);
    private static final int PIXEL_GOLD = abgr(255, 55, 48, 10);
    private static final long REDRAW_MS = 80L;

    private final Spec spec;
    private DynamicTexture liveTexture;
    private NativeImage baseSnapshot;
    private long lastDrawMs;
    private String lastFingerprint = "";
    private Boolean supported;

    private PipLiveDisplay(Spec spec) {
        this.spec = spec;
    }

    public static Spec specFor(PipRadioSkin skin) {
        // Distinct from PipDeadAirLcd radio HUD ids — same liveId fought in TextureManager.
        return new Spec(base(skin), live("pip_idle_lcd_" + skin.id), LCD, 512, 90);
    }

    public static Spec tpWristSpecFor(PipRadioSkin skin) {
        return new Spec(base(skin), live("pip_idle_lcd_tp_" + skin.id), LCD, 512, 90);
    }

    public static Spec gunFpWristSpecFor(PipRadioSkin skin) {
        return new Spec(base(skin), live("pip_idle_lcd_gunfp_" + skin.id), LCD, 512, 90);
    }

    private static ResourceLocation base(PipRadioSkin skin) {
        return ResourceLocation.fromNamespaceAndPath(
            Dead_air_pip_boy_radio_conversion.MODID, skin.texturePath());
    }

    private static ResourceLocation live(String path) {
        return ResourceLocation.fromNamespaceAndPath(
            Dead_air_pip_boy_radio_conversion.MODID, "dynamic/" + path);
    }

    public static PipLiveDisplay get(Spec spec) {
        return BY_LIVE_ID.computeIfAbsent(spec.liveId(), id -> new PipLiveDisplay(spec));
    }

    @Nullable
    public static ResourceLocation resolveTexture(
        PipRadioItem pip,
        ItemStack stack,
        boolean wristMode,
        boolean gunFpLcd
    ) {
        Spec spec = wristMode
            ? (gunFpLcd ? gunFpWristSpecFor(pip.getSkin()) : tpWristSpecFor(pip.getSkin()))
            : specFor(pip.getSkin());
        return get(spec).getTexture(stack);
    }

    public ResourceLocation getTexture(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return spec.baseTexture();
        }
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return spec.baseTexture();
        }
        try {
            if (!isSupported(mc)) {
                return spec.baseTexture();
            }
            ensureLive(mc);
            long now = System.currentTimeMillis();
            String fingerprint = "sil|" + (now / 160L) + "|r" + spec.paintRotateDegrees();
            if (fingerprint.equals(lastFingerprint) && now - lastDrawMs < REDRAW_MS) {
                return spec.liveId();
            }
            redraw(mc);
            lastFingerprint = fingerprint;
            lastDrawMs = now;
            liveTexture.upload();
            return spec.liveId();
        } catch (Exception e) {
            Dead_air_pip_boy_radio_conversion.LOGGER.debug("Pip live LCD failed: {}", e.toString());
            return spec.baseTexture();
        }
    }

    private boolean isSupported(Minecraft mc) {
        if (supported != null) {
            return supported;
        }
        try {
            probe(mc);
        } catch (Exception e) {
            supported = false;
        }
        return supported == Boolean.TRUE;
    }

    private void probe(Minecraft mc) throws Exception {
        try (InputStream stream = mc.getResourceManager().getResource(spec.baseTexture()).orElseThrow().open()) {
            NativeImage loaded = NativeImage.read(stream);
            try {
                LcdRect r = spec.rect();
                supported = loaded.getWidth() == spec.expectedAtlasSize()
                    && loaded.getHeight() == spec.expectedAtlasSize()
                    && loaded.getWidth() > r.x1()
                    && loaded.getHeight() > r.y1();
            } finally {
                loaded.close();
            }
        }
    }

    private void ensureLive(Minecraft mc) throws Exception {
        if (liveTexture != null && baseSnapshot != null) {
            return;
        }
        try (InputStream stream = mc.getResourceManager().getResource(spec.baseTexture()).orElseThrow().open()) {
            NativeImage loaded = NativeImage.read(stream);
            baseSnapshot = new NativeImage(loaded.getWidth(), loaded.getHeight(), false);
            baseSnapshot.copyFrom(loaded);
            liveTexture = new DynamicTexture(loaded);
            mc.getTextureManager().register(spec.liveId(), liveTexture);
            supported = true;
        }
    }

    private void redraw(Minecraft mc) {
        NativeImage image = liveTexture.getPixels();
        if (image == null || baseSnapshot == null) {
            return;
        }
        LcdRect atlas = spec.rect();
        image.copyFrom(baseSnapshot);

        boolean rot90 = Math.abs(spec.paintRotateDegrees()) == 90
            || Math.abs(spec.paintRotateDegrees()) == 270;
        int lw = rot90 ? atlas.height() : atlas.width();
        int lh = rot90 ? atlas.width() : atlas.height();

        NativeImage buf = new NativeImage(lw, lh, false);
        try {
            fill(buf, 0, 0, lw, lh, LCD_IDLE_BG);
            boolean painted = PipBoySilhouetteRenderer.paintLcd(
                buf, lw, lh, System.currentTimeMillis(), PipBoySilhouetteRenderer.LcdFlip.NONE);
            if (!painted || isMostlyEmpty(buf)) {
                paintStatusHud(buf, lw, lh, mc);
            }
            int deg = spec.paintRotateDegrees();
            if (deg == 90) {
                blitCcw90(buf, image, atlas);
            } else if (deg == -90) {
                blitCw90(buf, image, atlas);
            } else if (deg == 180) {
                blitFlip180(buf, image, atlas);
            } else {
                blitCopy(buf, image, atlas);
            }
        } finally {
            buf.close();
        }
    }

    private static boolean isMostlyEmpty(NativeImage buf) {
        int opaque = 0;
        int step = Math.max(1, Math.min(buf.getWidth(), buf.getHeight()) / 16);
        for (int y = 0; y < buf.getHeight(); y += step) {
            for (int x = 0; x < buf.getWidth(); x += step) {
                if (((buf.getPixelRGBA(x, y) >>> 24) & 0xFF) > 20) {
                    opaque++;
                    if (opaque > 8) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /** Compact Fallout status when the STAT silhouette has not been captured yet. */
    private static void paintStatusHud(NativeImage buf, int lw, int lh, Minecraft mc) {
        LocalPlayer player = mc.player;
        String facing = compassFromYaw(player != null ? player.getYRot() : 0f);
        String dayTime = dayTimeLine(mc);
        String hp = "HP --";
        if (player != null) {
            hp = String.format("HP %d/%d", Math.round(player.getHealth()), Math.round(player.getMaxHealth()));
        }
        int scale = lw >= 50 ? 1 : 1;
        int pad = 2;
        drawText(buf, "PIP-BOY", pad, pad, scale, PIXEL_LIME);
        drawText(buf, facing, pad, pad + 8, scale, PIXEL_GOLD);
        drawText(buf, dayTime, pad, pad + 16, scale, PIXEL_DIM);
        drawText(buf, hp, pad, pad + 24, scale, PIXEL_LIME);
    }

    private static String dayTimeLine(Minecraft mc) {
        if (mc.level == null) {
            return "DAY -";
        }
        long dayTime = mc.level.getDayTime();
        int day = (int) (dayTime / 24000) + 1;
        int tickOfDay = (int) (dayTime % 24000);
        int hour = (tickOfDay / 1000 + 6) % 24;
        int minute = (tickOfDay % 1000) * 60 / 1000;
        return String.format("D%d %02d:%02d", day, hour, minute);
    }

    private static void blitCopy(NativeImage src, NativeImage dest, LcdRect atlas) {
        int w = Math.min(src.getWidth(), atlas.width());
        int h = Math.min(src.getHeight(), atlas.height());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dest.setPixelRGBA(atlas.x0() + x, atlas.y0() + y, src.getPixelRGBA(x, y));
            }
        }
    }

    private static void blitFlip180(NativeImage src, NativeImage dest, LcdRect atlas) {
        int w = Math.min(src.getWidth(), atlas.width());
        int h = Math.min(src.getHeight(), atlas.height());
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                dest.setPixelRGBA(atlas.x0() + (w - 1 - x), atlas.y0() + (h - 1 - y), src.getPixelRGBA(x, y));
            }
        }
    }

    private static void blitCcw90(NativeImage src, NativeImage dest, LcdRect atlas) {
        int lw = src.getWidth();
        int lh = src.getHeight();
        for (int ly = 0; ly < lh; ly++) {
            for (int lx = 0; lx < lw; lx++) {
                int ax = atlas.x0() + ly;
                int ay = atlas.y0() + (lw - 1 - lx);
                if (ax >= atlas.x0() && ax < atlas.x1() && ay >= atlas.y0() && ay < atlas.y1()) {
                    dest.setPixelRGBA(ax, ay, src.getPixelRGBA(lx, ly));
                }
            }
        }
    }

    private static void blitCw90(NativeImage src, NativeImage dest, LcdRect atlas) {
        int lw = src.getWidth();
        int lh = src.getHeight();
        for (int ly = 0; ly < lh; ly++) {
            for (int lx = 0; lx < lw; lx++) {
                int ax = atlas.x0() + (lh - 1 - ly);
                int ay = atlas.y0() + lx;
                if (ax >= atlas.x0() && ax < atlas.x1() && ay >= atlas.y0() && ay < atlas.y1()) {
                    dest.setPixelRGBA(ax, ay, src.getPixelRGBA(lx, ly));
                }
            }
        }
    }

    private static void fill(NativeImage image, int x0, int y0, int x1, int y1, int color) {
        int maxX = Math.min(image.getWidth(), x1);
        int maxY = Math.min(image.getHeight(), y1);
        for (int y = Math.max(0, y0); y < maxY; y++) {
            for (int x = Math.max(0, x0); x < maxX; x++) {
                image.setPixelRGBA(x, y, color);
            }
        }
    }

    private static void drawText(NativeImage image, String text, int x, int y, int scale, int color) {
        int cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char c = Character.toUpperCase(text.charAt(i));
            int bits = glyph(c);
            for (int row = 0; row < 5; row++) {
                int rowBits = (bits >> ((4 - row) * 3)) & 0b111;
                for (int col = 0; col < 3; col++) {
                    if ((rowBits & (1 << (2 - col))) != 0) {
                        fill(image,
                            cursor + col * scale, y + row * scale,
                            cursor + (col + 1) * scale, y + (row + 1) * scale,
                            color);
                    }
                }
            }
            cursor += 4 * scale;
        }
    }

    private static int glyph(char c) {
        return switch (c) {
            case ' ' -> 0;
            case '-' -> 0b000_000_111_000_000;
            case '/' -> 0b001_001_010_100_100;
            case ':' -> 0b000_010_000_010_000;
            case '0' -> 0b111_101_101_101_111;
            case '1' -> 0b010_110_010_010_111;
            case '2' -> 0b110_001_111_100_111;
            case '3' -> 0b110_001_111_001_110;
            case '4' -> 0b101_101_111_001_001;
            case '5' -> 0b111_100_110_001_110;
            case '6' -> 0b011_100_111_101_111;
            case '7' -> 0b111_001_010_010_010;
            case '8' -> 0b111_101_111_101_111;
            case '9' -> 0b111_101_111_001_110;
            case 'A' -> 0b010_101_111_101_101;
            case 'B' -> 0b110_101_110_101_110;
            case 'C' -> 0b011_100_100_100_011;
            case 'D' -> 0b110_101_101_101_110;
            case 'E' -> 0b111_100_110_100_111;
            case 'H' -> 0b101_101_111_101_101;
            case 'I' -> 0b111_010_010_010_111;
            case 'N' -> 0b101_111_111_111_101;
            case 'O' -> 0b010_101_101_101_010;
            case 'P' -> 0b110_101_110_100_100;
            case 'R' -> 0b110_101_110_101_101;
            case 'S' -> 0b011_100_010_001_110;
            case 'T' -> 0b111_010_010_010_010;
            case 'U' -> 0b101_101_101_101_111;
            case 'W' -> 0b101_101_111_111_101;
            case 'Y' -> 0b101_101_010_010_010;
            default -> 0b111_001_011_000_010; // ?
        };
    }

    private static String compassFromYaw(float yaw) {
        while (yaw < 0) {
            yaw += 360;
        }
        while (yaw >= 360) {
            yaw -= 360;
        }
        if (yaw >= 337.5 || yaw < 22.5) {
            return "S";
        }
        if (yaw >= 22.5 && yaw < 67.5) {
            return "SE";
        }
        if (yaw >= 67.5 && yaw < 112.5) {
            return "E";
        }
        if (yaw >= 112.5 && yaw < 157.5) {
            return "NE";
        }
        if (yaw >= 157.5 && yaw < 202.5) {
            return "N";
        }
        if (yaw >= 202.5 && yaw < 247.5) {
            return "NW";
        }
        if (yaw >= 247.5 && yaw < 292.5) {
            return "W";
        }
        return "SW";
    }

    private static int abgr(int alpha, int red, int green, int blue) {
        return (alpha & 255) << 24 | (blue & 255) << 16 | (green & 255) << 8 | (red & 255);
    }
}
