/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.mojang.brigadier.CommandDispatcher
 *  net.minecraft.commands.CommandSourceStack
 *  net.minecraftforge.event.RegisterCommandsEvent
 *  net.minecraftforge.eventbus.api.SubscribeEvent
 */
package mod.gottsch.forge.everfurnace.core.command;

import com.mojang.brigadier.CommandDispatcher;
import mod.gottsch.forge.everfurnace.core.command.EverFurnaceCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class ModCommands {
    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        EverFurnaceCommand.register((CommandDispatcher<CommandSourceStack>)event.getDispatcher());
    }
}

