package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Own creative / JEI item tab. When Dead Air is present, ordered immediately after its tab.
 */
public final class PipCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> REGISTER =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Dead_air_pip_boy_radio_conversion.MODID);

    private static final ResourceLocation DEAD_AIR_TAB_ID =
        ResourceLocation.fromNamespaceAndPath("dead_air", "dead_air");

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> PIP_RADIO_TAB = REGISTER.register(
        "pip_radio",
        () -> {
            CreativeModeTab.Builder builder = CreativeModeTab.builder()
                .title(Component.translatable("itemGroup.dead_air_pip_boy_radio_conversion.main"))
                .icon(() -> new ItemStack(PipRadioItems.defaultT1().get()))
                .displayItems((parameters, output) -> {
                    for (PipRadioSkin skin : PipRadioSkin.values()) {
                        output.accept(PipRadioItems.T1.get(skin).get());
                    }
                    // T2 is Dead Air Link-tier — only list when Dead Air is present.
                    if (PipMods.deadAirLoaded()) {
                        for (PipRadioSkin skin : PipRadioSkin.values()) {
                            output.accept(PipRadioItems.T2.get(skin).get());
                        }
                    }
                });
            // Sit to the right of Dead Air when that tab exists; harmless if Dead Air is absent.
            if (PipMods.deadAirLoaded()) {
                builder.withTabsBefore(DEAD_AIR_TAB_ID);
            }
            return builder.build();
        }
    );

    private PipCreativeTabs() {}
}
