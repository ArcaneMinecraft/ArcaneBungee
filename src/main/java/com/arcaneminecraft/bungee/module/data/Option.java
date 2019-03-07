package com.arcaneminecraft.bungee.module.data;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Option {
    private final String name;
    private final BiFunction<UUID, String, Boolean> setter;
    private final Function<UUID, String> getter;
    private final String permission;
    private final Iterable<String> choices;
    private final BaseComponent description;

    public Option(String name, BiFunction<UUID, String, Boolean> setter, Function<UUID, String> getter) {
        this(name, setter,getter,null, null, null);
    }

    public Option(String name, BiFunction<UUID, String, Boolean> setter, Function<UUID, String> getter, String permission) {
        this(name, setter, getter, permission, null, null);
    }

    public Option(String name, BiFunction<UUID, String, Boolean> setter, Function<UUID, String> getter, String permission, Iterable<String> choices) {
        this(name, setter, getter, permission, choices, null);
    }

    public Option(String name, BiFunction<UUID, String, Boolean> setter, Function<UUID, String> getter, String permission, Iterable<String> choices, String description) {
        this.name = name;
        this.setter = setter;
        this.getter = getter;
        this.permission = permission;
        this.choices = choices;
        if (description == null) {
            this.description = null;
        } else {
            this.description = ArcaneText.url(description);
            this.description.setColor(ArcaneColor.CONTENT);
        }
    }

    public boolean set(UUID uuid, String set) {
        return setter.apply(uuid, set);
    }

    public String get(UUID uuid) {
        return getter.apply(uuid);
    }

    public boolean hasPermission(UUID uuid) {
        if (permission != null) {
            ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
            return p != null && p.hasPermission(permission);
        }
        return true;
    }

    public Iterable<String> getChoices() {
        return choices;
    }

    public BaseComponent getDescription() {
        return description;
    }

    public String getName() {
        return name;
    }
}
