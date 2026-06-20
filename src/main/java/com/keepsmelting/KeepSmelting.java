package com.keepsmelting;

import com.keepsmelting.api.CatchupHandlerRegistry;
import com.keepsmelting.command.KeepSmeltingCommand;
import com.keepsmelting.internal.ironfurnaces.handler.IronFurnaceCatchupHandler;
import ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(KeepSmelting.MOD_ID)
public class KeepSmelting {
    public static final String MOD_ID = "keepsmelting";
    public static final Logger LOGGER = LoggerFactory.getLogger("KeepSmelting");

    @SuppressWarnings("removal")
    public KeepSmelting() {
        KeepSmeltingConfig.register();

        // Register built-in handlers
        try {
            Class.forName("ironfurnaces.tileentity.furnaces.BlockIronFurnaceTileBase");
            CatchupHandlerRegistry.register(BlockIronFurnaceTileBase.class, IronFurnaceCatchupHandler.INSTANCE);
            LOGGER.info("Iron Furnaces support registered");
        } catch (ClassNotFoundException e) {
            LOGGER.info("Iron Furnaces not detected — skipping support");
        }

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::onCommonSetup);

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.addListener(this::onRegisterCommands);
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("KeepSmelting initialized — offline smelting enabled");
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        KeepSmeltingCommand.register(event.getDispatcher());
        LOGGER.info("KeepSmelting commands registered");
    }
}
