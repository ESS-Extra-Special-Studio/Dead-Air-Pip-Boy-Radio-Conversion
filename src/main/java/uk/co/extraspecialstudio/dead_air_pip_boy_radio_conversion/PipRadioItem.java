package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import software.bernie.geckolib.animatable.GeoItem;
import software.bernie.geckolib.animatable.SingletonGeoAnimatable;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block.PipBlocks;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block.PipRadioDecorBlockEntity;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipBoyScreen;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.client.PipRadioItemRenderer;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.compat.PipDeadAirBootstrap;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Consumer;

/**
 * Pip-Boy radio: worn on the off-hand / left wrist. Opens Pip GUI; Dead Air radio features
 * activate when Dead Air is installed (optional dependency).
 */
public class PipRadioItem extends Item implements GeoItem {
    private static final String GEO = "geo/item/pip_radio.geo.json";
    private static final String ANIM = "animations/item/pip_radio.animation.json";
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.pip_radio.idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private final PipRadioTier tier;
    private final PipRadioSkin skin;

    public PipRadioItem(Properties properties, PipRadioTier tier, PipRadioSkin skin) {
        super(properties);
        this.tier = tier;
        this.skin = skin;
        SingletonGeoAnimatable.registerSyncedAnimatable(this);
    }

    public PipRadioSkin getSkin() {
        return skin;
    }

    public PipRadioTier getTier() {
        return tier;
    }

    public boolean isLinkCapable() {
        return tier.isLinkCapable();
    }

    /** Forge: treat as off-hand equipment (left wrist). */
    @Override
    public EquipmentSlot getEquipmentSlot(ItemStack stack) {
        return EquipmentSlot.OFFHAND;
    }

    public ResourceLocation getGeoModelResource() {
        return ResourceLocation.fromNamespaceAndPath(Dead_air_pip_boy_radio_conversion.MODID, GEO);
    }

    public ResourceLocation getGeoTextureResource() {
        return ResourceLocation.fromNamespaceAndPath(Dead_air_pip_boy_radio_conversion.MODID, skin.texturePath());
    }

    public ResourceLocation getGeoAnimationResource() {
        return ResourceLocation.fromNamespaceAndPath(Dead_air_pip_boy_radio_conversion.MODID, ANIM);
    }

    /**
     * Crouch + right-click a block to place as decoration.
     * Normal right-click still opens / equips via {@link #use}.
     */
    @Override
    public @Nonnull InteractionResult useOn(@Nonnull UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        if (!player.isShiftKeyDown()) {
            return use(context.getLevel(), player, context.getHand()).getResult();
        }
        InteractionResult placed = tryPlaceDecor(context);
        return placed.consumesAction() ? placed : InteractionResult.PASS;
    }

    private InteractionResult tryPlaceDecor(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        ItemStack held = context.getItemInHand();
        BlockPlaceContext placeCtx = new BlockPlaceContext(context);
        if (!placeCtx.canPlace()) {
            return InteractionResult.FAIL;
        }

        BlockState state = PipBlocks.PIP_RADIO_DECOR.get().getStateForPlacement(placeCtx);
        if (state == null || !state.canSurvive(level, placeCtx.getClickedPos())) {
            return InteractionResult.FAIL;
        }
        if (!level.isClientSide && !mayPlace(player, placeCtx)) {
            return InteractionResult.FAIL;
        }

        BlockPos pos = placeCtx.getClickedPos();
        if (!level.setBlock(pos, state, 3)) {
            return InteractionResult.FAIL;
        }

        if (level.getBlockEntity(pos) instanceof PipRadioDecorBlockEntity be) {
            be.setStored(held.copyWithCount(1));
        }

        if (!level.isClientSide) {
            SoundType sound = state.getSoundType();
            level.playSound(
                null,
                pos,
                sound.getPlaceSound(),
                SoundSource.BLOCKS,
                (sound.getVolume() + 1.0f) / 2.0f,
                sound.getPitch() * 0.8f
            );
            if (player != null && !player.getAbilities().instabuild) {
                held.shrink(1);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static boolean mayPlace(@Nullable Player player, BlockPlaceContext ctx) {
        return player == null || ctx.getLevel().mayInteract(player, ctx.getClickedPos());
    }

    @Override
    public @Nonnull InteractionResultHolder<ItemStack> use(
        @Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand
    ) {
        ItemStack stack = player.getItemInHand(hand);

        if (hand == InteractionHand.OFF_HAND) {
            if (level.isClientSide()) {
                openTuningScreen(InteractionHand.OFF_HAND);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        ItemStack off = player.getOffhandItem();
        if (off.isEmpty()) {
            player.setItemInHand(InteractionHand.OFF_HAND, stack.copy());
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            if (level.isClientSide()) {
                openTuningScreen(InteractionHand.OFF_HAND);
            }
            return InteractionResultHolder.sidedSuccess(ItemStack.EMPTY, level.isClientSide());
        }

        if (off.getItem() instanceof PipRadioItem) {
            if (level.isClientSide()) {
                openTuningScreen(InteractionHand.OFF_HAND);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }

        player.setItemInHand(InteractionHand.OFF_HAND, stack.copy());
        player.setItemInHand(InteractionHand.MAIN_HAND, off.copy());
        if (level.isClientSide()) {
            openTuningScreen(InteractionHand.OFF_HAND);
        }
        return InteractionResultHolder.sidedSuccess(player.getMainHandItem(), level.isClientSide());
    }

    @OnlyIn(Dist.CLIENT)
    public void openTuningScreen(InteractionHand hand) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null) {
            return;
        }
        InteractionHand bound = hand;
        if (mc.player != null && mc.player.getOffhandItem().getItem() instanceof PipRadioItem) {
            bound = InteractionHand.OFF_HAND;
        }
        try {
            if (PipMods.deadAirLoaded()) {
                PipDeadAirBootstrap.openPipBoy(bound);
            } else {
                mc.setScreen(new PipBoyScreen(bound));
            }
        } catch (NoClassDefFoundError | Exception e) {
            Dead_air_pip_boy_radio_conversion.LOGGER.error("Could not open Pip-Boy screen", e);
        }
    }

    @Override
    public void appendHoverText(
        @Nonnull ItemStack stack, @Nullable Level level, @Nonnull List<Component> tip, @Nonnull TooltipFlag flag
    ) {
        tip.add(Component.literal("§7Equip: off-hand (left wrist)"));
        tip.add(Component.literal("§8Right-click to wear · crouch+click block to place"));
        if (!PipMods.deadAirLoaded()) {
            tip.add(Component.literal("§6Install Dead Air for radio tuning"));
        }
        super.appendHoverText(stack, level, tip, flag);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private BlockEntityWithoutLevelRenderer renderer;

            @Override
            public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if (renderer == null) {
                    renderer = new PipRadioItemRenderer();
                }
                return renderer;
            }
        });
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 0, this::predicate));
    }

    private PlayState predicate(AnimationState<PipRadioItem> state) {
        state.getController().setAnimation(IDLE);
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
