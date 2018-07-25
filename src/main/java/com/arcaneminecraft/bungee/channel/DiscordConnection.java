package com.arcaneminecraft.bungee.channel;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import javax.security.auth.login.LoginException;
import java.security.SecureRandom;
import java.util.HashMap;

public class DiscordConnection {
    private final ArcaneBungee plugin;
    private final JDA jda;
    private final WebhookClient webhookClient;
    private final Guild guild;
    private final TextChannel mcChatChannel;
    private final Role playerRole;
    private final HashMap<String, ProxiedPlayer> tokenMap;
    private static final String TOKEN_AB = "0123456789abcdefghjkmnpqrstuvwxyz";
    private static final SecureRandom rnd = new SecureRandom();
    private static final String PREFIX = "!";


    public DiscordConnection(ArcaneBungee plugin) throws LoginException, InterruptedException {
        this.plugin = plugin;
        String token = plugin.getConfig().getString("discord.token");
        this.jda = new JDABuilder(AccountType.BOT).setToken(token).buildBlocking();
        this.guild = jda.getGuildById(plugin.getConfig().getString("discord.guild-id"));
        this.mcChatChannel = this.guild.getTextChannelById(plugin.getConfig().getString("discord.mc-chat.channel-id"));
        this.playerRole = this.guild.getRoleById(plugin.getConfig().getString("discord.mc-chat.role-id"));
        this.webhookClient = new WebhookClientBuilder(plugin.getConfig().getString("discord.mc-chat.webhook-url")).build();
        this.jda.addEventListener(new DiscordListener());

        this.tokenMap = new HashMap<>();
    }

    public void onDisable() {
        this.jda.shutdown();
        this.webhookClient.close();
    }

    private Member getMember(ProxiedPlayer player) {
        long id = plugin.getSqlDatabase().getDiscordCache(player.getUniqueId());
        if (id == 0)
            return null;

        return guild.getMemberById(id);
    }

    void chatToDiscord(String user, String msg) {
        webhookClient.send(new WebhookMessageBuilder()
                .setUsername(user)
                .setContent(msg)
                .build()
        );
    }

    @SuppressWarnings("WeakerAccess")
    public void metaToDiscord(String msg) {
        mcChatChannel.sendMessage(msg).complete();
    }

    public void metaToDiscord(String msg, int count) {
        metaToDiscord(msg);

        Game g = Game.of(Game.GameType.STREAMING,count + " players", "https://arcaneminecraft.com/");
        jda.getPresence().setPresence(count == 0 ? OnlineStatus.IDLE : OnlineStatus.ONLINE, g);
    }

    private void chatToMinecraft(String mcName, Message msg) {
        User user = msg.getAuthor();
        String userTag = msg.isWebhookMessage() ? null : user.getName() + "#" + user.getDiscriminator();
        String name = user.getName();

        plugin.getPluginMessenger().chat("Discord", name, mcName, userTag, msg.getContentStripped(), ChatColor.DARK_PURPLE + "[Discord]");
    }

    public void userLink(ProxiedPlayer p) {
        Member member = getMember(p);
        if (member != null) {
            BaseComponent send = new TextComponent("Your MC account is already linked to '" + member.getNickname() + "'");
            send.setColor(ArcaneColor.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
            return;
        }

        // Remove if token was generated before
        tokenMap.entrySet().removeIf(entry -> entry.getValue() == p);

        String token = generateToken();
        while (tokenMap.containsKey(token.replaceAll("-", "")))
            token = generateToken();

        BaseComponent send = new TextComponent();
        send.setColor(ArcaneColor.CONTENT);
        BaseComponent a = new TextComponent("Token generated.");
        a.setColor(ArcaneColor.POSITIVE);
        send.addExtra(a);
        send.addExtra(" Stay online and send '");

        a = new TextComponent("!link " + p.getName() + " " + token);
        a.setColor(ArcaneColor.FOCUS);
        send.addExtra(a);
        send.addExtra("' on Discord to finish linking.\n Tokens can be used only one time!");

        tokenMap.put(token.replaceAll("-", ""), p);
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    private void userLinkConfirm(String username, Member member, String token, MessageChannel channel) {
        final String newTokenMsg = "Please generate a new token using `/discord link` in Minecraft server and try again.";
        ProxiedPlayer p = tokenMap.remove(token);

        if (p == null || p.getName().equalsIgnoreCase(username)) {
            channel.sendMessage("You provided invalid player name or token. " + newTokenMsg).complete();
            return;
        }

        if (!p.isConnected() || !plugin.getSqlDatabase().setDiscordCache(p.getUniqueId(), member.getUser().getIdLong())) {
            channel.sendMessage(p.getName() + " must be online. " + newTokenMsg).complete();
            return;
        }

        // Add user to the player role
        guild.getController().addSingleRoleToMember(member, playerRole).complete();
        channel.sendMessage(member.getAsMention() + " Discord account is successfully linked with '" + p.getName() + "' Minecraft account!").complete();

        BaseComponent send = new TextComponent("Your Minecraft account is ");
        send.setColor(ArcaneColor.CONTENT);
        BaseComponent pos = new TextComponent("successfully");
        pos.setColor(ArcaneColor.POSITIVE);

        send.addExtra(pos);
        send.addExtra(" linked with '" + member.getNickname() + "' Discord account!");
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    public void userUnlink(ProxiedPlayer p) {
        Member member = getMember(p);
        if (member == null) {
            BaseComponent send = new TextComponent("Your MC account was not linked to a Discord account");
            send.setColor(ArcaneColor.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
            return;
        }

        guild.getController().removeSingleRoleFromMember(member, playerRole).complete();
        plugin.getSqlDatabase().setDiscordCache(p.getUniqueId(), 0);
    }

    public class DiscordListener extends ListenerAdapter {
        @Override
        public void onReady(ReadyEvent event) {
            super.onReady(event);
        }

        @Override
        public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
            if (e.getChannel().equals(mcChatChannel)) {
                if (e.isWebhookMessage() || e.getAuthor().isBot()) {
                    chatToMinecraft(null, e.getMessage());
                    return;
                }
                User user = e.getAuthor();

                String name = plugin.getSqlDatabase().getUsernameFromDiscord(user.getIdLong());
                if (name == null) {
                    e.getMessage().delete().complete();
                    // Send message saying to register discord to mc account
                    if (user.hasPrivateChannel()) {
                        user.openPrivateChannel().queue(channel -> channel.sendMessage("You are not registered").complete());
                    }
                    return;
                }

                chatToMinecraft(name, e.getMessage());
            }
        }

        @Override
        public void onMessageReceived(MessageReceivedEvent e) {
            if (e.getMessage().getContentRaw().startsWith(PREFIX)) {
                String[] args = e.getMessage().getContentRaw().substring(1).split(" ");
                if (args[0].equalsIgnoreCase("link")) {
                    if (e.getAuthor().isBot()) {
                        e.getChannel().sendMessage("Bots cannot link Minecraft and Discord accounts.").complete();
                        return;
                    }
                    if (args.length > 3) {
                        String username = args[1];
                        String token = args[2].replaceAll("-", "");

                        userLinkConfirm(username, e.getMember(), token, e.getChannel());

                        return;
                    }
                    e.getChannel().sendMessage("Usage: `" + PREFIX + "link <player> <token>`").complete();
                }
            }
        }
    }

    private String generateToken() {
        int len = 14;
        StringBuilder sb = new StringBuilder(len);
        for( int i = 0; i < len; i++ ) {
            if (i == 4 || i == 9)
                sb.append('-');
            else
                sb.append(TOKEN_AB.charAt(rnd.nextInt(TOKEN_AB.length())));
        }
        return sb.toString();
    }
}
