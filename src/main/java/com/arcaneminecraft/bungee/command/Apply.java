package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;

public class Apply extends Command implements TabExecutor {
    private final ArcaneBungee plugin;

    public Apply(ArcaneBungee plugin) {
        super(BungeeCommandUsage.APPLY.getName(), BungeeCommandUsage.APPLY.getPermission(), BungeeCommandUsage.APPLY.getAliases());
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.APPLY.getCommand(), args);

        // TODO
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return Collections.emptyList();
    }
}
