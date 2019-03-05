package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.channel.DiscordConnection;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class Discord extends Command implements TabExecutor {
    private final BaseComponent offlineMsg;

    private static final BaseComponent DISCORD = ArcaneText.url("https://arcaneminecraft.com/discord");

    public Discord() {
        super(BungeeCommandUsage.DISCORD.getName(), BungeeCommandUsage.DISCORD.getPermission(), BungeeCommandUsage.DISCORD.getAliases());
        this.offlineMsg = new TextComponent("Discord Connection is not enabled or is offline");
        this.offlineMsg.setColor(ArcaneColor.CONTENT);
    }
    @Override
    public void execute(CommandSender sender, String[] args) {

        if (sender instanceof ProxiedPlayer && args.length != 0) {
            if (args[0].equalsIgnoreCase("link")) {
                DiscordConnection d = DiscordConnection.getInstance();
                if (d != null)
                    d.userLink((ProxiedPlayer) sender);
                else {
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, offlineMsg);
                }
                return;
            }

            if (args[0].equalsIgnoreCase("unlink")) {
                DiscordConnection d = DiscordConnection.getInstance();
                if (d != null)
                    d.userUnlink((ProxiedPlayer) sender);
                else
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, offlineMsg);
                return;
            }
        }

        if (sender instanceof ProxiedPlayer) {
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, singleLink());
            BaseComponent send = new TextComponent(" Other usage: /discord [link|unlink]");
            send.setColor(ArcaneColor.CONTENT);
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
        } else {
            sender.sendMessage(singleLink());
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return TabCompletePreset.argStartsWith(args, ImmutableSet.of("link","unlink"));
    }

    private BaseComponent singleLink() {
        BaseComponent ret = new TextComponent("Link to Discord:");
        ret.addExtra(Discord.DISCORD);
        ret.setColor(ArcaneColor.CONTENT);
        return ret;
    }
}

