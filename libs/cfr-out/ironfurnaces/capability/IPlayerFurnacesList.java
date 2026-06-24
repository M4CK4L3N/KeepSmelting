/*
 * Decompiled with CFR.
 */
package ironfurnaces.capability;

import java.util.List;
import net.minecraft.core.BlockPos;

public interface IPlayerFurnacesList {
    public List<BlockPos> get();

    public void add(BlockPos var1);

    public void remove(BlockPos var1);
}

