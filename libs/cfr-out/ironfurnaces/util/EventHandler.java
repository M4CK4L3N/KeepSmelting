/*
 * Decompiled with CFR.
 */
package ironfurnaces.util;

import ironfurnaces.capability.PlayerFurnacesListProvider;
import ironfurnaces.capability.PlayerShowConfigProvider;
import ironfurnaces.init.Registration;
import ironfurnaces.tileentity.furnaces.BlockMillionFurnaceTile;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.level.ExplosionEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.FORGE)
public class EventHandler {
    @SubscribeEvent
    public static void playerEvent(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(new ResourceLocation("ironfurnaces", "show_config"), (ICapabilityProvider)new PlayerShowConfigProvider());
            event.addCapability(new ResourceLocation("ironfurnaces", "furnaces_list"), (ICapabilityProvider)new PlayerFurnacesListProvider());
        }
    }

    @SubscribeEvent
    public static void explosionEvent(ExplosionEvent event) {
        List list = event.getExplosion().m_46081_();
        for (BlockPos pos : list) {
            Level world = event.getLevel();
            if (!(world.m_7702_(pos) instanceof BlockMillionFurnaceTile)) continue;
            event.getExplosion().m_46081_().remove(pos);
            world.m_46747_(pos);
            world.m_7471_(pos, false);
            world.m_7967_((Entity)new ItemEntity(world, (double)pos.m_123341_(), (double)((float)pos.m_123342_() + 6.0f), (double)pos.m_123343_(), new ItemStack((ItemLike)Registration.RAINBOW_COAL.get())));
        }
    }
}

