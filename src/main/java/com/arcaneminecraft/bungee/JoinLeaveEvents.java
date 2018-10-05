package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.bungee.channel.DiscordConnection;
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
import net.md_5.bungee.api.event.SettingsChangedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.text.DateFormat;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.TimeZone;

public class JoinLeaveEvents implements Listener {
    private final ArcaneBungee plugin;
    private final BaseComponent welcomeMessage;

    private static final String TIMEZONE_LINK = "https://game.arcaneminecraft.com/timezone/";
    private static final String[] DONOR = {
            "You're pretty awesome. Seriously.",
            "You're pretty awesome. Seriously. 100% awesome.",
            "By donating, you've helped make Arcane possible. " + ChatColor.BOLD + "Thanks!",
            "Welcome back to Arcane.",
            "You're way cooler than everybody else.",
            "Thank you for your support!",
            "You should make use of your powerful /slap command.",
            "Thank you!",
            "Thank you for supporting Arcane!",
            "Don't forget, you have access to /slap. Use it wisely.",
            "You can hide yourself from the Dynmap via /dynmap hide.",
            "Did you know there's a donor only section on the forums?",
            "Welcome back to Arcane Survival!",
            "We love you.",
            "We love you. A lot.",
            "Tip: To reappear from the Dynmap if you've hidden yourself, type /dynmap show.",
            "Thank you.",
            "If you'd like to hide yourself from our Dynmap, type /dynmap hide.",
            "Did we tell you you're awesome? You really are.",
            //"You're pretty awesome. Not as awesome as Agentred100 is, though.", // <.<
            //"You're pretty awesome. Almost as awesome as _NickV, keep it up.",  // >.>
            "Thank you. You're awesome.",
            "Welcome back to Arcane Survival.",
            "You're a pretty cool person.",
            "What cool stuff can we give to our donors? Let us know on the forums.",
            "We appreciate your support.",
            "If you're looking for some building ideas, you can type /dclem.",
            "We appreciate your support.",
    };
    private final LinkedHashMap<ProxiedPlayer, Joining> connecting = new LinkedHashMap<>();

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

    private static String getRandomDonorMsg() {
        Random rand = new Random();
        return DONOR[rand.nextInt(DONOR.length)];
    }

    @EventHandler
    public void onLoginJoin(PostLoginEvent e) {
        ProxiedPlayer p = e.getPlayer();
        connecting.put(p, new Joining(p));
    }

    @EventHandler
    public void onPlayerLeave(PlayerDisconnectEvent e) {
        // Check if player joined and left immediately
        Joining c = connecting.get(e.getPlayer());
        if (c != null) {
            c.cancel();
            return;
        }

        if (plugin.getSqlDatabase() != null)
            plugin.getSqlDatabase().playerLeave(e.getPlayer().getUniqueId());

        BaseComponent left = new TranslatableComponent("multiplayer.player.left", ArcaneText.playerComponentBungee(e.getPlayer()));
        left.setColor(ChatColor.YELLOW);
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            p.sendMessage(ChatMessageType.SYSTEM, left);
        }
        DiscordConnection d = plugin.getDiscordConnection();
        if (d != null)
            d.joinLeaveToDiscord(left.toPlainText(), plugin.getProxy().getOnlineCount() - 1);
    }

    @EventHandler
    public void onSettingsReceived(SettingsChangedEvent e) {
        Joining j = connecting.get(e.getPlayer());
        if (j != null)
            j.run();
    }

    private class Joining implements Runnable {
        private final ProxiedPlayer p;

        Joining(ProxiedPlayer p) {
            this.p = p;
        }

        @Override
        public void run() {
            // get player info form database
            if (plugin.getSqlDatabase() != null) {
                plugin.getSqlDatabase().playerJoinThen(p, (time, oldName) -> {
                    if (OptionsStorage.get(p, OptionsStorage.Toggles.SHOW_WELCOME_MESSAGE)) {
                        p.sendMessage(ChatMessageType.SYSTEM, welcomeMessage);
                    }

                    if (p.hasPermission("arcane.welcome.donor") && OptionsStorage.get(p, OptionsStorage.Toggles.SHOW_DONOR_WELCOME_MESSAGE)) {
                        BaseComponent send = new TextComponent(" ");
                        send.setColor(ArcaneColor.CONTENT);
                        BaseComponent urad = new TextComponent("You are a donor! ");
                        urad.setColor(ArcaneColor.DONOR);
                        send.addExtra(urad);

                        for (BaseComponent bp : TextComponent.fromLegacyText(getRandomDonorMsg()))
                            send.addExtra(bp);

                        p.sendMessage(ChatMessageType.SYSTEM, send);
                    }

                    if (time != null && OptionsStorage.get(p, OptionsStorage.Toggles.SHOW_LAST_LOGIN_MESSAGE)) {
                        // Scheduled because p.getLocale() does not load immediately
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
                        BaseComponent send = new TextComponent(" Your last login was on ");
                        send.setColor(ArcaneColor.HEADING);
                        BaseComponent timeFormat = new TextComponent(df.format(time));
                        timeFormat.setColor(ArcaneColor.FOCUS);
                        send.addExtra(timeFormat);
                        p.sendMessage(ChatMessageType.SYSTEM, send);

                        if (unsetTimezone) {
                            send = new TextComponent(" > Tip: Set to your local timezone! Visit ");
                            send.addExtra(ArcaneText.urlSingle(TIMEZONE_LINK));
                            send.addExtra(" for command info");
                            send.setColor(ArcaneColor.CONTENT);
                            p.sendMessage(ChatMessageType.SYSTEM, send);
                        }
                    }
                    sendJoin(oldName);

                    BaseComponent latest = new TextComponent(" Latest news");
                    latest.setColor(ArcaneColor.HEADING);

                    BaseComponent send = new TextComponent(latest);
                    send.addExtra(": ");
                    send.setColor(ArcaneColor.FOCUS);

                    plugin.getSqlDatabase().getLatestNewsThen(news -> {
                        send.addExtra(news);
                        send.addExtra("\n");
                        p.sendMessage(ChatMessageType.SYSTEM, send);
                    });
                });
            } else {
                p.sendMessage(ChatMessageType.SYSTEM, welcomeMessage);
                sendJoin(null);
            }
        }

        private void sendJoin(String oldName) {
            BaseComponent joined;
            if (oldName == null) {
                // Exceptioned out
                joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(p));
            } else if (oldName.equals("")) {
                // New player
                BaseComponent newPlayer = new TextComponent(ArcaneText.playerComponentBungee(p));
                newPlayer.addExtra(" has joined " + ArcaneText.getThisNetworkNameShort() + " for the first time!");
                newPlayer.setColor(ChatColor.YELLOW);

                for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
                    pl.sendMessage(ChatMessageType.SYSTEM, newPlayer);
                }
                joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(p));
            } else if (oldName.equals(p.getName())) {
                // Player with same old name
                joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(p));
            } else {
                // Player with new name
                joined = new TranslatableComponent("multiplayer.player.joined.renamed", ArcaneText.playerComponentBungee(p), oldName);
            }

            joined.setColor(ChatColor.YELLOW);

            for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
                if (pl.equals(p)) continue;
                pl.sendMessage(ChatMessageType.SYSTEM, joined);
            }
            DiscordConnection d = plugin.getDiscordConnection();
            if (d != null)
                d.joinLeaveToDiscord(joined.toPlainText(), plugin.getProxy().getOnlineCount());

            connecting.remove(p);
        }

        private void cancel() {
            connecting.remove(p);
        }
    }
}
