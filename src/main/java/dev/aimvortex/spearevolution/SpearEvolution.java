
package dev.aimvortex.spearevolution;

import org.bukkit.plugin.java.JavaPlugin;

public final class SpearEvolution extends JavaPlugin {

    private static SpearEvolution instance;

    private PlayerDataManager playerDataManager;
    private SpearManager spearManager;
    private SpearPowerManager spearPowerManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        playerDataManager = new PlayerDataManager(this);
        spearManager = new SpearManager(this);
        spearPowerManager = new SpearPowerManager(this, spearManager);

        getServer().getPluginManager().registerEvents(
                new SpearListener(this, spearManager),
                this
        );

        getServer().getPluginManager().registerEvents(
                spearPowerManager,
                this
        );

        SpearCommand spearCommand =
                new SpearCommand(this, spearManager);

        if (getCommand("spear") != null) {
            getCommand("spear").setExecutor(spearCommand);
            getCommand("spear").setTabCompleter(spearCommand);
        } else {
            getLogger().severe("Command /spear is missing from plugin.yml!");
        }

        getLogger().info("================================");
        getLogger().info("       SpearEvolution");
        getLogger().info("       Author: AimVortex");
        getLogger().info("       Plugin enabled!");
        getLogger().info("================================");
    }

    @Override
    public void onDisable() {
        if (playerDataManager != null) {
            playerDataManager.save();
        }

        getLogger().info("SpearEvolution disabled.");
    }

    public static SpearEvolution getInstance() {
        return instance;
    }

    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }

    public SpearManager getSpearManager() {
        return spearManager;
    }

    public SpearPowerManager getSpearPowerManager() {
        return spearPowerManager;
    }
}
