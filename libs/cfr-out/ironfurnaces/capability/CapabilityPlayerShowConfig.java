/*
 * Decompiled with CFR.
 */
package ironfurnaces.capability;

import ironfurnaces.capability.IPlayerShowConfig;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;

public class CapabilityPlayerShowConfig {
    public static final Capability<IPlayerShowConfig> CONFIG = CapabilityManager.get((CapabilityToken)new CapabilityToken<IPlayerShowConfig>(){});

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IPlayerShowConfig.class);
    }
}

