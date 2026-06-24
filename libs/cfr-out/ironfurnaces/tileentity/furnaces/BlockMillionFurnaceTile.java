/*
 * Decompiled with CFR.
 */
package ironfurnaces.tileentity.furnaces;

import com.google.common.collect.Lists;
import ironfurnaces.Config;
import ironfurnaces.container.furnaces.BlockMillionFurnaceContainer;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeConfigSpec;

public class BlockMillionFurnaceTile
extends BlockIronFurnaceTileBase {
    public List<BlockIronFurnaceTileBase> furnaces = Lists.newArrayList();
    public List<BlockPos> furnaces_to_load = Lists.newArrayList();

    public BlockMillionFurnaceTile(BlockPos pos, BlockState state) {
        super((BlockEntityType)Registration.MILLION_FURNACE_TILE.get(), pos, state);
    }

    @Override
    public void m_183515_(CompoundTag tag) {
        super.m_183515_(tag);
        CompoundTag furnaces = new CompoundTag();
        for (int i = 0; i < this.furnaces.size(); ++i) {
            CompoundTag tag2 = new CompoundTag();
            tag2.m_128405_("X", this.furnaces.get(i).m_58899_().m_123341_());
            tag2.m_128405_("Y", this.furnaces.get(i).m_58899_().m_123342_());
            tag2.m_128405_("Z", this.furnaces.get(i).m_58899_().m_123343_());
            furnaces.m_128365_("Furnace" + i, (Tag)tag2);
        }
        tag.m_128365_("Furnaces", (Tag)furnaces);
    }

    @Override
    public void m_142466_(CompoundTag tag) {
        super.m_142466_(tag);
        CompoundTag furnaces = tag.m_128469_("Furnaces");
        for (int i = 0; i < furnaces.m_128440_(); ++i) {
            CompoundTag furnace = furnaces.m_128469_("Furnace" + i);
            this.furnaces_to_load.add(new BlockPos(furnace.m_128451_("X"), furnace.m_128451_("Y"), furnace.m_128451_("Z")));
        }
    }

    @Override
    public ForgeConfigSpec.IntValue getCookTimeConfig() {
        return Config.millionFurnaceSpeed;
    }

    @Override
    public String IgetName() {
        return "container.ironfurnaces.million_furnace";
    }

    @Override
    public AbstractContainerMenu IcreateMenu(int i, Inventory playerInventory, Player playerEntity) {
        return new BlockMillionFurnaceContainer(i, this.f_58857_, this.f_58858_, playerInventory, playerEntity);
    }

    @Override
    public int getTier() {
        return (Integer)Config.millionFurnaceTier.get();
    }
}

