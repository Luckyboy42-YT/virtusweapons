package com.legendary.weapons;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class WeaponCommand implements CommandExecutor {

    private final LegendaryWeapons plugin;

    public WeaponCommand(LegendaryWeapons plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("virtus.weapons")) {
            sender.sendMessage("§cYou don't have permission to use this command!");
            return true;
        }

        if (args.length < 2) {

            sender.sendMessage("§cUsage: /legs <player> <seismic|shadow|light|inferno>");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("§cPlayer not found!");
            return true;
        }

        String weaponType = args[1].toLowerCase();
        ItemStack weapon = null;

        switch (weaponType) {
            case "seismic":
                weapon = plugin.getWeaponManager().createSeismicMace();
                break;
            case "shadow":
                weapon = plugin.getWeaponManager().createShadowBlade();
                break;
            case "light":
                weapon = plugin.getWeaponManager().createLightSentinel();
                break;
            case "inferno":
                weapon = plugin.getWeaponManager().createInfernoAxe();
                break;
            default:

                sender.sendMessage("§cInvalid weapon type! Available: seismic, shadow, light, inferno");
                return true;
        }

        if (weapon != null) {
            target.getInventory().addItem(weapon);

            sender.sendMessage("§aGiven " + weaponType + " weapon to " + target.getName());
            target.sendMessage("§aYou received a legendary weapon!");
        }

        return true;
    }
}
