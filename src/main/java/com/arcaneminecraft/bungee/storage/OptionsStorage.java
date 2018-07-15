package com.arcaneminecraft.bungee.storage;

import com.arcaneminecraft.bungee.ArcaneBungee;
import net.md_5.bungee.api.connection.ProxiedPlayer;

/**
 * Options storage on MySQL database work on bits.
 * Unsigned int has 8 byte, or 32 bits to work with,
 * meaning total of 32 options can be set.
 *
 * Default is always 0. 1 if toggled.
 */
public class OptionsStorage {
    private static ArcaneBungee sPlugin;

    public OptionsStorage(ArcaneBungee plugin) {
        OptionsStorage.sPlugin = plugin;
    }

    public enum Options {
        SHOW_WELCOME_MESSAGE(1, false), // if option returns 0, show welcome message
        SHOW_DONOR_WELCOME_MESSAGE(1 << 1, false),
        SHOW_LAST_LOGIN_MESSAGE(1 << 2, false);

        private int pos;
        private boolean on;

        Options(int pos, boolean defaultOn) {
            this.pos = pos;
            this.on = defaultOn;
        }
    }

    private static int o(ProxiedPlayer p) {
        return sPlugin.getSqlDatabase() == null ? 0 : sPlugin.getSqlDatabase().getOption(p);
    }

    public static void set(ProxiedPlayer p, Options o, boolean set) {
        sPlugin.getSqlDatabase().setOption(p,
                o.on == set
                        ? o(p) & ~o.pos
                        : o(p) | o.pos);
    }

    public static boolean get(ProxiedPlayer p, Options o) {
        return o.on == ((o(p) & o.pos) != 0);
    }
}
