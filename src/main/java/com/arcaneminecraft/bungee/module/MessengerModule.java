package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.channel.DiscordConnection;
import net.dv8tion.jda.core.entities.Message;
import net.md_5.bungee.api.connection.ProxiedPlayer;

public class MessengerModule {

    private DiscordConnection getDC() {
        return ArcaneBungee.getInstance().getDiscordConnection();
    }

    private DiscordUserModule getDUModule() {
        return ArcaneBungee.getInstance().getDiscordUserModule();
    }

    private MinecraftPlayerModule getMPModule() {
        return ArcaneBungee.getInstance().getMinecraftPlayerModule();
    }


    /**
     * Message Player.
     * @param from Message sender. If empty, the message is from the Server.
     * @param to Message receiver. If empty, the message is for the Server.
     * @param msg Message content.
     * @return if message was successfully sent, or if both sender and receiver are null
     */
    // TODO: Implement all message functions
    public boolean messagePlayer(ProxiedPlayer from, ProxiedPlayer to, String msg) {
        if (from == null && to == null)
            return false;



        return true;
    }

    public boolean messagePlayer(String from, String to, String msg) {
        return false;
    }

    public void sendToMinecraft(Message m) {

    }

    public void chatToDiscord(ProxiedPlayer p, String msg) {
        getDC().chatToDiscord(escapeNames(p.getDisplayName()), p.getUniqueId(), msg);
    }

    public void sendMetaToDiscord(String msg) {
        getDC().metaToDiscord(msg);
    }

    public void chatToMinecraft(Message m) {
        String name = getMPModule().getDisplayName(getDUModule().getMinecraftUuid(m.getAuthor().getIdLong()));
        getDC().chatToMinecraft(name, m);
    }

    private String escapeNames(String name) {
        // HTTP/1.1 400 prevention: Discord disallows bot username with "clyde"
        if (name.toLowerCase().contains("clyde")) {
            int index = name.toLowerCase().indexOf("clyde");
            char[] nc = name.toCharArray();
            if (nc[index + 4] == 'E')
                nc[index + 4] = '\u0395'; // Epsilon "E"
            else if (nc[index + 1] == 'l')
                nc[index+1] = 'I'; // Upper-case i "I"
            else if (nc[index + 4] == 'e')
                nc[index + 4] = '\u212E'; // Estimate sign "e"

            name = String.copyValueOf(nc);
        }
        return name;
    }

    private String escapeText(String text) {
        return text.replaceAll("([\\\\*_~])", "\\\\$1");
    }
}
