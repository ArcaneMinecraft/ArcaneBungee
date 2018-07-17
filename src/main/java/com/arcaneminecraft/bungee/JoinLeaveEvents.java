package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.bungee.storage.OptionsStorage;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.text.DateFormat;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class JoinLeaveEvents implements Listener {
    private final ArcaneBungee plugin;
    private final BaseComponent welcomeMessage;


    JoinLeaveEvents(ArcaneBungee plugin) {
        this.plugin = plugin;

        // BEGIN Welcome Message construction
        BaseComponent temp;
        TextComponent line = new TextComponent(new String(new char[27]).replace("\0", " "));
        line.setColor(ChatColor.GRAY);
        line.setStrikethrough(true);
        this.welcomeMessage = new TextComponent("\n");

        // First line
        this.welcomeMessage.addExtra(line);
        temp = new TextComponent(" Arcane Survival ");
        temp.setColor(ArcaneColor.HEADING);
        temp.setBold(true);
        this.welcomeMessage.addExtra(temp);

        this.welcomeMessage.addExtra(line);
        this.welcomeMessage.addExtra("\n\n");


        // help line
        this.welcomeMessage.addExtra("            You can type ");
        temp = new TextComponent("/help");
        temp.setColor(ArcaneColor.HEADING);
        temp.setBold(true);
        temp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND,"/help"));
        this.welcomeMessage.addExtra(temp);
        this.welcomeMessage.addExtra(" for a list of commands.\n");

        // Website
        this.welcomeMessage.addExtra("        Visit our website at ");
        temp = ArcaneText.urlSingle("https://arcaneminecraft.com/");
        temp.setColor(ArcaneColor.HEADING);
        this.welcomeMessage.addExtra(temp);
        this.welcomeMessage.addExtra("!\n\n");

        temp = new TextComponent(new String(new char[80]).replace("\0", " "));
        temp.copyFormatting(line);
        this.welcomeMessage.addExtra(temp);
        this.welcomeMessage.addExtra("\n");
    }

    @EventHandler
    public void onLoginJoin(PostLoginEvent e) {
        if (plugin.getSqlDatabase() != null) {
            plugin.getSqlDatabase().playerJoinThen(e.getPlayer(), (time, oldName) -> {
                ProxiedPlayer p = e.getPlayer();
                if (OptionsStorage.get(p, OptionsStorage.Toggles.SHOW_WELCOME_MESSAGE)) {
                    p.sendMessage(ChatMessageType.SYSTEM, this.welcomeMessage);
                }

                if (p.hasPermission("arcane.welcome.donor") && OptionsStorage.get(p, OptionsStorage.Toggles.SHOW_DONOR_WELCOME_MESSAGE)) { // TODO: Check permission node existence on ArcaneServer
                    p.sendMessage(ChatMessageType.SYSTEM, new TextComponent("You are a donor! This message is still a work in progress"));
                }

                if (time != null && OptionsStorage.get(p, OptionsStorage.Toggles.SHOW_LAST_LOGIN_MESSAGE)) {
                    // Scheduled because p.getLocale() does not load immediately
                    plugin.getProxy().getScheduler().schedule(plugin, () -> {
                        String timezone = plugin.getSqlDatabase().getTimeZoneSync(p.getUniqueId());
                        boolean unsetTimezone;
                        if (timezone == null) {
                            unsetTimezone = true;
                            timezone = "America/Toronto";
                        } else {
                            unsetTimezone = false;
                        }

                        DateFormat df = p.getLocale() == null
                                ? DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL)
                                : DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, p.getLocale());
                        df.setTimeZone(TimeZone.getTimeZone(timezone));
                        BaseComponent send = new TextComponent("Your last login was on ");
                        send.setColor(ArcaneColor.HEADING);
                        BaseComponent timeFormat = new TextComponent(df.format(time));
                        timeFormat.setColor(ArcaneColor.FOCUS);
                        send.addExtra(timeFormat);
                        p.sendMessage(ChatMessageType.SYSTEM, send);

                        if (unsetTimezone) {
                            send = new TextComponent("> Tip: set your timezone using '/option timeZone <time zone ID>'!");
                            send.setColor(ArcaneColor.CONTENT);
                            p.sendMessage(ChatMessageType.SYSTEM, send);
                        }
                    }, 1L, TimeUnit.SECONDS);
                }


                BaseComponent joined;
                if (oldName == null) {
                    // Exceptioned out
                    joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(e.getPlayer()));
                } else if (oldName.equals("")) {
                    // New player
                    BaseComponent newPlayer = new TextComponent(ArcaneText.playerComponentBungee(e.getPlayer()));
                    newPlayer.addExtra(" has joined  " + "Arcane" + " for the first time!");
                    newPlayer.setColor(ChatColor.YELLOW);

                    for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
                        pl.sendMessage(ChatMessageType.SYSTEM, newPlayer);
                    }
                    joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(e.getPlayer()));
                } else if (oldName.equals(e.getPlayer().getName())) {
                    // Player with same old name
                    joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(e.getPlayer()));
                } else {
                    // Player with new name
                    joined = new TranslatableComponent("multiplayer.player.joined.renamed", ArcaneText.playerComponentBungee(e.getPlayer()), oldName);
                }

                joined.setColor(ChatColor.YELLOW);

                for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
                    if (pl.equals(e.getPlayer())) continue;
                    pl.sendMessage(ChatMessageType.SYSTEM, joined);
                }
            });
        } else {
            // Fallback
            BaseComponent joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(e.getPlayer()));
            for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
                if (p.equals(e.getPlayer())) continue;
                p.sendMessage(ChatMessageType.SYSTEM, joined);
            }
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent e) {
        if (plugin.getSqlDatabase() != null)
            plugin.getSqlDatabase().playerLeave(e.getPlayer().getUniqueId());

        BaseComponent left = new TranslatableComponent("multiplayer.player.left", ArcaneText.playerComponentBungee(e.getPlayer()));
        left.setColor(ChatColor.YELLOW);
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            p.sendMessage(ChatMessageType.SYSTEM, left);
        }
    }
}
