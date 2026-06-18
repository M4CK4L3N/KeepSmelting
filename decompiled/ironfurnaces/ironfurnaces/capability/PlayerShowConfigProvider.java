/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.Direction
 *  net.minecraft.nbt.CompoundTag
 *  net.minecraftforge.common.capabilities.Capability
 *  net.minecraftforge.common.capabilities.ICapabilityProvider
 *  net.minecraftforge.common.capabilities.ICapabilitySerializable
 *  net.minecraftforge.common.util.LazyOptional
 *  org.jetbrains.annotations.NotNull
 *  org.jetbrains.annotations.Nullable
 */
package ironfurnaces.capability;

import ironfurnaces.capability.CapabilityPlayerShowConfig;
import ironfurnaces.capability.PlayerShowConfig;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class PlayerShowConfigProvider
implements ICapabilityProvider,
ICapabilitySerializable<CompoundTag> {
    public PlayerShowConfig config = new PlayerShowConfig(0);
    private LazyOptional<PlayerShowConfig> lazyConfig = LazyOptional.of(() -> this.config);

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        return cap == CapabilityPlayerShowConfig.CONFIG ? this.lazyConfig.cast() : LazyOptional.empty();
    }

    @NotNull
    public <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap) {
        return cap == CapabilityPlayerShowConfig.CONFIG ? this.lazyConfig.cast() : LazyOptional.empty();
    }

    public CompoundTag serializeNBT() {
        CompoundTag tag = new CompoundTag();
        tag.m_128405_("show", this.config.value);
        return tag;
    }

    public void deserializeNBT(CompoundTag nbt) {
        this.config.value = nbt.m_128451_("show");
    }
}

