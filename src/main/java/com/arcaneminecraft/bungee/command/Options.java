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

public class Options extends Command implements TabExecutor {
    private final ArcaneBungee plugin;


    private enum OptionEntries {
        SHOW_DONOR_WELCOME_MESSAGE ("showDonorWelcomeMessage", OptionsStorage.Toggles.SHOW_DONOR_WELCOME_MESSAGE, "arcane.welcome.donor"),
        SHOW_LAST_LOGIN_MESSAGE ("showLastLoginMessage", OptionsStorage.Toggles.SHOW_LAST_LOGIN_MESSAGE, "arcane.welcome.lastlogin.option"),
        SHOW_WELCOME_MESSAGE ("showWelcomeMessage", OptionsStorage.Toggles.SHOW_WELCOME_MESSAGE, "arcane.welcome.option"),
        TIMEZONE ("timeZone", SQLDatabase::getTimeZoneCache, SQLDatabase::setTimeZoneCache, null, false, new String[]{}),

        SHOW_COMMAND_ALERT ("showCommandAlert", SpyAlert::getReceiveCommandLevel, SpyAlert::setReceiveCommandLevel, "arcane.spy.receive.command", true),
        SHOW_COMMAND_ALL_ALERT ("showCommandAlert", SpyAlert::getReceiveCommandLevel, SpyAlert::setReceiveCommandLevel, "arcane.spy.receive.command.all", true, "all", "true", "some", "false"),
        SHOW_SIGN_ALERT ("showSignAlert", SpyAlert::getReceiveSign, SpyAlert::setReceiveSign, "arcane.spy.receive.sign", true),
        SHOW_XRAY_ALERT ("showXRayAlert", SpyAlert::getReceiveXRay, SpyAlert::setReceiveXRay, "arcane.spy.receive.xray", true);

        private final String name;
        private final String permission;
        private final OptionsStorage.Toggles opt;
        private final String[] choices;
        private final Function<ProxiedPlayer, String> get;
        private final BiConsumer<ProxiedPlayer, String> set;
        private final String defaultValue;
        /**
         * The toggle lasts only this session
         */
        private final boolean sessionOnly;

        OptionEntries(String name, OptionsStorage.Toggles opt) {
            this(name, opt, null);
        }

        OptionEntries(String name, OptionsStorage.Toggles opt, String permission) {
            if (opt == null)
                throw new IllegalArgumentException("OptionsStorage.Options opt is null");
            this.name = name;
            this.permission = permission;
            this.opt = opt;
            this.choices = null;
            this.get = null;
            this.set = null;
            this.defaultValue = String.valueOf(opt.getDefault());
            this.sessionOnly = false;
        }

        OptionEntries(String name, Function<ProxiedPlayer, String> get, BiConsumer<ProxiedPlayer, String> set, String permission, boolean sessionOnly) {
            this(name, get, set, permission, sessionOnly, (String[]) null);
        }

        OptionEntries(String name, Function<ProxiedPlayer, String> get, BiConsumer<ProxiedPlayer, String> set, String permission, boolean sessionOnly, String... accept) {
            if (get == null || set == null)
                throw new IllegalArgumentException("Function get or set is null");
            this.name = name;
            this.permission = permission;
            this.opt = null;
            this.choices = accept;
            this.get = get;
            this.set = set;
            this.defaultValue = get.apply(null);
            this.sessionOnly = sessionOnly;
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

    public Options(ArcaneBungee plugin) {
        super(BungeeCommandUsage.OPTION.getName(), BungeeCommandUsage.OPTION.getPermission(), BungeeCommandUsage.OPTION.getAliases());
        this.plugin = plugin;
    }


    @Override
    public void execute(CommandSender sender, String[] args) {
        plugin.logCommand(sender, BungeeCommandUsage.OPTION.getCommand(), args);

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

            for (OptionEntries o : OptionEntries.values()) {
                if (o.permission == null || sender.hasPermission(o.permission)) {
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

        OptionEntries noPerm = null;

        BaseComponent send;
        for (OptionEntries o : OptionEntries.values()) {
            if (!args[0].equalsIgnoreCase(o.name))
                continue;

            if (o.permission != null && !p.hasPermission(o.permission)) {
                noPerm = o;
                continue;
            }

            if (choice == null) {
                send = new TextComponent("Option " + o.name + " is currently " + o.get(p)); // TODO: Message
            } else if (o.set(p, choice)) {
                send = new TextComponent("Successfully set " + o.name + " to " + choice); // TODO: Message
                if (o.sessionOnly && !o.defaultValue.equalsIgnoreCase(choice))
                send.addExtra(". This option will reset to " + o.defaultValue + " on logout");
            } else {
                send = new TextComponent("Failed to set " + o.name + " to " + choice); // TODO: Message
            }
            send.setColor(ArcaneColor.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
            return;
        }

        if (noPerm != null)
            send = new TextComponent("You do not have permission to change " + noPerm.name); // TODO: Message
        else
            send = new TextComponent("This option does not exist"); // TODO: Message
        send.setColor(ArcaneColor.NEGATIVE);
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length > 1)
            return Collections.emptyList();

        ArrayList<String> ret = new ArrayList<>();
        for (OptionEntries o : OptionEntries.values()) {
            if ((o.permission == null || sender.hasPermission(o.permission)) && o.name.toLowerCase().startsWith(args[0]))
                ret.add(o.name);
        }

        return ret;
    }
}
