package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.ReturnRunnable;
import com.google.common.collect.ImmutableSet;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.Contexts;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.Node;
import me.lucko.luckperms.api.User;
import me.lucko.luckperms.api.caching.MetaData;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class BadgeCommands implements Listener {
    private final ArcaneBungee plugin;
    /** Players with modified badge */
    private final Set<UUID> alteredPrefix;

    private static final int CUSTOM_PREFIX_PRIORITY = 1000000;
    private static final String ALTERED_PREFIX_PATH = "badge-altered";
    private static final String PREFIX_PRIORITY_STRING = "PrefixPriority";
    private static final Set<String> ADMIN_SUBCOMMANDS = ImmutableSet.of("clear", "list", "reset", "set", "setpriority", "settemp");

    public BadgeCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.alteredPrefix = new HashSet<>();

        // Load UUID with modified player list
        List<String> l = plugin.getCacheData().getStringList(ALTERED_PREFIX_PATH);
        if (l != null) {
            for (String s : l) {
                this.alteredPrefix.add(UUID.fromString(s));
            }
        }
    }

    public void saveConfig() {
        List<String> ap = new ArrayList<>();

        for (UUID u : alteredPrefix)
            ap.add(u.toString());

        plugin.getCacheData().set(ALTERED_PREFIX_PATH, ap);
    }

    private LuckPermsApi getLpApi() {
        return LuckPerms.getApi();
    }

    private void clearCustomTagsAndPriority(User user) {
        MetaData md = user.getCachedData().getMetaData(Contexts.global());

        // Clear pre-existing custom prefix
        String prefix = md.getPrefixes().get(CUSTOM_PREFIX_PRIORITY);
        if (prefix != null) {
            Node node = getLpApi().getNodeFactory().makePrefixNode(CUSTOM_PREFIX_PRIORITY, prefix).build();
            if (user.unsetPermission(node).wasFailure()) {
                // Temporary nodes will fail. Find the correct node with temporary duration and then end it.
                for (Node n : user.getTemporaryPermissionNodes()) { // Not sure if this is big enough to hold the thread...
                    if (!n.isPrefix())
                        continue;
                    try {
                        if (!n.getPrefix().getKey().equals(CUSTOM_PREFIX_PRIORITY))
                            continue;
                        node = n;
                        break;
                    } catch (IllegalStateException ignore) {} // basically continue;
                }
                user.unsetPermission(node);
            }
        }

        // Clear PrefixPriority
        String ppNow = md.getMeta().get(PREFIX_PRIORITY_STRING);
        if (ppNow != null) {
            user.unsetPermission(getLpApi().getNodeFactory().makeMetaNode(PREFIX_PRIORITY_STRING, ppNow).build());
        }

        alteredPrefix.remove(user.getUuid());
    }

    private Integer prefixToPriority(User user, String prefix) {
        for (Map.Entry<Integer, String> e : user.getCachedData().getMetaData(Contexts.global()).getPrefixes().entrySet()) {
            if (prefix.equalsIgnoreCase(e.getValue()))
                return e.getKey();
        }
        return null;
    }

    private void getUserThen(UUID uuid, boolean save, ReturnRunnable<User> run) {
        User u = getLpApi().getUser(uuid);
        if (u == null) {
            getLpApi().getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
                run.run(user);
                if (save) {
                    getLpApi().getUserManager().saveUser(user);
                }
            });
        } else {
            run.run(u);
            if (save) {
                getLpApi().getUserManager().saveUser(u);
                getLpApi().getMessagingService().ifPresent(ms -> ms.pushUserUpdate(u));
            }
        }
    }

    private void badgeListThen(UUID uuid, boolean admin, ReturnRunnable<BaseComponent> run) {
        getUserThen(uuid, false, user -> {
            MetaData md = user.getCachedData().getMetaData(Contexts.global());
            SortedMap<Integer, String> l = md.getPrefixes();

            if (l.isEmpty()) {
                BaseComponent ret = new TextComponent((admin ? user.getName() + " does": "You do" ) + " not have any badges");
                ret.setColor(ArcaneColor.CONTENT);
                run.run(ret);
                return;
            }

            // Check for current PrefixPriority meta
            Integer prefixPriority;
            String prefixPriorityString = md.getMeta().get(PREFIX_PRIORITY_STRING);
            if (prefixPriorityString == null) {
                prefixPriority = null;
            } else {
                try {
                    prefixPriority = Integer.parseInt(prefixPriorityString);
                } catch (NumberFormatException e) {
                    prefixPriority = -1;
                }
            }

            BaseComponent ret = new TextComponent(admin ? user.getName() + "'s badges: " : "Your badges: ");
            ret.setColor(ArcaneColor.CONTENT);

            TextComponent tc = new TextComponent("(hide)");
            tc.setItalic(true);

            String pre = admin ? "/badgeadmin setpriority " + user.getName() + " " : "/badge ";

            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, pre + "-1"));
            ret.addExtra(tc);

            ret.addExtra(" ");

            Iterator<Map.Entry<Integer, String>> i = l.entrySet().iterator();
            String first = i.next().getValue();

            tc = new TextComponent(TextComponent.fromLegacyText(
                    ChatColor.translateAlternateColorCodes('&', first)
            ));
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND,
                    admin ? "/badgeadmin reset " + user.getName() : "/badge reset"));
            ret.addExtra(tc);

            while (i.hasNext()) {
                Map.Entry<Integer, String> e = i.next();

                ret.addExtra(" ");

                tc = new TextComponent(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', e.getValue())
                ));
                tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, pre + e.getKey()));
                ret.addExtra(tc);
            }

            ret.addExtra("; Current: ");

            String current;
            if (prefixPriority == null) {
                current = md.getPrefix();
            } else {
                current = l.get(prefixPriority);
            }

            if (current == null) {
                tc = new TextComponent("(none)");
                tc.setColor(ChatColor.GRAY);
            } else {
                tc = new TextComponent(TextComponent.fromLegacyText(
                        ChatColor.translateAlternateColorCodes('&', current)
                ));
            }
            ret.addExtra(tc);

            run.run(ret);
        });
    }

    private void setPriorityThen(UUID uuid, int priority, ReturnRunnable.More<User, String> run) {
        getUserThen(uuid, true, user -> {
            // Check if prefix by priority exists
            MetaData md = user.getCachedData().getMetaData(Contexts.global());
            String ret = priority == -1 ? "" : md.getPrefixes().get(priority);
            if (ret != null) {
                // Remove old prefix priority value
                String ppNow = md.getMeta().get(PREFIX_PRIORITY_STRING);
                if (ppNow != null) {
                    user.unsetPermission(getLpApi().getNodeFactory().makeMetaNode(PREFIX_PRIORITY_STRING, ppNow).build());
                }

                // Set new prefix priority if new priority is not the highest
                if (!ret.equals(md.getPrefix())) {
                    Node node = getLpApi().getNodeFactory().makeMetaNode(PREFIX_PRIORITY_STRING, String.valueOf(priority)).build();
                    user.setPermission(node);
                    alteredPrefix.add(uuid);
                }
            }
            run.run(user, ret);
        });
    }

    private void setCustomTagThen(UUID uuid, String prefix, ReturnRunnable<User> run) {
        getUserThen(uuid, true, user -> {
            clearCustomTagsAndPriority(user);

            Integer test = prefixToPriority(user, prefix);
            if (test != null) {
                MetaData md = user.getCachedData().getMetaData(Contexts.global());
                // Set prefix priority instead... if priority is not the higheset
                //noinspection ConstantConditions
                if (!md.getPrefix().equals( md.getPrefixes().get(test) )) {
                    Node node = getLpApi().getNodeFactory().makeMetaNode(PREFIX_PRIORITY_STRING, test.toString()).build();
                    user.setPermission(node);
                    alteredPrefix.add(uuid);
                }
            } else {
                // Create and set new node
                Node node = getLpApi().getNodeFactory().makePrefixNode(CUSTOM_PREFIX_PRIORITY, prefix).build();
                user.setPermission(node);
                alteredPrefix.add(uuid);
            }

            run.run(user);
        });
    }

    private void setTemporaryCustomTagThen(UUID uuid, String prefix, int duration, TimeUnit unit, ReturnRunnable<User> run) {
        getUserThen(uuid, true, user -> {
            clearCustomTagsAndPriority(user);

            Node node = getLpApi().getNodeFactory().makePrefixNode(CUSTOM_PREFIX_PRIORITY, prefix).setExpiry(duration, unit).build();
            user.setPermission(node);
            alteredPrefix.add(uuid);

            run.run(user);
        });
    }

    private void clearPriorityThen(UUID uuid, ReturnRunnable<User> run) {
        getUserThen(uuid, true, user -> {
            // Ideally there would be only one meta set
            Node node = getLpApi().getNodeFactory().makeMetaNode(PREFIX_PRIORITY_STRING,
                    user.getCachedData().getMetaData(Contexts.global()).getMeta().get(PREFIX_PRIORITY_STRING)
            ).build();
            user.unsetPermission(node);

            run.run(user);
        });
    }

    private void clearCustomTagThen(UUID uuid, ReturnRunnable<User> run) {
        getUserThen(uuid, true, user -> {
            clearCustomTagsAndPriority(user);
            run.run(user);
        });
    }

    public class Badge extends Command implements TabExecutor {


        public Badge() {
            super(BungeeCommandUsage.BADGE.getName(), BungeeCommandUsage.BADGE.getPermission(), BungeeCommandUsage.BADGE.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // Must be a player from this point on.
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }

            ProxiedPlayer p = (ProxiedPlayer)sender;

            if (args.length == 0) {
                badgeListThen(p.getUniqueId(), false, list -> p.sendMessage(ChatMessageType.SYSTEM, list));
                return;
            }

            Integer priority = null;
            String prefix = null;
            try {
                priority = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                if (args.length != 1) {
                    prefix = String.join(" ", args);
                }

                else if (args[0].equalsIgnoreCase("reset")) {
                    clearPriorityThen(p.getUniqueId(), user -> {
                        BaseComponent send = new TextComponent("Your tag has been reset");
                        send.setColor(ArcaneColor.CONTENT);

                        String newTag = user.getCachedData().getMetaData(Contexts.global()).getPrefix();
                        if (newTag != null) {
                            send.addExtra(" to ");
                            for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', newTag)))
                                send.addExtra(bp);
                        }

                        p.sendMessage(ChatMessageType.SYSTEM, send);
                    });
                    return;
                }

                else if (args[0].equalsIgnoreCase("hide")) {
                    priority = -1;
                }

                else {
                    prefix = args[0];
                }
            }

            final Integer pri = priority;
            final String pre = prefix;

            getUserThen(p.getUniqueId(), false, user -> {
                Integer pt = pri;
                if (pt == null)
                    pt = prefixToPriority(user, pre);

                if (pt == null) {
                    BaseComponent send = new TextComponent("That's an invalid choice");
                    send.setColor(ArcaneColor.NEGATIVE);
                    p.sendMessage(ChatMessageType.SYSTEM, send);
                    return;
                }

                setPriorityThen(p.getUniqueId(), pt, (u, newTag) -> {
                    if (newTag == null) {
                        BaseComponent send = new TextComponent("That's an invalid choice");
                        send.setColor(ArcaneColor.NEGATIVE);
                        p.sendMessage(ChatMessageType.SYSTEM, send);
                        return;
                    }

                    BaseComponent send = new TextComponent("Your tag is now ");
                    send.setColor(ArcaneColor.CONTENT);

                    if (newTag.isEmpty()) {
                        send.addExtra("empty");
                    } else {
                        for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', newTag)))
                            send.addExtra(bp);
                    }
                    p.sendMessage(ChatMessageType.SYSTEM, send);
                });
            });
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer))
                return Collections.emptyList();

            //noinspection ConstantConditions
            Collection<String> prefixes = getLpApi().getUser(((ProxiedPlayer) sender).getUniqueId()).getCachedData().getMetaData(Contexts.global()).getPrefixes().values();

            String buffer = String.join(" ", args).toLowerCase();
            ArrayList<String> ret = new ArrayList<>();

            if ("hide".startsWith(buffer)) {
                ret.add("hide");
            }

            if ("reset".startsWith(buffer)) {
                ret.add("reset");
            }

            for (String s : prefixes) {
                if (s.toLowerCase().startsWith(buffer)) {
                    ret.add(s);
                }
            }

            return ret;
        }
    }

    public class BadgeAdmin extends Command implements TabExecutor {

        public BadgeAdmin() {
            super(BungeeCommandUsage.BADGEADMIN.getName(), BungeeCommandUsage.BADGEADMIN.getPermission(), BungeeCommandUsage.BADGEADMIN.getAliases());
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

            UUID uuid;
            if (args.length > 1) {
                uuid = plugin.getSqlDatabase().getPlayerUUID(args[1]);
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

            if (args[0].equalsIgnoreCase("list")) {
                if (uuid != null) {
                    badgeListThen(uuid, true, send -> {
                        if (sender instanceof ProxiedPlayer)
                            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                        else
                            sender.sendMessage(send);
                    });
                } else {
                    BaseComponent send = new TextComponent("Players with modified prefix:");
                    send.setColor(ArcaneColor.CONTENT);

                    for (UUID u : alteredPrefix) {
                        send.addExtra(" ");

                        String name = plugin.getSqlDatabase().getPlayerName(u);
                        BaseComponent tc = new TextComponent(name);
                        tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badgeadmin list " + name));
                        send.addExtra(tc);
                    }

                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                    else
                        sender.sendMessage(send);
                }
                return;
            }

            if (args[0].equalsIgnoreCase("setpriority")) {
                if (args.length < 3) {
                    sender.sendMessage(ArcaneText.usage("/badgeadmin setpriority <player> <prefix priority>"));
                    return;
                }

                final int n;
                try {
                    n = Integer.parseInt(args[2]);
                } catch (NumberFormatException e) {
                    BaseComponent send = new TranslatableComponent("commands.generic.num.invalid", args[2]);
                    send.setColor(ChatColor.RED);
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                    else
                        sender.sendMessage(send);
                    return;
                }

                setPriorityThen(uuid, n, (user, newTag) -> {
                    if (newTag == null) {
                        BaseComponent send = new TextComponent("That's an invalid choice for " + user.getName());
                        send.setColor(ArcaneColor.NEGATIVE);
                        if (sender instanceof ProxiedPlayer)
                            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                        else
                            sender.sendMessage(send);
                        return;
                    }

                    BaseComponent send = new TextComponent(user.getName() + "'s tag is now ");
                    send.setColor(ArcaneColor.CONTENT);

                    if (newTag.isEmpty()) {
                        send.addExtra("empty");
                    } else {
                        for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', newTag)))
                            send.addExtra(bp);
                    }

                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                    else
                        sender.sendMessage(send);
                });

                return;
            }

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
                            BaseComponent send = new TranslatableComponent("commands.generic.num.invalid", durationString);
                            send.setColor(ChatColor.RED);
                            if (sender instanceof ProxiedPlayer)
                                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                            else
                                sender.sendMessage(send);
                            return;
                        }

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
                                BaseComponent send = new TranslatableComponent("commands.generic.parameter.invalid", unitString);
                                send.setColor(ChatColor.RED);
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

                ReturnRunnable<User> run = user -> {
                    BaseComponent send = new TextComponent(user.getName() + "'s tag is now ");
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
                    setTemporaryCustomTagThen(uuid, prefix, duration, unit, run);
                else
                    setCustomTagThen(uuid, prefix, run);
                return;
            }

            if (args[0].equalsIgnoreCase("clear")) {
                if (uuid == null) {
                    sender.sendMessage(ArcaneText.usage("/badgeadmin clear <player>"));
                    return;
                }

                clearCustomTagThen(uuid, user -> {
                    BaseComponent send = new TextComponent(user.getName() + "'s tag priority and custom tag has been cleared");
                    send.setColor(ArcaneColor.CONTENT);
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                    else
                        sender.sendMessage(send);
                });
                return;
            }

            if (args[0].equalsIgnoreCase("reset")) {
                if (uuid == null) {
                    sender.sendMessage(ArcaneText.usage("/badgeadmin reset <player>"));
                    return;
                }

                clearPriorityThen(uuid, user -> {
                    BaseComponent send = new TextComponent(user.getName() + "'s tag priority has been reset");
                    send.setColor(ArcaneColor.CONTENT);

                    String newTag = user.getCachedData().getMetaData(Contexts.global()).getPrefix();
                    if (newTag != null) {
                        send.addExtra(" to ");
                        for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', newTag)))
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
                return plugin.getTabCompletePreset().argStartsWith(args, ADMIN_SUBCOMMANDS);
            if (args.length == 2)
                return plugin.getTabCompletePreset().onlinePlayers(args);
            return Collections.emptyList();
        }
    }
}