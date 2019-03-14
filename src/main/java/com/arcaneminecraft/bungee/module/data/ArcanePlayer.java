package com.arcaneminecraft.bungee.module.data;

import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.sql.Timestamp;
import java.util.TimeZone;

public class ArcanePlayer {
    private final ProxiedPlayer player;
    private final int id;
    private final String oldName;
    private final Timestamp firstseen;
    private final Timestamp lastleft;
    private TimeZone timezone;
    private long discord;
    private String reddit;
    private int options;

    public ArcanePlayer(ProxiedPlayer p, int id, String oldName, Timestamp firstseen, Timestamp lastleft, TimeZone timezone, long discord, String reddit, int options) {
        this.player = p;
        this.id = id;
        this.oldName = oldName;
        this.firstseen = firstseen;
        this.timezone = timezone;
        this.lastleft = lastleft;
        this.discord = discord;
        this.reddit = reddit;
        this.options = options;
    }

    public ArcanePlayer(ProxiedPlayer p, int id) {
        this.player = p;
        this.id = id;
        this.oldName = "";
        this.firstseen = new Timestamp(System.currentTimeMillis());
        this.lastleft = null;
        this.timezone = null;
        this.discord = 0;
        this.reddit = null;
        this.options = 0;
    }

    public ProxiedPlayer getProxiedPlayer() {
        return this.player;
    }

    public int getId() {
        return this.id;
    }

    public String getOldName() {
        return this.oldName;
    }

    public Timestamp getFirstSeen() {
        return this.firstseen;
    }

    public Timestamp getLastLeft() {
        return this.lastleft;
    }

    public TimeZone getTimezone() {
        return timezone;
    }

    public void setTimeZone(TimeZone timezone) {
        this.timezone = timezone;
    }

    public long getDiscord() {
        return this.discord;
    }

    public void setDiscord(long discord) {
        this.discord = discord;
    }

    public String getReddit() {
        return this.reddit;
    }

    public void setReddit(String reddit) {
        this.reddit = reddit;
    }

    public int getOptions() {
        return options;
    }

    public void setOptions(int options) {
        this.options = options;
    }
}