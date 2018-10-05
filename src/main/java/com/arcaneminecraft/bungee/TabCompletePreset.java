package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

public class TabCompletePreset implements Listener {
    private final ArcaneBungee plugin;
    private final List<String> onlinePlayerList;

    TabCompletePreset(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.onlinePlayerList = new ArrayList<>();

        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            this.onlinePlayerList.add(p.getName());
        }
    }



    public Iterable<String> onlinePlayers(String[] args) {
        return argStartsWith(args, onlinePlayerList);
    }

    public Iterable<String> allPlayers(String[] args) {
        return argStartsWith(args, plugin.getSqlDatabase().getAllPlayerName());
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

    @EventHandler
    public void joinEvent(PostLoginEvent e) {
        onlinePlayerList.add(e.getPlayer().getName());
    }

    @EventHandler
    public void leaveEvent(PlayerDisconnectEvent e) {
        onlinePlayerList.remove(e.getPlayer().getName());
    }
}
