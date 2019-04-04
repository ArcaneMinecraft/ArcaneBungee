package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.MessengerModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.arcaneminecraft.bungee.module.PermissionsModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

public class GreylistCommands {
    private final BaseComponent link;
    private final PermissionsModule module = ArcaneBungee.getInstance().getPermissionsModule();
    private final MessengerModule mModule = ArcaneBungee.getInstance().getMessengerModule();
    private final MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();

    public GreylistCommands() {
        this.link = ArcaneText.urlSingle("https://arcaneminecraft.com/apply/");
        this.link.setColor(ArcaneColor.FOCUS);
    }

    public class Apply extends Command implements TabExecutor {

        public Apply() {
            super(BungeeCommandUsage.APPLY.getName(), BungeeCommandUsage.APPLY.getPermission(), BungeeCommandUsage.APPLY.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }

            ProxiedPlayer p = (ProxiedPlayer) sender;

            if (sender.hasPermission("arcane.build")) {
                BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.apply.already");
                send.setColor(ArcaneColor.CONTENT);
                p.sendMessage(ChatMessageType.SYSTEM, send);
                return;
            }

            BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.apply", link);
            send.setColor(ArcaneColor.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }

    public class Greylist extends Command implements TabExecutor {

        public Greylist() {
            super(BungeeCommandUsage.GREYLIST.getName(), BungeeCommandUsage.GREYLIST.getPermission(), BungeeCommandUsage.GREYLIST.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.GREYLIST.getUsage()));
            } else {
                for (String pl : args) {
                    ProxiedPlayer pp = ProxyServer.getInstance().getPlayer(pl);

                    UUID uuid = pp == null ? null : pp.getUniqueId();
                    if (uuid != null) {
                        greylist(sender, pl, uuid);
                        continue;
                    }

                    uuid = mpModule.getUUID(pl);
                    if (uuid != null) {
                        greylist(sender, pl, uuid);
                        continue;
                    }

                    module.getUUID(pl).thenAcceptAsync(uuidd -> greylist(sender, pl, uuidd));
                }
            }
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return TabCompletePreset.allPlayers(args);
        }

        private void greylist(CommandSender sender, String input, UUID uuid) {
            Locale locale = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getLocale() : null;

            if (uuid == null) {
                BaseComponent send = ArcaneText.translatable(locale, "commands.greylist.failed", input
                );
                send.setColor(ArcaneColor.NEGATIVE);
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                else
                    sender.sendMessage(send);
                return;
            }

            ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
            BaseComponent name;
            if (p == null) {
                String s = mpModule.getName(uuid);
                if (s == null) s = input;
                name = ArcaneText.playerComponent(s, null, uuid.toString());
            } else {
                name = ArcaneText.playerComponentBungee(p);
            }

            module.greylist(uuid).thenAcceptAsync(success -> {
                if (success) {
                    TranslatableComponent send = new TranslatableComponent(
                            ArcaneText.translatableString(null, "messages.meta.greylist"),
                            name
                    );
                    send.setColor(ArcaneColor.META);
                    ProxyServer.getInstance().getConsole().sendMessage(send);
                    mModule.sendMetaToDiscord(send.toPlainText());
                    
                    for (ProxiedPlayer receiver : ProxyServer.getInstance().getPlayers()) {
                        send.setTranslate(
                                ArcaneText.translatableString(receiver.getLocale(), "messages.meta.greylist")
                        );
                        receiver.sendMessage(ChatMessageType.SYSTEM, send);
                    }
                } else {
                    BaseComponent send = ArcaneText.translatable(locale, "commands.greylist.already", name);
                    send.setColor(ArcaneColor.CONTENT);

                    if (sender instanceof ProxiedPlayer)
                        ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                    else
                        sender.sendMessage(send);
                }
            });
        }
    }
}