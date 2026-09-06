package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion;

import com.mojang.logging.LogUtils;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import org.slf4j.Logger;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block.PipBlockEntities;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block.PipBlocks;

/**
 * Pip-Boy companion: Extra Special Radio Unit (T1/T2, colour shells).
 * Requires Extra Special Core + GeckoLib. Dead Air is optional — RADIO tab prompts to install it when missing.
 */
@Mod(Dead_air_pip_boy_radio_conversion.MODID)
public class Dead_air_pip_boy_radio_conversion {
    public static final String MODID = "dead_air_pip_boy_radio_conversion";
    public static final Logger LOGGER = LogUtils.getLogger();

    public Dead_air_pip_boy_radio_conversion(IEventBus bus) {
        PipBlocks.REGISTER.register(bus);
        PipBlockEntities.REGISTER.register(bus);
        PipRadioItems.REGISTER.register(bus);
        PipCreativeTabs.REGISTER.register(bus);
        bus.addListener(this::commonSetup);
        LOGGER.info("Extra Special Radio Unit registered (T1/T2, {} colours); Dead Air optional={}",
            PipRadioSkin.values().length, PipMods.deadAirLoaded());
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            try {
                if (!ModList.get().isLoaded("radiotowers")) {
                    return;
                }
                Class<?> catalog = Class.forName("net.mcreator.radiotowers.airdrop.AirdropCatalog");
                @SuppressWarnings("unchecked")
                java.util.List<Object> addons = (java.util.List<Object>) catalog.getField("ADDON_ENTRIES").get(null);
                Class<?> entryCl = Class.forName("net.mcreator.radiotowers.airdrop.AirdropCatalog$Entry");
                java.lang.reflect.Constructor<?> entryCtor =
                    entryCl.getConstructor(ResourceLocation.class, int.class, int.class);
                // Same cost as Dead Air T1.Radio: 5 points, stack of 1 (default Light Green).
                addons.add(entryCtor.newInstance(
                    ResourceLocation.fromNamespaceAndPath(MODID, PipRadioSkin.LIGHT_GREEN.itemId(false)),
                    5,
                    1));
                catalog.getMethod("invalidate").invoke(null);
                LOGGER.info("Registered Extra Special Radio Unit in airdrop catalog (1 / 5 points)");
            } catch (Throwable t) {
                LOGGER.debug("Could not register airdrop catalog entry: {}", t.toString());
            }
        });
    }
}
