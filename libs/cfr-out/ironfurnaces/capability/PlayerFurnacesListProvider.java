/*
 * Decompiled with CFR.
 */
package ironfurnaces.capability;

import ironfurnaces.capability.CapabilityPlayerFurnacesList;
import ironfurnaces.capability.PlayerFurnacesList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerFurnacesListProvider
implements ICapabilityProvider,
ICapabilitySerializable<CompoundTag> {
    public PlayerFurnacesList furnacesList = new PlayerFurnacesList();
    private LazyOptional<PlayerFurnacesList> lazyList = LazyOptional.of(() -> this.furnacesList);

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityPlayerFurnacesList.FURNACES_LIST ? this.lazyList.cast() : LazyOptional.empty();
    }

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return cap == CapabilityPlayerFurnacesList.FURNACES_LIST ? this.lazyList.cast() : LazyOptional.empty();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        CompoundTag furnaces = new CompoundTag();
        for (int i = 0; i < this.furnacesList.listFurances.size(); ++i) {
            CompoundTag blockpos = new CompoundTag();
            blockpos.m_128405_("X", this.furnacesList.listFurances.get(i).m_123341_());
            blockpos.m_128405_("Y", this.furnacesList.listFurances.get(i).m_123342_());
            blockpos.m_128405_("Z", this.furnacesList.listFurances.get(i).m_123343_());
            furnaces.m_128365_("furnace" + i, (Tag)blockpos);
        }
        tag.m_128365_("furnaces", (Tag)furnaces);
        tag.m_128405_("count", this.furnacesList.listFurances.size());
        return tag;
    }

    public void deserializeNBT(CompoundTag tag) {
        int size = tag.m_128451_("count");
        CompoundTag furances = tag.m_128469_("furnaces");
        for (int i = 0; i < size; ++i) {
            CompoundTag furance = furances.m_128469_("furnace" + i);
            BlockPos pos = new BlockPos(furance.m_128451_("X"), furance.m_128451_("Y"), furance.m_128451_("Z"));
            this.furnacesList.listFurances.add(pos);
        }
    }
}

