package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneText;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

public class SpyAlert implements Listener {
    private final ArcaneBungee plugin;

    SpyAlert(ArcaneBungee plugin) {
        this.plugin = plugin;
/*
        arcane.spy.receive.xray:
        arcane.spy.receive.sign:
*/
    }

    public void signAlert(String name, String[] lines, int[] loc) {
        BaseComponent s = new TextComponent("with: " + String.join(" ", lines));
        s.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(String.join("\n", lines)).create()));

        BaseComponent t = new TextComponent("Created sign at " + loc[0] + ", " + loc[1] + ", " + loc[2] + " ");
        t.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + loc[0] + " " + loc[1] + " " + loc[2]));
        t.addExtra(s);

        ProxiedPlayer p = plugin.getProxy().getPlayer(name);

        BaseComponent a = new TranslatableComponent("chat.type.admin",
                ArcaneText.playerComponentBungee(p, "Server: " + p.getServer().getInfo().getName()), t);
        a.setColor(ChatColor.GRAY);
        a.setItalic(true);

        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            if (rec.hasPermission("arcane.spy.receive.sign")) {
                rec.sendMessage(ChatMessageType.SYSTEM, a);
            }
        }
    }

    public void xRayAlert(String name, String block, int[] loc) {
        // TODO: Decrease spam: collect then run later.
        BaseComponent t = new TextComponent("Mined " + block + " at " + loc[0] + ", " + loc[1] + ", " + loc[2]);
        t.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp " + loc[0] + " " + loc[1] + " " + loc[2]));

        ProxiedPlayer p = plugin.getProxy().getPlayer(name);

        BaseComponent a = new TranslatableComponent("chat.type.admin",
                ArcaneText.playerComponentBungee(p, "Server: " + p.getServer().getInfo().getName()), t);
        a.setColor(ChatColor.GRAY);
        a.setItalic(true);

        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            if (rec.hasPermission("arcane.spy.receive.xray")) {
                rec.sendMessage(ChatMessageType.SYSTEM, a);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void command(ChatEvent e) {
        if (!e.isCommand() || !(e.getSender() instanceof ProxiedPlayer))
            return;

        ProxiedPlayer p = (ProxiedPlayer) e.getSender();

        BaseComponent a = new TranslatableComponent("chat.type.admin",
                ArcaneText.playerComponentBungee(p, "Server: " + p.getServer().getInfo().getName()), "ran " + e.getMessage());
        a.setColor(ChatColor.GRAY);
        a.setItalic(true);

        for (ProxiedPlayer rec : plugin.getProxy().getPlayers()) {
            // TODO: Separate
            if (rec.hasPermission("arcane.spy.receive.command") || rec.hasPermission("arcane.spy.receive.command.all")) {
                rec.sendMessage(ChatMessageType.SYSTEM, a);
            }
        }
    }
}
