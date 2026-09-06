package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import uk.co.extraspecialstudio.dead_air.audio.AudioManager;
import uk.co.extraspecialstudio.dead_air.client.RadioLiveDisplay;
import uk.co.extraspecialstudio.dead_air.radio.RadioStation;
import uk.co.extraspecialstudio.dead_air.radio.StationRegistry;
import uk.co.extraspecialstudio.dead_air.walkie.WalkieTalkieManager;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioSkin;
import uk.co.extraspecialstudio.dead_air.client.WalkieTalkieOverlay;

import org.jetbrains.annotations.Nullable;

/**
 * Dead Air radio HUD on the Pip CRT — only while music/stream is actually playing.
 * The idle mini character GUI lives entirely in Pip ({@code PipLiveDisplay}), not here.
 */
public final class PipDeadAirLcd {
    public static final RadioLiveDisplay.LcdRect LCD = new RadioLiveDisplay.LcdRect(194, 194, 254, 260);

    private PipDeadAirLcd() {}

    public static RadioLiveDisplay.Spec specFor(PipRadioSkin skin) {
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath(
            Dead_air_pip_boy_radio_conversion.MODID, skin.texturePath());
        // Distinct from PipLiveDisplay idle ids — sharing dynamic/pip_lcd_* broke music on/off swap.
        ResourceLocation live = ResourceLocation.fromNamespaceAndPath(
            Dead_air_pip_boy_radio_conversion.MODID, "dynamic/pip_radio_lcd_" + skin.id);
        return new RadioLiveDisplay.Spec(base, live, LCD, 512, false, 90);
    }

    public static RadioLiveDisplay.Spec tpWristSpecFor(PipRadioSkin skin) {
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath(
            Dead_air_pip_boy_radio_conversion.MODID, skin.texturePath());
        ResourceLocation live = ResourceLocation.fromNamespaceAndPath(
            Dead_air_pip_boy_radio_conversion.MODID, "dynamic/pip_radio_lcd_tp_" + skin.id);
        return new RadioLiveDisplay.Spec(base, live, LCD, 512, false, 90);
    }

    public static RadioLiveDisplay.Spec gunFpWristSpecFor(PipRadioSkin skin) {
        ResourceLocation base = ResourceLocation.fromNamespaceAndPath(
            Dead_air_pip_boy_radio_conversion.MODID, skin.texturePath());
        ResourceLocation live = ResourceLocation.fromNamespaceAndPath(
            Dead_air_pip_boy_radio_conversion.MODID, "dynamic/pip_radio_lcd_gunfp_" + skin.id);
        return new RadioLiveDisplay.Spec(base, live, LCD, 512, false, 90);
    }

    /**
     * Radio station HUD texture while actively listening, else {@code null}
     * so Pip can paint its own mini character GUI.
     */
    @Nullable
    public static ResourceLocation resolveRadioHudIfListening(
        PipRadioItem pip,
        ItemStack stack,
        boolean wristMode,
        boolean gunFpLcd
    ) {
        if (stack == null || stack.isEmpty() || !isActivelyListening(stack)) {
            return null;
        }
        RadioLiveDisplay.Spec spec = wristMode
            ? (gunFpLcd ? gunFpWristSpecFor(pip.getSkin()) : tpWristSpecFor(pip.getSkin()))
            : specFor(pip.getSkin());
        return RadioLiveDisplay.get(spec).getTexture(stack);
    }

    private static boolean isActivelyListening(ItemStack stack) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || mc.player == null) {
            return false;
        }
        var tag = uk.co.extraspecialstudio.dead_air.walkie.WalkieNbt.get(stack);
        boolean on = tag == null || !tag.contains("DeadAirOn") || tag.getBoolean("DeadAirOn");
        if (!on) {
            return false;
        }
        ResourceLocation stationId = WalkieTalkieManager.getTunedStationId(stack);
        RadioStation station = stationId != null ? StationRegistry.getStation(stationId) : null;
        if (station == null) {
            return false;
        }
        float signal = WalkieTalkieOverlay.calculateSignalStrengthForStation(mc, station);
        if (signal < 0.05f) {
            return false;
        }
        return AudioManager.hasActiveSoundForStation(station.getId());
    }
}
