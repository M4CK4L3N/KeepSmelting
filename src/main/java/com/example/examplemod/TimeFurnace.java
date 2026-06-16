package com.example.examplemod;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(TimeFurnace.MOD_ID)
public class TimeFurnace {
    public static final String MOD_ID = "timefurnace";
    public static final Logger LOGGER = LoggerFactory.getLogger("TimeFurnace");

    @SuppressWarnings("removal")
    public TimeFurnace() {
        TimeFurnaceConfig.register();

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("TimeFurnace initialized — offline smelting enabled");
    }
}
