package com.legendary.weapons;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;

public class LegendaryWeapons extends JavaPlugin implements Listener {

    private WeaponManager weaponManager;

    @Override
    public void onEnable() {
        this.weaponManager = new WeaponManager(this);

        getServer().getPluginManager().registerEvents(weaponManager, this);

        getCommand("legendaryweapons").setExecutor(new WeaponCommand(this));
        getCommand("legendaryweapons").setTabCompleter(new WeaponTabCompleter());

        getLogger().info("Legendary Weapons has been enabled!");
    }

    @Override
    public void onDisable() {
        getLogger().info("Legendary Weapons has been disabled!");
    }

    public WeaponManager getWeaponManager() {
        return weaponManager;
    }
}