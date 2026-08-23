package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.compat;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import uk.co.extraspecialstudio.dead_air.api.DeadAirAPI;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;

/**
 * Soft-loaded Dead Air integration. Call only when {@code dead_air} is on the classpath.
 * Idle CRT / mini character GUI is owned by Pip ({@code PipLiveDisplay}) — not registered here.
 */
public final class PipDeadAirBootstrap {
    private static boolean registered;

    private PipDeadAirBootstrap() {}

    public static void initClient() {
        if (registered) {
            return;
        }
        registered = true;
        DeadAirAPI.registerWalkieGuiOpener(
            stack -> stack.getItem() instanceof PipRadioItem,
            (hand, stack) -> {
                openPipBoy(hand);
                return true;
            }
        );
        Dead_air_pip_boy_radio_conversion.LOGGER.info(
            "Dead Air soft-integration ready (walkie GUI opener; Pip owns idle CRT)");
    }

    public static void openPipBoy(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        InteractionHand bound = hand;
        if (mc.player != null && mc.player.getOffhandItem().getItem() instanceof PipRadioItem) {
            bound = InteractionHand.OFF_HAND;
        }
        mc.setScreen(new PipBoyDeadAirScreen(bound));
    }
}
