package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

public class SeenCommands {
    final MinecraftPlayerModule module = ArcaneBungee.getInstance().getMinecraftPlayerModule();

    public class Seen extends Command implements TabExecutor {

        public Seen() {
            super(BungeeCommandUsage.SEEN.getName(), BungeeCommandUsage.SEEN.getPermission(), BungeeCommandUsage.SEEN.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // Console cannot use seen on itself
            if (args.length == 0 && !(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.usage("Server: /seen <player>"));
                return;
            }

            // Gather sender intel
            Locale locale = sender instanceof ProxiedPlayer
                    ? ((ProxiedPlayer) sender).getLocale()
                    : null;
            TimeZone tempTZ;
            try {
                tempTZ = sender instanceof ProxiedPlayer
                        ? module.getTimeZone(((ProxiedPlayer) sender).getUniqueId()).get()
                        : null;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                tempTZ = null;
            }
            final TimeZone timeZone = tempTZ;

            // Gather target intel
            final ProxiedPlayer target = args.length == 0
                    ? (ProxiedPlayer) sender
                    : ProxyServer.getInstance().getPlayer(args[0]);

            UUID uuid;
            if (target == null) {
                uuid = module.getUUID(args[0]);
                if (uuid == null) {
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound());
                    else
                        sender.sendMessage(ArcaneText.playerNotFound());
                    return;
                }
            } else {
                uuid = target.getUniqueId();
            }

            // If player is currently online
            if (target != null) {
                module.getLastSeen(uuid).thenAcceptAsync(time -> {
                    BaseComponent dateAgo = ArcaneText.timeText(time, locale, timeZone, ArcaneColor.FOCUS);

                    BaseComponent send;
                    if (sender == target)
                        send = ArcaneText.translatable(locale, "commands.seen.self.online", dateAgo);
                    else
                        send = ArcaneText.translatable(locale, "commands.seen.other.online",
                                ArcaneText.playerComponentBungee(target),
                                dateAgo);
                    send.setColor(ArcaneColor.CONTENT);

                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                    else
                        sender.sendMessage(send);
                });
                return;
            }

            // Player is offline
            module.getLastSeen(uuid).thenAcceptAsync(time -> {
                String name = module.getName(uuid);
                BaseComponent dateAgo = ArcaneText.timeText(time, locale, timeZone, ArcaneColor.FOCUS);
                BaseComponent send = ArcaneText.translatable(
                        locale,
                        "commands.seen.other",
                        ArcaneText.playerComponent(name, name, uuid.toString(), null, false),
                        dateAgo
                );
                send.setColor(ArcaneColor.CONTENT);

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
            });
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1)
                return TabCompletePreset.allPlayers(args);

            return Collections.emptyList();
        }
    }

    public class FirstSeen extends Command implements TabExecutor {

        public FirstSeen() {
            super(BungeeCommandUsage.FIRSTSEEN.getName(), BungeeCommandUsage.FIRSTSEEN.getPermission(), BungeeCommandUsage.FIRSTSEEN.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // Console cannot use seen on itself
            if (args.length == 0 && !(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.usage("Server: /seenf <player>"));
                return;
            }

            // Gather sender intel
            Locale locale = sender instanceof ProxiedPlayer
                    ? ((ProxiedPlayer) sender).getLocale()
                    : null;
            TimeZone tempTZ;
            try {
                tempTZ = sender instanceof ProxiedPlayer
                        ? module.getTimeZone(((ProxiedPlayer) sender).getUniqueId()).get()
                        : null;
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                tempTZ = null;
            }
            final TimeZone timeZone = tempTZ;

            // Gather target intel
            final ProxiedPlayer target = args.length == 0
                    ? (ProxiedPlayer) sender
                    : ProxyServer.getInstance().getPlayer(args[0]);

            UUID uuid;
            if (target == null) {
                uuid = module.getUUID(args[0]);
                if (uuid == null) {
                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound());
                    else
                        sender.sendMessage(ArcaneText.playerNotFound());
                    return;
                }
            } else {
                uuid = target.getUniqueId();
            }

            // Compile
            module.getFirstSeen(uuid).thenAcceptAsync(time -> {
                BaseComponent dateAgo = ArcaneText.timeText(time, locale, timeZone, ArcaneColor.FOCUS);
                BaseComponent send;

                if (target != sender) {
                    send = ArcaneText.translatable(
                            locale,
                            "commands.firstseen.other",
                            target == null
                                    ? ArcaneText.playerComponent(module.getName(uuid), null, uuid.toString(), null, false)
                                    : ArcaneText.playerComponentBungee(target),
                            dateAgo
                    );
                } else {
                    send = ArcaneText.translatable(
                            locale,
                            "commands.firstseen.self",
                            dateAgo
                    );
                }
                send.setColor(ArcaneColor.CONTENT);

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
            });        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1)
                return TabCompletePreset.allPlayers(args);

            return Collections.emptyList();
        }
    }
}
