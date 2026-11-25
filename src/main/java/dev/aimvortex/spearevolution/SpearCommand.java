

package dev.aimvortex.spearevolution;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public final class SpearCommand implements CommandExecutor, TabCompleter {

    private final SpearEvolution plugin;
    private final SpearManager spearManager;

    public SpearCommand(
            SpearEvolution plugin,
            SpearManager spearManager
    ) {
        this.plugin = plugin;
        this.spearManager = spearManager;
    }

    @Override
    public boolean onCommand(
            CommandSender sender,
            Command command,
            String label,
            String[] args
    ) {
        if (!sender.hasPermission("spearevolution.admin")) {
            sender.sendMessage(color("&cYou don't have permission."));
            return true;
        }

        if (args.length == 0) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give" -> giveSpear(sender, args);
            case "kills" -> setKills(sender, args);
            case "reload" -> reloadConfig(sender);
            default -> sendHelp(sender);
        }

        return true;
    }

    private void giveSpear(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(color("&cUsage: /spear give <player>"));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            sender.sendMessage(color("&cPlayer not found."));
            return;
        }

        int kills = plugin.getPlayerDataManager()
                .getKills(target.getUniqueId());

        ItemStack spear = spearManager.createSpear(kills);

        target.getInventory().addItem(spear);

        sender.sendMessage(color(
                "&aSpear given to &f" + target.getName()
        ));
    }

    private void setKills(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(color(
                    "&cUsage: /spear kills <player> <amount>"
            ));
            return;
        }

        Player target = Bukkit.getPlayerExact(args[1]);

        if (target == null) {
            sender.sendMessage(color("&cPlayer not found."));
            return;
        }

        int kills;

        try {
            kills = Integer.parseInt(args[2]);
        } catch (NumberFormatException exception) {
            sender.sendMessage(color(
                    "&cKills must be a valid number."
            ));
            return;
        }

        if (kills < 0) {
            sender.sendMessage(color("&cKills cannot be negative."));
            return;
        }

        plugin.getPlayerDataManager().setKills(
                target.getUniqueId(),
                kills
        );

        ItemStack current =
                target.getInventory().getItemInMainHand();

        if (spearManager.isEvolutionSpear(current)) {
            target.getInventory().setItemInMainHand(
                    spearManager.createSpear(kills)
            );
        }

        sender.sendMessage(color(
                "&aSet &f" + target.getName()
                + "&a's spear kills to &f" + kills
        ));
    }

    private void reloadConfig(CommandSender sender) {
        plugin.reloadConfig();

        sender.sendMessage(color(
                "&aSpearEvolution configuration reloaded!"
        ));
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(color("&b&lSpearEvolution Commands"));
        sender.sendMessage(color("&f/spear give <player>"));
        sender.sendMessage(color("&f/spear kills <player> <amount>"));
        sender.sendMessage(color("&f/spear reload"));
    }

    private String color(String message) {
        return ChatColor.translateAlternateColorCodes(
                '&',
                message
        );
    }

    @Override
    public List<String> onTabComplete(
            CommandSender sender,
            Command command,
            String alias,
            String[] args
    ) {
        if (!sender.hasPermission("spearevolution.admin")) {
            return List.of();
        }

        if (args.length == 1) {
            return List.of("give", "kills", "reload")
                    .stream()
                    .filter(value -> value.startsWith(
                            args[0].toLowerCase()
                    ))
                    .toList();
        }

        if (args.length == 2
                && !args[0].equalsIgnoreCase("reload")) {
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(
                            args[1].toLowerCase()
                    ))
                    .toList();
        }

        return List.of();
    }
}
