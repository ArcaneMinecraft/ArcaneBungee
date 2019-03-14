package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class Report extends Command implements TabExecutor {
    public Report(ArcaneBungee plugin) {
        super("report","arcane.command.report");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        TextComponent a = new TextComponent("This command is not implemented yet");
        a.setColor(ChatColor.GRAY);

        if (sender instanceof ProxiedPlayer)
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, a);
        else
            sender.sendMessage(a);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
