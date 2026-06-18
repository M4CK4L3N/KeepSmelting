/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.core.BlockPos
 */
package ironfurnaces.capability;

import ironfurnaces.capability.IPlayerFurnacesList;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;

public class PlayerFurnacesList
implements IPlayerFurnacesList {
    public List<BlockPos> listFurances = new ArrayList<BlockPos>();

    @Override
    public List<BlockPos> get() {
        return this.listFurances;
    }

    @Override
    public void add(BlockPos pos) {
        int check = 0;
        for (int i = 0; i < this.listFurances.size(); ++i) {
            if (this.listFurances.get(i).m_123341_() != pos.m_123341_() || this.listFurances.get(i).m_123342_() != pos.m_123342_() || this.listFurances.get(i).m_123343_() != pos.m_123343_()) continue;
            ++check;
        }
        if (check == 0) {
            this.listFurances.add(pos);
        }
    }

    @Override
    public void remove(BlockPos pos) {
        for (int i = 0; i < this.listFurances.size(); ++i) {
            if (this.listFurances.get(i).m_123341_() != pos.m_123341_() || this.listFurances.get(i).m_123342_() != pos.m_123342_() || this.listFurances.get(i).m_123343_() != pos.m_123343_()) continue;
            this.listFurances.remove(i);
        }
    }
}

