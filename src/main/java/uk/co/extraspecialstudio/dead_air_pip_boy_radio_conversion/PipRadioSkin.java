package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * Extra Special Radio Unit colour shells. Same mesh; only the texture atlas changes.
 */
public enum PipRadioSkin {
    LIGHT_GREEN("light_green", "Light Green", Items.LIME_DYE),
    DARK_GREEN("dark_green", "Dark Green", Items.GREEN_DYE),
    GREY("grey", "Grey", Items.GRAY_DYE),
    DARK_BLUE("dark_blue", "Dark Blue", Items.BLUE_DYE),
    DARK_BROWN("dark_brown", "Dark Brown", Items.BROWN_DYE);

    public final String id;
    public final String displayName;
    public final Item dye;

    PipRadioSkin(String id, String displayName, Item dye) {
        this.id = id;
        this.displayName = displayName;
        this.dye = dye;
    }

    public String itemId(boolean t2) {
        return (t2 ? "pip_radio_t2_" : "pip_radio_t1_") + id;
    }

    public String texturePath() {
        return "textures/item/pip_radio_" + id + ".png";
    }
}
