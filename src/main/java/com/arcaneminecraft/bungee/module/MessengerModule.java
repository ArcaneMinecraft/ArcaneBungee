package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.channel.DiscordBot;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.awt.*;
import java.util.List;
import java.util.UUID;

public class MessengerModule {

    private DiscordBot getDB() {
        return DiscordBot.getInstance();
    }

    private DiscordUserModule getDUModule() {
        return ArcaneBungee.getInstance().getDiscordUserModule();
    }

    /**
     * Directly message a player.
     * @param from Message sender. If empty, the message is from the Server.
     * @param to Message receiver. If empty, the message is for the Server.
     * @param msg Message content. setColor() and setItalic() will be run on it.
     */
    public void sendP2pMessage(CommandSender from, CommandSender to, BaseComponent msg) {
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
            ((ProxiedPlayer) to).sendMessage(ChatMessageType.SYSTEM, sendTo);
        else
            to.sendMessage(sendTo);

    }

    public void staffChat(CommandSender sender, String msg) {
        if (!sender.hasPermission(BungeeCommandUsage.STAFFCHAT.getPermission())) {
            return;
        }

        BaseComponent send = new TextComponent("Staff // ");
        send.setColor(ArcaneColor.HEADING);

        BaseComponent name = ArcaneText.playerComponentBungee(sender);
        name.setColor(ArcaneColor.FOCUS);
        name.addExtra(": ");

        send.addExtra(name);
        send.addExtra(ArcaneText.url(ChatColor.translateAlternateColorCodes('&', msg)));

        ProxyServer.getInstance().getConsole().sendMessage(send);
        for (ProxiedPlayer recipient : ProxyServer.getInstance().getPlayers()) {
            if (recipient.hasPermission(BungeeCommandUsage.STAFFCHAT.getPermission())) {
                recipient.sendMessage(ChatMessageType.SYSTEM, send);
            }
        }

    }

    public void chatToDiscord(String name, UUID uuid, String msg) {
        getDB().chatToDiscord(escapeNames(name), uuid, escapeFormatters(msg));
    }

    public void sendMetaToDiscord(String msg) {
        getDB().metaToDiscord(msg);
    }

    public void chatToMinecraft(String mcName, Message msg) {
        long id = msg.getAuthor().getIdLong();
        Member member = msg.getMember();
        String userTag = msg.isWebhookMessage() ? null : getDUModule().getUserTag(id);
        String name = member.getEffectiveName();
        StringBuilder m = new StringBuilder(escapeEmojis(msg.getContentDisplay()));

        // If it contains an embed
        List<MessageEmbed> embeds = msg.getEmbeds();
        if (!embeds.isEmpty()) {
            for (MessageEmbed e : embeds) {
                // Skip if embed was called in response to a message containing an URL.
                if (e.getUrl() != null)
                    continue;

                MessageEmbed.AuthorInfo author = e.getAuthor();
                String title = e.getTitle();
                //MessageEmbed.Provider provider = e.getSiteProvider();
                String description = e.getDescription();

                List<MessageEmbed.Field> fields = e.getFields();

                MessageEmbed.ImageInfo image = e.getImage();
                MessageEmbed.VideoInfo videoInfo = e.getVideoInfo();

                MessageEmbed.Footer footer = e.getFooter();

                Color color = e.getColor();

                int r = color.getRed();
                int g = color.getGreen();
                int b = color.getBlue();
                int cc = 0;

                if (r >= 0x55 && g >= 0x55 && b >= 0x55) {
                    cc += 1 << 3;
                    if (r >= 0xAA)
                        cc += 1 << 2;
                    if (g >= 0xAA)
                        cc += 1 << 1;
                    if (b >= 0xAA)
                        cc += 1;
                } else {
                    if (r >= 0x55)
                        cc += 1 << 2;
                    if (g >= 0x55)
                        cc += 1 << 1;
                    if (b >= 0x55)
                        cc += 1;
                }

                String c = "\n" + ChatColor.getByChar(Integer.toHexString(cc).charAt(0)) + ChatColor.BOLD + "| ";


                m.append(c);

                if (author != null)
                    m.append(c).append(ChatColor.WHITE).append(author.getName());
                if (title != null)
                    m.append(c).append(ChatColor.WHITE).append(title);
                if (description != null)
                    m.append(c).append(ChatColor.GRAY).append(description);

                for (MessageEmbed.Field f : fields) {
                    if (f.getName() != null)
                        m.append(c).append(ChatColor.WHITE).append(f.getName());
                    if (f.getValue() != null)
                        m.append(c).append(ChatColor.GRAY).append(f.getValue());
                }

                if (image != null) {
                    m.append(c).append(ChatColor.GRAY).append(ChatColor.ITALIC).append("Image:")
                            .append(ChatColor.BLUE).append(' ').append(image.getUrl());
                }
                if (videoInfo != null) {
                    m.append(c).append(ChatColor.GRAY).append(ChatColor.ITALIC).append("Video:")
                            .append(ChatColor.BLUE).append(' ').append(videoInfo.getUrl());
                }
                if (footer != null) {
                    m.append(c).append(ChatColor.GRAY).append(footer.getText());
                }

                m.append(c);

            }
        }

        // Show link to attachments in-game
        List<Message.Attachment> attachments = msg.getAttachments();
        if (!attachments.isEmpty()) {
            for (Message.Attachment a : attachments) {
                if (m.length() == 0)
                    m.append(a.getUrl());
                else
                    m.append(" ").append(a.getUrl());
            }
        }

        ArcaneBungee.getInstance().getPluginMessenger().chat("Discord", name, mcName, userTag, m.toString(), ChatColor.DARK_GREEN + "[Web]");
        TextComponent log = new TextComponent("Discord: ");
        BaseComponent tag = new TextComponent("[Web]");
        tag.setColor(ChatColor.DARK_GREEN);
        log.addExtra(tag);

        log.addExtra(" <" + (mcName == null ? name : mcName) + "> " + m.toString());
        ProxyServer.getInstance().getConsole().sendMessage(log);
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

    private String escapeFormatters(String text) {
        return text.replaceAll("([\\\\*_~])", "\\\\$1");
    }

    private String escapeEmojis(String text) {
        return EmojiParser.parseToAliases(text);
    }
}
