package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.Dead_air_pip_boy_radio_conversion;

/**
 * Placeable Pip decor (same idea as Dead Air disc compendiums — Block + place from item).
 */
public final class PipBlocks {
    public static final DeferredRegister<Block> REGISTER =
        DeferredRegister.create(ForgeRegistries.BLOCKS, Dead_air_pip_boy_radio_conversion.MODID);

    public static final RegistryObject<PipRadioDecorBlock> PIP_RADIO_DECOR = REGISTER.register(
        "pip_radio_decor",
        () -> new PipRadioDecorBlock(BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_GREEN)
            .strength(0.5f)
            .sound(SoundType.METAL)
            .noOcclusion())
    );

    private PipBlocks() {}
}
