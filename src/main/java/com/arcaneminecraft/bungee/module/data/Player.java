package com.arcaneminecraft.bungee.module.data;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Timestamp;
import java.util.TimeZone;

public class Player {
    private final ProxiedPlayer player;
    private final String oldName;
    private final Timestamp firstseen;
    private final Timestamp lastleft;
    private TimeZone timezone;
    private long discord;
    private int options;

    public Player(ProxiedPlayer p, String oldName, Timestamp firstseen, Timestamp lastleft, TimeZone timezone, long discord, int options) {
        this.player = p;
        this.oldName = oldName;
        this.firstseen = firstseen;
        this.timezone = timezone;
        this.lastleft = lastleft;
        this.discord = discord;
        this.options = options;
/*
*/
    }

    public Player(ProxiedPlayer p) {
        this.player = p;
        this.oldName = null;
        this.firstseen = new Timestamp(System.currentTimeMillis());
        this.lastleft = null;
        this.timezone = null;
        this.discord = 0;
        this.options = 0;
    }

    public ProxiedPlayer getProxiedPlayer() {
        return player;
    }

    public String getOldName() {
        return this.oldName;
    }

    public Timestamp getFirstSeen() {
        return firstseen;
    }

    public Timestamp getLastLeft() {
        return lastleft;
    }

    public TimeZone getTimezone() {
        return timezone;
    }

    public void setTimezone(TimeZone timezone) {
        this.timezone = timezone;
    }

    public long getDiscord() {
        return discord;
    }

    public void setDiscord(long discord) {
        this.discord = discord;
    }

    public int getOptions() {
        return options;
    }

    public void setOptions(int options) {
        this.options = options;
    }
}