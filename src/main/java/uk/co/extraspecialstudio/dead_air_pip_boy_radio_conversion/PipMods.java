package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion;

import net.neoforged.fml.ModList;

/** Soft-dependency presence checks. Keep companion class loads behind these guards. */
public final class PipMods {
    public static final String DEAD_AIR = "dead_air";

    private PipMods() {}

    public static boolean deadAirLoaded() {
        return ModList.get().isLoaded(DEAD_AIR);
    }
}
