package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.bungee.module.SettingModule;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
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
    public static final String RECEIVE_XRAY_PERMISSION = "arcane.spy.receive.xray";
    public static final String RECEIVE_SIGN_PERMISSION = "arcane.spy.receive.sign";
    private static final String ON_COMMAND_PERMISSION = "arcane.spy.on.command";
    private static final String ON_SIGN_PERMISSION = "arcane.spy.on.sign";
    private static final String ON_XRAY_PERMISSION = "arcane.spy.on.xray";
    private static final String ON_ALL_COMMAND_PERMISSION = "arcane.spy.on.command.all";
    public static final String RECEIVE_COMMAND_PERMISSION = "arcane.spy.receive.command";
    public static final String RECEIVE_COMMAND_ALL_PERMISSION = "arcane.spy.receive.command.all";
    private static final String SUSPICIOUS_COMMAND_NODE = "spy.command.ignore";
    private static final String IGNORE_COMMAND_NODE = "spy.command.ignore";

    private static SpyAlert instance;

    private final int xRayWaitDuration;
    private final SettingModule sModule = ArcaneBungee.getInstance().getSettingModule();
    private final Map<UUID, XRayCounter> diamondMineMap = new HashMap<>();
    private final Set<UUID> receiveAllCommands = new HashSet<>();

    /**
     * Commands to ignore for everyone, e.g. /me which broadcasts to everyone
     */
    private final HashSet<String> cmdIgnore;

    /**
     * Suspicious commands to alert at all times
     */
    private final HashSet<String> cmdSuspicious;

    SpyAlert(ArcaneBungee plugin) {
        SpyAlert.instance = this;
        this.xRayWaitDuration = plugin.getConfig().getInt("spy.xray-wait-duration", 5);
        this.cmdIgnore = new HashSet<>(plugin.getCacheData().getStringList(IGNORE_COMMAND_NODE));
        this.cmdSuspicious = new HashSet<>(plugin.getCacheData().getStringList(SUSPICIOUS_COMMAND_NODE));
    }

    public static SpyAlert getInstance() {
        return instance;
    }

    void saveConfig() {
        List<String> ci = new ArrayList<>(cmdIgnore);
        ArcaneBungee.getInstance().getCacheData().set(IGNORE_COMMAND_NODE, ci);
        List<String> cs = new ArrayList<>(cmdSuspicious);
        ArcaneBungee.getInstance().getCacheData().set(SUSPICIOUS_COMMAND_NODE, cs);
    }

    public void setAllCommandReceiver(UUID p, boolean put) {
        if (put)
            receiveAllCommands.add(p);
        else
            receiveAllCommands.remove(p);
    }

    public boolean getAllCommandReceiver(UUID p) {
        return receiveAllCommands.contains(p);
    }


    private BaseComponent adminMessage(CommandSender actor, Object action) {
        BaseComponent ret = new TranslatableComponent("chat.type.admin",
                actor instanceof ProxiedPlayer
                        ? ArcaneText.playerComponentBungee(actor, "Server: " + ((ProxiedPlayer) actor).getServer().getInfo().getName())
                        : ArcaneText.playerComponentBungee(actor)
                , action);
        ret.setColor(ChatColor.GRAY);
        ret.setItalic(true);

        return ret;
    }

    private BaseComponent diamondAlertMsg(ProxiedPlayer p, int count, String block, int[] loc, String world) {
        BaseComponent t = new TextComponent("Mined " + count + " " + block.toLowerCase() + (count == 1 ? "" : "s"));
        t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Near " + loc[0] + " " + loc[1] + " " + loc[2] + " in " + world).create()));
        t.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + loc[0] + " " + loc[1] + " " + loc[2]));

        return adminMessage(p, t);
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

        return adminMessage(p, t);
    }

    private BaseComponent commandAlertMsg(CommandSender p, String command) {
        return adminMessage(p, "ran " + command);
    }

    public void signAlert(UUID uuid, String[] lines, int[] loc, String world) {
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
        BaseComponent msg = signAlertMsg(p, lines, loc, world);
        for (ProxiedPlayer receiver : ProxyServer.getInstance().getPlayers()) {
            if (receiver.hasPermission(RECEIVE_SIGN_PERMISSION)
                    && sModule.getNow(SettingModule.Option.SPY_SIGNS, receiver.getUniqueId())
                    && (p.hasPermission(ON_SIGN_PERMISSION) || sModule.getNow(SettingModule.Option.SPY_ON_TRUSTED, receiver.getUniqueId()))
            ) {
                receiver.sendMessage(ChatMessageType.SYSTEM, msg);
            }
        }
    }

    public void xRayAlert(UUID uuid, String block, int[] loc, String world) {
        XRayCounter c;
        if ((c = diamondMineMap.get(uuid)) == null)
            diamondMineMap.put(uuid, new XRayCounter(ProxyServer.getInstance().getPlayer(uuid), block, loc, world));
        else
            c.increment(block, loc, world);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onCommand(ChatEvent e) {
        if (!e.isCommand() || !(e.getSender() instanceof CommandSender))
            return;

        CommandSender sender = (CommandSender) e.getSender();
        String cmd = e.getMessage().split(" ", 2)[0].substring(1);
        // Don't notify about empty commands
        if (cmd.isEmpty() || cmdIgnore.contains(cmd))
            return;

        boolean isSuspicious = cmdSuspicious.contains(cmd), onCommandPermission, onAllCommands;

        if (sender instanceof ProxiedPlayer) {
            ProxiedPlayer p = (ProxiedPlayer) e.getSender();
            onCommandPermission = p.hasPermission(ON_COMMAND_PERMISSION);
            onAllCommands = p.hasPermission(ON_ALL_COMMAND_PERMISSION);
        } else {
            onCommandPermission = onAllCommands = false;
        }

        BaseComponent msg = commandAlertMsg(sender, e.getMessage());

        for (ProxiedPlayer receiver : ProxyServer.getInstance().getPlayers()) {
            boolean receiveAll = receiver.hasPermission(RECEIVE_COMMAND_ALL_PERMISSION);
            if (!receiver.hasPermission(RECEIVE_COMMAND_PERMISSION) && !receiveAll)
                continue;

            UUID uuid = receiver.getUniqueId();

            // Permission Juggle
            /*
             * If ignored command, false.
             * If has receive all permission and toggle is on, true.
             * If by new player and toggle is on, true.
             * If suscious, then
             *      if
             *      if listening on trusted, false.
             */


            if ((receiveAll && receiveAllCommands.contains(uuid)) || (onAllCommands && sModule.getNow(SettingModule.Option.SPY_NEW_PLAYER, uuid))
                    || (isSuspicious && (onCommandPermission || sModule.getNow(SettingModule.Option.SPY_ON_TRUSTED, uuid)))
            )
                receiver.sendMessage(ChatMessageType.SYSTEM, msg);
        }
    }

    private class XRayCounter implements Runnable {
        private final ProxiedPlayer p;
        private final String block;
        private final String world;
        private int count;
        private int[] lastLocation;
        private ScheduledTask task;
        private final ArcaneBungee plugin = ArcaneBungee.getInstance();

        private XRayCounter(ProxiedPlayer p, String block, int[] loc, String world) {
            this.p = p;
            this.block = block;
            this.world = world;
            this.count = 1;
            this.lastLocation = loc;
            this.task = ProxyServer.getInstance().getScheduler().schedule(plugin, this, xRayWaitDuration, TimeUnit.SECONDS);
        }

        void increment(String block, int[] loc, String world) {
            task.cancel();
            if (this.block.equals(block) && this.world.equals(world)) {
                lastLocation = loc;
                count++;
                task = ProxyServer.getInstance().getScheduler().schedule(plugin, this, xRayWaitDuration, TimeUnit.SECONDS);
            } else {
                this.run();
                diamondMineMap.put(p.getUniqueId(), new XRayCounter(this.p, block, loc, world));
            }
        }

        @Override
        public void run() {
            BaseComponent msg = diamondAlertMsg(p, count, block, lastLocation, world);
            plugin.getProxy().getConsole().sendMessage(msg);

            for (ProxiedPlayer receiver : plugin.getProxy().getPlayers()) {
                if (receiver.hasPermission(RECEIVE_XRAY_PERMISSION)
                        && sModule.getNow(SettingModule.Option.SPY_XRAY, receiver.getUniqueId())
                        && (p.hasPermission(ON_XRAY_PERMISSION) || sModule.getNow(SettingModule.Option.SPY_ON_TRUSTED, receiver.getUniqueId()))
                ) {
                    receiver.sendMessage(ChatMessageType.SYSTEM, msg);
                }
            }
            diamondMineMap.remove(p.getUniqueId());
        }
    }
}
