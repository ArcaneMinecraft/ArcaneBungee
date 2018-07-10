package com.arcaneminecraft.bungee.command;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.ArcaneColor;

import com.arcaneminecraft.bungee.ArcaneBungee;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

final class BadgeCommands implements Listener {
    private final ArcaneBungee plugin;
    private final String TAG = "Badge";
    private final FileConfiguration config;
    private final File configFile;
    /** Current Badge */
    private final HashMap<UUID, String> badgeOn = new HashMap<>();
    /** Allowed Badges */
    private final HashMap<UUID, List<String>> tagAllowed = new HashMap<>();
    /** Presetted Badges*/
    private final HashMap<String, String> tagPreset = new HashMap<>();

    private static final String BADGE_TAG_PERMISSION = "arcane.command.badgeadmin";

    private static final String[][] BADGE_HELP = {
            {"badge", "show this screen", "Alias:\n /b"},
            {"badgetoggle", "Togggle your badge quicly", "Alias:\n /bt"},
            {"badge list", "list your badge collection"},
            {"badge use", "use last used or specified tag", "Usage: /badge use [badge]"},
            {"badge off", "remove your tag from chat"},
            {"Your tag appears by your name in chat for everyone."},
    };
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

    BadgeCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "badge.yml");

        // SaveDefaultConfig
        if (!configFile.exists()) {
            plugin.saveResource("badge.yml", false);
        }

        config = YamlConfiguration.loadConfiguration(configFile);

        ConfigurationSection cs = config.getConfigurationSection("badges");
        // Load default list of tags
        if (cs != null) {
            for (String tag : cs.getKeys(false)) {
                tagPreset.put(tag, cs.getString(tag));
            }
        }

        cs = config.getConfigurationSection("players");

        // Load player's badge status
        if (cs != null) {
            for (String uuid : cs.getKeys(false)) {
                UUID u = UUID.fromString(uuid);
                ConfigurationSection cs2 = cs.getConfigurationSection(uuid);
                tagAllowed.put(u, cs2.getStringList("badges"));
                String tag = cs2.getString("tag");
                // If player has a tag applied already
                if (tag != null && tag != "")
                    badgeOn.put(u, ChatColor.translateAlternateColorCodes('&', tag));
            }
        }
    }

    void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save badge configuration to " + configFile, e);
        }
    }

    private OfflinePlayer getPlayerFromName(String pname) {
        Player p = plugin.getServer().getPlayer(pname);
        if (p != null)
            return p;

        @SuppressWarnings("deprecation")
        OfflinePlayer op = plugin.getServer().getOfflinePlayer(pname);
        if (op.hasPlayedBefore())
            return op;

        return null;
    }

    private String playerNotExistTagMsg(CommandSender sender, String pname) {
        return ArcaneText.tag(TAG, "Player \"" + ArcaneColor.FOCUS + pname + ArcaneColor.CONTENT + "\" does not exist.");
    }

    private BaseComponent getBadgeList(List<String> l) {
        BaseComponent ret = ArcaneText.tagTC(TAG);
        ret.addExtra("Available badges:");

        if (l == null || l.size() == 0) {
            TextComponent tc = new TextComponent(" none");
            tc.setItalic(true);
            ret.addExtra(tc);
            return ret;
        }

        for (String s : l) {
            String t = tagPreset.get(s);
            if (t == null) {
                l.remove(s);
                continue;
            }
            ret.addExtra(" ");
            TextComponent tc = new TextComponent('[' + s + ']');
            tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badge use " + s));
            tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(ChatColor.translateAlternateColorCodes('&', t)).create()));
            tc.setColor(ArcaneColor.FOCUS);
            ret.addExtra(tc);
        }
        return ret;
    }

    private String setBadge(UUID u, String b) {
        // Set badge to configuration file
        config.set("players."+u.toString()+".tag", b);

        String c = ChatColor.translateAlternateColorCodes('&', b);
        badgeOn.put(u, c);

        return c;
    }

    private boolean removeBadge(UUID u) {
        config.set("players."+u.toString()+".tag", null);
        return badgeOn.remove(u) != null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("badgeadmin")) {
            if (!sender.hasPermission(BADGE_TAG_PERMISSION)) {
                sender.sendMessage(ArcaneText.noPermissionMsg(label));
                return true;
            }

            if (args.length == 0) {
                ArcaneText.sendCommandMenu(sender, "Badge Tag Help", BADGE_TAG_HELP, new String[]{"You can /b use any badge as badge admin."});
                return true;
            }

            if (args[0].equalsIgnoreCase("allow")) {
                if (args.length != 3) {
                    sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin allow <badge> <player>"));
                    return true;
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
                    sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin disallow <badge> <player>"));
                    return true;
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

                    String current = badgeOn.get(e.getKey());

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
                    sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin check <player>"));
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

                String current = badgeOn.get(u);

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
                    sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin set <tag...> <player>"));
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

                String c = setBadge(u, b);

                sender.sendMessage(ArcaneText.tag(TAG, ArcaneColor.FOCUS + p.getName() + ArcaneColor.CONTENT + "'s tag is now " + c + "."));
                return true;
            }

            if (args[0].equalsIgnoreCase("clear")) {
                if (args.length != 2) {
                    sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin clear <player>"));
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
                    sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin library (list|add|remove) [<badge> [<tag...>]]"));
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
                        sender.sendMessage(ArcaneText.tag(TAG, "Usage: /badgeadmin library remove <badge>"));
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
                return true;
            }
            return false;
        }

        // Must be a player from this point on.
        if (!(sender instanceof Player)) {
            sender.sendMessage(ArcaneText.noConsoleMsg());
            return true;
        }

        UUID u = ((Player)sender).getUniqueId();

        if (cmd.getName().equalsIgnoreCase("badgetoggle")) {
            if (removeBadge(u)) {
                sender.sendMessage(ArcaneText.tag(TAG, "You put your badge away."));
                return true;
            }

            List<String> ls = tagAllowed.get(u);
            if (ls != null && ls.size() == 1) {
                // only one
                for (String s : ls) {
                    String t = tagPreset.get(s);
                    setBadge(u, t);
                    sender.sendMessage(ArcaneText.tag(TAG, "Your tag is now " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', t) + ArcaneColor.CONTENT + "."));
                    return true;
                }
            }
            ((Player)sender).spigot().sendMessage(getBadgeList(ls));
            return true;
        }

        // Command: badge

        //String b = tagAllowed.get(p).get(0);
        if (args.length == 0) {
            String t = badgeOn.get(u);
            ArcaneText.sendCommandMenu(sender, "Badge Help", BADGE_HELP, new String[]{"Currently using", t == null ? ChatColor.ITALIC + "none" : ChatColor.RESET + t});
            return true;
        }

        if (args[0].equalsIgnoreCase("off")) {
            if (removeBadge(u))
                sender.sendMessage(ArcaneText.tag(TAG, "You put your badge away."));
            else
                sender.sendMessage(ArcaneText.tag(TAG, "You didn't have any badge on."));
            return true;
        }

        if (args[0].equalsIgnoreCase("use")) {
            if (args.length >= 2) {
                if (tagAllowed.get(u).contains(args[1]) || sender.hasPermission(BADGE_TAG_PERMISSION)) {
                    String b = tagPreset.get(args[1]);
                    setBadge(u, b);
                    sender.sendMessage(ArcaneText.tag(TAG, "Your tag is now " + ChatColor.RESET + ChatColor.translateAlternateColorCodes('&', b) + ArcaneColor.CONTENT + "."));
                    return true;
                }

                sender.sendMessage(ArcaneText.tag(TAG, "You don't have that badge!"));
            }
        }

        // List
        List<String> ls = tagAllowed.get(u);
        ((Player)sender).spigot().sendMessage(getBadgeList(ls));

        return true;
    }

    @EventHandler(priority=EventPriority.HIGHEST)
    public void addBadge(AsyncPlayerChatEvent e) {
        String badge = badgeOn.get(e.getPlayer().getUniqueId());
        if (badge != null) {
            e.setFormat(badge + ' ' + ChatColor.RESET + e.getFormat());
        }
    }



    public void getPrefix(String player) {
        LuckPermsApi api = (LuckPermsApi) plugin.getProxy().getPluginManager().getPlugin("LuckPerms");
        User u = api.getUser(player);


    }
}