package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.ArrayList;
import java.util.List;

public class TabCompletePreset implements Listener {
    private final List<String> playerList;

    TabCompletePreset(ArcaneBungee plugin) {
        playerList = new ArrayList<>();

        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            playerList.add(p.getName());
        }
    }

    public Iterable<String> onlinePlayers(String[] args) {
        String arg = args[args.length - 1];
        if (arg.equals(""))
            return playerList;

        List<String> ret = new ArrayList<>();
        String argL = arg.toLowerCase();

        for (String n : playerList)
            if (n.toLowerCase().startsWith(argL))
                ret.add(n);

        return ret;
    }

    public Iterable<String> validChoices(String[] args, Iterable<String> choices) {
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
        playerList.add(e.getPlayer().getName());
    }

    @EventHandler
    public void leaveEvent(PlayerDisconnectEvent e) {
        playerList.remove(e.getPlayer().getName());
    }
}
