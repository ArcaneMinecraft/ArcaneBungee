package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

public class SeenCommands {
    private final ArcaneBungee plugin;

    public SeenCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
    }

    private BaseComponent timeText(Timestamp time, Locale locale, String timezone) {
        BaseComponent ret = new TextComponent();
        TimeZone zone = TimeZone.getTimeZone((timezone == null) ? "America/Toronto" : timezone);
        // TODO: Set timezone hint message
        int diff = (int) ((System.currentTimeMillis() - time.getTime()) / 1000);

        if (diff < 60) {
            // Within a minute
            BaseComponent sec = new TextComponent(String.valueOf(diff));
                    sec.addExtra(" second");
            if (diff != 1)
                sec.addExtra("s");
            sec.setColor(ArcaneColor.FOCUS);
            ret.addExtra(sec);
            ret.addExtra(" ago");
        } else if (diff < 3600) {
            // Within an hour
            int m = diff / 60;

            BaseComponent hour = new TextComponent(String.valueOf(m));
            hour.addExtra(" minute");
            if (m != 1)
                hour.addExtra("s");
            hour.setColor(ArcaneColor.FOCUS);
            ret.addExtra(hour);
            ret.addExtra(" ago");
        } else if (diff < 86400) {
            // Within a day
            int h = diff / 3600;

            BaseComponent day = new TextComponent(String.valueOf(h));
            day.addExtra(" hour");
            if (h != 1)
                day.addExtra("s");
            day.setColor(ArcaneColor.FOCUS);
            ret.addExtra(day);
            ret.addExtra(" ago");
        } else if (diff < 604800) {
            // Within a week
            int d = diff / 86400;
            int h = diff / 3600 % 24;

            BaseComponent day = new TextComponent(String.valueOf(d));
            day.addExtra(" day");
            if (d != 1)
                day.addExtra("s");
            day.setColor(ArcaneColor.FOCUS);
            ret.addExtra(day);
            ret.addExtra(" and ");
            ret.addExtra(String.valueOf(h));
            ret.addExtra(" hour");
            if (h != 1)
                ret.addExtra("s");
            ret.addExtra(" ago");
        } else {
            // Over a week
            ret.addExtra("on ");

            DateFormat d = DateFormat.getDateInstance(DateFormat.LONG, locale);
            d.setTimeZone(zone);

            BaseComponent date = new TextComponent(d.format(time));
            date.setColor(ArcaneColor.FOCUS);
            ret.addExtra(date);
            ret.addExtra(" at ");

            DateFormat t = DateFormat.getTimeInstance(DateFormat.FULL, locale);
            t.setTimeZone(zone);
            ret.addExtra(t.format(time));
        }

        DateFormat d = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, locale);
        d.setTimeZone(zone);

        ret.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(d.format(time)).create())
        );

        return ret;
    }

    public class Seen extends Command implements TabExecutor {

        public Seen() {
            super(BungeeCommandUsage.SEEN.getName(), BungeeCommandUsage.SEEN.getPermission(), BungeeCommandUsage.SEEN.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.SEEN.getCommand(), args);

            // Look for player in question.
            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.SEEN.getUsage()));
                else
                    sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.SEEN.getUsage()));
                return;
            }

            ProxiedPlayer p;
            UUID uuid;
            try {
                uuid = UUID.fromString(args[0]);
                p = plugin.getProxy().getPlayer(uuid);
            } catch (IllegalArgumentException e) {
                uuid = null;
                p = plugin.getProxy().getPlayer(args[0]);
            }

            // If player is currently online
            if (p != null) {
                BaseComponent send;
                if (p == sender) {
                    send = new TextComponent("You are");
                } else {
                    send = new TextComponent("Say hi! ");
                    send.addExtra(ArcaneText.playerComponentBungee(sender));
                    send.addExtra(" is");
                }
                send.addExtra(" currently online!");
                send.setColor(ArcaneColor.CONTENT);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(ArcaneText.usage(send));
                return;
            }

            Locale locale;
            if (sender instanceof ProxiedPlayer)
                locale = ((ProxiedPlayer) sender).getLocale();
            else
                locale = Locale.getDefault();

            if (uuid == null) {
                uuid = plugin.getSqlDatabase().getPlayerUUID(args[0]);
                if (uuid == null) {
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(args[0]));
                    else
                        sender.sendMessage(ArcaneText.playerNotFound(args[0]));
                    return;
                }
            }

            // is offline:
            plugin.getSqlDatabase().getSeenThen(uuid,
                    false, (Timestamp time, String[] pData) ->
            {
                if (time == null) {
                    // will not likely reach this point
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(args[0]));
                    else
                        sender.sendMessage(ArcaneText.playerNotFound(args[0]));
                    return;
                }
                BaseComponent send = new TextComponent(ArcaneText.playerComponent(pData[0], pData[0], pData[1], null, false));
                send.addExtra(" was last seen ");
                send.addExtra(timeText(time, locale,
                        (sender instanceof ProxiedPlayer)
                                ? plugin.getSqlDatabase().getTimeZoneSync(((ProxiedPlayer) sender).getUniqueId())
                                : pData[2]
                ));
                send.setColor(ArcaneColor.CONTENT);

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(ArcaneText.usage(send));
            });
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return plugin.getTabCompletePreset().allPlayers(args);
        }
    }

    public class FirstSeen extends Command implements TabExecutor {

        public FirstSeen() {
            super(BungeeCommandUsage.FIRSTSEEN.getName(), BungeeCommandUsage.FIRSTSEEN.getPermission(), BungeeCommandUsage.FIRSTSEEN.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.FIRSTSEEN.getCommand(), args);

            // Look for player in question.
            ProxiedPlayer p;
            UUID uuid;
            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer) {
                    p = (ProxiedPlayer) sender;
                    uuid = p.getUniqueId();
                } else {
                    sender.sendMessage(ArcaneText.usage("Server: /firstseen <player>"));
                    return;
                }
            } else {
                try {
                    uuid = UUID.fromString(args[0]);
                    p = plugin.getProxy().getPlayer(uuid);
                } catch (IllegalArgumentException e) {
                    p = plugin.getProxy().getPlayer(args[0]);
                    if (p != null)
                        uuid = p.getUniqueId();
                    else
                        uuid = null;
                }
            }

            Locale locale;
            if (sender instanceof ProxiedPlayer)
                locale = ((ProxiedPlayer) sender).getLocale();
            else
                locale = Locale.getDefault();

            final ProxiedPlayer player = p;

            if (uuid == null) {
                uuid = plugin.getSqlDatabase().getPlayerUUID(args[0]);
                if (uuid == null) {
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(args[0]));
                    else
                        sender.sendMessage(ArcaneText.playerNotFound(args[0]));
                    return;
                }
            }



            plugin.getSqlDatabase().getSeenThen(uuid, true, (Timestamp time, String[] pData) ->
            {
                if (time == null) {
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(args[0]));
                    else
                        sender.sendMessage(ArcaneText.playerNotFound(args[0]));
                    return;
                }

                BaseComponent send = sender == player ? new TextComponent("You") : new TextComponent(ArcaneText.playerComponent(pData[0], pData[0], pData[1], null, false));
                send.addExtra(" first logged in ");
                send.addExtra(timeText(time, locale,
                        (sender instanceof ProxiedPlayer)
                                ? plugin.getSqlDatabase().getTimeZoneSync(((ProxiedPlayer) sender).getUniqueId())
                                : pData[2]
                ));
                send.setColor(ArcaneColor.CONTENT);

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(ArcaneText.usage(send));
            });
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            // Check for online players first
            Iterable<String> ret = plugin.getTabCompletePreset().onlinePlayers(args);
            if (((List<String>)ret).size() == 0)
                ret = plugin.getTabCompletePreset().allPlayers(args);
            return ret;
        }
    }
}
