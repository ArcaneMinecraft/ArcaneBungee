package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class Option extends Command implements TabExecutor {
    private final ArcaneBungee plugin;

    private enum Options {
        SHOW_DONOR_WELCOME_MESSAGE ("showDonorWelcomeMessage"),
        SHOW_LAST_LOGIN_MESSAGE ("showLastLoginMessage"),
        SHOW_WELCOME_MESSAGE ("showWelcomeMessage"),
        TIMEZONE ("timezone"),

        SHOW_COMMAND_ALERT ("showCommandAlert", "arcane.spy.receive.command"),
        SHOW_COMMAND_ALL_ALERT ("showCommandAlert", "arcane.spy.receive.command.all", "all", "true", "some", "false"),
        SHOW_SIGN_ALERT ("showSignAlert", "arcane.spy.receive.sign"),
        SHOW_XRAY_ALERT ("showXRayAlert", "arcane.spy.receive.xray");

        private static final String PERM_PREFIX = "arcane.option.";
        private final String name;
        private final String permission;
        private final String[] choices;

        Options(String name) {
            this(name, null);
        }

        Options(String name, String permission) {
            this(name, permission, (String[]) null);
        }

        Options(String name, String permission, String... accept) {
            this.name = name;
            this.permission = (permission == null ? PERM_PREFIX + name : permission);
            this.choices = accept;
        }
    }

    /*
commands.gamerule.usage=/gamerule <rule name> [value]
commands.gamerule.success=Game rule %s has been updated to %s
commands.gamerule.norule=No game rule called '%s' is available
commands.gamerule.nopermission=Only server owners can change '%s'
     */

    Option(ArcaneBungee plugin) {
        super(BungeeCommandUsage.OPTION.getName(), BungeeCommandUsage.OPTION.getPermission(), BungeeCommandUsage.OPTION.getAliases());
        this.plugin = plugin;
    }

    private BaseComponent optionValue(Options option, Boolean value) {
        return optionValue(option, String.valueOf(value));
    }

    private BaseComponent optionValue(Options option, String value) {
        BaseComponent ret = new TextComponent(option.name + " = " + value);
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
        }

        String choice;

        if (args.length > 1)
            choice = args[1];
        else
            choice = null;

        if (args[0].equalsIgnoreCase(Options.SHOW_WELCOME_MESSAGE.name)) {
            if (choice == null)
                p.sendMessage(optionValue(Options.SHOW_WELCOME_MESSAGE, plugin.getOptionsStorage().getShowWelcomeMessage(p)));
            // TODO: Add code for setting true/false
            return;
        }

        if (args[0].equalsIgnoreCase(Options.SHOW_DONOR_WELCOME_MESSAGE.name)) {
            if (choice == null)
                p.sendMessage(optionValue(Options.SHOW_DONOR_WELCOME_MESSAGE, plugin.getOptionsStorage().getShowWelcomeMessage(p)));
            // TODO: Add code for setting true/false
            return;
        }

    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        return null;
    }
}
