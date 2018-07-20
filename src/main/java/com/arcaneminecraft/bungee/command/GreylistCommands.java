package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.ReturnRunnable;
import me.lucko.luckperms.LuckPerms;
import me.lucko.luckperms.api.*;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

// TODO: Cleanup all the messages
public class GreylistCommands {
    private final ArcaneBungee plugin;
    private final BaseComponent link;
    private final String group;
    private final String track;

    public GreylistCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
        this.link = ArcaneText.urlSingle("https://arcaneminecraft.com/apply/");
        this.link.setColor(ArcaneColor.FOCUS);

        this.group = plugin.getConfig().getString("greylist.group");
        this.track = plugin.getConfig().getString("greylist.track");

        plugin.getProxy().getScheduler().runAsync(plugin, () -> {
            while (!LuckPerms.getApiSafe().isPresent()) {
                try {
                    Thread.sleep(1000L);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            Node node = getLpApi().getNodeFactory().newBuilder("arcane.build").build();

            Group g = getLpApi().getGroup(this.group);

            if (g == null)
                this.plugin.getLogger().warning("The greylist group '" + group + "' does not exist");
            else if (!g.hasPermission(node).asBoolean())
                this.plugin.getLogger().warning("The greylist group '" + group + "' does not have arcane.build permission");

        });

    }

    private LuckPermsApi getLpApi() {
        return LuckPerms.getApi();
    }

    private void isNull(CommandSender sender, String what) {
        BaseComponent send = new TextComponent("Greylist > An error occured: " + what + " returns null. Please contact the server administrators!");
        send.setColor(ChatColor.RED);
        if (sender instanceof ProxiedPlayer)
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
        else
            sender.sendMessage(send);
    }

    private void addToGreylistThen(CommandSender sender, String toGreylist, ReturnRunnable.More<Boolean, User> run) {
        Consumer<User> then = (user) -> {
            Group g = getLpApi().getGroup(group);
            if (g == null) {
                isNull(sender, "Group " + group);
                return;
            }
            Node node = getLpApi().getNodeFactory().makeGroupNode(g).build();
            Boolean isSuccess = user.setPermission(node).wasSuccess();

            run.run(isSuccess, user);

            Track t = getLpApi().getTrack("Track " + track);
            if (t == null) {
                isNull(sender, track);
                return;
            }

            String before = t.getPrevious(g);
            if (before == null) {
                isNull(sender, "Previous group in track " + track);
                return;
            }

            node = getLpApi().getNodeFactory().makeGroupNode(before).build();
            user.unsetPermission(node);
        };

        User u = getLpApi().getUser(toGreylist);

        if (u == null) {
            UUID uuid = plugin.getSqlDatabase().getPlayerUUID(toGreylist);

            if (uuid == null) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound(toGreylist));
                else
                    sender.sendMessage(ArcaneText.playerNotFound(toGreylist));
                return;
            }

            getLpApi().getUserManager().loadUser(uuid).thenAcceptAsync(then);
        } else {
            then.accept(u);
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
            send.addExtra(link);
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
            plugin.getCommandLogger().coreprotect(sender, BungeeCommandUsage.GREYLIST.getCommand(), args);

            if (args.length == 0) {
                sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.GREYLIST.getUsage()));
            } else {
                for (String pl : args) {
                    addToGreylistThen(sender, pl, (success, user) -> {
                        BaseComponent send = new TextComponent(ArcaneText.playerComponent(pl, null, user.getUuid().toString()));
                        if (success) {
                            send.addExtra(" is now greylisted");
                            send.setColor(ArcaneColor.META);

                            plugin.getProxy().getConsole().sendMessage(send);
                            for (ProxiedPlayer p : plugin.getProxy().getPlayers())
                                p.sendMessage(ChatMessageType.SYSTEM, send);
                        } else {
                            send.addExtra(" was already greylisted");
                            send.setColor(ChatColor.GRAY);

                            if (sender instanceof ProxiedPlayer)
                                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
                            else
                                sender.sendMessage(send);
                        }
                    });
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