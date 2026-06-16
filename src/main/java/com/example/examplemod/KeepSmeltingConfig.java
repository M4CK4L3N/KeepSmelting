package com.example.examplemod;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import org.apache.commons.lang3.tuple.Pair;

public final class KeepSmeltingConfig {
    public static final Common COMMON;
    public static final ForgeConfigSpec COMMON_SPEC;

    static {
        Pair<Common, ForgeConfigSpec> commonPair = new ForgeConfigSpec.Builder().configure(Common::new);
        COMMON = commonPair.getLeft();
        COMMON_SPEC = commonPair.getRight();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, COMMON_SPEC);
    }

    public static final class Common {
        public final ForgeConfigSpec.BooleanValue catchupEnabled;
        public final ForgeConfigSpec.LongValue maxCatchupTicks;
        public final ForgeConfigSpec.IntValue minDeltaThreshold;
        public final ForgeConfigSpec.EnumValue<DebugMode> debugMode;

        Common(ForgeConfigSpec.Builder builder) {
            builder.comment("KeepSmelting — offline furnace smelting").push("catchup");
            this.catchupEnabled = builder
                .comment("Master toggle. False = vanilla smelting only.")
                .define("catchupEnabled", true);
            this.maxCatchupTicks = builder
                .comment("Max offline ticks to simulate per catch-up. Default: 24000 (1 MC day). Range: 1 – 192000.")
                .defineInRange("maxCatchupTicks", 24000L, 1L, 192000L);
            this.minDeltaThreshold = builder
                .comment("Min tick gap before catchup fires. Default: 20 (1 sec). Below this = vanilla tick.")
                .defineInRange("minDeltaThreshold", 20, 1, 72000);
            this.debugMode = builder
                .comment("Debug output mode: OFF, CHAT (send to nearby players), LOG (print to Minecraft log).")
                .defineEnum("debugMode", DebugMode.OFF);
            builder.pop();
        }
    }

    public enum DebugMode {
        OFF,
        CHAT,
        LOG
    }
}
