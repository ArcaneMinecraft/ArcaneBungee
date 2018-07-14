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
    private final ArcaneBungee plugin;

    /**
     * Option to show welcome message, DEFAULT on
     */
    private static final int SHOW_WELCOME_MESSAGE = 1;
    /**
     * Option to show donor's welcome message, DEFAULT on
     */
    private static final int SHOW_DONOR_WELCOME_MESSAGE = 1 << 1;
    /**
     * Option to show last login message, DEFAULT on
     */
    private static final int SHOW_LAST_LOGIN_MESSAGE = 1 << 2;

    public OptionsStorage(ArcaneBungee plugin) {
        this.plugin = plugin;
    }

    private int option(ProxiedPlayer p) {
        return plugin.getSqlDatabase() == null ? 0 : plugin.getSqlDatabase().getOption(p);
    }

    private int setOption(ProxiedPlayer p, int option, boolean toOne) {
        /* Example to help understand logic:
        00001010 |  00000001
        00001011 & ~00000001
        00001010
         */

        return toOne ? option(p) | SHOW_WELCOME_MESSAGE : option(p) & ~SHOW_WELCOME_MESSAGE;
    }

    public boolean getShowWelcomeMessage(ProxiedPlayer p) {
        return (option(p) & SHOW_WELCOME_MESSAGE) == 0;
    }

    public void setShowWelcomeMessage(ProxiedPlayer p, boolean toggle) {
        plugin.getSqlDatabase().setOption(p, setOption(p, SHOW_WELCOME_MESSAGE, !toggle));
    }

    public boolean getShowDonorWelcomeMessage(ProxiedPlayer p) {
        return (option(p) & SHOW_DONOR_WELCOME_MESSAGE) == 0;
    }

    public void setShowDonorWelcomeMessage(ProxiedPlayer p, boolean toggle) {
        plugin.getSqlDatabase().setOption(p, setOption(p, SHOW_DONOR_WELCOME_MESSAGE, !toggle));
    }

    public boolean getShowLastLoginMessage(ProxiedPlayer p) {
        return (option(p) & SHOW_LAST_LOGIN_MESSAGE) == 0;
    }
    public void setShowLastLoginMessage(ProxiedPlayer p, boolean toggle) {
        plugin.getSqlDatabase().setOption(p, setOption(p, SHOW_LAST_LOGIN_MESSAGE, !toggle));
    }

}
