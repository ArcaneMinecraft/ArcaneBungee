package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.DataMutateResult;
import me.lucko.luckperms.api.LuckPermsApi;
import me.lucko.luckperms.api.User;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;

// TODO: Cleanup all the messages
public class GreylistCommands {
    private final ArcaneBungee plugin;
    private static final BaseComponent LINK = ArcaneText.urlSingle("https://arcaneminecraft.com/apply/");

    public GreylistCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
    }

    private void addToGreylist(User u, CommandSender sender) {
        try {
            // Load API
            LuckPermsApi api = LuckPerms.getApi();

            // set permission and stuff by simulating track promotion
            DataMutateResult r = u.setPermission(
                    api.getNodeFactory().makeGroupNode(
                            api.getGroup("trusted")
                    ).build());
            if (r.wasFailure()) {
                sender.sendMessage("Player " + ArcaneColor.FOCUS + u.getName() + ArcaneColor.CONTENT + " was already greylisted!");
                return;
            }
            u.unsetPermission(
                    api.getNodeFactory().makeGroupNode(
                            api.getGroup("default")
                    ).build());
            api.getStorage().saveUser(u).thenAcceptAsync( success ->{
                if (!success) {
                    sender.sendMessage("There was a problem while modifying groups for " + ArcaneColor.FOCUS + u.getName() + ArcaneColor.CONTENT + ".  Please contact an administrator.");
                    return;
                }

                u.refreshCachedData();
                plugin.getProxy().broadcast(ArcaneColor.META + u.getName()
                        + " is now greylisted");
            });

        } catch (IllegalStateException | NoClassDefFoundError e) {
            sender.sendMessage("Is LuckPerms loaded on the server?");
        }
    }

    public class Apply extends Command implements TabExecutor {

        public Apply() {
            super(BungeeCommandUsage.APPLY.getName(), BungeeCommandUsage.APPLY.getPermission(), BungeeCommandUsage.APPLY.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.APPLY.getCommand(), args);

            if (!(sender instanceof ProxiedPlayer)) {
                sender.sendMessage(ArcaneText.noConsoleMsg());
                return;
            }

            ProxiedPlayer p = (ProxiedPlayer) sender;

            if (sender.hasPermission("arcane.build")) {
                BaseComponent send = new TextComponent("You are already greylisted and have permission to build!");
                send.setColor(ArcaneColor.CONTENT);
                p.sendMessage(ChatMessageType.SYSTEM, send);
                return;
            }

            BaseComponent send = new TextComponent("Apply at: ");
            send.addExtra(LINK);
            send.setColor(ArcaneColor.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
            // TODO: Write application in-game?
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return Collections.emptyList();
        }
    }

    public class Greylist extends Command implements TabExecutor {

        public Greylist() {
            super(BungeeCommandUsage.GREYLIST.getName(), BungeeCommandUsage.GREYLIST.getPermission(), BungeeCommandUsage.GREYLIST.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            if (args.length == 0) {
                sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.GREYLIST.getUsage()));
            } else {
                for (String pl : args) {
                    try {
                        // Load API
                        LuckPermsApi api = LuckPerms.getApi();

                        // Get lp user object
                        User u = api.getUser(pl);
                        if (u == null) {
                            plugin.getSqlDatabase().getPlayerUUID(pl, uuid -> {
                                if (uuid == null) {
                                    sender.sendMessage("Player " + ArcaneColor.FOCUS + pl + ArcaneColor.CONTENT + " doesn't exist on ArcaneBungee database.");
                                    return;
                                }
                                api.getUserManager().loadUser(uuid).thenAcceptAsync(user -> {
                                    if (user == null) {
                                        // it for some reason never reaches this point.
                                        sender.sendMessage("Player " + ArcaneColor.FOCUS + pl + ArcaneColor.CONTENT + " doesn't exist on LuckPerms database.");
                                        return;
                                    }
                                    User ul = api.getUser(uuid);
                                    addToGreylist(ul, sender);
                                    api.cleanupUser(ul);
                                }, api.getStorage().getSyncExecutor());
                            });
                        } else {
                            addToGreylist(api.getUser(pl), sender);
                        }
                    } catch (IllegalStateException | NoClassDefFoundError e) {
                        sender.sendMessage("Is LuckPerms loaded on the server?");
                    }
                }
            }
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            if (args.length == 1)
                return plugin.getTabCompletePreset().allPlayers(args);
            return Collections.emptyList();
        }
    }
}