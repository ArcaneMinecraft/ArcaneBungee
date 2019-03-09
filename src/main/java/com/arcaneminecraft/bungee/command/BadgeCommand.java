package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.module.ChatPrefixModule;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.ExecutionException;

public class BadgeCommand extends Command implements TabExecutor {
    private static final String SUBC_RESET = "-reset";
    private static final String SUBC_HIDE = "-hide";

    private final ChatPrefixModule module = ArcaneBungee.getInstance().getChatPrefixModule();

    public BadgeCommand() {
        super(BungeeCommandUsage.BADGE.getName(), BungeeCommandUsage.BADGE.getPermission(), BungeeCommandUsage.BADGE.getAliases());
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // Must be a player from this point on.
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ArcaneText.noConsoleMsg());
            return;
        }

        ProxiedPlayer p = (ProxiedPlayer) sender;

        // If no args, send list of options
        if (args.length == 0) {
            // TODO: see ChatPrefixModule.badgeList()
            module.badgeList(p.getUniqueId(), false).thenAcceptAsync(list -> p.sendMessage(ChatMessageType.SYSTEM, list));
            return;
        }

        String prefix;

        // Interpret the argument
        if (args.length == 1) {
            prefix = args[0];
        } else {
            prefix = String.join(" ", args);
        }

        try {
            setPrefix(p, Integer.parseInt(prefix));
        } catch (NumberFormatException e) {
            if (prefix.equalsIgnoreCase(SUBC_RESET))
                resetPrefix(p);
            else if (prefix.equalsIgnoreCase(SUBC_HIDE))
                hidePrefix(p);
            else
                setPrefix(p, prefix);
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {

        if (!(sender instanceof ProxiedPlayer))
            return Collections.emptyList();

        Collection<String> prefixes;
        try {
            prefixes = module.listBadges(((ProxiedPlayer) sender).getUniqueId()).get().values();
        } catch (InterruptedException | ExecutionException e) {
            return Collections.emptyList();
        }

        ArrayList<String> ret = new ArrayList<>();

        if (args.length == 1) {
            String lower = args[0].toLowerCase();
            if (SUBC_HIDE.startsWith(lower)) {
                ret.add(SUBC_HIDE);
            }

            if (SUBC_RESET.startsWith(lower)) {
                ret.add(SUBC_RESET);
            }

            for (String s : prefixes) {
                if (s.toLowerCase().startsWith(lower)) {
                    ret.add(s);
                }
            }
        } else {
            for (String s : prefixes) {
                String[] comp = s.split(" ");
                if (comp.length < args.length)
                    continue;

                boolean add = true;
                for (int i = 0; i < args.length; i++) {
                    if (!comp[i].toLowerCase().startsWith(args[i].toLowerCase())) {
                        add = false;
                        break;
                    }
                }
                if (add)
                    ret.add(comp[args.length-1]);
            }
        }

        return ret;
    }

    private void resetPrefix(ProxiedPlayer p) {
        module.clearPriority(p.getUniqueId()).thenAcceptAsync(prefix -> {
            BaseComponent send;
            if (prefix == null) {
                send = ArcaneText.translatable(p.getLocale(), "commands.badge.reset");
            } else {
                BaseComponent prefixBC = new TextComponent();
                for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix)))
                    prefixBC.addExtra(bp);

                send = ArcaneText.translatable(p.getLocale(), "commands.badge.reset.to", prefixBC);
            }
            send.setColor(ArcaneColor.CONTENT);

            p.sendMessage(ChatMessageType.SYSTEM, send);
        });
    }

    private void hidePrefix(ProxiedPlayer p) {
        module.setPriority(p.getUniqueId(), -1).thenAcceptAsync(prefix -> {
            BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.badge.hide");
            send.setColor(ArcaneColor.CONTENT);

            p.sendMessage(ChatMessageType.SYSTEM, send);
        });
    }

    private void setPrefix(ProxiedPlayer p, String prefix) {
        if (prefix.equals("-1"))
            hidePrefix(p);

        module.setPrefix(p.getUniqueId(), prefix).thenAcceptAsync(
                success -> p.sendMessage(ChatMessageType.SYSTEM, success ? prefixSetMsg(p, prefix) : prefixFailedMsg(p))
        );
    }

    private void setPrefix(ProxiedPlayer p, int priority) {
        module.setPriority(p.getUniqueId(), priority).thenAcceptAsync(prefix -> {
            if (prefix == null)
                setPrefix(p, String.valueOf(priority));
            else
                p.sendMessage(ChatMessageType.SYSTEM, prefixSetMsg(p, prefix));
        });
    }

    private BaseComponent prefixSetMsg(ProxiedPlayer p, String prefix) {
        if (prefix == null)
            throw new NullPointerException();

        BaseComponent prefixBC = new TextComponent();
        for (BaseComponent bp : TextComponent.fromLegacyText(ChatColor.translateAlternateColorCodes('&', prefix)))
            prefixBC.addExtra(bp);

        BaseComponent ret = ArcaneText.translatable(p.getLocale(), "commands.badge.set", prefixBC);
        ret.setColor(ArcaneColor.CONTENT);
        return ret;
    }

    private BaseComponent prefixFailedMsg(ProxiedPlayer p) {
        BaseComponent ret = ArcaneText.translatable(p.getLocale(), "commands.badge.invalid");
        ret.setColor(ArcaneColor.NEGATIVE);

        return ret;
    }
}