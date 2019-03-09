package com.arcaneminecraft.bungee.channel.discord;


import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.channel.DiscordBot;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.webhook.WebhookClient;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {
    private static final String META_MSG_MARKER = "\u200B";
    private static final String PREFIX = "!";
    private final DiscordBot dc;
    private final ArcaneBungee plugin;
    private final WebhookClient webhookClient;
    private final TextChannel mcChatChannel;


    public DiscordListener (DiscordBot dc, WebhookClient webhookClient, TextChannel mcChatChannel) {
        this.dc = dc;
        this.plugin = dc.getPlugin();
        this.webhookClient = webhookClient;
        this.mcChatChannel = mcChatChannel;
    }


    @Override
    public void onReady(ReadyEvent event) {
        super.onReady(event);
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent e) {
        // TODO: Remove player status on player leave
    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (e.getChannel().equals(mcChatChannel)) {
            if (e.isWebhookMessage()) {
                if (webhookClient.getIdLong() != e.getAuthor().getIdLong())
                    dc.chatToMinecraft(null, e.getMessage());
                return;
            } else if (e.getAuthor().isBot()) {
                // Don't send "joined/left"/other meta messages
                if (dc.getJDA().getSelfUser() != e.getAuthor() || !e.getMessage().getContentRaw().startsWith(META_MSG_MARKER))
                    dc.chatToMinecraft(null, e.getMessage());
                return;
            }
            User user = e.getAuthor();

            UUID uuid = plugin.getDiscordUserModule().getMinecraftUuid(user.getIdLong());
            if (uuid == null) {
                e.getMessage().delete().complete();
                // Send message saying to register discord to mc account
                user.openPrivateChannel().queue(channel ->
                        channel
                                .sendMessage("Your message in \\#" + mcChatChannel.getName()
                                        + " was deleted because your account is not linked. Please link your account first using `/discord link` in-game.")
                                .complete());
                return;
            }

            dc.chatToMinecraft(plugin.getMinecraftPlayerModule().getDisplayName(uuid), e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        // Bots cannot send commands
        if (e.getAuthor().isBot())
            return;

        if (e.getMessage().getContentRaw().startsWith(PREFIX)) {
            String[] args = e.getMessage().getContentRaw().substring(1).split(" ");

            if (args[0].equalsIgnoreCase("help")) {
                // TODO: Store this static value as class variable
                EmbedBuilder embed = new EmbedBuilder()
                        .setDescription("**Hello!** These are Arcane commands on Discord so far.  Message the staff if you need help.\n" +
                                "More commands are in the works. If there's any we must have right now, you may suggest them!\n\n" +
                                "**Come build with us!**\n" +
                                "Arcane v" + plugin.getDescription().getVersion() + "\n"
                        )
                        .setThumbnail("https://arcaneminecraft.com/res/img/icon/512.png")
                        .addField(PREFIX + "help","Shows this view.",true)
                        .addField(PREFIX + "link","__Usage:__ " + PREFIX + "link <in-game name> <token>\n" +
                                "Links your Minecraft account with Discord account. Run `/discord link` in-game first!",true)
                        .addField(PREFIX + "list","__Alias:__ " + PREFIX + "players\n" +
                                "__Usage:__ " + PREFIX + "list [uuid]\n" +
                                "Lists all players that are currently in-game.",true)
                        .setColor(0xFFAA00);

                e.getChannel().sendMessage(embed.build()).complete();

                return;
            }

            if (args[0].equalsIgnoreCase("link")) {
                if (e.getAuthor().isBot()) {
                    e.getChannel().sendMessage("Bots cannot link Minecraft and Discord accounts.").complete();
                    return;
                }
                if (args.length >= 3) {
                    String username = args[1];
                    int token = Integer.parseInt(args[2]);

                    UUID uuid = plugin.getMinecraftPlayerModule().getUUID(username);

                    if (plugin.getDiscordUserModule().confirmLink(uuid, e.getAuthor().getIdLong(), token)) {
                        e.getChannel().sendMessage("Success").complete();
                        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
                        if (p != null)
                            p.sendMessage(ChatMessageType.SYSTEM, new TextComponent("Linked to Discord"));
                    } else {
                        e.getChannel().sendMessage("Failure").complete();
                        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
                        if (p != null)
                            p.sendMessage(ChatMessageType.SYSTEM, new TextComponent("Attempt made to link to Discord; if you attempted try again"));
                    }


                    return;
                }
                e.getChannel().sendMessage("Usage: `" + PREFIX + "link <player> <token>`").complete();
                return;
            }

            if (args[0].equalsIgnoreCase("list") || args[0].equalsIgnoreCase("players")) {
                boolean uuid = args.length == 2 && args[1].equalsIgnoreCase("uuid");

                final String onlineFormat = "There are **%d**/%d players online";

                List<ProxiedPlayer> afk = plugin.getAfkList();
                StringBuilder online;
                Iterator<ProxiedPlayer> i = plugin.getProxy().getPlayers().iterator();
                if (!i.hasNext()) {
                    online = new StringBuilder("*nobody*");
                } else {
                    online = new StringBuilder();
                    while (i.hasNext()) {
                        ProxiedPlayer p = i.next();
                        if (afk.contains(p))
                            online.append("[AFK] ");
                        online.append("**").append(p.getName()).append("**");
                        if (uuid)
                            online.append(" (").append(p.getUniqueId()).append(")");
                        if (i.hasNext())
                            online.append('\n');
                    }
                }


                int onlineCount = plugin.getProxy().getOnlineCount();
                EmbedBuilder embed = new EmbedBuilder()
                        .setTitle("Online Players")
                        .setDescription("Usage: " + PREFIX + "list [uuid]")
                        .addField(
                                String.format(onlineFormat, onlineCount, plugin.getProxy().getConfig().getPlayerLimit()),
                                online.toString(),
                                false
                        )
                        .setColor(onlineCount == 0 ? 0xFFAA00 : 0x00AA00);

                e.getChannel().sendMessage(embed.build()).complete();

                //return;
            }
        }
    }
}
