
package dev.aimvortex.spearevolution;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class SpearPowerManager implements Listener {

    private final SpearEvolution plugin;
    private final SpearManager spearManager;

    private final Map<String, Long> cooldowns = new HashMap<>();

    public SpearPowerManager(
            SpearEvolution plugin,
            SpearManager spearManager
    ) {
        this.plugin = plugin;
        this.spearManager = spearManager;
    }

    @EventHandler
    public void onSpearUse(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND) {
            return;
        }

        Action action = event.getAction();

        if (action != Action.RIGHT_CLICK_AIR
                && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        if (!plugin.getConfig().getBoolean(
                "powers.enabled",
                true
        )) {
            return;
        }

        Player player = event.getPlayer();
        ItemStack item = event.getItem();

        if (!spearManager.isEvolutionSpear(item)) {
            return;
        }

        int kills = spearManager.getSpearKills(item);

        String power = getUnlockedPower(kills);

        if (power == null) {
            return;
        }

        event.setCancelled(true);

        if (!isReady(player, power)) {
            player.sendMessage(color(
                    "&cPower is on cooldown!"
            ));
            return;
        }

        usePower(player, power);
        startCooldown(player, power);

        player.sendMessage(color(
                "&b&lSpear Power &8» &f"
                        + capitalize(power)
                        + " &aactivated!"
        ));
    }

    private String getUnlockedPower(int kills) {
        String[] powers = {
                "lightning-spear",
                "flame-spear",
                "wind-spear",
                "shadow-spear"
        };

        String unlocked = null;

        for (String power : powers) {
            String path = "powers." + power;

            if (!plugin.getConfig().getBoolean(
                    path + ".enabled",
                    false
            )) {
                continue;
            }

            int requiredKills = plugin.getConfig().getInt(
                    path + ".unlock-kills",
                    Integer.MAX_VALUE
            );

            if (kills >= requiredKills) {
                unlocked = power;
            }
        }

        return unlocked;
    }

    private void usePower(Player player, String power) {
        switch (power) {
            case "lightning-spear" -> lightningPower(player);
            case "flame-spear" -> flamePower(player);
            case "wind-spear" -> windPower(player);
            case "shadow-spear" -> shadowPower(player);
            default -> {
            }
        }
    }

    private void lightningPower(Player player) {
        LivingEntity target = getTarget(player);

        if (target == null) {
            player.sendMessage(color(
                    "&cLook at a target to use Lightning Spear!"
            ));
            return;
        }

        Location location = target.getLocation();

        player.getWorld().strikeLightningEffect(location);

        target.damage(4.0, player);
    }

    private void flamePower(Player player) {
        LivingEntity target = getTarget(player);

        if (target == null) {
            player.sendMessage(color(
                    "&cLook at a target to use Flame Spear!"
            ));
            return;
        }

        target.setFireTicks(60);
        target.damage(2.0, player);
    }

    private void windPower(Player player) {
        Vector direction = player.getLocation()
                .getDirection()
                .normalize();

        player.setVelocity(
                direction.multiply(1.5)
                        .setY(0.8)
        );

        for (Entity entity : player.getNearbyEntities(
                5.0, 3.0, 5.0
        )) {
            if (!(entity instanceof LivingEntity target)) {
                continue;
            }

            if (target.equals(player)) {
                continue;
            }

            Vector push = target.getLocation()
                    .toVector()
                    .subtract(player.getLocation().toVector())
                    .normalize()
                    .multiply(1.2);

            push.setY(0.5);

            target.setVelocity(push);
        }
    }

    private void shadowPower(Player player) {
        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.INVISIBILITY,
                        100,
                        0,
                        false,
                        false,
                        true
                )
        );

        player.addPotionEffect(
                new PotionEffect(
                        PotionEffectType.SPEED,
                        100,
                        1,
                false,
                false,
                true
                )
        );
    }

    private LivingEntity getTarget(Player player) {
        Entity target = player.getWorld().rayTraceEntities(
                player.getEyeLocation(),
                player.getEyeLocation().getDirection(),
                20.0,
                0.5,
                entity -> entity instanceof LivingEntity
                        && !entity.equals(player)
        ) == null
                ? null
                : player.getWorld().rayTraceEntities(
                        player.getEyeLocation(),
                        player.getEyeLocation().getDirection(),
                        20.0,
                        0.5,
                        entity -> entity instanceof LivingEntity
                                && !entity.equals(player)
                ).getHitEntity();

        if (target instanceof LivingEntity living) {
            return living;
        }

        return null;
    }

    private String cooldownKey(UUID uuid, String power) {
        return uuid + ":" + power;
    }

    private boolean isReady(Player player, String power) {
        String path = "powers." + power;

        int cooldownSeconds = plugin.getConfig().getInt(
                path + ".cooldown-seconds",
                30
        );

        long cooldownMillis = cooldownSeconds * 1000L;

        long lastUsed = cooldowns.getOrDefault(
                cooldownKey(player.getUniqueId(), power),
                0L
        );

        return System.currentTimeMillis() - lastUsed
                >= cooldownMillis;
    }

    private void startCooldown(Player player, String power) {
        cooldowns.put(
                cooldownKey(player.getUniqueId(), power),
                System.currentTimeMillis()
        );
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                message
        );
    }

    private String capitalize(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        return text.substring(0, 1).toUpperCase()
                + text.substring(1).toLowerCase();
    }
              }
