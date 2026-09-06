package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion;

/** Local radio tier (mirrors Dead Air walkie T1/T2 without importing Dead Air). */
public enum PipRadioTier {
    T1,
    T2;

    public boolean isLinkCapable() {
        return this == T2;
    }
}
