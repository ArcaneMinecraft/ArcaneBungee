package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.module.data.Player;
import com.arcaneminecraft.bungee.storage.SQLDatabase;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Timestamp;
import java.util.Collection;
import java.util.HashMap;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;

public class MinecraftPlayerModule {
    private final HashMap<UUID, Player> onlinePlayerCache = new HashMap<>();
    private final HashMap<String, UUID> allNameToUuid = new HashMap<>();
    private final HashMap<UUID, String> allUuidToName = new HashMap<>();

    private DiscordUserModule getDUModule() {
        return ArcaneBungee.getInstance().getDiscordUserModule();
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
        return allNameToUuid.get(name.toLowerCase());
    }

    public Player getPlayerData(UUID uuid) {
        return onlinePlayerCache.get(uuid);
    }

    public CompletableFuture<Player> onJoin(ProxiedPlayer p) {
        CompletableFuture<Player> future = new CompletableFuture<>();

        SQLDatabase.getInstance().playerJoin(p).thenAccept(player -> {
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
        SQLDatabase.getInstance().updatePlayer(player);
    }

    private CompletableFuture<?> get(UUID uuid, Supplier<?> online, Function<UUID, CompletableFuture<?>> offline) {
        Player data = getPlayerData(uuid);
        if (data != null) {
            return CompletableFuture.completedFuture(online);
        }

        return offline.apply(uuid);
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Timestamp> getFirstSeen(UUID uuid) {
        return (CompletableFuture<Timestamp>) get(
                uuid,
                getPlayerData(uuid)::getFirstSeen,
                SQLDatabase.getInstance()::getFirstSeen
        );
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<Timestamp> getLastSeen(UUID uuid) {
        return (CompletableFuture<Timestamp>) get(
                uuid,
                getPlayerData(uuid)::getLastLeft,
                SQLDatabase.getInstance()::getLastSeen
        );
    }

    @SuppressWarnings("unchecked")
    public CompletableFuture<TimeZone> getTimeZone(UUID uuid) {
        return (CompletableFuture<TimeZone>) get(
                uuid,
                getPlayerData(uuid)::getTimezone,
                SQLDatabase.getInstance()::getTimeZone
        );
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

    @SuppressWarnings("unchecked")
    public CompletableFuture<Long> getDiscord(UUID uuid) {
        return (CompletableFuture<Long>) get(
                uuid,
                getPlayerData(uuid)::getDiscord,
                SQLDatabase.getInstance()::getDiscord
        );
    }
}
