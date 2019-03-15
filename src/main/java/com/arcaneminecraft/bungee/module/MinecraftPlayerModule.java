package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.module.data.ArcanePlayer;
import com.arcaneminecraft.bungee.storage.SQLDatabase;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class MinecraftPlayerModule {
    private final HashMap<UUID, ArcanePlayer> onlinePlayerCache = new HashMap<>();
    private final HashMap<String, UUID> allNameToUuid = new HashMap<>();
    private final HashMap<UUID, String> allUuidToName = new HashMap<>();
    private final ArrayList<ProxiedPlayer> afkList = new ArrayList<>();

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

    public ArcanePlayer getPlayerData(UUID uuid) {
        return onlinePlayerCache.get(uuid);
    }

    public CompletableFuture<ArcanePlayer> onJoin(ProxiedPlayer p) {
        CompletableFuture<ArcanePlayer> future = new CompletableFuture<>();

        getSQLDatabase().playerJoin(p).thenAccept(arcanePlayer -> {
            if (arcanePlayer == null) {
                future.complete(null);
                return;
            }
            onlinePlayerCache.put(p.getUniqueId(), arcanePlayer);
            if (!p.getName().equals(arcanePlayer.getOldName()))
                put(p.getUniqueId(), p.getName());

            future.complete(arcanePlayer);
        });

        return future;
    }

    public void onLeave(ProxiedPlayer p) {
        ArcanePlayer arcanePlayer = onlinePlayerCache.remove(p.getUniqueId());
        getSQLDatabase().updatePlayer(arcanePlayer);
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
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getFirstSeen());
        }

        return getSQLDatabase().getFirstSeen(uuid);
    }

    public CompletableFuture<Timestamp> getLastSeen(UUID uuid) {
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getLastLeft());
        }

        return getSQLDatabase().getLastSeen(uuid);
    }

    public void setTimeZone(UUID uuid, TimeZone timeZone) {
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            data.setTimeZone(timeZone);
            return;
        }

        SQLDatabase.getInstance().setTimeZone(uuid, timeZone);
    }

    public CompletableFuture<TimeZone> getTimeZone(UUID uuid) {
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getTimezone());
        }

        return getSQLDatabase().getTimeZone(uuid);
    }

    public void setDiscord(UUID uuid, long id) {
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            data.setDiscord(id);
        }

        SQLDatabase.getInstance().setDiscord(uuid, id);
    }

    public CompletableFuture<Long> getDiscord(UUID uuid) {
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getDiscord());
        }

        return getSQLDatabase().getDiscord(uuid);
    }

    public boolean setReddit(UUID uuid, String reddit) {
        if (reddit == null || reddit.isEmpty()) {
            ArcanePlayer data = getPlayerData(uuid);
            if (data != null) {
                data.setReddit(null);
                return true;
            }

            SQLDatabase.getInstance().setReddit(uuid, null);
            return true;
        }

        if (reddit.startsWith("/u/"))
            reddit = reddit.substring(3);
        else if (reddit.startsWith("u/"))
            reddit = reddit.substring(2);

        if (reddit.length() > 20 || !reddit.matches("[A-Za-z\\d_\\-]{3,}"))
            return false;

        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            data.setReddit(reddit);
            return true;
        }

        SQLDatabase.getInstance().setReddit(uuid, reddit);
        return true;
    }

    public CompletableFuture<String> getReddit(UUID uuid) {
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getReddit());
        }

        return getSQLDatabase().getReddit(uuid);
    }

    public void setOptions(UUID uuid, int options) {
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            data.setOptions(options);
            return;
        }

        SQLDatabase.getInstance().setOption(uuid, options);
    }

    public CompletableFuture<Integer> getOptions(UUID uuid) {
        ArcanePlayer data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(getPlayerData(uuid).getOptions());
        }

        return SQLDatabase.getInstance().getOptions(uuid);
    }
}
