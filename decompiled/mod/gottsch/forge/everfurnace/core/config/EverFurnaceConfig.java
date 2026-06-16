/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraftforge.common.ForgeConfigSpec
 *  net.minecraftforge.common.ForgeConfigSpec$BooleanValue
 *  net.minecraftforge.common.ForgeConfigSpec$Builder
 *  net.minecraftforge.common.ForgeConfigSpec$IntValue
 *  net.minecraftforge.common.ForgeConfigSpec$LongValue
 *  net.minecraftforge.fml.ModLoadingContext
 *  net.minecraftforge.fml.config.IConfigSpec
 *  net.minecraftforge.fml.config.ModConfig$Type
 *  org.apache.commons.lang3.tuple.Pair
 */
package mod.gottsch.forge.everfurnace.core.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public final class EverFurnaceConfig {
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;
    public static final Client CLIENT;
    public static final ForgeConfigSpec CLIENT_SPEC;

    private EverFurnaceConfig() {
    }

    public static void register() {
        ModLoadingContext ctx = ModLoadingContext.get();
        ctx.registerConfig(ModConfig.Type.COMMON, (IConfigSpec)COMMON_SPEC);
        ctx.registerConfig(ModConfig.Type.CLIENT, (IConfigSpec)CLIENT_SPEC);
    }

    static {
        Pair commonPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = (Common)commonPair.getLeft();
        COMMON_SPEC = (ForgeConfigSpec)commonPair.getRight();
        Pair clientPair = new ForgeConfigSpec.Builder().configure(Client::new);
        CLIENT = (Client)clientPair.getLeft();
        CLIENT_SPEC = (ForgeConfigSpec)clientPair.getRight();
    }

    public static final class Common {
        public final ForgeConfigSpec.BooleanValue catchupEnabled;
        public final ForgeConfigSpec.LongValue maxCatchupTicks;
        public final ForgeConfigSpec.IntValue minDeltaThreshold;
        public final ForgeConfigSpec.BooleanValue notifyPlayerOnCatchup;
        public final ForgeConfigSpec.LongValue notificationCooldownTicks;
        public final ForgeConfigSpec.BooleanValue notifyOnLogin;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("EverFurnace \u2014 Common (server-side) configuration").push("catchup");
            this.catchupEnabled = builder.comment(new String[]{"Master toggle for the catch-up mechanic.", "Set to false to disable EverFurnace entirely and let vanilla handle all smelting."}).define("catchupEnabled", true);
            this.maxCatchupTicks = builder.comment(new String[]{"Maximum ticks of offline time to simulate in one catch-up pass.", "Default: 24000 (1 in-game day). Range: 1 \u2013 192000."}).defineInRange("maxCatchupTicks", 24000L, 1L, 192000L);
            this.minDeltaThreshold = builder.comment(new String[]{"Minimum tick gap required before catch-up logic fires.", "Default: 20 (1 second at normal TPS). Below this the furnace is considered", "actively ticking and vanilla handles smelting. Raise to require a larger gap."}).defineInRange("minDeltaThreshold", 20, 1, 72000);
            builder.pop().push("notifications");
            this.notifyPlayerOnCatchup = builder.comment(new String[]{"Send a chat message to the player when they open a furnace that", "cooked items while they were away. Requires Feature B to be implemented."}).define("notifyPlayerOnCatchup", true);
            this.notificationCooldownTicks = builder.comment(new String[]{"Minimum ticks between notification arms for a single furnace.", "Items cooked during the cooldown are still counted \u2014 they are batched", "into the existing pending notification rather than dropped.", "Set to 0 to disable the cooldown (notify on every catch-up pass).", "Range: 0 \u2013 72 000  |  Default: 200 (10 seconds)"}).defineInRange("notificationCooldownTicks", 200L, 0L, 72000L);
            this.notifyOnLogin = builder.comment(new String[]{"Deliver pending furnace notifications when the player logs in,", "in addition to when they open a furnace.", "Recommended on multiplayer servers where catch-up may fire before", "the owning player has connected.", "Default: true"}).define("notifyOnLogin", true);
            builder.pop();
        }
    }

    public static final class Client {
        public final ForgeConfigSpec.BooleanValue particleBurstEnabled;
        public final ForgeConfigSpec.BooleanValue soundCueEnabled;
        public final ForgeConfigSpec.BooleanValue lightFlickerEnabled;

        Client(ForgeConfigSpec.Builder builder) {
            builder.comment("EverFurnace \u2014 Client-side configuration").push("visuals");
            this.particleBurstEnabled = builder.comment(new String[]{"Spawn a flame/smoke particle burst at a furnace when catch-up completes", "and at least one item was cooked. Client-side only \u2014 no effect on servers."}).define("particleBurstEnabled", true);
            this.soundCueEnabled = builder.comment(new String[]{"Play a furnace crackle sound when catch-up completes", "and at least one item was cooked."}).define("soundCueEnabled", true);
            this.lightFlickerEnabled = builder.comment(new String[]{"Immediately sync the furnace LIT block state on the client", "when catch-up completes, so the light level updates without", "waiting for the next server block update."}).define("lightFlickerEnabled", true);
            builder.pop();
        }
    }
}

