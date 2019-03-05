package com.arcaneminecraft.bungee.channel;

import com.arcaneminecraft.api.ArcaneColor;
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
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import javax.security.auth.login.LoginException;
import java.awt.*;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.UUID;

public class DiscordConnection {
    private static DiscordConnection instance;

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
    private final LinkedHashMap<Integer, ProxiedPlayer> tokenMap;


    public DiscordConnection(ArcaneBungee plugin) throws LoginException, InterruptedException {
        DiscordConnection.instance = this;

        this.plugin = plugin;
        this.avatarSourceFormat = plugin.getConfig().getString("discord.avatar-source", "https://crafatar.com/avatars/%s?overlay");
        String token = plugin.getConfig().getString("discord.token");
        this.jda = new JDABuilder(AccountType.BOT).setToken(token).build().awaitReady();
        this.guild = jda.getGuildById(plugin.getConfig().getLong("discord.guild-id"));
        this.mcChatChannel = this.guild.getTextChannelById(plugin.getConfig().getLong("discord.mc-chat.channel-id"));
        this.playerRole = this.guild.getRoleById(plugin.getConfig().getLong("discord.player-role-id"));
        this.webhookClient = new WebhookClientBuilder(plugin.getConfig().getString("discord.mc-chat.webhook-url")).build();
        this.jda.addEventListener(new DiscordListener(this, webhookClient, mcChatChannel));

        this.tokenMap = new LinkedHashMap<>();

        mcChatChannel.sendMessage(":ok_hand: *Server is now online*").complete();
    }

    public static DiscordConnection getInstance() {
        return instance;
    }

    public ArcaneBungee getPlugin() {
        return plugin;
    }

    public JDA getJDA() {
        return jda;
    }

    public synchronized void onDisable() {
        mcChatChannel.sendMessage(":wave: *Server is now offline*").complete();
        jda.getPresence().setStatus(OnlineStatus.INVISIBLE);
        webhookClient.close();
        jda.shutdown();
    }

    public String[] getNicknameUsernameDiscriminator(long id) {
        Member m = guild.getMemberById(id);
        return new String[]{m.getNickname(),m.getUser().getName(),m.getUser().getDiscriminator()};
    }

    private Member getMember(ProxiedPlayer player) {
        return getMember(player.getUniqueId());
    }

    private Member getMember(UUID uuid) {
        long id = plugin.getSqlDatabase().getDiscordCache(uuid);
        if (id == 0)
            return null;

        return guild.getMemberById(id);
    }

    void chatToDiscord(String user, String uuid, String msg) {
        // HTTP/1.1 400 prevention: Discord disallows bot username with "clyde"
        if (user.toLowerCase().contains("clyde")) {
            int index = user.toLowerCase().indexOf("clyde");
            char[] nc = user.toCharArray();
            if (nc[index + 4] == 'E')
                nc[index + 4] = '\u0395'; // Epsilon "E"
            else if (nc[index + 1] == 'l')
                nc[index+1] = 'I'; // Upper-case i "I"
            else if (nc[index + 4] == 'e')
                nc[index + 4] = '\u212E'; // Estimate sign "e"

            user = String.copyValueOf(nc);
        }

        // Send message
        webhookClient.send(new WebhookMessageBuilder()
                .setUsername(user)
                .setContent(escapeText(msg))
                .setAvatarUrl(String.format(avatarSourceFormat, uuid))
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

    public void chatToMinecraft(String mcName, Message msg) {
        Member Member = msg.getMember();
        String userTag = msg.isWebhookMessage() ? null : Member.getUser().getName() + "#" + Member.getUser().getDiscriminator();
        String name = Member.getEffectiveName();
        StringBuilder m = new StringBuilder(msg.getContentStripped());

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

        plugin.getPluginMessenger().chat("Discord", name, mcName, userTag, m.toString(), ChatColor.DARK_GREEN + "[Web]");
        TextComponent log = new TextComponent("Discord: ");
        BaseComponent tag = new TextComponent("[Web]");
        tag.setColor(ChatColor.DARK_GREEN);
        log.addExtra(tag);
        log.addExtra(" <" + (mcName == null ? name : mcName) + "> " + m.toString());
        plugin.getProxy().getConsole().sendMessage(log);
    }

    public void userLink(ProxiedPlayer p) {
        Member member = getMember(p);
        if (member != null) {
            BaseComponent send = new TextComponent("Your MC account is already linked to '" + member.getEffectiveName() + "'. Use '/discord unlink' and try again");
            send.setColor(ArcaneColor.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
            return;
        }

        // Remove if token was generated before
        tokenMap.entrySet().removeIf(entry -> entry.getValue() == p);

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
        if (!p.isConnected() || !plugin.getSqlDatabase().setDiscordCache(p.getUniqueId(), user.getIdLong())) {
            channel.sendMessage(p.getName() + " must be online. " + newTokenMsg).complete();
            return;
        }

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

    // TODO: Make ProxiedPlayer-independent version
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
        BaseComponent send = new TextComponent("Your MC account is no longer linked to '" + member.getEffectiveName() + "' Discord account");
        send.setColor(ArcaneColor.CONTENT);
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    private int generateToken() {
        return rnd.nextInt(999999);
    }

    private String escapeText(String text) {
        return text.replaceAll("([\\\\*_~])", "\\\\$1");
    }
}
