/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.client.gui.screens.MenuScreens
 *  net.minecraft.world.inventory.MenuType
 *  net.minecraftforge.api.distmarker.Dist
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber
 *  net.minecraftforge.fml.common.Mod$EventBusSubscriber$Bus
 *  net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
 */
package ironfurnaces.init;

import ironfurnaces.gui.BlockWirelessEnergyHeaterScreen;
import ironfurnaces.gui.furnaces.BlockCopperFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockCrystalFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockDiamondFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockEmeraldFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockGoldFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockIronFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockMillionFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockNetheriteFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockObsidianFurnaceScreen;
import ironfurnaces.gui.furnaces.BlockSilverFurnaceScreen;
import ironfurnaces.gui.furnaces.other.BlockAllthemodiumFurnaceScreen;
import ironfurnaces.gui.furnaces.other.BlockUnobtainiumFurnaceScreen;
import ironfurnaces.gui.furnaces.other.BlockVibraniumFurnaceScreen;
import ironfurnaces.init.Registration;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@Mod.EventBusSubscriber(modid="ironfurnaces", value={Dist.CLIENT}, bus=Mod.EventBusSubscriber.Bus.MOD)
public class ClientSetup {
    public static void init(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.IRON_FURNACE_CONTAINER.get()), BlockIronFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.GOLD_FURNACE_CONTAINER.get()), BlockGoldFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.DIAMOND_FURNACE_CONTAINER.get()), BlockDiamondFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.EMERALD_FURNACE_CONTAINER.get()), BlockEmeraldFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.OBSIDIAN_FURNACE_CONTAINER.get()), BlockObsidianFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.CRYSTAL_FURNACE_CONTAINER.get()), BlockCrystalFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.NETHERITE_FURNACE_CONTAINER.get()), BlockNetheriteFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.COPPER_FURNACE_CONTAINER.get()), BlockCopperFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.SILVER_FURNACE_CONTAINER.get()), BlockSilverFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.MILLION_FURNACE_CONTAINER.get()), BlockMillionFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.HEATER_CONTAINER.get()), BlockWirelessEnergyHeaterScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.ALLTHEMODIUM_FURNACE_CONTAINER.get()), BlockAllthemodiumFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.VIBRANIUM_FURNACE_CONTAINER.get()), BlockVibraniumFurnaceScreen::new);
            MenuScreens.m_96206_((MenuType)((MenuType)Registration.UNOBTAINIUM_FURNACE_CONTAINER.get()), BlockUnobtainiumFurnaceScreen::new);
        });
    }
}

