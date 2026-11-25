
package dev.aimvortex.spearevolution;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class SpearManager {

    private final SpearEvolution plugin;
    private final NamespacedKey spearKey;
    private final NamespacedKey killsKey;
    private final NamespacedKey tierKey;

    private static final List<String> TIERS = List.of(
            "wooden",
            "stone",
            "diamond",
            "netherite"
    );

    public SpearManager(SpearEvolution plugin) {
        this.plugin = plugin;

        spearKey = new NamespacedKey(plugin, "evolution_spear");
        killsKey = new NamespacedKey(plugin, "spear_kills");
        tierKey = new NamespacedKey(plugin, "spear_tier");
    }

    public ItemStack createSpear(int kills) {
        String tier = getTier(kills);

        Material material = getMaterial(tier);

        ItemStack spear = new ItemStack(material);
        ItemMeta meta = spear.getItemMeta();

        if (meta == null) {
            return spear;
        }

        meta.setDisplayName(
                ChatColor.translateAlternateColorCodes(
                        '&',
                        "&b&l" + capitalize(tier) + " Spear"
                )
        );

        meta.setLore(createLore(tier, kills));

        meta.getPersistentDataContainer().set(
                spearKey,
                PersistentDataType.BYTE,
                (byte) 1
        );

        meta.getPersistentDataContainer().set(
                killsKey,
                PersistentDataType.INTEGER,
                kills
        );

        meta.getPersistentDataContainer().set(
                tierKey,
                PersistentDataType.STRING,
                tier
        );

        applyEnchantments(meta, kills);

        spear.setItemMeta(meta);

        return spear;
    }

    public ItemStack updateSpear(ItemStack oldSpear, int kills) {
        if (!isEvolutionSpear(oldSpear)) {
            return oldSpear;
        }

        return createSpear(kills);
    }

    public boolean isEvolutionSpear(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        if (!item.hasItemMeta()) {
            return false;
        }

        ItemMeta meta = item.getItemMeta();

        Byte value = meta.getPersistentDataContainer().get(
                spearKey,
                PersistentDataType.BYTE
        );

        return value != null && value == (byte) 1;
    }

    public int getSpearKills(ItemStack item) {
        if (!isEvolutionSpear(item)) {
            return 0;
        }

        return item.getItemMeta()
                .getPersistentDataContainer()
                .getOrDefault(
                        killsKey,
                        PersistentDataType.INTEGER,
                        0
                );
    }

    public String getTier(int kills) {
        String currentTier = "wooden";

        for (String tier : TIERS) {
            int requiredKills = plugin.getConfig().getInt(
                    "progression.tiers." + tier + ".required-kills",
                    0
            );

            if (kills >= requiredKills) {
                currentTier = tier;
            }
        }

        return currentTier;
    }

    public Material getMaterial(String tier) {
        String materialName = plugin.getConfig().getString(
                "progression.tiers." + tier + ".material",
                "TRIDENT"
        );

        Material material = Material.matchMaterial(materialName);

        if (material == null) {
            plugin.getLogger().warning(
                    "Invalid spear material: " + materialName
            );

            return Material.TRIDENT;
        }

        return material;
    }

    private void applyEnchantments(ItemMeta meta, int kills) {
        int sharpness = 0;
        int loyalty = 0;

        List<Map<?, ?>> milestones = plugin.getConfig()
                .getMapList("progression.enchantment-milestones");

        for (Map<?, ?> milestone : milestones) {
            int requiredKills = getNumber(milestone, "kills", 0);

            if (kills < requiredKills) {
                continue;
            }

            sharpness = Math.max(
                    sharpness,
                    getNumber(milestone, "sharpness", 0)
            );

            loyalty = Math.max(
                    loyalty,
                    getNumber(milestone, "loyalty", 0)
            );

            sharpness += getNumber(
                    milestone,
                    "sharpness-increase",
                    0
            );

            loyalty += getNumber(
                    milestone,
                    "loyalty-increase",
                    0
            );
        }

        if (sharpness > 0) {
            meta.addEnchant(
                    Enchantment.SHARPNESS,
                    sharpness,
                    true
            );
        }

        if (loyalty > 0) {
            meta.addEnchant(
                    Enchantment.LOYALTY,
                    loyalty,
                    true
            );
        }
    }

    private int getNumber(
            Map<?, ?> map,
            String key,
            int defaultValue
    ) {
        Object value = map.get(key);

        if (value instanceof Number number) {
            return number.intValue();
        }

        return defaultValue;
    }

    private List<String> createLore(String tier, int kills) {
        List<String> lore = new ArrayList<>();

        lore.add(ChatColor.GRAY + "Evolution Weapon");
        lore.add(ChatColor.DARK_GRAY + "--------------------");
        lore.add(ChatColor.AQUA + "Tier: " + capitalize(tier));
        lore.add(ChatColor.YELLOW + "Spear Kills: " + kills);
        lore.add(ChatColor.DARK_GRAY + "--------------------");
        lore.add(ChatColor.GRAY + "Your progression is saved.");
        lore.add(ChatColor.GRAY + "This spear cannot be crafted.");

        return lore;
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.substring(0, 1).toUpperCase()
                + text.substring(1).toLowerCase();
    }
}
