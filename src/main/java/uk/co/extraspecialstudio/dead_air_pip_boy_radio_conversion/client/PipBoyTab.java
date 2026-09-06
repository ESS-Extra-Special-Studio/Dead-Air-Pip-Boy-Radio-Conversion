package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

/**
 * Tab strip for the framed Pip-Boy GUI.
 * STAT is the default Fallout status page; RADIO embeds Dead Air tuning;
 * INV / MAP / SPECIAL are stubs this pass.
 */
public enum PipBoyTab {
    STAT("STAT", true),
    INV("INV", false),
    MAP("MAP", false),
    RADIO("RADIO", true),
    SPECIAL("SPECIAL", false);

    public final String label;
    public final boolean implemented;

    PipBoyTab(String label, boolean implemented) {
        this.label = label;
        this.implemented = implemented;
    }

    public static PipBoyTab fromId(String id) {
        if (id == null) {
            return STAT;
        }
        return switch (id.toLowerCase()) {
            case "inv" -> INV;
            case "map" -> MAP;
            case "radio" -> RADIO;
            case "special" -> SPECIAL;
            default -> STAT;
        };
    }
}
