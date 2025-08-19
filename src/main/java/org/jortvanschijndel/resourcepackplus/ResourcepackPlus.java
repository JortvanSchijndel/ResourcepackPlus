package org.jortvanschijndel.resourcepackplus;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jortvanschijndel.resourcepackplus.commands.RppCommand;
import org.jortvanschijndel.resourcepackplus.storage.TokenStore;
import org.bstats.bukkit.Metrics;

import java.util.logging.Logger;

public class ResourcepackPlus extends JavaPlugin {

    private static ResourcepackPlus instance;
    private TokenStore tokenStore;
    private Logger log;

    public static ResourcepackPlus getInstance() {
        return instance;
    }

    public TokenStore getTokenStore() {
        return tokenStore;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.log = this.getLogger();

        // Initialize token store
        this.tokenStore = new TokenStore(getDataFolder());

        // Register command executor + tab completion + listener
        final PluginCommand cmd = getCommand("rpp");
        if (cmd != null) {
            RppCommand rpp = new RppCommand(this);
            cmd.setExecutor(rpp);
            cmd.setTabCompleter(rpp);

            // Register chat listener for Dropbox auth code
            getServer().getPluginManager().registerEvents(rpp, this);
        } else {
            log.severe("Command 'rpp' not found in plugin.yml! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        int pluginId = 26937;
        Metrics metrics = new Metrics(this, pluginId);

        log.info("ResourcepackPlus enabled.");
    }


    @Override
    public void onDisable() {
        log.info("ResourcepackPlus disabled.");
    }
}
