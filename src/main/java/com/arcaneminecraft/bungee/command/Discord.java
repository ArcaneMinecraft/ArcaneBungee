package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.channel.DiscordConnection;
import com.arcaneminecraft.bungee.module.DiscordUserModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class Discord extends Command implements TabExecutor {
    private static final BaseComponent DISCORD = ArcaneText.url("https://arcaneminecraft.com/discord");

    private final DiscordConnection dc = ArcaneBungee.getInstance().getDiscordConnection();
    private final DiscordUserModule module = ArcaneBungee.getInstance().getDiscordUserModule();
    private final MinecraftPlayerModule mcModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();


    public Discord() {
        super(BungeeCommandUsage.DISCORD.getName(), BungeeCommandUsage.DISCORD.getPermission(), BungeeCommandUsage.DISCORD.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer && args.length != 0) {
            ProxiedPlayer p = (ProxiedPlayer) sender;
            if (args[0].equalsIgnoreCase("link")) {
                int token = module.linkToken(p.getUniqueId());
                if (token == -1) {
                    BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.discord.link.exists", module.getUserTag(module.getDiscordId(p.getUniqueId())));
                    send.setColor(ArcaneColor.NEGATIVE);

                    p.sendMessage(ChatMessageType.SYSTEM, send);
                    return;
                } else {
                    String commandString = dc.getPrefix() + "link " + sender.getName() + " " + token;
                    TextComponent command = new TextComponent();
                    command.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandString));
                    command.setColor(ArcaneColor.FOCUS);

                    BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.discord.link", command);
                    send.setColor(ArcaneColor.CONTENT);

                    p.sendMessage(ChatMessageType.SYSTEM, send);
                    return;
                }
            }

            if (args[0].equalsIgnoreCase("unlink")) {
                Long discordId = module.unlink(p.getUniqueId());
                if (discordId == null) {
                    // failure
                } else {
                    // success
                }
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

