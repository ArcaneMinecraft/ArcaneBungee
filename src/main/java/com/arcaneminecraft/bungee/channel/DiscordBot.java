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

import javax.security.auth.login.LoginException;
import java.util.UUID;

public class DiscordBot {
    private static DiscordBot instance;

    private static final String META_MSG_MARKER = "\u200B";

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
        if (u == null)
            return null;
        return "@" + u.getName() + "#" + u.getDiscriminator();
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

        String name = parts[0].startsWith("@") ? parts[0].substring(1) : parts[0];
        for (Member m : guild.getMembersByName(name, true)) {
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
