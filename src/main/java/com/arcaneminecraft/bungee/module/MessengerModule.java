package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.channel.DiscordConnection;
import net.dv8tion.jda.core.entities.Message;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
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
     * Directly message a player.
     * @param from Message sender. If empty, the message is from the Server.
     * @param to Message receiver. If empty, the message is for the Server.
     * @param message Message content.
     * @return if message was successfully sent to both parties.
     */
    public boolean sendP2pMessage(CommandSender from, CommandSender to, String message) {
        if (from == null || to == null)
            return false;

        BaseComponent msg = ArcaneText.url(message);

        return sendP2pMessage(from, to, msg);
    }

    /**
     * Directly message a player.
     * @param from Message sender. If empty, the message is from the Server.
     * @param to Message receiver. If empty, the message is for the Server.
     * @param msg Message content. setColor() and setItalic() will be run on it.
     * @return if message was successfully sent to both parties.
     */
    // TODO: Implement all message functions
    public boolean sendP2pMessage(CommandSender from, CommandSender to, BaseComponent msg) {
        if (from == null || to == null)
            return false;

        msg.setColor(ArcaneColor.CONTENT);
        msg.setItalic(true);

        TextComponent gt = new TextComponent("> ");
        gt.setColor(ChatColor.DARK_GRAY);

        TextComponent colon = new TextComponent(": ");
        colon.setColor(ChatColor.DARK_GRAY);

        BaseComponent sendFrom = new TextComponent();
        BaseComponent toPerson = ArcaneText.playerComponentBungee(to);
        if (!(to instanceof ProxiedPlayer))
            toPerson.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/r "));
        BaseComponent toText = ArcaneText.translatable(
                to instanceof ProxiedPlayer ? ((ProxiedPlayer) from).getLocale() : null,
                "commands.message.to",
                toPerson
        );
        toText.setColor(ArcaneColor.HEADING);
        sendFrom.addExtra(gt);
        sendFrom.addExtra(toText);
        sendFrom.addExtra(colon);
        sendFrom.addExtra(msg);

        TextComponent sendTo = new TextComponent();
        BaseComponent fromPerson = ArcaneText.playerComponentBungee(from);
        if (!(from instanceof ProxiedPlayer))
            fromPerson.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/r "));
        BaseComponent fromText = ArcaneText.translatable(
                to instanceof ProxiedPlayer ? ((ProxiedPlayer) to).getLocale() : null,
                "commands.message.from",
                fromPerson
        );
        fromText.setColor(ArcaneColor.HEADING);
        sendTo.addExtra(gt);
        sendTo.addExtra(fromText);
        sendTo.addExtra(colon);
        sendTo.addExtra(msg);

        if (from instanceof ProxiedPlayer)
            ((ProxiedPlayer) from).sendMessage(ChatMessageType.SYSTEM, sendFrom);
        else
            from.sendMessage(sendFrom);
        if (to instanceof ProxiedPlayer)
            ((ProxiedPlayer) from).sendMessage(ChatMessageType.SYSTEM, sendTo);
        else
            to.sendMessage(sendTo);

        return true;
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
