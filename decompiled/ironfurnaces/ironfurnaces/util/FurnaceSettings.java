/*
 * Decompiled with CFR 0.152.
 * 
 * Could not load the following classes:
 *  net.minecraft.nbt.CompoundTag
 */
package ironfurnaces.util;

import ironfurnaces.Config;
import ironfurnaces.IronFurnaces;
import net.minecraft.nbt.CompoundTag;

public class FurnaceSettings {
    public int[] settings = new int[]{0, 0, 0, 0, 0, 0};
    public int[] autoIO = new int[]{0, 0};
    public int[] redstoneSettings = new int[]{0, 0};
    public int augmentGUI = 0;
    public int autoSplit = 0;

    public int get(int index) {
        try {
            switch (index) {
                case 0: {
                    return this.settings[0];
                }
                case 1: {
                    return this.settings[1];
                }
                case 2: {
                    return this.settings[2];
                }
                case 3: {
                    return this.settings[3];
                }
                case 4: {
                    return this.settings[4];
                }
                case 5: {
                    return this.settings[5];
                }
                case 6: {
                    return this.autoIO[0];
                }
                case 7: {
                    return this.autoIO[1];
                }
                case 8: {
                    return this.redstoneSettings[0];
                }
                case 9: {
                    return this.redstoneSettings[1];
                }
                case 10: {
                    return this.augmentGUI;
                }
                case 11: {
                    return this.autoSplit;
                }
            }
            return 0;
        }
        catch (ArrayIndexOutOfBoundsException e) {
            if (((Boolean)Config.showErrors.get()).booleanValue()) {
                IronFurnaces.LOGGER.error("Something went wrong.");
                for (int i = 0; i < e.getStackTrace().length; ++i) {
                    IronFurnaces.LOGGER.error(e.getStackTrace()[i].toString());
                }
            }
            return 0;
        }
    }

    public void set(int index, int value) {
        block17: {
            try {
                switch (index) {
                    case 0: {
                        this.settings[0] = value;
                        break;
                    }
                    case 1: {
                        this.settings[1] = value;
                        break;
                    }
                    case 2: {
                        this.settings[2] = value;
                        break;
                    }
                    case 3: {
                        this.settings[3] = value;
                        break;
                    }
                    case 4: {
                        this.settings[4] = value;
                        break;
                    }
                    case 5: {
                        this.settings[5] = value;
                        break;
                    }
                    case 6: {
                        this.autoIO[0] = value;
                        break;
                    }
                    case 7: {
                        this.autoIO[1] = value;
                        break;
                    }
                    case 8: {
                        this.redstoneSettings[0] = value;
                        break;
                    }
                    case 9: {
                        this.redstoneSettings[1] = value;
                        break;
                    }
                    case 10: {
                        this.augmentGUI = value;
                        break;
                    }
                    case 11: {
                        this.autoSplit = value;
                        break;
                    }
                }
                this.onChanged();
            }
            catch (ArrayIndexOutOfBoundsException e) {
                if (!((Boolean)Config.showErrors.get()).booleanValue()) break block17;
                IronFurnaces.LOGGER.error("Something went wrong.");
                for (int i = 0; i < e.getStackTrace().length; ++i) {
                    IronFurnaces.LOGGER.error(e.getStackTrace()[i].toString());
                }
            }
        }
    }

    public int size() {
        return this.settings.length + this.autoIO.length + this.redstoneSettings.length + 2;
    }

    public void read(CompoundTag tag) {
        this.settings = tag.m_128465_("Settings");
        this.autoIO = tag.m_128465_("AutoIO");
        this.redstoneSettings = tag.m_128465_("Redstone");
        this.augmentGUI = tag.m_128451_("AugmentGUI");
        this.autoSplit = tag.m_128451_("AutoSplit");
        this.onChanged();
    }

    public void write(CompoundTag tag) {
        tag.m_128385_("Settings", this.settings);
        tag.m_128385_("AutoIO", this.autoIO);
        tag.m_128385_("Redstone", this.redstoneSettings);
        tag.m_128405_("AugmentGUI", this.augmentGUI);
        tag.m_128405_("AutoSplit", this.autoSplit);
    }

    public void onChanged() {
    }
}

