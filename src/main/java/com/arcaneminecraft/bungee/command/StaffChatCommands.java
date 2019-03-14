package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.MessengerModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ChatEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.TabExecutor;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.event.EventPriority;

import java.util.Collections;
import java.util.HashSet;

public class StaffChatCommands implements Listener {
    private final HashSet<ProxiedPlayer> toggled = new HashSet<>();
    private final MessengerModule module = ArcaneBungee.getInstance().getMessengerModule();

    public class Chat extends Command implements TabExecutor {
        public Chat() {
            super(BungeeCommandUsage.STAFFCHAT.getName(), BungeeCommandUsage.STAFFCHAT.getPermission(), BungeeCommandUsage.STAFFCHAT.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // Command logger inside this.broadcast()

            if (args.length == 0) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.STAFFCHAT.getUsage()));
                else
                    sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.STAFFCHAT.getUsage()));
                return;
            }
            module.staffChat(sender, String.join(" ", args));
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }

    public class Toggle extends Command implements TabExecutor {

        public Toggle() {
            super(BungeeCommandUsage.STAFFCHATTOGGLE.getName(), BungeeCommandUsage.STAFFCHATTOGGLE.getPermission(), BungeeCommandUsage.STAFFCHATTOGGLE.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // No real reason to log toggle command

            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }

            ProxiedPlayer p = (ProxiedPlayer) sender;

            BaseComponent tog;
            if (toggled.add(p)){
                tog = new TranslatableComponent("options.on");
                tog.setColor(ArcaneColor.POSITIVE);
            } else {
                toggled.remove(p);
                tog = new TranslatableComponent("options.off");
                tog.setColor(ArcaneColor.NEGATIVE);
            }
            BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.staffchat.toggle", tog);
            send.setColor(ArcaneColor.CONTENT);

            p.sendMessage(ChatMessageType.SYSTEM, send);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void staffChatGrabber(ChatEvent e) {
        // if is a command and staff chat is not toggled
        //noinspection SuspiciousMethodCalls - e.getSender() can be a ProxiedPlayer
        if (e.isCommand() || !toggled.contains(e.getSender()))
            return;

        e.setCancelled(true);
        module.staffChat((ProxiedPlayer) e.getSender(), e.getMessage());
    }
}
