
package dev.aimvortex.spearevolution;

import org.bukkit.plugin.java.JavaPlugin;

public final class SpearEvolution extends JavaPlugin {

    private static SpearEvolution instance;

    private PlayerDataManager playerDataManager;

    @Override
    public void onEnable() {
        instance = this;

        saveDefaultConfig();

        playerDataManager = new PlayerDataManager(this);

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
}
