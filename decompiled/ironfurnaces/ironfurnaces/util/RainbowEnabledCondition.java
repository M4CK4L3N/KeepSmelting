/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  com.google.gson.JsonObject
 *  net.minecraft.resources.ResourceLocation
 *  net.minecraftforge.common.crafting.conditions.ICondition
 *  net.minecraftforge.common.crafting.conditions.ICondition$IContext
 *  net.minecraftforge.common.crafting.conditions.IConditionSerializer
 */
package ironfurnaces.util;

import com.google.gson.JsonObject;
import ironfurnaces.Config;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

public class RainbowEnabledCondition
implements ICondition {
    private static final ResourceLocation NAME = new ResourceLocation("ironfurnaces", "rainbow");

    public ResourceLocation getID() {
        return NAME;
    }

    public boolean test(ICondition.IContext context) {
        return (Boolean)Config.enableRainbowContent.get();
    }

    public String toString() {
        return "enabled(\"" + String.valueOf(Config.enableRainbowContent.get()) + "\")";
    }

    public static class Serializer
    implements IConditionSerializer<RainbowEnabledCondition> {
        public static final Serializer INSTANCE = new Serializer();

        public void write(JsonObject json, RainbowEnabledCondition value) {
        }

        public RainbowEnabledCondition read(JsonObject json) {
            return new RainbowEnabledCondition();
        }

        public ResourceLocation getID() {
            return NAME;
        }
    }
}

