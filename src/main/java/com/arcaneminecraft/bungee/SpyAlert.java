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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class SpyAlert implements Listener {
    private static final String XRAY_PERMISSION = "arcane.spy.receive.xray";
    private static final String SIGN_PERMISSION = "arcane.spy.receive.sign";
    private static final String COMMAND_PERMISSION = "arcane.spy.receive.command";
    private static final String ALL_COMMAND_PERMISSION = "arcane.spy.receive.command.all";
    private final ArcaneBungee plugin;
    private final int xRayWaitDuration;
    private Map<UUID, XRayCounter> diamondMineMap;

    SpyAlert(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.diamondMineMap = new HashMap<>();
        this.xRayWaitDuration = plugin.getConfig().getInt("spy.xray-wait-duration", 10);
    }

    private BaseComponent diamondAlertMsg(ProxiedPlayer p, int count, String block, int[] loc) {
        BaseComponent t = new TextComponent("Mined " + count + " " + block.toLowerCase() + (count == 1 ? "" : "s"));
        t.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("Near " + loc[0] + " " + loc[1] + " " + loc[2]).create()));
        t.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + loc[0] + " " + loc[1] + " " + loc[2]));

        BaseComponent ret = new TranslatableComponent("chat.type.admin",
                ArcaneText.playerComponentBungee(p, "Server: " + p.getServer().getInfo().getName()), t);
        ret.setColor(ChatColor.GRAY);
        ret.setItalic(true);

        return ret;
    }

    private BaseComponent signAlertMsg(ProxiedPlayer p, String[] lines, int[] loc) {
        StringBuilder c = new StringBuilder();
        for (String q : lines) {
            if (q.isEmpty())
                continue;
            c.append(" ");
            c.append(q);
        }

        BaseComponent s = new TextComponent("with:" + c.toString());
        s.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("At " + loc[0] + " " + loc[1] + " " + loc[2]
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

    void signAlert(UUID uuid, String[] lines, int[] loc) {
        BaseComponent msg = signAlertMsg(plugin.getProxy().getPlayer(uuid), lines, loc);
        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            if (rec.hasPermission(SIGN_PERMISSION)) {
                rec.sendMessage(ChatMessageType.SYSTEM, msg);
            }
        }
    }

    void xRayAlert(UUID uuid, String block, int[] loc) {
        // TODO: Differenciate between different types of blocks
        XRayCounter c;
        if ((c = diamondMineMap.get(uuid)) == null)
            diamondMineMap.put(uuid, new XRayCounter(plugin.getProxy().getPlayer(uuid), block, loc));
        else
            c.increment(loc);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void commandAlert(ChatEvent e) {
        if (!e.isCommand() || !(e.getSender() instanceof ProxiedPlayer))
            return;

        if (!(e.getSender() instanceof ProxiedPlayer))
            return;

        BaseComponent msg = commandAlertMsg((ProxiedPlayer) e.getSender(), e.getMessage());

        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            // TODO: Separate
            if (rec.hasPermission(COMMAND_PERMISSION) || rec.hasPermission(ALL_COMMAND_PERMISSION)) {
                rec.sendMessage(ChatMessageType.SYSTEM, msg);
            }
        }
    }


    private class XRayCounter implements Runnable {
        private final ProxiedPlayer p;
        private final String block;
        private int count;
        private int[] lastLocation;
        private ScheduledTask task;

        XRayCounter(ProxiedPlayer p, String block, int[] loc) {
            this.p = p;
            this.block = block;
            this.count = 1;
            this.lastLocation = loc;
            this.task = plugin.getProxy().getScheduler().schedule(plugin, this, xRayWaitDuration, TimeUnit.SECONDS);
        }

        void increment(int[] location) {
            lastLocation = location;
            count++;
            task.cancel();
            task = plugin.getProxy().getScheduler().schedule(plugin, this, xRayWaitDuration, TimeUnit.SECONDS);
        }

        @Override
        public void run() {
            BaseComponent msg = diamondAlertMsg(p, count, block, lastLocation);
            plugin.getProxy().getConsole().sendMessage(msg);
            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                if (p.hasPermission(XRAY_PERMISSION)) {
                    p.sendMessage(ChatMessageType.SYSTEM, msg);
                }
            }
            diamondMineMap.remove(p.getUniqueId());
        }
    }}
