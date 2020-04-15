package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.ArcaneBungee;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
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
        Group gr = getLpApi().getGroupManager().getGroup(group);
        if (tr == null) {
            ArcaneBungee.getInstance().getLogger().warning("Track '" + track + "' does not exist!");
            return false;
        }
        if (gr == null) {
            ArcaneBungee.getInstance().getLogger().warning("Group '" + group + "' does not exist!");
            return false;
        }

/*
        if (tr != null) {
            PromotionResult res = tr.promote(user, QueryOptions.nonContextual().context());
            boolean success = res.wasSuccessful();
            if (success) {
                user.setPrimaryGroup(group);
            }
            return success;
        }
*/

        // TODO: This kept not working.
        //PromotionResult res = tr.promote(user, QueryOptions.nonContextual().context());
        //boolean success = res.wasSuccessful();

        // Workaround:
        String oldGroup = tr.getPrevious(gr);
        Node oldGrNode = InheritanceNode.builder(oldGroup).build();
        Node grNode = InheritanceNode.builder(gr).build();
        boolean success = user.data().remove(oldGrNode).wasSuccessful() && user.data().add(grNode).wasSuccessful();
        // Workaround end
        if (success) {
            user.setPrimaryGroup(group);
            getLpApi().getUserManager().saveUser(user);
        }

        return success;
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
