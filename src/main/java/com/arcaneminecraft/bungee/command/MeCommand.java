package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.channel.DiscordConnection;
import com.arcaneminecraft.bungee.module.DiscordUserModule;
import com.arcaneminecraft.bungee.module.MessengerModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class MeCommand extends Command implements TabExecutor {
    private final MessengerModule module = ArcaneBungee.getInstance().getMessengerModule();

    public MeCommand() {
        super(BungeeCommandUsage.ME.getName(), BungeeCommandUsage.ME.getPermission(), BungeeCommandUsage.ME.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.ME.getUsage()));
            else
                sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.ME.getUsage()));
            return;
        }

        BaseComponent send = new TranslatableComponent("chat.type.emote", ArcaneText.playerComponentBungee(sender), String.join(" ", args));

        ProxyServer server = ProxyServer.getInstance();

        server.getConsole().sendMessage(send);
        for (ProxiedPlayer p : server.getPlayers()) {
            p.sendMessage(ChatMessageType.SYSTEM, send);
        }

        module.sendMetaToDiscord(send.toPlainText());
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return TabCompletePreset.onlinePlayers(args);
    }
}
