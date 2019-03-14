package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.bungee.channel.DiscordBot;
import com.arcaneminecraft.bungee.module.MessengerModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.arcaneminecraft.bungee.module.NewsModule;
import com.arcaneminecraft.bungee.module.SettingModule;
import com.arcaneminecraft.bungee.module.data.ArcanePlayer;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.event.SettingsChangedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.sql.Timestamp;
import java.util.LinkedHashMap;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class JoinLeaveEvents implements Listener {
    private final ArcaneBungee plugin;
    private final BaseComponent welcomeHeader;
    private final BaseComponent welcomeFooter;
    private final SettingModule sModule = ArcaneBungee.getInstance().getSettingModule();
    private final MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();
    private final NewsModule nModule = ArcaneBungee.getInstance().getNewsModule();
    private MessengerModule mModule = ArcaneBungee.getInstance().getMessengerModule();

    private static final String TIMEZONE_LINK = "https://game.arcaneminecraft.com/timezone/";
    private final LinkedHashMap<ProxiedPlayer, Joining> connecting = new LinkedHashMap<>();

    JoinLeaveEvents(ArcaneBungee plugin) {
        this.plugin = plugin;

        // BEGIN Welcome Message construction
        TextComponent line = new TextComponent("                           ");
        line.setColor(ChatColor.GRAY);
        line.setStrikethrough(true);

        // Header
        BaseComponent temp = new TextComponent(" " + ArcaneText.translatableString(null, "server.name") + " ");
        temp.setColor(ArcaneColor.HEADING);
        temp.setStrikethrough(false);
        temp.setBold(true);

        this.welcomeHeader = new TextComponent("\n");
        this.welcomeHeader.addExtra(line);
        this.welcomeHeader.addExtra(temp);
        this.welcomeHeader.addExtra(line);


        // Footer
        BaseComponent temp2 = new TextComponent("/links");
        temp2.setColor(ChatColor.AQUA);
        temp2.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/links"));
        temp = new TextComponent(" Run '");
        temp.addExtra(temp2);
        temp.addExtra("' for more info ");
        temp.setColor(ArcaneColor.FOCUS);
        line = new TextComponent("                       ");
        line.setColor(ChatColor.GRAY);
        line.setStrikethrough(true);
        this.welcomeFooter = new TextComponent();
        this.welcomeFooter.addExtra(line);
        this.welcomeFooter.addExtra(temp);
        this.welcomeFooter.addExtra(line);
    }

    private void sendWelcomeMessage(ProxiedPlayer p, boolean newPlayer) {
        BaseComponent player = ArcaneText.playerComponentBungee(p);
        player.setColor(ArcaneColor.META);
        BaseComponent server = ArcaneText.translatable(p.getLocale(), "server.name");
        server.setColor(ArcaneColor.HEADING);

        BaseComponent title;
        BaseComponent subtitle;
        if (newPlayer) {
            title = ArcaneText.translatable(p.getLocale(), "messages.join.new.title", server, player);
            BaseComponent apply = new TextComponent("/apply");
            apply.setColor(ChatColor.DARK_AQUA);
            apply.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/apply"));
            subtitle = ArcaneText.translatable(p.getLocale(), "messages.join.new.subtitle", apply);
        }
        else {
            title = ArcaneText.translatableListRandom(p.getLocale(), "messages.join.titles", server, player);
            subtitle = ArcaneText.translatableListRandom(p.getLocale(), "messages.join.subtitles");
        }
        title.setColor(ArcaneColor.FOCUS);

        subtitle.setColor(ArcaneColor.CONTENT);

        BaseComponent send = new TextComponent(" ");
        send.addExtra(title);
        send.addExtra(" ");
        send.addExtra(subtitle);

        p.sendMessage(ChatMessageType.SYSTEM, welcomeHeader);
        p.sendMessage(ChatMessageType.SYSTEM, send);
        p.sendMessage(ChatMessageType.SYSTEM, welcomeFooter);
    }

    private void sendDonorMessage(ProxiedPlayer p) {
        BaseComponent thanks = ArcaneText.translatableListRandom(p.getLocale(), "messages.join.donor.head");
        thanks.setColor(ArcaneColor.DONOR);

        BaseComponent rand = ArcaneText.translatableListRandom(p.getLocale(), "messages.join.donor.body");
        rand.setColor(ArcaneColor.CONTENT);

        BaseComponent send = new TextComponent(" ");
        send.addExtra(thanks);
        send.addExtra(" ");
        send.addExtra(rand);
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    private void sendNews(ProxiedPlayer p, TimeZone zone) {
        NewsModule.Entry newsEntry = nModule.getLatest();

        BaseComponent news = ArcaneText.translatableListRandom(p.getLocale(), "messages.join.news");
        news.setColor(ArcaneColor.HEADING);
        news.setHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("By ").append(mpModule.getDisplayName(newsEntry.getAuthor()))
                        .append("\n ").append(ArcaneText.timeText(newsEntry.getTime(), p.getLocale(), zone, ArcaneColor.FOCUS))
                        .create()
        ));

        BaseComponent send = new TextComponent();
        send.addExtra(news);
        send.addExtra(" ");
        send.addExtra(newsEntry.getContent());
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    private void sendLastLoginMessage(ProxiedPlayer p, Timestamp last, TimeZone zone) {
        BaseComponent time = ArcaneText.timeText(last, p.getLocale(), zone, ArcaneColor.FOCUS);
        time.setColor(ArcaneColor.FOCUS);

        BaseComponent msg = ArcaneText.translatableListRandom(p.getLocale(), "messages.join.lastlogin");
        msg.setColor(ArcaneColor.HEADING);

        BaseComponent send = new TextComponent();
        send.addExtra(msg);
        send.addExtra(" ");
        send.addExtra(time);

        if (zone == null) {
            BaseComponent temp = new TextComponent(" > ");
            temp.setColor(ChatColor.DARK_GRAY);
            send.addExtra(temp);
            temp = ArcaneText.urlSingle(TIMEZONE_LINK);
            temp.setColor(ChatColor.DARK_AQUA);
            msg = ArcaneText.translatable(p.getLocale(), "messages.join.tip.timezone", temp);
            msg.setColor(ArcaneColor.CONTENT);
            send.addExtra(msg);
        }

        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    @EventHandler
    public void onLoginJoin(PostLoginEvent e) {
        ProxiedPlayer p = e.getPlayer();
        CompletableFuture<ArcanePlayer> future = plugin.getMinecraftPlayerModule().onJoin(p);
        connecting.put(p, new Joining(p, future));
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
        left.setColor(ArcaneColor.META);
        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            p.sendMessage(ChatMessageType.SYSTEM, left);
        }
        DiscordBot.getInstance().joinLeaveToDiscord(left.toPlainText(), plugin.getProxy().getOnlineCount() - 1);
    }

    @EventHandler
    public void onSettingsReceived(SettingsChangedEvent e) {
        Joining j = connecting.get(e.getPlayer());
        if (j != null)
            j.run();
    }

    private class Joining implements Runnable {
        private final ProxiedPlayer p;
        private final CompletableFuture<ArcanePlayer> future;

        Joining(ProxiedPlayer p, CompletableFuture<ArcanePlayer> future) {
            this.p = p;
            this.future = future;
        }

        @Override
        public void run() {
            connecting.remove(p);

            // get player info form database
            future.thenAccept(arcanePlayer -> {
                Timestamp lastLeft = arcanePlayer.getLastLeft();
                String oldName = arcanePlayer.getOldName();
                UUID uuid = p.getUniqueId();

                if (sModule.getNow(SettingModule.Option.SHOW_WELCOME_MESSAGE, uuid)) {
                    sendWelcomeMessage(p, !p.hasPermission("arcane.build"));
                }

                if (p.hasPermission("arcane.welcome.donor") && sModule.getNow(SettingModule.Option.SHOW_DONOR_WELCOME_MESSAGE, uuid)) {
                    sendDonorMessage(p);
                }

                if (sModule.getNow(SettingModule.Option.SHOW_NEWS_ON_JOIN, uuid)) {
                    sendNews(p, arcanePlayer.getTimezone());
                }

                if (lastLeft != null && sModule.getNow(SettingModule.Option.SHOW_LAST_LOGIN_ON_JOIN, uuid)) {
                    sendLastLoginMessage(p, lastLeft, arcanePlayer.getTimezone());
                }

                BaseComponent joined;
                if (oldName == null) {
                    // Exception'd out
                    joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(p));
                } else if (oldName.equals("")) {
                    // New player
                    TranslatableComponent newPlayer = new TranslatableComponent(
                            ArcaneText.translatableString(null, "messages.meta.first"),
                            ArcaneText.playerComponentBungee(p),
                            ArcaneText.translatableString(null, "server.name.short")
                    );
                    newPlayer.setColor(ArcaneColor.META);

                    ProxyServer.getInstance().getConsole().sendMessage(newPlayer);
                    mModule.sendMetaToDiscord(newPlayer.toPlainText());
                    for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
                        newPlayer.setTranslate(ArcaneText.translatableString(
                                pl.getLocale(),
                                "messages.meta.first"
                        ));
                        pl.sendMessage(ChatMessageType.SYSTEM, newPlayer);
                    }
                    joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(p));
                } else {
                    Timestamp first = arcanePlayer.getFirstSeen();
                    long diff = (System.currentTimeMillis() - first.getTime()) / 1000;
                    if (diff < 604800) {
                        // Recently joined player
                        // Joined less than 7 days ago
                        TranslatableComponent newPlayer = new TranslatableComponent(
                                ArcaneText.translatableString(null, "messages.meta.new"),
                                ArcaneText.playerComponentBungee(p),
                                // The time isn't long enough for the int to overflow
                                ArcaneText.timeText(first, (int) diff, true, null, null, ArcaneColor.META)
                        );
                        newPlayer.setColor(ArcaneColor.META);

                        ProxyServer.getInstance().getConsole().sendMessage(newPlayer);
                        mModule.sendMetaToDiscord(newPlayer.toPlainText());
                        for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
                            newPlayer.setTranslate(ArcaneText.translatableString(
                                    pl.getLocale(),
                                    "messages.meta.new"
                            ));
                            pl.sendMessage(ChatMessageType.SYSTEM, newPlayer);
                        }
                    }

                    if (oldName.equals(p.getName())) {
                        // Player with same old name
                        joined = new TranslatableComponent("multiplayer.player.joined", ArcaneText.playerComponentBungee(p));
                    } else {
                        // Player with new name
                        joined = new TranslatableComponent("multiplayer.player.joined.renamed", ArcaneText.playerComponentBungee(p), oldName);
                    }
                }

                joined.setColor(ArcaneColor.META);

                for (ProxiedPlayer pl : plugin.getProxy().getPlayers()) {
                    if (pl.equals(p)) continue;
                    pl.sendMessage(ChatMessageType.SYSTEM, joined);
                }

                DiscordBot.getInstance().joinLeaveToDiscord(joined.toPlainText(), plugin.getProxy().getOnlineCount());
            });
        }

        private void cancel() {
            connecting.remove(p);
        }
    }
}
