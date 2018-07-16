package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.SpyAlert;
import com.arcaneminecraft.bungee.storage.OptionsStorage;
import com.arcaneminecraft.bungee.storage.SQLDatabase;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class Option extends Command implements TabExecutor {
    private final ArcaneBungee plugin;


    private enum Options {
        SHOW_DONOR_WELCOME_MESSAGE ("showDonorWelcomeMessage", OptionsStorage.Options.SHOW_DONOR_WELCOME_MESSAGE),
        SHOW_LAST_LOGIN_MESSAGE ("showLastLoginMessage", OptionsStorage.Options.SHOW_LAST_LOGIN_MESSAGE),
        SHOW_WELCOME_MESSAGE ("showWelcomeMessage", OptionsStorage.Options.SHOW_WELCOME_MESSAGE),
        TIMEZONE ("timezone", SQLDatabase::getTimeZoneCache, SQLDatabase::setTimeZoneCache, null, new String[]{}),

        SHOW_COMMAND_ALERT ("showCommandAlert", SpyAlert::getReceiveCommandLevel, SpyAlert::setReceiveCommandLevel, "arcane.spy.receive.command"),
        SHOW_COMMAND_ALL_ALERT ("showCommandAlert", SpyAlert::getReceiveCommandLevel, SpyAlert::setReceiveCommandLevel, "arcane.spy.receive.command.all", "all", "true", "some", "false"),
        SHOW_SIGN_ALERT ("showSignAlert", SpyAlert::getReceiveSign, SpyAlert::setReceiveSign, "arcane.spy.receive.sign"),
        SHOW_XRAY_ALERT ("showXRayAlert", SpyAlert::getReceiveXRay, SpyAlert::setReceiveXRay, "arcane.spy.receive.xray");

        private static final String PERM_PREFIX = "arcane.option.";
        private final String name;
        private final String permission;
        private final OptionsStorage.Options opt;
        private final String[] choices;
        private final Function<ProxiedPlayer, String> get;
        private final BiConsumer<ProxiedPlayer, String> set;

        Options(String name, OptionsStorage.Options opt) {
            this(name, opt, null);
        }

        Options(String name, OptionsStorage.Options opt, String permission) {
            if (opt == null)
                throw new IllegalArgumentException("OPtionsStorage.Options opt is null");
            this.name = name;
            this.permission = (permission == null ? PERM_PREFIX + name : permission);
            this.opt = opt;
            this.choices = null;
            this.get = null;
            this.set = null;
        }

        Options(String name, Function<ProxiedPlayer, String> get, BiConsumer<ProxiedPlayer, String> set, String permission) {
            this(name, get, set, permission, (String[]) null);
        }

        Options(String name, Function<ProxiedPlayer, String> get, BiConsumer<ProxiedPlayer, String> set, String permission, String... accept) {
            if (get == null || set == null)
                throw new IllegalArgumentException("Function get or set is null");
            this.name = name;
            this.permission = (permission == null ? PERM_PREFIX + name : permission);
            this.opt = null;
            this.choices = accept;
            this.get = get;
            this.set = set;
        }

        private String get(ProxiedPlayer p) {
            if (opt != null) {
                return String.valueOf(OptionsStorage.get(p, opt));
            }
            return get != null ? get.apply(p) : null;
        }

        private boolean set(ProxiedPlayer p, String value) {
            // null choices mean true/false
            if (opt != null || choices == null) {
                boolean bool;
                if (value.equalsIgnoreCase("true"))
                    bool = true;
                else if (value.equalsIgnoreCase("false"))
                    bool = false;
                else
                    return false;

                if (opt != null)
                    OptionsStorage.set(p, opt, bool);
                else if (set != null)
                    set.accept(p, value);
                return true;
            }

            // length of 0 means accept any
            if (choices.length == 0) {
                if (set != null)
                    set.accept(p, value);
                return true;
            }

            for (String c : choices) {
                if (value.equalsIgnoreCase(c)) {
                    if (set != null)
                        set.accept(p, value);
                    return true;
                }
            }
            return false;
        }
    }

    public Option(ArcaneBungee plugin) {
        super(BungeeCommandUsage.OPTION.getName(), BungeeCommandUsage.OPTION.getPermission(), BungeeCommandUsage.OPTION.getAliases());
        this.plugin = plugin;
    }

    private BaseComponent optionValue(Options option, ProxiedPlayer p) {
        BaseComponent ret = new TextComponent(option.name + " = " + option.get(p));
        ret.setColor(ArcaneColor.CONTENT);
        return ret;
    }


    @Override
    public void execute(CommandSender sender, String[] args) {
        if (plugin.getSqlDatabase() == null) {
            BaseComponent send = new TextComponent("SQL database does not exist. Unable to set or change options");
            send.setColor(ArcaneColor.NEGATIVE);
            if (sender instanceof ProxiedPlayer)
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
            else
                sender.sendMessage(send);
            return;
        }

        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ArcaneText.noConsoleMsg());
            return;
        }

        ProxiedPlayer p = (ProxiedPlayer) sender;

        if (args.length == 0) {
            BaseComponent send = new TextComponent("Available options:");
            send.setColor(ArcaneColor.CONTENT);

            for (Options o : Options.values()) {
                if (sender.hasPermission(o.permission)) {
                    send.addExtra(" ");
                    BaseComponent bp = new TextComponent(o.name);
                    bp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/option " + o.name + " "));
                    bp.setColor(ArcaneColor.FOCUS);
                    send.addExtra(bp);
                }
            }
            p.sendMessage(ChatMessageType.SYSTEM, send);
            return;
        }

        String choice;

        if (args.length > 1)
            choice = args[1];
        else
            choice = null;

        Option.Options noPerm = null;
    /*
commands.gamerule.usage=/gamerule <rule name> [value]
commands.gamerule.success=Game rule %s has been updated to %s
commands.gamerule.norule=No game rule called '%s' is available
commands.gamerule.nopermission=Only server owners can change '%s'
     */

        for (Options o : Options.values()) {
            if (!args[0].equalsIgnoreCase(o.name))
                continue;

            if (!p.hasPermission(o.permission)) {
                noPerm = o;
                continue;
            }

            if (choice == null) {
                p.sendMessage(optionValue(o, p));
                return;
            }

            if (o.set(p, choice)) {
                p.sendMessage(o + "Success"); // TODO: Message
                return;
            }

            p.sendMessage(o + "failure args"); // TODO: Message
            return;
        }

        if (noPerm != null)
            p.sendMessage(noPerm + "no perm msg"); // TODO: Message
        else
            p.sendMessage("does not exist msg"); // TODO: Message
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length > 1)
            return Collections.emptyList();

        ArrayList<String> ret = new ArrayList<>();
        for (Options o : Options.values()) {
            if (sender.hasPermission(o.permission) && o.name.toLowerCase().startsWith(args[0]))
                ret.add(o.name);
        }

        return ret;
    }
}
