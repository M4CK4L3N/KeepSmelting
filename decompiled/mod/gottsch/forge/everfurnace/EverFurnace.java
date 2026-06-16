/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity
 *  net.minecraft.world.level.block.entity.BlockEntityType
 *  net.minecraftforge.common.MinecraftForge
 *  net.minecraftforge.fml.common.Mod
 */
package mod.gottsch.forge.everfurnace;

import mod.gottsch.forge.everfurnace.api.EverFurnaceApi;
import mod.gottsch.forge.everfurnace.core.catchup.CampfireCatchupHandler;
import mod.gottsch.forge.everfurnace.core.catchup.FurnaceCatchupHandler;
import mod.gottsch.forge.everfurnace.core.command.ModCommands;
import mod.gottsch.forge.everfurnace.core.config.EverFurnaceConfig;
import mod.gottsch.forge.everfurnace.core.network.ModNetwork;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;

@Mod(value="everfurnace")
public class EverFurnace {
    public static final String MOD_ID = "everfurnace";

    public EverFurnace() {
        EverFurnaceConfig.register();
        ModNetwork.register();
        EverFurnaceApi.bindConfig(() -> (Boolean)EverFurnaceConfig.COMMON.catchupEnabled.get(), () -> (Long)EverFurnaceConfig.COMMON.maxCatchupTicks.get(), () -> (Integer)EverFurnaceConfig.COMMON.minDeltaThreshold.get());
        FurnaceCatchupHandler furnaceHandler = new FurnaceCatchupHandler();
        CampfireCatchupHandler campfireHandler = new CampfireCatchupHandler();
        EverFurnaceApi.registerHandler(BlockEntityType.f_58917_, furnaceHandler);
        EverFurnaceApi.registerHandler(BlockEntityType.f_58907_, furnaceHandler);
        EverFurnaceApi.registerHandler(BlockEntityType.f_58906_, furnaceHandler);
        EverFurnaceApi.registerHandler(BlockEntityType.f_58911_, campfireHandler);
        EverFurnaceApi.registerFallback(be -> be instanceof AbstractFurnaceBlockEntity, furnaceHandler);
        MinecraftForge.EVENT_BUS.register(ModCommands.class);
    }
}

