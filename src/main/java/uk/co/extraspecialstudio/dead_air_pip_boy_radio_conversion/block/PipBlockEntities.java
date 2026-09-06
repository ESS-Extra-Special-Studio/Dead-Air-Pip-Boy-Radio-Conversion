package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;

public final class PipBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Dead_air_pip_boy_radio_conversion.MODID);

    @SuppressWarnings("ConstantConditions")
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PipRadioDecorBlockEntity>> PIP_RADIO_DECOR =
        REGISTER.register(
            "pip_radio_decor",
            () -> BlockEntityType.Builder.of(PipRadioDecorBlockEntity::new, PipBlocks.PIP_RADIO_DECOR.get())
                .build(null)
        );

    private PipBlockEntities() {}
}
