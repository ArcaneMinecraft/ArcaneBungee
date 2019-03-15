package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.DiscordUserModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.arcaneminecraft.bungee.module.SettingModule;
import com.arcaneminecraft.bungee.module.data.ArcanePlayer;
import com.arcaneminecraft.bungee.storage.SQLDatabase;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class ProfileCommand extends Command implements TabExecutor {
    private static final String BYPASS_PUBLIC_TOGGLE = "arcane.command.profile.seeall";

    private final MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();
    private final DiscordUserModule duModule = ArcaneBungee.getInstance().getDiscordUserModule();
    private final SettingModule sModule = ArcaneBungee.getInstance().getSettingModule();
    private final SQLDatabase sqlDatabase = SQLDatabase.getInstance();

    public ProfileCommand() {
        super(BungeeCommandUsage.PROFILE.getName(), BungeeCommandUsage.PROFILE.getPermission(), BungeeCommandUsage.PROFILE.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        UUID uuid;
        if (args.length == 0) {
            if (sender instanceof ProxiedPlayer) {
                uuid = ((ProxiedPlayer) sender).getUniqueId();
            } else {
                sender.sendMessage(ArcaneText.usage("/profile <player>"));
                return;
            }
        } else {
            uuid = mpModule.getUUID(args[0]);
            if (uuid == null) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound());
                else
                    sender.sendMessage(ArcaneText.playerNotFound());
                return;
            }
        }

        ArcanePlayer mp = mpModule.getPlayerData(uuid);
        if (mp == null) {
            sqlDatabase.fetchPlayerData(uuid).thenAcceptAsync(arcPlayer -> sendProfile(sender, arcPlayer));
        } else {
            sendProfile(sender, mp);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return TabCompletePreset.allPlayers(args);
        return Collections.emptyList();
    }

    private void sendProfile(CommandSender sender, ArcanePlayer pl) {
        Locale l;
        TimeZone zone;
        if (sender instanceof ProxiedPlayer) {
            l = ((ProxiedPlayer) sender).getLocale();
            zone = mpModule.getPlayerData(((ProxiedPlayer) sender).getUniqueId()).getTimezone();
        } else {
            l = null;
            zone = TimeZone.getDefault();
        }

        BaseComponent pcList = ArcaneText.playerComponent(
                mpModule.getName(pl.getUniqueID()),
                null,
                pl.getUniqueID().toString()
        );
        pcList.setColor(ArcaneColor.LIST_VARS);
        boolean online = ProxyServer.getInstance().getPlayer(pl.getUniqueID()) != null;

        // Prepare message to send
        ComponentBuilder send = new ComponentBuilder(ArcaneText.translatable(l, "commands.profile.header", pcList))
                .color(ArcaneColor.LIST)
                .append("\n ")
                .append(ArcaneText.translatable(l, "commands.profile.id",
                        numerate(pl.getId()),
                        ArcaneText.translatable(l, "server.name")
                ))
                .color(ArcaneColor.CONTENT)
                .append("\n ")
                .append(ArcaneText.translatable(l, "commands.profile.firstseen",
                        ArcaneText.timeText(pl.getFirstSeen(), l, zone, ArcaneColor.FOCUS)
                ))
                .color(ArcaneColor.HEADING)
                .append("\n ")
                .append(ArcaneText.translatable(l, "commands.profile.seen" + (online ? ".online" : ""),
                        ArcaneText.timeText(pl.getLastLeft(), l, zone, ArcaneColor.FOCUS)
                ));


        int option = pl.getOptions();
        boolean bypass = sender.hasPermission(BYPASS_PUBLIC_TOGGLE);
        // Attach Discord
        boolean publicDiscord = sModule.getNow(SettingModule.Option.SET_DISCORD_PUBLIC, option);
        ProxyServer.getInstance().getLogger().info("Discord: " + pl.getDiscord());
        if ((publicDiscord || bypass) && pl.getDiscord() != 0L) {
            BaseComponent discord = new TextComponent(duModule.getUserTag(pl.getDiscord()));
            discord.setColor(ArcaneColor.FOCUS);
            if (!publicDiscord)
                discord.setStrikethrough(true);

            send.append("\n ")
                    .append(ArcaneText.translatable(l, "commands.profile.discord", discord), ComponentBuilder.FormatRetention.NONE)
                    .color(ArcaneColor.HEADING);
        }

        // Attach Reddit
        boolean publicReddit = sModule.getNow(SettingModule.Option.SET_DISCORD_PUBLIC, option);
        if ((publicReddit || bypass) && pl.getReddit() != null) {
            BaseComponent reddit = ArcaneText.urlSingleSpecial(pl.getReddit(), "https://reddit.com" + pl.getReddit());
            reddit.setColor(ArcaneColor.LINK_FOCUS);
            if (!publicReddit)
                reddit.setStrikethrough(true);

            send.append("\n ")
                    .append(ArcaneText.translatable(l, "commands.profile.reddit", reddit), ComponentBuilder.FormatRetention.NONE)
                    .color(ArcaneColor.HEADING);
        }

        if (sender instanceof ProxiedPlayer) {
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send.create());
        } else {
            sender.sendMessage(send.create());
        }
    }

    private String numerate(int i) {
        String ret = String.valueOf(i);
        switch (ret.charAt(ret.length() - 1)) {
            case '1':
                if (!ret.endsWith("11")) {
                    ret += "st";
                    break;
                }
            case '2':
                if (!ret.endsWith("12")) {
                    ret += "nd";
                    break;
                }
            case '3':
                if (!ret.endsWith("13")) {
                    ret += "rd";
                    break;
                }
            default:
                ret += "th";
        }
        return ret;
    }
}
