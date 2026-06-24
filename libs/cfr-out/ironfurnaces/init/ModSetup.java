/*
 * Decompiled with CFR.
 */
package ironfurnaces.init;

import ironfurnaces.capability.CapabilityPlayerFurnacesList;
import ironfurnaces.capability.CapabilityPlayerShowConfig;
import ironfurnaces.util.RainbowEnabledCondition;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.core.Holder;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class ModSetup {
    public static final Map<Holder.Reference<Item>, Integer> SMOKING_BURNS = new HashMap<Holder.Reference<Item>, Integer>();
    public static final Map<Holder.Reference<Item>, Boolean> HAS_RECIPE = new HashMap<Holder.Reference<Item>, Boolean>();
    public static final Map<Holder.Reference<Item>, Boolean> HAS_RECIPE_SMOKING = new HashMap<Holder.Reference<Item>, Boolean>();
    public static final Map<Holder.Reference<Item>, Boolean> HAS_RECIPE_BLASTING = new HashMap<Holder.Reference<Item>, Boolean>();

    public static void init(FMLCommonSetupEvent event) {
        CraftingHelper.register((IConditionSerializer)RainbowEnabledCondition.Serializer.INSTANCE);
    }

    @SubscribeEvent
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        CapabilityPlayerShowConfig.register(event);
        CapabilityPlayerFurnacesList.register(event);
    }
}

