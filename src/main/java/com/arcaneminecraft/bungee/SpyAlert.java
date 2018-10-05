package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneText;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class SpyAlert implements Listener {
    private static final String XRAY_PERMISSION = "arcane.spy.receive.xray";
    private static final String SIGN_PERMISSION = "arcane.spy.receive.sign";
    private static final String ON_COMMAND_PERMISSION = "arcane.spy.on.command";
    private static final String ON_COMMAND_ALL_PERMISSION = "arcane.spy.on.command.all";
    private static final String RECEIVE_COMMAND_PERMISSION = "arcane.spy.receive.command";
    private static final String RECEIVE_COMMAND_ALL_PERMISSION = "arcane.spy.receive.command.all";
    private static final String SUSPICIOUS_COMMAND_PATH = "spy.command.suspicious";
    private static final String IGNORE_COMMAND_PATH = "spy.command.ignore";

    private static SpyAlert instance;
    private final ArcaneBungee plugin;
    private final int xRayWaitDuration;
    private final Map<UUID, XRayCounter> diamondMineMap;
    private final HashMap<ProxiedPlayer, CommandListenLevel> commandListenMod;
    private final HashSet<ProxiedPlayer> ignoreXRay;
    private final HashSet<ProxiedPlayer> ignoreSign;
    /**
     * Commands to ignore for everyone, e.g. everyone receives result of this command
     */
    private final HashSet<String> cmdIgnore;
    /**
     * Suspicious commands to alert at all times
     */
    private final HashSet<String> cmdSuspicious;

    private enum CommandListenLevel {
        NONE("false"),
        SOME("some"),
        ALL("true"),
        EVERYTHING("all");
        private final String input;

        CommandListenLevel(String input) {
            this.input = input;
        }
    }

    SpyAlert(ArcaneBungee plugin) {
        SpyAlert.instance = this;
        this.plugin = plugin;
        this.xRayWaitDuration = plugin.getConfig().getInt("spy.xray-wait-duration", 5);
        this.diamondMineMap = new HashMap<>();
        this.commandListenMod = new HashMap<>();
        this.ignoreXRay = new HashSet<>();
        this.ignoreSign = new HashSet<>();
        this.cmdIgnore = new HashSet<>(plugin.getCacheData().getStringList(IGNORE_COMMAND_PATH));
        this.cmdSuspicious = new HashSet<>(plugin.getCacheData().getStringList(SUSPICIOUS_COMMAND_PATH));
    }

    void saveConfig() {
        List<String> ci = new ArrayList<>(cmdIgnore);
        plugin.getCacheData().set(IGNORE_COMMAND_PATH, ci);
        List<String> cs = new ArrayList<>(cmdSuspicious);
        plugin.getCacheData().set(SUSPICIOUS_COMMAND_PATH, cs);
    }

    public static String getReceiveXRay(ProxiedPlayer p) {
        if (p == null)
            return "true";
        return String.valueOf(!instance.ignoreXRay.contains(p));
    }

    public static void setReceiveXRay(ProxiedPlayer p, String bool) {
        if (bool.equalsIgnoreCase("true"))
            instance.ignoreXRay.remove(p);
        else
            instance.ignoreXRay.add(p);
    }

    public static String getReceiveSign(ProxiedPlayer p) {
        if (p == null)
            return "true";
        return String.valueOf(!instance.ignoreSign.contains(p));
    }

    public static void setReceiveSign(ProxiedPlayer p, String bool) {
        if (bool.equalsIgnoreCase("true"))
            instance.ignoreSign.remove(p);
        else
            instance.ignoreSign.add(p);
    }

    public static String getReceiveCommandLevel(ProxiedPlayer p) {
        if (p == null)
            return CommandListenLevel.ALL.input;
        return instance.getPlayerListenLevel(p, true).input;
    }

    public static void setReceiveCommandLevel(ProxiedPlayer p, String level) {
        for (CommandListenLevel l : CommandListenLevel.values()) {
            if (l.input.equalsIgnoreCase(level)) {
                instance.setPlayerListenLevel(p, l);
                return;
            }
        }
    }

    private CommandListenLevel getPlayerListenLevel(ProxiedPlayer p) {
        return getPlayerListenLevel(p, false);
    }

    private CommandListenLevel getPlayerListenLevel(ProxiedPlayer p, boolean forOptionDisplay) {
        CommandListenLevel level = commandListenMod.get(p);

        if (p.hasPermission(RECEIVE_COMMAND_ALL_PERMISSION)) {
            if (level == null)
                return CommandListenLevel.ALL;
            return level;
        }

        if (p.hasPermission(RECEIVE_COMMAND_PERMISSION)) {
            if (level == null || level != CommandListenLevel.NONE) {
                if (forOptionDisplay)
                    return CommandListenLevel.ALL;
                return CommandListenLevel.SOME;
            }

            return level;
        }

        return CommandListenLevel.NONE;
    }

    private void setPlayerListenLevel(ProxiedPlayer p, CommandListenLevel level) {
        if (p.hasPermission(RECEIVE_COMMAND_ALL_PERMISSION)) {
            if (level == CommandListenLevel.ALL)
                commandListenMod.remove(p);
            else
                commandListenMod.put(p, level);
        }

        if (p.hasPermission(RECEIVE_COMMAND_PERMISSION)) {
            if (level == CommandListenLevel.SOME || level == CommandListenLevel.ALL)
                commandListenMod.remove(p);
            else if (level != CommandListenLevel.EVERYTHING)
                commandListenMod.put(p, level);
        }
    }

    private BaseComponent diamondAlertMsg(ProxiedPlayer p, int count, String block, int[] loc, String world) {
        BaseComponent t = new TextComponent("Mined " + count + " " + block.toLowerCase() + (count == 1 ? "" : "s"));
        t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Near " + loc[0] + " " + loc[1] + " " + loc[2] + " in " + world).create()));
        t.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + loc[0] + " " + loc[1] + " " + loc[2]));

        BaseComponent ret = new TranslatableComponent("chat.type.admin",
                ArcaneText.playerComponentBungee(p, "Server: " + p.getServer().getInfo().getName()), t);
        ret.setColor(ChatColor.GRAY);
        ret.setItalic(true);

        return ret;
    }

    private BaseComponent signAlertMsg(ProxiedPlayer p, String[] lines, int[] loc, String world) {
        StringBuilder c = new StringBuilder();
        for (String q : lines) {
            if (q.isEmpty())
                continue;
            c.append(" ");
            c.append(q);
        }

        BaseComponent s = new TextComponent("with:" + c.toString());
        s.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("At " + loc[0] + " " + loc[1] + " " + loc[2] + " in " + world
                        + "; Contents:\n"
                        + String.join("\n", lines)).create()));

        BaseComponent t = new TextComponent("Created sign ");
        t.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + loc[0] + " " + loc[1] + " " + loc[2]));
        t.addExtra(s);

        BaseComponent ret = new TranslatableComponent("chat.type.admin",
                ArcaneText.playerComponentBungee(p, "Server: " + p.getServer().getInfo().getName()), t);
        ret.setColor(ChatColor.GRAY);
        ret.setItalic(true);

        return ret;
    }

    private BaseComponent commandAlertMsg(ProxiedPlayer p, String command) {
        BaseComponent ret = new TranslatableComponent("chat.type.admin",
                ArcaneText.playerComponentBungee(p, "Server: " + p.getServer().getInfo().getName()),
                "ran " + command);
        ret.setColor(ChatColor.GRAY);
        ret.setItalic(true);
        return ret;
    }

    public void signAlert(UUID uuid, String[] lines, int[] loc, String world) {
        BaseComponent msg = signAlertMsg(plugin.getProxy().getPlayer(uuid), lines, loc, world);
        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            if (rec.hasPermission(SIGN_PERMISSION) && !ignoreSign.contains(rec)) {
                rec.sendMessage(ChatMessageType.SYSTEM, msg);
            }
        }
    }

    public void xRayAlert(UUID uuid, String block, int[] loc, String world) {
        XRayCounter c;
        if ((c = diamondMineMap.get(uuid)) == null)
            diamondMineMap.put(uuid, new XRayCounter(plugin.getProxy().getPlayer(uuid), block, loc, world));
        else
            c.increment(block, loc, world);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void commandAlert(ChatEvent e) {
        if (!e.isCommand() || !(e.getSender() instanceof ProxiedPlayer))
            return;

        String cmd = e.getMessage().split(" ", 2)[0].substring(1);
        // Don't notify about empty commands
        if (cmd.isEmpty())
            return;

        /* Handy chart:
         *    sender  |  listener
         *            | All | Some
         * -----------+-----+------
         *        All |  x  |  x
         *       Some |  x  |  x
         *     ignore |     |
         *       None |     |
         */

        boolean someRec;
        boolean allRec;

        ProxiedPlayer p = (ProxiedPlayer) e.getSender();
        if (cmdSuspicious.contains(cmd) || p.hasPermission(ON_COMMAND_ALL_PERMISSION)) {
            // Listened by all & Suspicious commands are listened by all
            allRec = someRec = true;
        } else if (!cmdIgnore.contains(cmd) || p.hasPermission(ON_COMMAND_PERMISSION)) {
            // Not suspicious nor ignored is heard by select few
            allRec = true;
            someRec = false;
        } else {
            // Default don't send
            allRec = someRec = false;
        }

        BaseComponent msg = commandAlertMsg(p, e.getMessage());

        loop:
        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            switch (getPlayerListenLevel(rec)) {
                case NONE:
                    continue loop;
                case EVERYTHING:
                    rec.sendMessage(ChatMessageType.SYSTEM, msg);
                    continue loop;
                case ALL:
                    if (allRec)
                        rec.sendMessage(ChatMessageType.SYSTEM, msg);
                    continue loop;
                case SOME:
                    if (someRec)
                        rec.sendMessage(ChatMessageType.SYSTEM, msg);
            }
        }
    }

    private class XRayCounter implements Runnable {
        private final ProxiedPlayer p;
        private final String block;
        private final String world;
        private int count;
        private int[] lastLocation;
        private ScheduledTask task;

        private XRayCounter(ProxiedPlayer p, String block, int[] loc, String world) {
            this.p = p;
            this.block = block;
            this.world = world;
            this.count = 1;
            this.lastLocation = loc;
            this.task = plugin.getProxy().getScheduler().schedule(plugin, this, xRayWaitDuration, TimeUnit.SECONDS);
        }

        void increment(String block, int[] loc, String world) {
            task.cancel();
            if (this.block.equals(block) && this.world.equals(world)) {
                lastLocation = loc;
                count++;
                task = plugin.getProxy().getScheduler().schedule(plugin, this, xRayWaitDuration, TimeUnit.SECONDS);
            } else {
                this.run();
                diamondMineMap.put(p.getUniqueId(), new XRayCounter(this.p, block, loc, world));
            }
        }

        @Override
        public void run() {
            BaseComponent msg = diamondAlertMsg(p, count, block, lastLocation, world);
            plugin.getProxy().getConsole().sendMessage(msg);
            for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
                if (rec.hasPermission(XRAY_PERMISSION) && !ignoreXRay.contains(rec)) {
                    rec.sendMessage(ChatMessageType.SYSTEM, msg);
                }
            }
            diamondMineMap.remove(p.getUniqueId());
        }
    }
}
