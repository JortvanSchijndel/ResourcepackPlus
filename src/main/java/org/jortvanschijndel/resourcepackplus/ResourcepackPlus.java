package org.jortvanschijndel.resourcepackplus;

import org.bukkit.Bukkit;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.jortvanschijndel.resourcepackplus.commands.RppCommand;
import org.jortvanschijndel.resourcepackplus.listeners.JoinListener;
import org.jortvanschijndel.resourcepackplus.storage.PackStore;
import org.jortvanschijndel.resourcepackplus.storage.TokenStore;
import org.bstats.bukkit.Metrics;
import org.jortvanschijndel.resourcepackplus.util.ServerPropertiesUtil;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

public class ResourcepackPlus extends JavaPlugin {

    private static ResourcepackPlus instance;
    private TokenStore tokenStore;
    private PackStore packStore;
    private Logger log;
    private String resourcePackUrl;
    private String resourcePackSha1;

    public static ResourcepackPlus getInstance() {
        return instance;
    }

    public TokenStore getTokenStore() {
        return tokenStore;
    }
    public PackStore getPackStore() {
        return packStore;
    }

    @Override
    public void onEnable() {
        instance = this;
        this.saveDefaultConfig();
        this.log = this.getLogger();

        // Initialize token store
        this.tokenStore = new TokenStore(getDataFolder());

        // Initialize pack store
        this.packStore = new PackStore(getDataFolder());

        // Register command executor + tab completion + listener
        final PluginCommand cmd = getCommand("rpp");
        if (cmd != null) {
            RppCommand rpp = new RppCommand(this);
            cmd.setExecutor(rpp);
            cmd.setTabCompleter(rpp);

            // Register chat listener for Dropbox auth code
            getServer().getPluginManager().registerEvents(rpp, this);
            getServer().getPluginManager().registerEvents(new JoinListener(this), this);
        } else {
            log.severe("Command 'rpp' not found in plugin.yml! Disabling plugin.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        resourcePackUrl = packStore.getUrl();
        resourcePackSha1 = packStore.getSha1();

        if(resourcePackUrl != null){
            log.info("Found resource pack in server.properties: " + resourcePackUrl);
        }

        int pluginId = 26937;
        Metrics metrics = new Metrics(this, pluginId);


        log.info("ResourcepackPlus enabled.");
    }


    @Override
    public void onDisable() {
        log.info("ResourcepackPlus disabled.");
    }

    public String getResourcePackUrl() {
        return resourcePackUrl;
    }

    public String getResourcePackSha1() {
        return resourcePackSha1;
    }

    public void setResourcePackUrl(String url) {
        this.resourcePackUrl = url;
    }

    public void setResourcePackSha1(String sha1) {
        this.resourcePackSha1 = sha1;
    }


}
