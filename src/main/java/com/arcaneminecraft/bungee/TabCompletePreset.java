package com.arcaneminecraft.bungee;

import com.arcaneminecraft.api.ArcaneColor;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

import java.util.*;

public class TabCompletePreset implements Listener {
    private final ArcaneBungee plugin;
    private final List<String> onlinePlayerList;
    private final Set<String> allPlayerList;

    TabCompletePreset(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.onlinePlayerList = new ArrayList<>();

        for (ProxiedPlayer p : plugin.getProxy().getPlayers()) {
            this.onlinePlayerList.add(p.getName());
        }

        this.allPlayerList = new HashSet<>();

        plugin.getSqlDatabase().getAllPlayers(allPlayerList);
    }



    public Iterable<String> onlinePlayers(String[] args) {
        return argStartsWith(args, onlinePlayerList);
    }

    public Iterable<String> allPlayers(String[] args) {
        return argStartsWith(args, allPlayerList);
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

    /**
     * Taxing operation: this should NOT be used for TabCompleting
     * @param search Part of string to search for
     * @param run Code to run after getting result
     */
    @SuppressWarnings("RedundantStringOperation") // why did this appear on String::substring???
    public void getAllByPartOfName(String search, ReturnRunnable<List<String>> run) {
        String searchLower = search.toLowerCase();
        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            List<String> pl = new ArrayList<>();

            // Match all players
            for (String name : allPlayerList) {
                int start;
                int end;
                String nameLower = name.toLowerCase();
                if ((start = nameLower.indexOf(searchLower)) != -1) {
                    // make it so end - beginning = searching string
                    end = start + search.length();

                    StringBuilder toAdd = new StringBuilder(name.substring(0, start));

                    // Highlight matched portion
                    toAdd.append(ArcaneColor.FOCUS)
                            .append(name.substring(start, end))
                            .append(ArcaneColor.CONTENT);

                    // Search again until it goes through and matches the entire string.
                    while ((start = name.toLowerCase().indexOf(searchLower, end)) != -1) {
                        toAdd.append(name.substring(end, start))
                                .append(ArcaneColor.FOCUS);

                        end += search.length();

                        toAdd.append(name.substring(start, end))
                                .append(ArcaneColor.CONTENT);
                    }
                    toAdd.append(name.substring(end, name.length()));

                    // Add it to the list.
                    pl.add(toAdd.toString());
                }
            }

            if (pl.isEmpty()) {
                run.run(Collections.emptyList());
                return;
            }

            Collections.sort(pl);
            run.run(pl);
        });
    }

    @EventHandler
    public void joinEvent(PostLoginEvent e) {
        onlinePlayerList.add(e.getPlayer().getName());
        allPlayerList.add(e.getPlayer().getName()); // is set: duplicates are not added.
    }

    @EventHandler
    public void leaveEvent(PlayerDisconnectEvent e) {
        onlinePlayerList.remove(e.getPlayer().getName());
    }
}
