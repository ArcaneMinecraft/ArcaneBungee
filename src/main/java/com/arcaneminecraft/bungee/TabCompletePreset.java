package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

public class TabCompletePreset implements Listener {
    private final List<String> onlinePlayerList;
    private final Set<String> allPlayerSet;

    TabCompletePreset(ArcaneBungee plugin) {
        this.onlinePlayerList = new ArrayList<>();

        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            this.onlinePlayerList.add(p.getName());
        }

        this.allPlayerSet = new HashSet<>();

        if (plugin.getSqlDatabase() != null)
            plugin.getSqlDatabase().getAllPlayers(allPlayerSet);
    }



    public Iterable<String> onlinePlayers(String[] args) {
        return argStartsWith(args, onlinePlayerList);
    }

    public Iterable<String> allPlayers(String[] args) {
        return argStartsWith(args, allPlayerSet);
    }



    public Iterable<String> argStartsWith(String[] args, Iterable<String> choices) {
        String arg = args[args.length - 1];
        if (arg.equals(""))
            return choices;

        List<String> ret = new ArrayList<>();
        String argL = arg.toLowerCase();

        for (String n : choices)
            if (n.toLowerCase().startsWith(argL))
                ret.add(n);

        return ret;
    }

    // Get list of all players
    public Set<String> getAllPlayerSet() {
        return allPlayerSet;
    }

    @EventHandler
    public void joinEvent(PostLoginEvent e) {
        onlinePlayerList.add(e.getPlayer().getName());
        allPlayerSet.add(e.getPlayer().getName()); // is set: duplicates are not added.
    }

    @EventHandler
    public void leaveEvent(PlayerDisconnectEvent e) {
        onlinePlayerList.remove(e.getPlayer().getName());
    }
}
