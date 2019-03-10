package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.DiscordCommandExecutor;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.TranslatableComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.Iterator;
import java.util.List;

public class ListCommand extends Command implements TabExecutor, DiscordCommandExecutor {
    private final MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();

    public ListCommand() {
        super(BungeeCommandUsage.LIST.getName(), BungeeCommandUsage.LIST.getPermission(), BungeeCommandUsage.LIST.getAliases());
        registerDiscordCommand(BungeeCommandUsage.LIST.getName(), BungeeCommandUsage.LIST.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        boolean showUUID = args.length != 0 && args[0].equalsIgnoreCase("uuids");

        ProxyServer server = ProxyServer.getInstance();

        TranslatableComponent send = new TranslatableComponent("commands.list.players", // 1.13
                String.valueOf(server.getOnlineCount()),
                String.valueOf(server.getConfig().getPlayerLimit())
        );

        BaseComponent body = new TextComponent();
        Iterator<ProxiedPlayer> i = server.getPlayers().iterator();
        if (i.hasNext()) {
            ProxiedPlayer first = i.next();
            body.addExtra(ArcaneText.playerComponentBungee(first));
            if (showUUID)
                body.addExtra("(" + first.getUniqueId() + ")");

            i.forEachRemaining((ProxiedPlayer p) -> {
                body.addExtra(", ");
                body.addExtra(ArcaneText.playerComponentBungee(p));
                if (showUUID)
                    body.addExtra("(" + p.getUniqueId() + ")");
            });
        }

        send.addWith(body);

        if (sender instanceof ProxiedPlayer) {
            ((ProxiedPlayer)sender).sendMessage(ChatMessageType.SYSTEM, send);
        } else {
            sender.sendMessage(send);
        }

    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return ImmutableSet.of("uuids");
    }

    @Override
    public String getDiscordUsage() {
        return "list [uuids]";
    }

    @Override
    public String getDiscordDescription() {
        return "Lists all players that are current in-game.";
    }

    @Override
    public boolean executeDiscordCommand(Message m, String[] args) {
        boolean uuid = args.length == 2 && args[1].equalsIgnoreCase("uuids");

        final String onlineFormat = "There are **%d**/%d players online";

        List<ProxiedPlayer> afk = mpModule.getAFKList();
        StringBuilder online;
        Iterator<ProxiedPlayer> i = ProxyServer.getInstance().getPlayers().iterator();
        if (!i.hasNext()) {
            online = new StringBuilder("*nobody*");
        } else {
            online = new StringBuilder();
            while (i.hasNext()) {
                ProxiedPlayer p = i.next();
                if (afk.contains(p))
                    online.append("[AFK] ");
                online.append("**").append(p.getName()).append("**");
                if (uuid)
                    online.append(" (").append(p.getUniqueId()).append(")");
                if (i.hasNext())
                    online.append('\n');
            }
        }


        int onlineCount = ProxyServer.getInstance().getOnlineCount();
        EmbedBuilder embed = new EmbedBuilder()
                .setTitle("Online Players")
                .setDescription("Usage: " + getDiscordPrefix() + args[0] + " [uuids]")
                .addField(
                        String.format(onlineFormat, onlineCount, ProxyServer.getInstance().getConfig().getPlayerLimit()),
                        online.toString(),
                        false
                )
                .setColor(onlineCount == 0 ? 0xFFAA00 : 0x00AA00);

        m.getChannel().sendMessage(embed.build()).complete();

        return true;
    }
}
