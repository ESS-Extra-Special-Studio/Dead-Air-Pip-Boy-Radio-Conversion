package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion;

import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.EnumMap;
import java.util.Map;

public final class PipRadioItems {
    public static final DeferredRegister.Items REGISTER =
        DeferredRegister.createItems(Dead_air_pip_boy_radio_conversion.MODID);

    public static final Map<PipRadioSkin, DeferredHolder<Item, PipRadioItem>> T1 = new EnumMap<>(PipRadioSkin.class);
    public static final Map<PipRadioSkin, DeferredHolder<Item, PipRadioItem>> T2 = new EnumMap<>(PipRadioSkin.class);

    static {
        for (PipRadioSkin skin : PipRadioSkin.values()) {
            T1.put(skin, REGISTER.register(
                skin.itemId(false),
                () -> new PipRadioItem(new Item.Properties().stacksTo(1), PipRadioTier.T1, skin)
            ));
            T2.put(skin, REGISTER.register(
                skin.itemId(true),
                () -> new PipRadioItem(new Item.Properties().stacksTo(1), PipRadioTier.T2, skin)
            ));
        }
    }

    /** Default craft / tab lead — classic CRT green. */
    public static DeferredHolder<Item, PipRadioItem> defaultT1() {
        return T1.get(PipRadioSkin.LIGHT_GREEN);
    }

    public static DeferredHolder<Item, PipRadioItem> defaultT2() {
        return T2.get(PipRadioSkin.LIGHT_GREEN);
    }

    private PipRadioItems() {}
}
