package uk.co.extraspecialstudio.dead_air_pip_boy_radio_conversion.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the placed Pip stack (skin / tier / radio NBT) so break returns the same item.
 */
public final class PipRadioDecorBlockEntity extends BlockEntity {
    private ItemStack stored = ItemStack.EMPTY;

    public PipRadioDecorBlockEntity(BlockPos pos, BlockState state) {
        super(PipBlockEntities.PIP_RADIO_DECOR.get(), pos, state);
    }

    public ItemStack getStored() {
        return stored;
    }

    public void setStored(ItemStack stack) {
        this.stored = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1);
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public void dropContents(Level level, BlockPos pos) {
        if (stored.isEmpty()) {
            return;
        }
        ItemEntity drop = new ItemEntity(
            level,
            pos.getX() + 0.5,
            pos.getY() + 0.25,
            pos.getZ() + 0.5,
            stored.copy()
        );
        drop.setDefaultPickUpDelay();
        level.addFreshEntity(drop);
        stored = ItemStack.EMPTY;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!stored.isEmpty()) {
            tag.put("Pip", stored.save(registries));
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.contains("Pip")
            ? ItemStack.parse(registries, tag.getCompound("Pip")).orElse(ItemStack.EMPTY)
            : ItemStack.EMPTY;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Nullable
    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt, HolderLookup.Provider lookup) {
        CompoundTag tag = pkt.getTag();
        if (tag != null) {
            loadAdditional(tag, lookup);
        }
    }
}
