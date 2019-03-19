package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

import java.util.Locale;

public class ArcaneSurvivalCommand extends Command {
    public ArcaneSurvivalCommand() {
        super(BungeeCommandUsage.ARCANESURVIVAL.getName(), BungeeCommandUsage.ARCANESURVIVAL.getPermission(), BungeeCommandUsage.ARCANESURVIVAL.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        Locale locale = sender instanceof ProxiedPlayer ? ((ProxiedPlayer) sender).getLocale() : null;

        ComponentBuilder send = new ComponentBuilder("[").color(ArcaneColor.META).bold(true)
                .append(ArcaneText.translatableString(locale, "server.name")).color(ArcaneColor.HEADING).bold(true)
                .append("]").color(ArcaneColor.META).bold(true)
                .append(" ", ComponentBuilder.FormatRetention.NONE)
                .append(ArcaneText.translatable(locale, "commands.arcanesurvival.version", ArcaneBungee.getInstance().getDescription().getVersion()))
                .append("\n");

        BaseComponent credit = ArcaneText.translatable(locale, "commands.arcanesurvival.credit", "SimonOrJ, Agentred100, Jugglingman456, & Morios");
        BaseComponent transCredit = ArcaneText.translatable(locale, "commands.arcanesurvival.translation", ArcaneText.translatableString(locale, "language.translatedby"));
        credit.setColor(ArcaneColor.CONTENT);
        transCredit.setColor(ArcaneColor.CONTENT);

        send.append(credit);
        send.append("\n");
        send.append(transCredit);

        if (sender instanceof ProxiedPlayer)
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send.create());
        else
            sender.sendMessage(send.create());

    }
}
