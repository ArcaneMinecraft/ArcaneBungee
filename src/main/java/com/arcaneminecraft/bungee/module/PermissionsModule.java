package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.ArcaneBungee;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.user.User;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.track.PromotionResult;
import net.luckperms.api.track.Track;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class PermissionsModule {
    private final String group = ArcaneBungee.getInstance().getConfig().getString("greylist.group", "trusted");
    private final String track = ArcaneBungee.getInstance().getConfig().getString("greylist.track", "greylist");

    private LuckPerms getLpApi() {
        return LuckPermsProvider.get();
    }

    private boolean greylist(@Nonnull User user) {
        Track tr = getLpApi().getTrackManager().getTrack(track);
        if (tr != null) {
            PromotionResult res = tr.promote(user, QueryOptions.nonContextual().context());
            boolean success = res.wasSuccessful();
            if (success) {
                user.setPrimaryGroup(group);
            }
            return success;
        }
        return false;
    }

    public CompletableFuture<UUID> getUUID(String string) {
        return getLpApi().getUserManager().lookupUniqueId(string);
    }

    public CompletableFuture<Boolean> greylist(UUID uuid) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        getLpApi().getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
            boolean success = greylist(user);
            future.complete(success);

            if (success) {
                getLpApi().getUserManager().saveUser(user);
                getLpApi().getMessagingService().ifPresent(ms -> ms.pushUserUpdate(user));
            }
            getLpApi().getUserManager().cleanupUser(user);
        });
        return future;

    }
}
