package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.MessengerModule;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;

public class SlapCommand extends Command implements TabExecutor {
    private MessengerModule module = ArcaneBungee.getInstance().getMessengerModule();

    public SlapCommand() {
        super(BungeeCommandUsage.SLAP.getName(), BungeeCommandUsage.SLAP.getPermission(), BungeeCommandUsage.SLAP.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length == 0) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.SLAP.getUsage()));
            else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.SLAP.getUsage()));
            return;
        }

        ProxiedPlayer victim = ProxyServer.getInstance().getPlayer(args[0]);

        if (victim == null) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound());
            else sender.sendMessage(ArcaneText.playerNotFound());
            return;
        }

        TranslatableComponent send = new TranslatableComponent(
                ArcaneText.translatableString(null, "messages.meta.slap"),
                ArcaneText.playerComponentBungee(sender),
                ArcaneText.playerComponentBungee(victim)
        );
        send.setColor(ArcaneColor.META);

        module.sendMetaToDiscord(send.toPlainText());

        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            send.setTranslate(ArcaneText.translatableString(p.getLocale(), "messages.meta.slap"));
            p.sendMessage(ChatMessageType.SYSTEM, send);
        }

    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return TabCompletePreset.onlinePlayers(args);
        return Collections.emptyList();
    }
}
