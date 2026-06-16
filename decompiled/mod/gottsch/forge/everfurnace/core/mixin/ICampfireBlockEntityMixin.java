/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.entity.CampfireBlockEntity
 *  org.spongepowered.asm.mixin.Mixin
 *  org.spongepowered.asm.mixin.gen.Accessor
 */
package mod.gottsch.forge.everfurnace.core.mixin;

import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value={CampfireBlockEntity.class})
public interface ICampfireBlockEntityMixin {
    @Accessor
    public int[] getCookingProgress();

    @Accessor
    public int[] getCookingTime();
}

