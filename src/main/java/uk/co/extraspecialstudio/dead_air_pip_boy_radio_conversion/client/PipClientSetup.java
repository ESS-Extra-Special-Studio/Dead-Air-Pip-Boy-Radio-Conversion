package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.entity.player.PlayerRenderer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipMods;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.PipRadioItem;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block.PipBlockEntities;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.compat.PipDeadAirBootstrap;

@Mod.EventBusSubscriber(
    modid = Dead_air_pip_boy_radio_conversion.MODID,
    value = Dist.CLIENT,
    bus = Mod.EventBusSubscriber.Bus.MOD
)
public final class PipClientSetup {
    private PipClientSetup() {}

    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            if (PipMods.deadAirLoaded()) {
                PipDeadAirBootstrap.initClient();
            } else {
                Dead_air_pip_boy_radio_conversion.LOGGER.info(
                    "Dead Air not installed — Pip shell runs without radio tuning / live LCD");
            }
        });
    }

    @SubscribeEvent
    public static void onRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerBlockEntityRenderer(
            PipBlockEntities.PIP_RADIO_DECOR.get(),
            PipRadioDecorRenderer::new
        );
    }

    @SubscribeEvent
    public static void onAddLayers(EntityRenderersEvent.AddLayers event) {
        for (String skin : event.getSkins()) {
            PlayerRenderer renderer = event.getSkin(skin);
            if (renderer != null) {
                renderer.addLayer(new PipWristLayer(renderer));
            }
        }
    }

    @Mod.EventBusSubscriber(
        modid = Dead_air_pip_boy_radio_conversion.MODID,
        value = Dist.CLIENT,
        bus = Mod.EventBusSubscriber.Bus.FORGE
    )
    public static final class ClientGui {
        private ClientGui() {}

        @SubscribeEvent
        public static void onRenderGuiPost(RenderGuiEvent.Post event) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player == null || mc.level == null) {
                return;
            }
            if (mc.screen instanceof PipBoyGui
                || holdingPip(mc.player.getMainHandItem())
                || holdingPip(mc.player.getOffhandItem())) {
                PipBoySilhouetteRenderer.captureFromGuiPass();
            }
        }

        /**
         * After setModelProperties; force EMPTY arm poses for the CRT doll so arms hang
         * down while Pip still draws on the cuff via {@link PipWristLayer}.
         */
        @SubscribeEvent(priority = EventPriority.LOWEST)
        public static void onRenderLivingPre(RenderLivingEvent.Pre<?, ?> event) {
            if (!Boolean.TRUE.equals(PipBoySilhouetteRenderer.NEUTRAL_ARM_POSES.get())) {
                return;
            }
            if (!(event.getRenderer() instanceof PlayerRenderer playerRenderer)) {
                return;
            }
            PlayerModel<?> model = playerRenderer.getModel();
            model.leftArmPose = HumanoidModel.ArmPose.EMPTY;
            model.rightArmPose = HumanoidModel.ArmPose.EMPTY;
        }

        private static boolean holdingPip(ItemStack stack) {
            return stack != null && !stack.isEmpty() && stack.getItem() instanceof PipRadioItem;
        }
    }
}
