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
    private final ArcaneBungee plugin;
    private final int xRayWaitDuration;
    private final Map<UUID, XRayCounter> diamondMineMap;
    private final HashMap<ProxiedPlayer, Integer> listenerMod;
    /**
     * Commands to ignore for everyone, e.g. everyone receives result of this command
     */
    private final HashSet<String> cmdIgnore;
    /**
     * Suspicious commands to alert at all times
     */
    private final HashSet<String> cmdSuspicious;

    /**
     * Nothing
     */
    public static final int LISTEN_NONE = 0;
    /**
     * Some, Non-all level listener
     */
    public static final int LISTEN_SOME = 1;
    /**
     * All, equal to some for non-all level listener
     */
    public static final int LISTEN_ALL = 2;
    /**
     * Everything, even if input is normally ignored
     */
    public static final int LISTEN_EVERYTHING = 3;

    SpyAlert(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.xRayWaitDuration = plugin.getConfig().getInt("spy.xray-wait-duration", 5);
        this.diamondMineMap = new HashMap<>();
        this.listenerMod = new HashMap<>();
        this.cmdIgnore = new HashSet<>(plugin.getCacheData().getStringList(IGNORE_COMMAND_PATH));
        this.cmdSuspicious = new HashSet<>(plugin.getCacheData().getStringList(SUSPICIOUS_COMMAND_PATH));
    }

    void saveConfig() {
        List<String> ci = new ArrayList<>(cmdIgnore);
        plugin.getCacheData().set(IGNORE_COMMAND_PATH, ci);
        List<String> cs = new ArrayList<>(cmdSuspicious);
        plugin.getCacheData().set(SUSPICIOUS_COMMAND_PATH, cs);
    }

    boolean setPlayerListenLevel(ProxiedPlayer p, int level) {
        if (p.hasPermission(RECEIVE_COMMAND_ALL_PERMISSION)) {
            if (level == LISTEN_ALL)
                listenerMod.remove(p);
            else
                listenerMod.put(p, level);
            return true;
        }

        if (p.hasPermission(RECEIVE_COMMAND_PERMISSION)) {
            if (level == LISTEN_SOME || level == LISTEN_ALL) {
                listenerMod.remove(p);
            } else if (level == LISTEN_EVERYTHING) {
                return false;
            } else {
                listenerMod.put(p, level);
            }
            return true;
        }

        return false;
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

    void signAlert(UUID uuid, String[] lines, int[] loc, String world) {
        BaseComponent msg = signAlertMsg(plugin.getProxy().getPlayer(uuid), lines, loc, world);
        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            if (rec.hasPermission(SIGN_PERMISSION)) {
                rec.sendMessage(ChatMessageType.SYSTEM, msg);
            }
        }
    }

    void xRayAlert(UUID uuid, String block, int[] loc, String world) {
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

        boolean onAll;
        boolean on;

        ProxiedPlayer p = (ProxiedPlayer) e.getSender();
        if (cmdSuspicious.contains(cmd) || p.hasPermission(ON_COMMAND_ALL_PERMISSION)) {
            // Listened by all & Suspicious commands are listened by all
            on = onAll = true;
        } else if (!cmdIgnore.contains(cmd) || p.hasPermission(ON_COMMAND_PERMISSION)) {
            // Not suspicious nor ignored is heard by select few
            on = true;
            onAll = false;
        } else {
            // Default don't send
            on = onAll = false;
        }

        BaseComponent msg = commandAlertMsg(p, e.getMessage());

        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            Integer listenLevel = listenerMod.get(p);
            if (listenLevel == null || listenLevel == LISTEN_ALL) {
                if (on && (rec.hasPermission(RECEIVE_COMMAND_ALL_PERMISSION) || (onAll && rec.hasPermission(RECEIVE_COMMAND_PERMISSION))))
                    rec.sendMessage(ChatMessageType.SYSTEM, msg);
            } else if (listenLevel == LISTEN_EVERYTHING) {
                if (rec.hasPermission(RECEIVE_COMMAND_ALL_PERMISSION) || (on && onAll && rec.hasPermission(RECEIVE_COMMAND_PERMISSION)))
                    rec.sendMessage(ChatMessageType.SYSTEM, msg);
            } else if (listenLevel == LISTEN_SOME) {
                if (on && onAll && (rec.hasPermission(RECEIVE_COMMAND_ALL_PERMISSION) || rec.hasPermission(RECEIVE_COMMAND_PERMISSION)))
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
            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                if (p.hasPermission(XRAY_PERMISSION)) {
                    p.sendMessage(ChatMessageType.SYSTEM, msg);
                }
            }
            diamondMineMap.remove(p.getUniqueId());
        }
    }
}
