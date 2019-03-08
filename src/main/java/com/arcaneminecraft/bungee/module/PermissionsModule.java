package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.ArcaneBungee;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.*;
import me.lucko.luckperms.api.context.ContextSet;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PermissionsModule {
    private final String group = ArcaneBungee.getInstance().getConfig().getString("greylist.group", "trusted");
    private final String track = ArcaneBungee.getInstance().getConfig().getString("greylist.track", "greylist");

    private LuckPermsApi getLpApi() {
        return LuckPerms.getApi();
    }

    private boolean greylist(@Nonnull User user) {
        Track tr = getLpApi().getTrack(track);
        if (tr != null) {
            return tr.promote(user, ContextSet.empty()).getStatus().wasSuccess();
        }
        Group g = getLpApi().getGroup(group);
        if (g == null) {
            return false;
        }
        Node node = getLpApi().getNodeFactory().makeGroupNode(g).build();
        Boolean isSuccess = user.setPermission(node).wasSuccess();

        return true;
    }

    public CompletableFuture<UUID> getUUID(String string) {
        return getLpApi().getUserManager().lookupUuid(string);
    }

    public CompletableFuture<Boolean> greylist(UUID uuid) {
        User u = getLpApi().getUser(uuid);
        if (u != null)
            return CompletableFuture.completedFuture(greylist(u));

        CompletableFuture<Boolean> future = new CompletableFuture<>();
        getLpApi().getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            future.complete(greylist(user));

            getLpApi().getUserManager().saveUser(user);
            getLpApi().getMessagingService().ifPresent(ms -> ms.pushUserUpdate(user));
            getLpApi().getUserManager().cleanupUser(user);
        });
        return future;

    }
}
