package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.ArrayList;

public class TabCompletePreset {
    private static ArcaneBungee plugin;

    static void setPlugin(ArcaneBungee plugin) {
        TabCompletePreset.plugin = plugin;
    }

    public static Iterable<String> onlinePlayers(String[] args) {
        ArrayList<String> ret = new ArrayList<>();

        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            if (p.getName().toLowerCase().startsWith(args[args.length - 1]))
                ret.add(p.getName());
        }

        return ret;
    }
}
