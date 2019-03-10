package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.module.data.Player;
import com.arcaneminecraft.bungee.storage.SQLDatabase;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class MinecraftPlayerModule {
    private final HashMap<UUID, Player> onlinePlayerCache = new HashMap<>();
    private final HashMap<String, UUID> allNameToUuid = new HashMap<>();
    private final HashMap<UUID, String> allUuidToName = new HashMap<>();
    private final ArrayList<ProxiedPlayer> afkList = new ArrayList<>();

    private DiscordUserModule getDUModule() {
        return ArcaneBungee.getInstance().getDiscordUserModule();
    }

    private SQLDatabase getSQLDatabase() {
        return SQLDatabase.getInstance();
    }

    public void put(UUID uuid, String name) {
        String oldName = allUuidToName.put(uuid, name);
        if (oldName != null)
            allNameToUuid.remove(oldName.toLowerCase());

        allNameToUuid.put(name.toLowerCase(), uuid);
    }

    public Collection<String> getAllNames() {
        return allUuidToName.values();
    }

    public String getName(UUID uuid) {
        return allUuidToName.get(uuid);
    }

    public String getDisplayName(UUID uuid) {
        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
        if (p == null)
            return getName(uuid);
        else
            return p.getDisplayName();
    }

    public UUID getUUID(String name) {
        UUID uuid = allNameToUuid.get(name.toLowerCase());

        if (uuid == null) {
            try {
                UUID test = UUID.fromString(name);
                if (allUuidToName.containsKey(test))
                    uuid = test;
            } catch (IllegalArgumentException ignore) {}
        }

        return uuid;
    }

    public Player getPlayerData(UUID uuid) {
        return onlinePlayerCache.get(uuid);
    }

    public CompletableFuture<Player> onJoin(ProxiedPlayer p) {
        CompletableFuture<Player> future = new CompletableFuture<>();

        getSQLDatabase().playerJoin(p).thenAccept(player -> {
            if (player == null) {
                future.complete(null);
                return;
            }
            onlinePlayerCache.put(p.getUniqueId(), player);
            if (!p.getName().equals(player.getOldName()))
                put(p.getUniqueId(), p.getName());

            future.complete(player);
        });

        return future;
    }

    public void onLeave(ProxiedPlayer p) {
        Player player = onlinePlayerCache.remove(p.getUniqueId());
        getSQLDatabase().updatePlayer(player);
    }

    public void setAFK(ProxiedPlayer p) {
        afkList.add(p);
    }

    public void unsetAFK(ProxiedPlayer p) {
        afkList.remove(p);
    }

    public List<ProxiedPlayer> getAFKList() {
        return afkList;
    }

    public CompletableFuture<Timestamp> getFirstSeen(UUID uuid) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getFirstSeen());
        }

        return getSQLDatabase().getFirstSeen(uuid);
    }

    public CompletableFuture<Timestamp> getLastSeen(UUID uuid) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getLastLeft());
        }

        return getSQLDatabase().getLastSeen(uuid);
    }

    public void setTimeZone(UUID uuid, TimeZone timeZone) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            data.setTimeZone(timeZone);
            return;
        }

        SQLDatabase.getInstance().setTimeZone(uuid, timeZone);
    }

    public CompletableFuture<TimeZone> getTimeZone(UUID uuid) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getTimezone());
        }

        return getSQLDatabase().getTimeZone(uuid);
    }

    public void setDiscord(UUID uuid, long id) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            data.setDiscord(id);
            return;
        }

        getDUModule().put(uuid, id);
        SQLDatabase.getInstance().setDiscord(uuid, id);
    }

    public CompletableFuture<Long> getDiscord(UUID uuid) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getDiscord());
        }

        return getSQLDatabase().getDiscord(uuid);
    }

    public void setOptions(UUID uuid, int options) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            data.setOptions(options);
            return;
        }

        SQLDatabase.getInstance().setOption(uuid, options);
    }

    public CompletableFuture<Integer> getOptions(UUID uuid) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getOptions());
        }

        return SQLDatabase.getInstance().getOptions(uuid);
    }
}
