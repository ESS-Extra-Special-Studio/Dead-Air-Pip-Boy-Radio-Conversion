package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;

/** Shared open-raise timing for empty-hand FP when any {@link PipBoyGui} is open. */
public final class PipBoyRaise {
    private static final float RAISE_SECONDS = 0.3f;
    private static long openStartedMs;

    private PipBoyRaise() {}

    public static void onOpen() {
        openStartedMs = System.currentTimeMillis();
    }

    public static void onClose() {
        openStartedMs = 0L;
    }

    /** 0..1 raise progress while a Pip-Boy GUI is open. */
    public static float raiseProgress() {
        if (!(Minecraft.getInstance().screen instanceof PipBoyGui)) {
            return 0f;
        }
        if (openStartedMs <= 0L) {
            return 1f;
        }
        float t = (System.currentTimeMillis() - openStartedMs) / 1000f / RAISE_SECONDS;
        return Mth.clamp(t, 0f, 1f);
    }
}
