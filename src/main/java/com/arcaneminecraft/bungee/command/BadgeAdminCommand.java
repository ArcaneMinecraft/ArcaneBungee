package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.TimeShorthandUtils;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.ChatPrefixModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;


public class BadgeAdminCommand extends Command implements TabExecutor {
    private static final Set<String> ADMIN_SUBCOMMANDS = ImmutableSet.of("unset", "unsettemp", "list", "reset", "set", "setpriority", "settemp");

    private final ChatPrefixModule module = ArcaneBungee.getInstance().getChatPrefixModule();
    private final MinecraftPlayerModule mp = ArcaneBungee.getInstance().getMinecraftPlayerModule();

    public BadgeAdminCommand() {
        super(BungeeCommandUsage.BADGEADMIN.getName(), BungeeCommandUsage.BADGEADMIN.getPermission(), BungeeCommandUsage.BADGEADMIN.getAliases());
    }

    private void send(CommandSender to, BaseComponent send) {
        if (to instanceof ProxiedPlayer)
            ((ProxiedPlayer) to).sendMessage(ChatMessageType.SYSTEM, send);
        else
            to.sendMessage(send);
    }

    private Locale getLocale(CommandSender sender) {
        return sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getLocale() : null;
    }

    private void hidePrefix(CommandSender sender, UUID uuid) {
        module.setPriority(uuid, -1).thenAcceptAsync(ignore -> {
            BaseComponent nameBC = new TextComponent(mp.getName(uuid));
            nameBC.setColor(ArcaneColor.FOCUS);

            BaseComponent send = ArcaneText.translatable(getLocale(sender), "commands.badgeadmin.hide", nameBC);
            send.setColor(ArcaneColor.CONTENT);

            send(sender, send);
        });
    }

    private void setPrefix(CommandSender sender, UUID uuid, String prefix) {
        module.setPrefix(uuid, prefix, true).thenAcceptAsync(
                success -> send(sender, prefixSetMsg(sender, mp.getName(uuid), prefix, success))
        );
    }

    private void setPriority(CommandSender sender, UUID uuid, int priority) {
        String name = mp.getName(uuid);
        module.setPriority(uuid, priority).thenAcceptAsync(prefix ->
                send(sender, prefix == null ? invalidPriorityMsg(sender, name) : prefixSetMsg(sender, name, prefix, true))
        );
    }

    private BaseComponent invalidPriorityMsg(CommandSender sender, String name) {
        BaseComponent ret = ArcaneText.translatable(getLocale(sender), "commands.badgeadmin.invalid", name);
        ret.setColor(ArcaneColor.NEGATIVE);
        return ret;
    }

    private BaseComponent prefixSetMsg(CommandSender sender, String name, String prefix, boolean success) {
        if (prefix == null)
            throw new NullPointerException();

        BaseComponent nameBC = new TextComponent(name);
        nameBC.setColor(ArcaneColor.FOCUS);

        BaseComponent prefixBC = new TextComponent();
        for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix)))
            prefixBC.addExtra(bp);

        BaseComponent ret;

        if (success)
            ret = ArcaneText.translatable(getLocale(sender), "commands.badgeadmin.set", nameBC, prefixBC);
        else
            ret = ArcaneText.translatable(getLocale(sender), "commands.badgeadmin.set.custom", nameBC, prefixBC);
        ret.setColor(ArcaneColor.CONTENT);
        return ret;
    }


    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.BADGEADMIN.getUsage()));
            else
                sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.BADGEADMIN.getUsage()));
            return;
        }

        // Get UUID
        final UUID uuid;
        if (args.length > 1) {
            uuid = mp.getUUID(args[1]);
            if (uuid == null) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(args[1]));
                else
                    sender.sendMessage(ArcaneText.playerNotFound(args[1]));
                return;
            }
        } else {
            uuid = null;
        }

        // Filter through each option
        if (args[0].equalsIgnoreCase("list")) {
            if (args.length == 1) {
                // TODO: Move this into own method
                BaseComponent send = new TextComponent("Players with modified prefix:");
                send.setColor(ArcaneColor.CONTENT);

                for (UUID u : module.getAlteredPrefixPlayers()) {
                    send.addExtra(" ");

                    String name = mp.getName(u);
                    BaseComponent tc = new TextComponent(name);
                    tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badgeadmin list " + name));
                    send.addExtra(tc);
                }

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
            } else {
                // TODO: see ChatPrefixModule.badgeList()
                module.badgeList(uuid, true).thenAcceptAsync(send -> {
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                    else
                        sender.sendMessage(send);
                });
            }
            return;
        }

        if (args[0].equalsIgnoreCase("setpriority")) {
            if (args.length < 3) {
                sender.sendMessage(ArcaneText.usage("/badgeadmin setpriority <player> <prefix priority>"));
                return;
            }

            // Parse number; error message when failure
            final int priority;
            try {
                priority = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                BaseComponent send = new TranslatableComponent("parsing.int.invalid", args[2]);
                send.setColor(ArcaneColor.NEGATIVE);
                send(sender, send);
                return;
            }

            setPriority(sender, uuid, priority);
            return;
        }

        // TODO: Move this into own logic
        boolean isTemp = args[0].equalsIgnoreCase("settemp");
        if (args[0].equalsIgnoreCase("set") || isTemp) {
            if (args.length < 4 && isTemp) {
                sender.sendMessage(ArcaneText.usage("/badgeadmin settemp <player> <time[w|d|h|m|s]> <tag ...>"));
                return;
            } else if (args.length < 3) {
                sender.sendMessage(ArcaneText.usage("/badgeadmin set <player> <tag ...>"));
                return;
            }

            StringBuilder prefixBuffer = new StringBuilder();

            int start = isTemp ? 3 : 2;

            for (int i = start; i < args.length; i++) {
                if (i != start) {
                    prefixBuffer.append(" ");
                }
                prefixBuffer.append(args[i]);
            }

            String prefix = prefixBuffer.toString();

            int duration;
            TimeUnit unit;
            final String forNextString;
            if (isTemp) {
                try {
                    duration = Integer.parseInt(args[2]);
                    unit = TimeUnit.SECONDS;
                } catch (NumberFormatException e) {
                    String durationString = args[2].substring(0, args[2].length() - 1);
                    try {
                        duration = Integer.parseInt(durationString);
                    } catch (NumberFormatException e1) {
                        BaseComponent send = new TranslatableComponent("'%s' is not a valid number", durationString); // TODO: Update Translatable node
                        send.setColor(ArcaneColor.NEGATIVE);
                        if (sender instanceof ProxiedPlayer)
                            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                        else
                            sender.sendMessage(send);
                        return;
                    }

                    // TODO: Move this time selecting thingy to ArcaneAPI
                    String unitString = args[2].substring(args[2].length() - 1);
                    switch (unitString) {
                        case "w":
                            duration *= 7;
                        case "d":
                            unit = TimeUnit.DAYS;
                            break;
                        case "h":
                            unit = TimeUnit.HOURS;
                            break;
                        case "m":
                            unit = TimeUnit.MINUTES;
                            break;
                        case "s":
                            unit = TimeUnit.SECONDS;
                            break;
                        default:
                            BaseComponent send = new TranslatableComponent("'%s' is not a valid parameter", unitString); // TODO: Update Translatable node
                            send.setColor(ArcaneColor.NEGATIVE);
                            if (sender instanceof ProxiedPlayer)
                                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                            else
                                sender.sendMessage(send);
                            return;
                    }
                }

                forNextString = " for the next " + duration + " " + unit.name().toLowerCase();
            } else {
                duration = 0;
                unit = null;
                forNextString = "";
            }

            Consumer<Object> then = ignore -> {
                // TODO: When setPrefix returns false; it has made a custom prefix instead of reassigning old prefix.
                BaseComponent send = new TextComponent(mp.getName(uuid) + "'s tag is now ");
                send.setColor(ArcaneColor.CONTENT);
                for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix)))
                    send.addExtra(bp);
                send.addExtra(forNextString);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
            };

            if (isTemp)
                module.setTempPrefix(uuid, prefix, duration, unit).thenAcceptAsync(then);
            else
                module.setPrefix(uuid, prefix, true).thenAcceptAsync(then);
            return;
        }

        isTemp = args[0].equalsIgnoreCase("unsettemp");
        if (args[0].equalsIgnoreCase("unset") || isTemp) {
            if (uuid == null) {
                if (isTemp)
                    sender.sendMessage(ArcaneText.usage("/badgeadmin unsettemp <player>"));
                else
                    sender.sendMessage(ArcaneText.usage("/badgeadmin unset <player>"));
                return;
            }

            // TODO: Move to own method and separate between temp and custom
            Consumer<String> then = oldPrefix -> {
                BaseComponent send = new TextComponent(mp.getName(uuid) + "'s tag priority and custom tag has been cleared");
                send.setColor(ArcaneColor.CONTENT);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
            };

            if (isTemp)
                module.clearTempPrefix(uuid).thenAcceptAsync(then);
            else
                module.clearCustomPrefix(uuid).thenAcceptAsync(then);
            return;
        }

        if (args[0].equalsIgnoreCase("reset")) {
            if (uuid == null) {
                sender.sendMessage(ArcaneText.usage("/badgeadmin reset <player>"));
                return;
            }

            // TODO: Move to own method
            module.clearPriority(uuid).thenAcceptAsync(prefix -> {
                BaseComponent send = new TextComponent(mp.getName(uuid) + "'s tag priority has been reset");
                send.setColor(ArcaneColor.CONTENT);

                if (prefix != null) {
                    send.addExtra(" to ");
                    for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix)))
                        send.addExtra(bp);
                }

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
            });
            return;
        }

        // No command matched
        if (sender instanceof ProxiedPlayer)
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.BADGEADMIN.getUsage()));
        else
            sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.BADGEADMIN.getUsage()));
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return TabCompletePreset.argStartsWith(args, ADMIN_SUBCOMMANDS);
        if (args.length == 2)
            return TabCompletePreset.allPlayers(args);
        if (args.length == 3)
            return TimeShorthandUtils.tabComplete(args[2]);

        return Collections.emptyList();
    }
}
