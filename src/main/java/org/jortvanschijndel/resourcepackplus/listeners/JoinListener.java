package org.jortvanschijndel.resourcepackplus.listeners;

import net.kyori.adventure.resource.ResourcePackInfo;
import net.kyori.adventure.resource.ResourcePackRequest;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jortvanschijndel.resourcepackplus.ResourcepackPlus;
import java.net.URI;

public class JoinListener implements Listener {

    private final ResourcepackPlus plugin;

    public JoinListener(ResourcepackPlus plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        String resourcePackUrl = plugin.getResourcePackUrl();
        String resourcePackSha1 = plugin.getResourcePackSha1();

        if(resourcePackUrl == null || resourcePackSha1 == null) return;

        final ResourcePackInfo packInfo = ResourcePackInfo.resourcePackInfo()
                .uri(URI.create(resourcePackUrl))
                .hash(resourcePackSha1)
                .build();

        final ResourcePackRequest request = ResourcePackRequest.resourcePackRequest()
                .packs(packInfo)
                .prompt(Component.text("Please download the resource pack!"))
                .required(true)
                .build();

        // Send the resource pack request to the target audience
        player.sendResourcePacks(request);
    }

}
