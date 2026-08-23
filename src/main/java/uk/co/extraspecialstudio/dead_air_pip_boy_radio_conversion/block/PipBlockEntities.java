package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;

public final class PipBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> REGISTER =
        DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Dead_air_pip_boy_radio_conversion.MODID);

    @SuppressWarnings("ConstantConditions")
    public static final RegistryObject<BlockEntityType<PipRadioDecorBlockEntity>> PIP_RADIO_DECOR =
        REGISTER.register(
            "pip_radio_decor",
            () -> BlockEntityType.Builder.of(PipRadioDecorBlockEntity::new, PipBlocks.PIP_RADIO_DECOR.get())
                .build(null)
        );

    private PipBlockEntities() {}
}
