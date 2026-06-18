/*
 * Decompiled with CFR 0.152.
 */
package ironfurnaces.update;

import ironfurnaces.IronFurnaces;
import ironfurnaces.update.UpdateChecker;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Properties;

public class ThreadUpdateChecker
extends Thread {
    public ThreadUpdateChecker() {
        this.setName("Iron Furnaces Update Checker");
        this.setDaemon(true);
        this.start();
    }

    @Override
    public void run() {
        IronFurnaces.LOGGER.info("Starting Update Check...");
        try {
            URL newestURL = new URL("https://raw.githubusercontent.com/Qelifern/IronFurnaces/1.20.1/update/updateVersions.properties");
            Properties updateProperties = new Properties();
            updateProperties.load(new InputStreamReader(newestURL.openStream()));
            String currentMcVersion = "1.20.1";
            String newestVersionProp = updateProperties.getProperty(currentMcVersion);
            UpdateChecker.updateVersionInt = Integer.parseInt(newestVersionProp);
            UpdateChecker.updateVersionString = currentMcVersion + "-release" + newestVersionProp;
            int clientVersion = Integer.parseInt("418");
            if (UpdateChecker.updateVersionInt > clientVersion) {
                UpdateChecker.needsUpdateNotify = true;
            }
            IronFurnaces.LOGGER.info("Update Check done!");
        }
        catch (Exception e) {
            IronFurnaces.LOGGER.error("Update Check failed!", (Throwable)e);
            UpdateChecker.checkFailed = true;
        }
        if (!UpdateChecker.checkFailed) {
            if (UpdateChecker.needsUpdateNotify) {
                IronFurnaces.LOGGER.info("There is an Update for Iron Furnaces available!");
                IronFurnaces.LOGGER.info("Current Version: 1.20.1-418, newest Version: " + UpdateChecker.updateVersionString + "!");
                IronFurnaces.LOGGER.info("View the Changelog at https://raw.githubusercontent.com/Qelifern/IronFurnaces/1.20.1/ifchangelog.txt");
                IronFurnaces.LOGGER.info("Download at https://www.curseforge.com/minecraft/mc-mods/iron-furnaces");
            } else {
                IronFurnaces.LOGGER.info("Iron Furnaces is up to date!");
            }
        }
        UpdateChecker.threadFinished = true;
    }
}

