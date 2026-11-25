
package dev.aimvortex.spearevolution;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SpearListener implements Listener {

    private final SpearEvolution plugin;
    private final SpearManager spearManager;

    private final Map<UUID, List<ItemStack>> pendingSpears =
            new HashMap<>();

    public SpearListener(
            SpearEvolution plugin,
            SpearManager spearManager
    ) {
        this.plugin = plugin;
        this.spearManager = spearManager;
    }

    @EventHandler
    public void onFirstJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        if (!plugin.getConfig().getBoolean(
                "settings.give-spear-on-first-join",
                true
        )) {
            return;
        }

        if (plugin.getPlayerDataManager()
                .hasPlayerData(player.getUniqueId())) {
            return;
        }

        plugin.getPlayerDataManager().setKills(
                player.getUniqueId(),
                0
        );

        ItemStack spear = spearManager.createSpear(0);
        player.getInventory().addItem(spear);

        player.sendMessage(color(
                plugin.getConfig().getString(
                        "messages.prefix",
                        "&8[&bSpearEvolution&8] "
                )
                + plugin.getConfig().getString(
                        "messages.first-spear",
                        "&aYou received your Wooden Spear!"
                )
        ));
    }

    @EventHandler
    public void onPlayerKill(PlayerDeathEvent event) {
        Player victim = event.getEntity();
        Player killer = victim.getKiller();

        if (killer == null || killer.equals(victim)) {
            return;
        }

        ItemStack weapon =
                killer.getInventory().getItemInMainHand();

        if (!spearManager.isEvolutionSpear(weapon)) {
            return;
        }

        int kills = plugin.getPlayerDataManager().addKill(
                killer.getUniqueId()
        );

        ItemStack upgradedSpear =
                spearManager.updateSpear(weapon, kills);

        killer.getInventory().setItemInMainHand(upgradedSpear);

        String newTier = spearManager.getTier(kills);

        String oldTier = weapon.getItemMeta()
                .getPersistentDataContainer()
                .get(
                        new NamespacedKey(plugin, "spear_tier"),
                        PersistentDataType.STRING
                );

        if (oldTier == null || !oldTier.equals(newTier)) {
            killer.sendMessage(color(
                    plugin.getConfig().getString(
                            "messages.prefix",
                            "&8[&bSpearEvolution&8] "
                    )
                    + plugin.getConfig().getString(
                            "messages.spear-upgraded",
                            "&bYour spear has evolved to &f%tier%&b!"
                    ).replace("%tier%", newTier)
            ));
        } else {
            killer.sendMessage(color(
                    plugin.getConfig().getString(
                            "messages.prefix",
                            "&8[&bSpearEvolution&8] "
                    )
                    + plugin.getConfig().getString(
                            "messages.kill-progress",
                            "&7Spear kills: &e%kills%"
                    ).replace("%kills%", String.valueOf(kills))
            ));
        }
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        if (!plugin.getConfig().getBoolean(
                "settings.prevent-spear-drop-on-death",
                true
        )) {
            return;
        }

        if (event.getKeepInventory()) {
            return;
        }

        Player player = event.getEntity();
        List<ItemStack> protectedItems = new ArrayList<>();

        for (ItemStack item : player.getInventory().getContents()) {
            if (spearManager.isEvolutionSpear(item)) {
                protectedItems.add(item.clone());
            }
        }

        if (protectedItems.isEmpty()) {
            return;
        }

        event.getDrops().removeIf(
                spearManager::isEvolutionSpear
        );

        pendingSpears.put(
                player.getUniqueId(),
                protectedItems
        );
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        UUID uuid = event.getPlayer().getUniqueId();

        List<ItemStack> items = pendingSpears.remove(uuid);

        if (items == null || items.isEmpty()) {
            return;
        }

        Player player = event.getPlayer();

        for (ItemStack item : items) {
            Map<Integer, ItemStack> leftovers =
                    player.getInventory().addItem(item);

            if (!leftovers.isEmpty()) {
                pendingSpears
                        .computeIfAbsent(
                                uuid,
                                key -> new ArrayList<>()
                        )
                        .addAll(leftovers.values());

                player.sendMessage(color(
                        "&cYour protected spear is waiting. "
                        + "Make space in your inventory!"
                ));
            }
        }
    }

    @EventHandler
    public void onCraft(PrepareItemCraftEvent event) {
        ItemStack result = event.getInventory().getResult();

        if (isSpearMaterial(result)) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler
    public void onEnchant(EnchantItemEvent event) {
        if (!plugin.getConfig().getBoolean(
                "settings.prevent-enchanting-table",
                true
        )) {
            return;
        }

        if (spearManager.isEvolutionSpear(event.getItem())) {
            event.setCancelled(true);

            event.getEnchanter().sendMessage(color(
                    plugin.getConfig().getString(
                            "messages.no-enchant",
                            "&cYou cannot enchant your evolution spear!"
                    )
            ));
        }
    }

    @EventHandler
    public void onAnvilPrepare(PrepareAnvilEvent event) {
        if (!plugin.getConfig().getBoolean(
                "settings.prevent-anvil-enchanting",
                true
        )) {
            return;
        }

        ItemStack first = event.getInventory().getItem(0);
        ItemStack second = event.getInventory().getItem(1);

        if (spearManager.isEvolutionSpear(first)
                || spearManager.isEvolutionSpear(second)) {
            event.setResult(null);
        }
    }

    private boolean isSpearMaterial(ItemStack item) {
        if (item == null || item.getType().isAir()) {
            return false;
        }

        for (String tier : List.of(
                "wooden",
                "stone",
                "diamond",
                "netherite"
        )) {
            Material material = spearManager.getMaterial(tier);

            if (item.getType() == material) {
                return true;
            }
        }

        return false;
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                message == null ? "" : message
        );
    }
    }
