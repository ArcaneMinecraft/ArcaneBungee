package com.arcaneminecraft.bungee.module;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.SpyAlert;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Options storage on MySQL database work on bits.
 * Unsigned int has 8 byte, or 32 bits to work with,
 * meaning total of 32 options can be set.
 *
 * Default is always 0. 1 if toggled.
 */
public class SettingModule {
    private MinecraftPlayerModule getModule() {
        return ArcaneBungee.getInstance().getMinecraftPlayerModule();
    }

    public enum Option {
        SHOW_WELCOME_MESSAGE        (1, "showWelcomeMessage", true, "arcane.welcome.option"), // e.g. if option is not set (0), show welcome message. If set (1), do the opposite and don't show welcome message
        SHOW_DONOR_WELCOME_MESSAGE  (1 << 1, "showDonorWelcomeMessage", true, "arcane.welcome.donor"),
        SHOW_LAST_LOGIN_ON_JOIN     (1 << 2, "showLastLoginOnJoin", true, "arcane.welcome.option"),
        SHOW_NEWS_ON_JOIN           (1 << 2, "showNewsOnJoin", true, "arcane.welcome.option"),
        SET_DISCORD_PUBLIC          (1 << 3, "setDiscordPublic", true),
        SET_REDDIT_PUBLIC           (1 << 4, "setRedditPublic", true),
        RECEIVE_DISCORD_MESSAGE_WHEN_INACTIVE(1 << 5, "receiveInactiveMessageOnDiscord", true, null,
                "When someone is looking for you (by using, for example, /seen) and you have been inactive for a month, " +
                        "you'll be notified on Discord only once if you have it linked to this MC account."),

        SPY_SIGNS       (1 << 26, "spySigns", false, SpyAlert.RECEIVE_SIGN_PERMISSION),
        SPY_XRAY        (1 << 27, "spyXray", true, SpyAlert.RECEIVE_XRAY_PERMISSION),
        SPY_ON_TRUSTED  (1 << 28, "spyOnTrusted", true, SpyAlert.RECEIVE_COMMAND_PERMISSION, "Listen on helpers and staff members"),
        SPY_NEW_PLAYER  (1 << 29, "spyNewPlayerCommands", true, SpyAlert.RECEIVE_COMMAND_PERMISSION, "This is independent of spyCommands"),
        SPY_COMMANDS    (1 << 30, "spyCommands", true, SpyAlert.RECEIVE_COMMAND_PERMISSION);

        public final int position;
        public final String name;
        public final boolean onZero;
        public final String permission;
        public final String description;

        Option(int position, String name, boolean onZero) {
            this(position, name, onZero, null, null);
        }

        Option(int position, String name, boolean onZero, String permission) {
            this(position, name, onZero, permission, null);
        }

        Option(int position, String name, boolean onZero, String permission, String description) {
            this.position = position;
            this.name = name;
            this.onZero = onZero;
            this.permission = permission;
            this.description = description;
        }
    }

    public CompletableFuture<Boolean> get(Option option, UUID u) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        getModule().getOptions(u).thenAcceptAsync(
                options -> future.complete(
                        option.onZero == ((options & option.position) == 0)
                )
        );
        return future;
    }

    public boolean getNow(Option option, UUID u) {
        try {
            int options = getModule().getOptions(u).get();
            return option.onZero == ((options & option.position) == 0);
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
            return option.onZero;
        }
    }

    public void set(Option option, UUID u, boolean set) {
        getModule().getOptions(u).thenAcceptAsync(currentOptions -> {
            int store = option.onZero == set
                            ? currentOptions & ~option.position
                            : currentOptions | option.position;


            getModule().setOptions(u, store);
        });
    }
}
