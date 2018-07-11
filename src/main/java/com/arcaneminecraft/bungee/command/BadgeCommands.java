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
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.config.Configuration;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.*;
import java.util.logging.Level;

public class BadgeCommands implements Listener {
    private final ArcaneBungee plugin;
    private final File file;
    /** Players with modified badge */
    private final Set<UUID> alteredPrefix; // TODO: Make use of this.
    private Configuration config;

    private static final String CONFIG_FILENAME = "altered-prefix.yml";
    private static final String BADGE_ADMIN_PERMISSION = "arcane.command.badgeadmin";
    private static final String[][] BADGE_TAG_HELP = {
            {"badgeadmin", "show this screen", "Aliases:\n /ba\n /nt"},
            {"badgeadmin allow", "give new badge option", "Usage: /badgeadmin allow <badge> <player>"},
            {"badgeadmin disallow", "remove existing badge option", "Usage: /badgeadmin disallow <badge> <player>"},
            {"badgeadmin check", "check player's badges", "Usage: /badgeadmin check <player>"},
            {"badgeadmin list", "list every player's badges"},
            {"badgeadmin set", "set player custom tag", "Usage: /badgeadmin set <tag...> <player>\n Tag may contain multiple spaces."},
            {"badgeadmin clear", "clear player tag", "Usage: /badgeadmin clear <player>"},
            {"badgeadmin library", "add/remove/list badge type", "Usage:\n /badgeadmin library list\n /badgeadmin library add <badge> <tag...>\n /badgeadmin library remove <badge>"},
    };

    public BadgeCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), CONFIG_FILENAME);
        this.alteredPrefix = new HashSet<>();

        // saveDefaultConfig
        if (!plugin.getDataFolder().exists())
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdir();

        if (!file.exists()) {
            try (InputStream in = plugin.getResourceAsStream(CONFIG_FILENAME)) {
                Files.copy(in, file.toPath());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // getConfig
        try {
            this.config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(file);
            // Load UUID with modified player list
            List<String> l = this.config.getStringList("a");
            if (l != null) {
                for (String s : l) {
                    this.alteredPrefix.add(UUID.fromString(s));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveConfig() {
        config.set("a", alteredPrefix);
        try {
            ConfigurationProvider.getProvider(YamlConfiguration.class).save(config, file);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save " + CONFIG_FILENAME, e);
        }
    }

    private LuckPermsApi getLpApi() {
        return LuckPerms.getApi();
    }

    private BaseComponent badgeList(SortedMap<Integer, String> l) {
        BaseComponent ret = new TextComponent("Available badges: ");
        ret.setColor(ArcaneColor.CONTENT);

        TextComponent tc = new TextComponent("(none)");
        tc.setItalic(true);
        tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badge -1"));
        ret.addExtra(tc);

        tc = new TextComponent("(reset)");
        tc.setItalic(true);
        tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badge reset"));
        ret.addExtra(tc);

        HashSet<Integer> visited = new HashSet<>();

        for (Map.Entry<Integer, String> e : l.entrySet()) {
            if (!visited.add(e.getKey()))
                continue; // already has this weight

            ret.addExtra(" ");

            tc = new TextComponent(TextComponent.fromLegacyText(
                    ChatColor.translateAlternateColorCodes('&', e.getValue())
            ));
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badge " + e.getKey()));
            ret.addExtra(tc);
        }

        return ret;
    }

    private void getUserThen(UUID uuid, boolean save, ReturnRunnable<User> run) {
        User u = getLpApi().getUser(uuid);
        if (u == null) {
            getLpApi().getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
                run.run(user);
                if (save)
                    getLpApi().getUserManager().saveUser(user);
            });
        } else {
            run.run(u);
            if (save)
                getLpApi().getUserManager().saveUser(u);
        }
    }

    private void setPriorityThen(UUID uuid, int priority, ReturnRunnable<String> run) {
        getUserThen(uuid, true, user -> {
            // Check if prefix by priority exists
            String ret = priority == -1 ? "" : user.getCachedData().getMetaData(Contexts.global()).getPrefixes().get(priority);
            if (ret != null) {
                Node node = getLpApi().getNodeFactory().makeMetaNode("PrefixPriority", String.valueOf(priority)).build();
                user.setPermission(node); // TODO: Remove duplicate key stuffs
            }
            run.run(ret);
        });
    }

    private void clearPriorityThen(UUID uuid, ReturnRunnable<String> run) {
        getUserThen(uuid, true, user -> {
            Node node = getLpApi().getNodeFactory().makeMetaNode("PrefixPriority", "").build();
            user.unsetPermission(node);

            MetaData md = user.getCachedData().getMetaData(Contexts.global());
            run.run(md.getPrefix());
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
                //noinspection ConstantConditions
                p.sendMessage(ChatMessageType.SYSTEM, badgeList(
                        getLpApi().getUser(p.getUniqueId()).getCachedData().getMetaData(Contexts.global()).getPrefixes()
                ));
                return;
            }

            try {
                int priority = Integer.parseInt(args[0]);
                setPriorityThen(p.getUniqueId(), priority, newTag -> {
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
            } catch (NumberFormatException e) {
                if (args[0].equalsIgnoreCase("reset")) {
                    clearPriorityThen(p.getUniqueId(), newTag -> {
                        BaseComponent send = new TextComponent("Your tag has been reset");
                        send.setColor(ArcaneColor.CONTENT);

                        if (newTag != null) {
                            send.addExtra(" to ");
                            for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', newTag)))
                                send.addExtra(bp);
                        }
                        p.sendMessage(ChatMessageType.SYSTEM, send);
                    });
                } else {
                    // TODO: Get badge priority by name
                    p.sendMessage(ChatMessageType.SYSTEM, new TextComponent("TODO: unimplemented"));
                }
            }
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return ImmutableSet.of("reset"); // TODO
        }
    }

/*    public class BadgeAdmin extends Command implements TabExecutor {

        public Badge() {
            super(BungeeCommandUsage.BADGEADMIN.getName(), BungeeCommandUsage.BADGEADMIN.getPermission(), BungeeCommandUsage.BADGEADMIN.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                // TODO: Help or Usage menu
                return;
            }

            if (args[0].equalsIgnoreCase("allow")) {
                if (args.length != 3) {
                    sender.sendMessage(ArcaneText.usage("/badgeadmin allow <badge> <player>"));
                    return;
                }

                String b = args[1].toLowerCase();
                OfflinePlayer p = getPlayerFromName(args[2]);
                UUID u = p.getUniqueId();
                if (u == null) {
                    sender.sendMessage(playerNotExistTagMsg(sender,args[2]));
                    return true;
                }

                List<String> l = tagAllowed.get(u);
                if (l == null) {
                    l = new LinkedList<String>();
                    tagAllowed.put(u, l);
                }
                if (l.contains(b)) {
                    sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + " already has this badge."));
                    return true;
                }

                String t = tagPreset.get(b);
                if (t == null) {
                    sender.sendMessage(ArcaneText.tag(TAG, "The badge \"" + ArcaneColor.FOCUS + b + ArcaneColor.CONTENT + "\" does not exist."));
                    return true;
                }

                l.add(b);

                config.set("players."+u.toString()+".badges", l);
                sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + " can now use " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', t) + ArcaneColor.CONTENT + "."));
                return true;
            }

            if (args[0].equalsIgnoreCase("disallow")) {
                if (args.length != 3) {
                    sender.sendMessage(ArcaneText.usage("/badgeadmin disallow <badge> <player>"));
                    return;
                }

                OfflinePlayer p = getPlayerFromName(args[2]);
                UUID u = p.getUniqueId();
                if (u == null) {
                    sender.sendMessage(playerNotExistTagMsg(sender,args[2]));
                    return true;
                }

                List<String> l = tagAllowed.get(u);
                String b = args[1].toLowerCase();
                if (l == null || !l.remove(b)) {
                    sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + " did not have \"" + ArcaneColor.FOCUS + b + ArcaneColor.CONTENT + "\"."));
                    return true;
                }

                config.set("players."+u.toString()+".badges", l);

                sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + " is not allowed to use \"" + ArcaneColor.FOCUS + b + ArcaneColor.CONTENT + "\" anymore."));
                return true;
            }

            if (args[0].equalsIgnoreCase("list")) {
                TextComponent send = ArcaneText.tagTC(TAG);
                send.addExtra("Available badge for each player:\n");

                for (Entry<UUID, List<String>> e : tagAllowed.entrySet()) {
                    OfflinePlayer p = plugin.getServer().getOfflinePlayer(e.getKey());

                    TextComponent a = new TextComponent("> " + p.getName());
                    a.setColor(ArcaneColor.FOCUS);
                    send.addExtra(a);

                    send.addExtra(" can use:");

                    String current = alteredPrefix.get(e.getKey());

                    for (String s : e.getValue()) {
                        String t = tagPreset.get(s);
                        if (t == null) {
                            e.getValue().remove(s);
                            continue;
                        }
                        send.addExtra(" ");
                        TextComponent tc = new TextComponent('[' + s + ']');
                        if (t.equals(current)) tc.setBold(true);
                        tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/badgeadmin disallow " + s));
                        tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(t).create()));
                        tc.setColor(ArcaneColor.FOCUS);
                        send.addExtra(tc);
                    }
                    send.addExtra("\n");
                }
                sender.spigot().sendMessage(send);
            }

            if (args[0].equalsIgnoreCase("check")) {
                if (args.length != 2) {
                    sender.sendMessage(ArcaneText.usage("/badgeadmin check <player>"));
                    return true;
                }

                OfflinePlayer p = getPlayerFromName(args[1]);
                UUID u = p.getUniqueId();
                if (u == null) {
                    sender.sendMessage(playerNotExistTagMsg(sender,args[2]));
                    return true;
                }

                List<String> l = tagAllowed.get(u);

                if (l == null || l.size() == 0) {
                    sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + " does not have any badges."));
                    return true;
                }

                TextComponent send = ArcaneText.tagTC(TAG);

                TextComponent a = new TextComponent(p.getName());
                a.setColor(ArcaneColor.FOCUS);
                send.addExtra(a);

                send.addExtra(" can use:");

                String current = alteredPrefix.get(u);

                for (String s : l) {
                    String t = tagPreset.get(s);
                    if (t == null) {
                        l.remove(s);
                        continue;
                    }
                    send.addExtra(" ");

                    TextComponent tc = new TextComponent('[' + s + ']');
                    if (t.equals(current)) tc.setBold(true);
                    tc.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/badgeadmin disallow " + s));
                    tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(t).create()));
                    tc.setColor(ArcaneColor.FOCUS);
                    send.addExtra(tc);
                }

                sender.spigot().sendMessage(send);
                return true;
            }

            if (args[0].equalsIgnoreCase("set")) {
                if (args.length < 3) {
                    sender.sendMessage(ArcaneText.usage("/badgeadmin set <tag...> <player>"));
                    return true;
                }

                int plPos = args.length - 1;

                OfflinePlayer p = getPlayerFromName(args[plPos]);
                UUID u = p.getUniqueId();
                if (u == null) {
                    sender.sendMessage(playerNotExistTagMsg(sender,args[plPos]));
                    return true;
                }

                String b = "";
                for (int i = 1; i < plPos; i++) {
                    b += " " + args[i];
                }
                b = b.substring(1);

                String c = setPriorityThen(u, b);

                sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + "'s tag is now " + c + "."));
                return true;
            }

            if (args[0].equalsIgnoreCase("clear")) {
                if (args.length != 2) {
                    sender.sendMessage(ArcaneText.usage("/badgeadmin clear <player>"));
                    return true;
                }

                OfflinePlayer p = getPlayerFromName(args[1]);
                UUID u = p.getUniqueId();
                if (u == null) {
                    sender.sendMessage(playerNotExistTagMsg(sender,args[1]));
                    return true;
                }

                if (!removeBadge(u)) {
                    sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + " wasn't holding a tag."));
                    return true;
                }

                sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + "'s tag is cleared successfully."));
                return true;
            }

            // Books!... er, tags!
            if (args[0].equalsIgnoreCase("library")) {
                if (args.length == 1) {
                    sender.sendMessage(ArcaneText.usage("Usage: /badgeadmin library (list|add|remove) [<badge> [<tag...>]]"));
                    return true;
                }
                if (args[1].equalsIgnoreCase("list")) {
                    sender.sendMessage(ArcaneText.tag(TAG, "Badge Presets: " + ArcaneColor.FOCUS + String.join(ArcaneColor.CONTENT + ", " + ArcaneColor.FOCUS, tagPreset.keySet())));
                    return true;
                }

                if (args[1].equalsIgnoreCase("add")) {
                    if (args.length < 4) {
                        sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin library add <badge> <tag...>"));
                        return true;
                    }

                    String b = args[2].toLowerCase();
                    String t = "";
                    for (int i = 3; i < args.length; i++) {
                        t += " " + args[i];
                    }

                    t = t.substring(1);

                    config.set("badges."+b, t);

                    // The previous value
                    String prev = tagPreset.put(b, t);

                    t = ChatColor.translateAlternateColorCodes('&', t);

                    if (prev != null) {
                        sender.sendMessage(ArcaneText.tag(TAG, "The badge \"" + ArcaneColor.FOCUS + b + ArcaneColor.CONTENT + "\"'s tag is now " + ChatColor.RESET + t + ArcaneColor.CONTENT + ", replacing " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', prev) + ArcaneColor.CONTENT + "."));
                        return true;
                    }

                    sender.sendMessage(ArcaneText.tag(TAG, "The new badge \"" + ArcaneColor.FOCUS + b + ArcaneColor.CONTENT + "\" has the tag " + ChatColor.RESET + t + ArcaneColor.CONTENT + "."));
                    return true;
                }

                if (args[1].equalsIgnoreCase("remove")) {
                    if (args.length != 3) {
                        sender.sendMessage(ArcaneText.usage("/badgeadmin library remove <badge>"));
                        return true;
                    }

                    String b = args[2].toLowerCase();
                    String prev = tagPreset.remove(b);

                    if (prev == null) {
                        sender.sendMessage(ArcaneText.tag(TAG, "The badge \"" + ArcaneColor.FOCUS + b + ArcaneColor.CONTENT + "\" does not exist."));
                        return true;
                    }

                    config.set("badges."+b, null);
                    sender.sendMessage(ArcaneText.tag(TAG, "The badge \"" + ArcaneColor.FOCUS + b + ArcaneColor.CONTENT + "\", which represented " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', prev) + ArcaneColor.CONTENT + ", has been deleted."));
                    return true;
                }

                sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin library (add|remove|list) [<badge> [<tag...>]]"));
                return;
            }
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList(); // TODO
        }
    }
*/
}