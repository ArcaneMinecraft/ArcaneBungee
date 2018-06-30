package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.google.common.collect.ImmutableSet;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.List;

public class Links extends Command implements TabExecutor {
    private final ArcaneBungee plugin;
    private final List<String> candidates;
    // unfortunately gotta show all the links

    public Links(ArcaneBungee plugin) {
        super(BungeeCommandUsage.LINKS.getName(), BungeeCommandUsage.LINKS.getPermission(), BungeeCommandUsage.LINKS.getAliases());
        this.plugin = plugin;
        candidates = new ArrayList<>();
        candidates.add("website");
        candidates.add("forums");
        candidates.add("discord");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.LINKS.getCommand(), args);

        // TODO
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length > 1) {
            return ImmutableSet.of();
        } else if (args.length == 0) {
            return candidates;
        } else {
            List<String> ret = new ArrayList<>();
            for (String s : candidates)
                if (s.startsWith(args[0]))
                    ret.add(s);
            return ret;
        }
    }
}
