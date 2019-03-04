package com.arcaneminecraft.bungee;

import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

public interface TabCompletePreset {

    static Iterable<String> onlinePlayers(String[] args) {
        ArrayList<String> a = new ArrayList<>();
        for (ProxiedPlayer p : ProxyServer.getInstance().getPlayers()) {
            a.add(p.getName());
        }

        return argStartsWith(args, a);
    }

    static Iterable<String> allPlayers(String[] args) {
        return argStartsWith(args, ArcaneBungee.getInstance().getSqlDatabase().getAllPlayerName());
    }

    static Iterable<String> argStartsWith(String[] args, Iterable<String> choices) {
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
}
