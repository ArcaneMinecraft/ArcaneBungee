package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.*;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.HashMap;
import java.util.UUID;

public class TellCommands {
    private final ArcaneBungee plugin;
    private final HashMap<CommandSender, CommandSender> lastReceived = new HashMap<>();

    public TellCommands(ArcaneBungee plugin) {
        this.plugin = plugin;
     }

    public class Message extends Command implements TabExecutor {

        public Message() {
            super(BungeeCommandUsage.MSG.getName(), BungeeCommandUsage.MSG.getPermission(), BungeeCommandUsage.MSG.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            plugin.logCommand(sender, BungeeCommandUsage.MSG.getCommand(), args);

            if (args.length < 2) {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.MSG.getUsage()));
                else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.MSG.getUsage()));
                return;
            }

            // Get recipient
            CommandSender p = plugin.getProxy().getPlayer(args[0]);
            if (p == null) {
                notFound(sender, args[0], null, false);
                return;
            }

            messenger(sender, p, args, 1);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return plugin.getTabCompletePreset().onlinePlayers(args);
        }
    }

    public class Reply extends Command implements TabExecutor {

        public Reply() {
            super(BungeeCommandUsage.REPLY.getName(), BungeeCommandUsage.REPLY.getPermission(), BungeeCommandUsage.REPLY.getAliases());
        }

        @Override
        public void execute(CommandSender sender, String[] args) {
            // CoreProtect logger in each if statements below

            if (args.length == 0) {
                plugin.logCommand(sender, BungeeCommandUsage.REPLY.getCommand(), args);

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.usage(BungeeCommandUsage.REPLY.getUsage()));
                else sender.sendMessage(ArcaneText.usage(BungeeCommandUsage.REPLY.getUsage()));
                return;
            }

            CommandSender p = lastReceived.get(sender);
            if (p == null) {
                plugin.logCommand(sender, BungeeCommandUsage.REPLY.getCommand(), args);

                BaseComponent send = new TextComponent("There is nobody to reply to");
                send.setColor(ChatColor.RED);

                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
                else sender.sendMessage(send);
                return;
            }

            if (p instanceof ProxiedPlayer) {
                // Log with /msg instead for easier readability.
                plugin.logCommand(sender, BungeeCommandUsage.MSG.getCommand() + " " + p.getName() + " " + String.join(" ", args));

                if (!((ProxiedPlayer) p).isConnected()) {
                    notFound(sender, p.getName(), ((ProxiedPlayer) p).getUniqueId(), true);
                    return;
                }
            } else {
                plugin.logCommand(sender, BungeeCommandUsage.REPLY.getCommand(), args);
            }

            messenger(sender, p, args, 0);
        }

        @Override
        public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
            return plugin.getTabCompletePreset().onlinePlayers(args);
        }
    }
    
    private void notFound(CommandSender sender, String name, UUID uuid, final boolean isProperCase) {
        if (uuid == null)
            uuid = plugin.getSqlDatabase().getPlayerUUID(name);
        
        if (uuid == null) {
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, ArcaneText.playerNotFound());
            else
                sender.sendMessage(ArcaneText.playerNotFound());
        } else {
            String caseCorrectedName = isProperCase ? name : plugin.getSqlDatabase().getPlayerName(uuid);
            plugin.getSqlDatabase().getDiscordThen(uuid, id -> {
                if (sender instanceof ProxiedPlayer)
                    ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, receipentNotOnline(caseCorrectedName, id));
                else
                    sender.sendMessage(receipentNotOnline(caseCorrectedName, id));
            });
        }
    }
    
    private BaseComponent receipentNotOnline(String name, long id) {
        TextComponent gt = new TextComponent("> ");
        gt.setColor(ChatColor.DARK_GRAY);

        BaseComponent ret = new TextComponent(gt);

        TextComponent n = new TextComponent(name);
        n.setColor(ArcaneColor.FOCUS);
        ret.addExtra(n);

        if (id == 0) {
            ret.addExtra("Player '");
            ret.setColor(ArcaneColor.CONTENT);
            ret.addExtra(n);
            ret.addExtra("' is not online and does not have a linked Discord account");
        } else {
            ret.addExtra(n);
            ret.setColor(ArcaneColor.CONTENT);
            ret.addExtra(" can be reached on Discord @");

            String[] info = plugin.getDiscordConnection().getNicknameUsernameDiscriminator(id);
            TextComponent main,
                    discord = new TextComponent();
            discord.setColor(ArcaneColor.CONTENT);
            discord.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Search for this user on Discord").create()));

            // if no nickname
            if (info[0] == null) {
                main = new TextComponent(info[1]);
                main.setColor(ArcaneColor.FOCUS);
                discord.addExtra(main);
                discord.addExtra("#" + info[2]);
            } else {
                main = new TextComponent(info[0]);
                main.setColor(ArcaneColor.FOCUS);
                discord.addExtra(main);
                discord.addExtra(" (" + info[1] + "#" + info[2] + ")");
            }
            ret.addExtra("instead");

        }
        return ret;
    }
    
    private void messenger(CommandSender from, CommandSender to, String[] args, int fromIndex) {
        BaseComponent msg = ArcaneText.url(args, fromIndex);

        msg.setColor(ArcaneColor.CONTENT);
        msg.setItalic(true);

        messageSender(from, to, msg, false); // send to "sender" as "To p: msg"
        messageSender(to, from, msg, true);

        // Update sender-receiver map
        lastReceived.put(to, from);
    }

    // TODO: Should we make messaging using vanilla translatable?
    private void messageSender(CommandSender player, CommandSender name, BaseComponent msg, boolean isReceiving) {
        TextComponent send = new TextComponent();

        // Beginning
        TextComponent header = new TextComponent();
        header.setColor(ArcaneColor.HEADING);

        TextComponent in = new TextComponent("> ");
        in.setColor(ChatColor.DARK_GRAY);
        header.addExtra(in);

        header.addExtra((isReceiving ? "From" : "To") + " ");

        // Add a click action
        BaseComponent receiving = ArcaneText.playerComponentBungee(name);
        if (!(name instanceof ProxiedPlayer))
            receiving.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, BungeeCommandUsage.REPLY.getCommand() + " "));
        header.addExtra(receiving);

        TextComponent out = new TextComponent(": ");
        out.setColor(ChatColor.DARK_GRAY);
        header.addExtra(out);

        send.addExtra(header);

        // Message
        send.addExtra(msg);

        // Send Messages
        if (player instanceof ProxiedPlayer)
            ((ProxiedPlayer) player).sendMessage(ChatMessageType.SYSTEM, send);
        else
            player.sendMessage(send);
    }
}
