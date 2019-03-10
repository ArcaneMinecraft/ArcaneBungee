package com.arcaneminecraft.bungee.channel.discord;


import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.DiscordCommandExecutor;
import com.arcaneminecraft.bungee.channel.DiscordBot;
import com.arcaneminecraft.bungee.command.DHelpCommand;
import com.arcaneminecraft.bungee.module.DiscordUserModule;
import com.arcaneminecraft.bungee.module.MessengerModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.webhook.WebhookClient;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;

public class DiscordListener extends ListenerAdapter {
    private static final String META_MSG_MARKER = "\u200B";
    private static final String PREFIX = "!";

    private final DiscordUserModule duModule = ArcaneBungee.getInstance().getDiscordUserModule();
    private final MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();
    private final MessengerModule mModule = ArcaneBungee.getInstance().getMessengerModule();
    private final DiscordBot bot;
    private final WebhookClient webhookClient;
    private final TextChannel mcChatChannel;

    private final TreeMap<String, DiscordCommandExecutor> commandMap = new TreeMap<>();
    private final HashMap<String, DiscordCommandExecutor> aliasMap = new HashMap<>();

    public DiscordListener(DiscordBot bot, WebhookClient webhookClient, TextChannel mcChatChannel) {
        this.bot = bot;
        this.webhookClient = webhookClient;
        this.mcChatChannel = mcChatChannel;

        // Register Discord-only commands
        new DHelpCommand();
    }

    public void registerCommand(String name, DiscordCommandExecutor executor, String... aliases) {
        commandMap.put(name, executor);

        aliasMap.put(name, executor);
        for (String alias : aliases) {
            aliasMap.put(alias, executor);
        }

    }

    public Map<String, DiscordCommandExecutor> getCommandMap() {
        return commandMap;
    }

    @Override
    public void onReady(ReadyEvent event) {
        super.onReady(event);
    }

    @Override
    public void onGuildMemberLeave(GuildMemberLeaveEvent e) {

    }

    @Override
    public void onGuildMessageReceived(GuildMessageReceivedEvent e) {
        if (e.getChannel().equals(mcChatChannel)) {
            if (e.isWebhookMessage()) {
                if (webhookClient.getIdLong() != e.getAuthor().getIdLong())
                    mModule.chatToMinecraft(null, e.getMessage());
                return;
            } else if (e.getAuthor().isBot()) {
                // Don't send "joined/left"/other meta messages
                if (bot.getJDA().getSelfUser() != e.getAuthor() || !e.getMessage().getContentRaw().startsWith(META_MSG_MARKER))
                    mModule.chatToMinecraft(null, e.getMessage());
                return;
            }
            User user = e.getAuthor();

            UUID uuid = duModule.getMinecraftUuid(user.getIdLong());
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

            mModule.chatToMinecraft(mpModule.getDisplayName(uuid), e.getMessage());
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent e) {
        // Bots cannot send commands
        if (e.getAuthor().isBot())
            return;

        if (e.getMessage().getContentRaw().startsWith(PREFIX)) {
            String[] args = e.getMessage().getContentRaw().substring(1).split(" ");

            DiscordCommandExecutor dce = aliasMap.get(args[0].toLowerCase());
            if (dce != null)
                dce.executeDiscordCommand(e.getMessage(), args);
        }
    }

    public String getPrefix() {
        return PREFIX;
    }
}
