package org.jortvanschijndel.resourcepackplus.util;

import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;

public class Messaging {

    /**
     * Sends a message to the player using MiniMessage formatting
     * @param player the player to send the message to
     * @param message the message to send in MiniMessage formatting
     */
    public static void sendMini(CommandSender player, String message){
        player.sendMessage(MiniMessage.miniMessage().deserialize(message));
    }

}
