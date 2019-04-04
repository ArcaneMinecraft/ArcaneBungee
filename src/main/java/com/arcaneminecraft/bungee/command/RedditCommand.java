package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class RedditCommand extends Command implements TabExecutor {
    private final MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();

    public RedditCommand() {
        super(BungeeCommandUsage.REDDIT.getName(), BungeeCommandUsage.REDDIT.getPermission(), BungeeCommandUsage.REDDIT.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer && args.length != 0) {
            ProxiedPlayer p = (ProxiedPlayer) sender;
            if (args[0].equalsIgnoreCase("link")) {
                if (args.length > 1) {
                    link(p, args[1]);
                    return;
                }
                p.sendMessage(ArcaneText.usage("/reddit link <username>"));
                return;
            }

            if (args[0].equalsIgnoreCase("unlink")) {
                unlink(p);
                return;
            }
        }

        if (sender instanceof ProxiedPlayer) {
            BaseComponent send = new TextComponent(" Other usage: /reddit <link <username>|unlink>");
            send.setColor(ArcaneColor.CONTENT);
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, LinkCommands.singleLink(((ProxiedPlayer) sender).getLocale(), "Reddit", LinkCommands.REDDIT));
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
        } else {
            sender.sendMessage(LinkCommands.singleLink(null, "Reddit", LinkCommands.REDDIT));
        }
    }

    private void link(ProxiedPlayer p, String reddit) {
        mpModule.getReddit(p.getUniqueId()).thenAccept(current -> {
            if (current == null) {
                if (mpModule.setReddit(p.getUniqueId(), reddit)) {
                    BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.reddit.link.success", reddit);
                    send.setColor(ArcaneColor.CONTENT);
                    p.sendMessage(send);
                } else {
                    BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.reddit.link.badValue", reddit);
                    send.setColor(ArcaneColor.NEGATIVE);
                    p.sendMessage(send);
                }
            } else {
                BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.reddit.link.exists", current);
                send.setColor(ArcaneColor.NEGATIVE);
                p.sendMessage(send);
            }
        });
    }

    private void unlink(ProxiedPlayer p) {
        mpModule.getReddit(p.getUniqueId()).thenAccept(reddit -> {
            if (reddit == null){
                BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.reddit.unlink.failure");
                send.setColor(ArcaneColor.NEGATIVE);
                p.sendMessage(send);
            } else{
                mpModule.setReddit(p.getUniqueId(), null);
                BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.reddit.unlink.success");
                send.setColor(ArcaneColor.CONTENT);
                p.sendMessage(send);
            }
        });
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return TabCompletePreset.argStartsWith(args, ImmutableList.of("link", "unlink"));
        if (args.length == 2 && args[0].equalsIgnoreCase("link"))
            return TabCompletePreset.argStartsWith(args, ImmutableList.of("/u/"));
        return ImmutableList.of();
    }
}
