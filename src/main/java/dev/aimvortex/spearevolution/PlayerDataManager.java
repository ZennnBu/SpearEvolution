
package dev.aimvortex.spearevolution;

import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

public final class PlayerDataManager {

    private final JavaPlugin plugin;
    private final File dataFile;
    private YamlConfiguration data;

    public PlayerDataManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "players.yml");

        load();
    }

    private void load() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        if (!dataFile.exists()) {
            try {
                dataFile.createNewFile();
            } catch (IOException exception) {
                plugin.getLogger().severe(
                        "Could not create players.yml!"
                );
                exception.printStackTrace();
            }
        }

        data = YamlConfiguration.loadConfiguration(dataFile);
    }

    public int getKills(UUID uuid) {
        return Math.max(
                0,
                data.getInt("players." + uuid + ".kills", 0)
        );
    }

    public int addKill(UUID uuid) {
        int newKills = getKills(uuid) + 1;

        data.set("players." + uuid + ".kills", newKills);
        save();

        return newKills;
    }

    public void setKills(UUID uuid, int kills) {
        data.set(
                "players." + uuid + ".kills",
                Math.max(0, kills)
        );

        save();
    }

    public boolean hasPlayerData(UUID uuid) {
        return data.contains("players." + uuid);
    }

    public void save() {
        try {
            data.save(dataFile);
        } catch (IOException exception) {
            plugin.getLogger().severe(
                    "Could not save player progression data!"
            );
            exception.printStackTrace();
        }
    }
}
