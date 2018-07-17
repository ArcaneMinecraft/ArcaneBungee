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

    public enum Toggles {
        SHOW_WELCOME_MESSAGE(1, true), // e.g. if option is not set (0), show welcome message. If set (1), do the opposite and don't show welcome message
        SHOW_DONOR_WELCOME_MESSAGE(1 << 1, true),
        SHOW_LAST_LOGIN_MESSAGE(1 << 2, true);

        private int pos;
        private boolean defaultOnZero;

        Toggles(int pos, boolean defaultOnZero) {
            this.pos = pos;
            this.defaultOnZero = defaultOnZero;
        }

        public boolean getDefault() {
            return defaultOnZero;
        }
    }

    private static int o(ProxiedPlayer p) {
        return sPlugin.getSqlDatabase() == null ? 0 : sPlugin.getSqlDatabase().getOption(p);
    }

    public static void set(ProxiedPlayer p, Toggles o, boolean set) {
        sPlugin.getSqlDatabase().setOption(p,
                o.defaultOnZero == set
                        ? o(p) | o.pos
                        : o(p) & ~o.pos);
    }

    public static boolean get(ProxiedPlayer p, Toggles o) {
        return o.defaultOnZero == ((o(p) & o.pos) == 0);
    }
}
