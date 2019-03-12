package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.SpyAlert;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.arcaneminecraft.bungee.module.SettingModule;
import com.arcaneminecraft.bungee.module.data.Option;
import com.google.common.collect.ImmutableList;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

public class OptionCommand extends Command implements TabExecutor {

    private HashMap<String, Option> options = new HashMap<>();

    public OptionCommand() {
        super(BungeeCommandUsage.OPTION.getName(), BungeeCommandUsage.OPTION.getPermission(), BungeeCommandUsage.OPTION.getAliases());

        Iterable<String> values = ImmutableList.of("true", "false");

        SettingModule sModule = ArcaneBungee.getInstance().getSettingModule();
        MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();
        SpyAlert spyAlert = SpyAlert.getInstance();

        for (SettingModule.Option opt : SettingModule.Option.values()) {
            options.put(opt.name.toLowerCase(), new Option(
                    opt.name,
                    setBoolean(opt, sModule::set),
                    getBoolean(opt, sModule::get),
                    opt.permission,
                    values,
                    opt.description
            ));
        }

        options.put("spyallcommands", new Option(
                "spyAllCommands",
                setBoolean(spyAlert::setAllCommandReceiver),
                getBoolean(spyAlert::getAllCommandReceiver),
                SpyAlert.RECEIVE_COMMAND_ALL_PERMISSION,
                values,
                "This option resets to false upon server restart"
        ));

        options.put("timezone", new Option(
                "timeZone",
                setTimeZone(mpModule::setTimeZone),
                getTimeZone(mpModule::getTimeZone),
                null,
                ImmutableList.copyOf(TimeZone.getAvailableIDs()),
                "Check https://game.arcaneminecraft.com/timezone/ for more info"
        ));
    }


    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(ArcaneText.noConsoleMsg());
            return;
        }

        ProxiedPlayer p = (ProxiedPlayer) sender;

        if (args.length == 0) {
            listOptions(p);
            return;
        }

        if (args.length == 1) {
            getOption(p, args[0]);
            return;
        }

        setOption(p, args[0], args[1]);

    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer))
            return Collections.emptyList();

        UUID uuid = ((ProxiedPlayer) sender).getUniqueId();

        if (args.length == 1) {
            // Copied from TabCompletePreset.argStartsWith() and edited

            List<String> ret = new ArrayList<>();
            String argL = args[0].toLowerCase();

            for (Map.Entry<String, Option> opt : options.entrySet()) {
                if (opt.getValue().hasPermission(uuid) && opt.getKey().toLowerCase().startsWith(argL))
                    ret.add(opt.getValue().getName());
            }

            ret.sort(Comparator.naturalOrder());
            return ret;
        }

        if (args.length == 2) {
            Option o = options.get(args[0].toLowerCase());
            if (o != null && o.hasPermission(uuid))
                return TabCompletePreset.argStartsWith(args, o.getChoices());
        }

        return Collections.emptyList();
    }

    @FunctionalInterface
    public interface TriConsumer<T, U, V> {
        void accept(T t, U u, V v);
    }

    private boolean isTrue(String val) {
        return val.equalsIgnoreCase("true") || val.equalsIgnoreCase("on");
    }

    private boolean isFalse(String val) {
        return val.equalsIgnoreCase("false") || val.equalsIgnoreCase("off");
    }

    private BiFunction<UUID, String, Boolean> setBoolean(SettingModule.Option smOption, TriConsumer<SettingModule.Option, UUID, Boolean> setter) {
        return (uuid, value) -> {
            boolean b;
            if (isTrue(value)) {
                b = true;
            } else if (isFalse(value)) {
                b = false;
            } else {
                return false;
            }
            setter.accept(smOption, uuid, b);
            return true;
        };
    }

    private BiFunction<UUID, String, Boolean> setBoolean(BiConsumer<UUID, Boolean> setter) {
        return (uuid, value) -> {
            boolean b;
            if (isTrue(value)) {
                b = true;
            } else if (isFalse(value)) {
                b = false;
            } else {
                return false;
            }
            setter.accept(uuid, b);
            return true;
        };
    }

    private Function<UUID, String> getBoolean(SettingModule.Option smOption, BiFunction<SettingModule.Option, UUID, CompletableFuture<Boolean>> getter) {
        return (uuid) -> {
            try {
                return String.valueOf(getter.apply(smOption, uuid).get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return "AN INTERNAL ERROR OCCURRED"; // hmm
            }
        };
    }

    private Function<UUID, String> getBoolean(Function<UUID, Boolean> getter) {
        return (uuid) -> String.valueOf(getter.apply(uuid));
    }

    private BiFunction<UUID, String, Boolean> setTimeZone(BiConsumer<UUID, TimeZone> setter) {
        return (uuid, value) -> {
            TimeZone timeZone = TimeZone.getTimeZone(value);
            if (timeZone.getID().equals("GMT")) timeZone = null;
            setter.accept(uuid, timeZone);
            return true;
        };
    }

    private Function<UUID, String> getTimeZone(Function<UUID, CompletableFuture<TimeZone>> getter) {
        return (uuid) -> {
            try {
                TimeZone tz = getter.apply(uuid).get();
                if (tz != null)
                    return tz.getID();
                else
                    return "(none)";
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return "AN INTERNAL ERROR OCCURRED"; // hmm...
            }
        };
    }

    private void listOptions(ProxiedPlayer p) {
        BaseComponent list = new TextComponent();
        UUID uuid = p.getUniqueId();

        ArrayList<Option> ordered = new ArrayList<>(options.values());
        ordered.sort(Comparator.comparing(Option::getName));

        for (Option o : ordered) {
            if (o.hasPermission(uuid)) {
                if (ordered.indexOf(o) != 0)
                    list.addExtra(", ");

                BaseComponent bp = new TextComponent(o.getName());
                bp.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/option " + o.getName() + " "));
                switch (o.get(uuid)) {
                    case "true":
                        bp.setColor(ArcaneColor.POSITIVE);
                        break;
                    case "false":
                        bp.setColor(ArcaneColor.NEGATIVE);
                        break;
                    default:
                        bp.setColor(ArcaneColor.FOCUS);
                        break;
                }
                list.addExtra(bp);
            }
        }

        BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.option.list", list);
        send.setColor(ArcaneColor.HEADING);

        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    private void sendDescription(ProxiedPlayer p, BaseComponent description) {
        if (description != null)
            p.sendMessage(ChatMessageType.SYSTEM, description);
    }

    private void setOption(ProxiedPlayer p, String opt, String value) {
        Option o = options.get(opt.toLowerCase());
        BaseComponent send;
        if (o == null) {
            send = ArcaneText.translatable(p.getLocale(), "commands.option.invalid", opt);
            send.setColor(ArcaneColor.NEGATIVE);
        } else if (!o.hasPermission(p.getUniqueId())) {
            send = ArcaneText.translatable(p.getLocale(), "commands.option.noPermission", o.getName());
            send.setColor(ArcaneColor.NEGATIVE);
        } else if (!o.set(p.getUniqueId(), value)) {
            sendDescription(p, o.getDescription());
            send = ArcaneText.translatable(p.getLocale(), "commands.option.badValue", value, o.getName());
            send.setColor(ArcaneColor.NEGATIVE);
        } else {
            sendDescription(p, o.getDescription());
            send = ArcaneText.translatable(p.getLocale(), "commands.option.set", o.getName(), colorizeChoice(value));
            send.setColor(ArcaneColor.CONTENT);
        }
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    private void getOption(ProxiedPlayer p, String opt) {
        Option o = options.get(opt.toLowerCase());
        BaseComponent send;
        if (o == null) {
            send = ArcaneText.translatable(p.getLocale(), "commands.option.invalid", opt);
            send.setColor(ArcaneColor.NEGATIVE);
        } else if (!o.hasPermission(p.getUniqueId())) {
            send = ArcaneText.translatable(p.getLocale(), "commands.option.noPermission", o.getName());
            send.setColor(ArcaneColor.NEGATIVE);
        } else {
            sendDescription(p, o.getDescription());
            send = ArcaneText.translatable(p.getLocale(), "commands.option.get", o.getName(), colorizeChoice(o.get(p.getUniqueId())));
            send.setColor(ArcaneColor.CONTENT);
        }
        p.sendMessage(ChatMessageType.SYSTEM, send);
    }

    private BaseComponent colorizeChoice(String text) {
        BaseComponent ret = new TextComponent(text);
        switch (text) {
            case "true":
            case "on":
                ret.setColor(ArcaneColor.POSITIVE);
                break;
            case "false":
            case "off":
                ret.setColor(ArcaneColor.NEGATIVE);
                break;
            default:
                ret.setColor(ArcaneColor.FOCUS);
                break;
        }
        return ret;
    }
}
