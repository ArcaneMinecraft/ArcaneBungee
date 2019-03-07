package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.bungee.channel.DiscordConnection;
import com.arcaneminecraft.bungee.module.data.NewsEntry;
import com.arcaneminecraft.bungee.module.SettingModule;
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

import java.sql.Timestamp;
import java.text.DateFormat;
import java.util.LinkedHashMap;
import java.util.Random;
import java.util.TimeZone;
import java.util.UUID;

public class JoinLeaveEvents implements Listener {
    private final ArcaneBungee plugin;
    private final BaseComponent welcomeMessage;
    private final SettingModule sModule = ArcaneBungee.getInstance().getSettingModule();

    private static final String TIMEZONE_LINK = "https://game.arcaneminecraft.com/timezone/";
    private static final String[] DONOR = {
        "Updating the plugin; donor messages will return!"
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

        plugin.getMinecraftPlayerModule().onLeave(e.getPlayer());

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
            plugin.getMinecraftPlayerModule().onJoin(p).thenAccept(player -> {
                Timestamp time = player.getLastLeft();
                String oldName = player.getOldName();
                UUID uuid = p.getUniqueId();

                if (sModule.getNow(SettingModule.Option.SHOW_WELCOME_MESSAGE, uuid)) {
                    p.sendMessage(ChatMessageType.SYSTEM, welcomeMessage);
                }

                if (p.hasPermission("arcane.welcome.donor") && sModule.getNow(SettingModule.Option.SHOW_DONOR_WELCOME_MESSAGE, uuid)) {
                    BaseComponent send = new TextComponent(" ");
                    send.setColor(ArcaneColor.CONTENT);
                    BaseComponent urad = new TextComponent("You are a donor! ");
                    urad.setColor(ArcaneColor.DONOR);
                    send.addExtra(urad);

                    for (BaseComponent bp : TextComponent.fromLegacyText(getRandomDonorMsg()))
                        send.addExtra(bp);

                    p.sendMessage(ChatMessageType.SYSTEM, send);
                }

                if (time != null && sModule.getNow(SettingModule.Option.SHOW_LAST_LOGIN_MESSAGE, uuid)) {
                    // Scheduled because p.getLocale() does not load immediately
                    TimeZone timezone = player.getTimezone();

                    DateFormat df = p.getLocale() == null
                            ? DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL)
                            : DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.FULL, p.getLocale());
                    df.setTimeZone(timezone); // TODO: Test if null timezone works
                    BaseComponent send = new TextComponent(" Your last login was on ");
                    send.setColor(ArcaneColor.HEADING);
                    BaseComponent timeFormat = new TextComponent(df.format(time));
                    timeFormat.setColor(ArcaneColor.FOCUS);
                    send.addExtra(timeFormat);
                    p.sendMessage(ChatMessageType.SYSTEM, send);

                    if (timezone == null) {
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

                NewsEntry news = plugin.getNewsModule().getLatest();

                // TODO: Maybe update content i guess
                send.addExtra(news.getContent());
                send.addExtra("\n");
                p.sendMessage(ChatMessageType.SYSTEM, send);
            });
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
