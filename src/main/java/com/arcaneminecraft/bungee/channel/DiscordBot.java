package com.arcaneminecraft.bungee.channel;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.channel.discord.DiscordListener;
import net.dv8tion.jda.core.AccountType;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.JDABuilder;
import net.dv8tion.jda.core.OnlineStatus;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.webhook.WebhookClient;
import net.dv8tion.jda.webhook.WebhookClientBuilder;
import net.dv8tion.jda.webhook.WebhookMessageBuilder;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class DiscordBot {
    private static DiscordBot instance;

    private static final SecureRandom rnd = new SecureRandom();
    private static final String META_MSG_MARKER = "\u200B";
    private static final String PREFIX = "!";

    private final ArcaneBungee plugin;
    private final String avatarSourceFormat;
    private final JDA jda;
    private final Guild guild;
    private final WebhookClient webhookClient;
    private final TextChannel mcChatChannel;
    private final Role playerRole;
    private final DiscordListener listener;

    public DiscordBot(ArcaneBungee plugin) throws LoginException, InterruptedException {
        DiscordBot.instance = this;

        this.plugin = plugin;
        this.avatarSourceFormat = plugin.getConfig().getString("discord.avatar-source", "https://crafatar.com/avatars/%s?overlay");
        String token = plugin.getConfig().getString("discord.token");
        this.jda = new JDABuilder(AccountType.BOT).setToken(token).build().awaitReady();
        this.guild = jda.getGuildById(plugin.getConfig().getLong("discord.guild-id"));
        this.mcChatChannel = this.guild.getTextChannelById(plugin.getConfig().getLong("discord.mc-chat.channel-id"));
        this.playerRole = this.guild.getRoleById(plugin.getConfig().getLong("discord.player-role-id"));
        this.webhookClient = new WebhookClientBuilder(plugin.getConfig().getString("discord.mc-chat.webhook-url")).build();

        this.listener = new DiscordListener(this, webhookClient, mcChatChannel);
        this.jda.addEventListener(getListener());

        mcChatChannel.sendMessage(":ok_hand: *Server is now online*").complete();
    }

    public static DiscordBot getInstance() {
        return instance;
    }

    public String getPrefix() {
        return PREFIX;
    }

    public ArcaneBungee getPlugin() {
        return plugin;
    }

    public JDA getJDA() {
        return jda;
    }

    public synchronized void disable() {
        mcChatChannel.sendMessage(":wave: *Server is now offline*").complete();
        jda.getPresence().setStatus(OnlineStatus.INVISIBLE);
        webhookClient.close();
        jda.shutdown();

        DiscordBot.instance = null;
    }


    public String getNickname(long id) {
        return guild.getMemberById(id).getEffectiveName();
    }

    public String getUserTag(long id) {
        User u = jda.getUserById(id);
        return u.getName() + "#" + u.getDiscriminator();
    }

    // wtf??
    @Deprecated
    public String[] getNicknameUsernameDiscriminator(long id) {
        Member m = guild.getMemberById(id);
        return new String[]{m.getNickname(),m.getUser().getName(),m.getUser().getDiscriminator()};
    }

    @Deprecated
    private Member getMember(ProxiedPlayer player) {
        return getMember(player.getUniqueId());
    }

    @Deprecated
    private Member getMember(UUID uuid) {
        Long id = 0L;
        try {
            id = plugin.getMinecraftPlayerModule().getDiscord(uuid).get();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        if (id == null || id == 0L)
            return null;

        return guild.getMemberById(id);
    }

    public void chatToDiscord(String user, UUID uuid, String msg) {
        // Send message
        webhookClient.send(new WebhookMessageBuilder()
                .setUsername(user)
                .setContent(msg)
                .setAvatarUrl(String.format(avatarSourceFormat, uuid.toString()))
                .build()
        );
    }

    public void metaToDiscord(String msg) {
        mcChatChannel.sendMessage(META_MSG_MARKER + msg).complete();
    }

    public void joinLeaveToDiscord(String msg, int count) {
        metaToDiscord(msg);

        Game g = Game.of(Game.GameType.WATCHING,count + " player" + (count == 1 ? "" : "s"));
        jda.getPresence().setPresence(count == 0 ? OnlineStatus.IDLE : OnlineStatus.ONLINE, g);
    }

/*
    public void userLink(ProxiedPlayer p) {
        Member member = getMember(p);
        if (member != null) {
            BaseComponent send = new TextComponent("Your MC account is already linked to '" + member.getEffectiveName() + "'. Use '/discord unlink' and try again");
            send.setColor(ArcaneColor.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
            return;
        }

        // Remove if token was generated before

        int token = generateToken();
        while (tokenMap.containsKey(token))
            token = generateToken();

        String tokCommand = PREFIX + "link " + p.getName() + " " + token;

        BaseComponent send = new TextComponent();
        send.setColor(ArcaneColor.CONTENT);
        BaseComponent a = new TextComponent("Token generated.");
        a.setColor(ArcaneColor.POSITIVE);
        send.addExtra(a);
        send.addExtra(" Stay online and send '");

        a = new TextComponent(tokCommand);
        a.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/ " + tokCommand));
        a.setColor(ArcaneColor.FOCUS);
        send.addExtra(a);
        send.addExtra("' on Discord to finish linking.\n Tokens can be used only one time!");

        tokenMap.put(token, p);
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }
*/

/*
    public void userLinkConfirm(String username, String token, User user, MessageChannel channel) {
        Member member = guild.getMember(user);

        if (member == null) {
            channel.sendMessage("You are not in the '" + guild.getName() + "' server! Please join the server first, then try again.").complete();
            return;
        }

        final String newTokenMsg = "Please generate a new token using `/discord link` in Minecraft server and try again.";
        ProxiedPlayer p;
        try {
            p = tokenMap.remove(Integer.parseInt(token));
        } catch (NumberFormatException e) {
            p = null;
        }

        if (p == null || !p.getName().equalsIgnoreCase(username)) {
            plugin.getLogger().info("DiscordConn - " + user.getName() + " attempted to link using " + token + " which is " + (p == null ? "nobody's" : "generated by " + p.getName()));
            if (p != null) {
                BaseComponent send = new TextComponent("Your Discord link token has expired because someone attempted to use it wrongly.  Please generate a new token.");
                send.setColor(ArcaneColor.CONTENT);
                p.sendMessage(ChatMessageType.SYSTEM, send);
            }
            channel.sendMessage("You provided invalid player name or token. " + newTokenMsg).complete();
            return;
        }

        // do linked account things
        plugin.getMinecraftPlayerModule().setDiscord(p.getUniqueId(), user.getIdLong());

        guild.getController().addSingleRoleToMember(member, playerRole).complete();

        // Success!
        channel.sendMessage(user.getAsMention() + " Discord account is successfully linked with '" + p.getName() + "' Minecraft account!").complete();

        BaseComponent send = new TextComponent("Your Minecraft account is ");
        send.setColor(ArcaneColor.CONTENT);
        BaseComponent pos = new TextComponent("successfully");
        pos.setColor(ArcaneColor.POSITIVE);

        send.addExtra(pos);
        send.addExtra(" linked with '" + member.getEffectiveName() + "' Discord account!");
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }
*/

    public void userUnlink(long id) {
        Member member = guild.getMemberById(id);
        if (member == null) {
            return;
        }

        guild.getController().removeSingleRoleFromMember(member, playerRole).complete();
    }

    public Member getMember(String userTag) {
        String[] parts = userTag.split("#");
        if (parts.length != 2)
            return null;

        for (Member m : guild.getMembersByName(parts[0], true)) {
            if (m.getUser().getDiscriminator().equals(parts[1]))
                return m;
        }
        return null;
    }

    public Member getMember(long id) {
        return guild.getMemberById(id);
    }

    public DiscordListener getListener() {
        return listener;
    }
}
