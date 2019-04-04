package com.arcaneminecraft.bungee.module.data;

import java.sql.Timestamp;
import java.util.TimeZone;
import java.util.UUID;

public class ArcanePlayer {
    private final UUID uuid;
    private final int id;
    private final String oldName;
    private final Timestamp firstseen;
    private final Timestamp lastleft;
    private TimeZone timezone;
    private long discord;
    private String reddit;
    private int options;

    public ArcanePlayer(UUID uuid, int id, String oldName, Timestamp firstseen, Timestamp lastleft, TimeZone timezone, long discord, String reddit, int options) {
        this.uuid = uuid;
        this.id = id;
        this.oldName = oldName;
        this.firstseen = firstseen;
        this.timezone = timezone;
        this.lastleft = lastleft;
        this.discord = discord;
        this.reddit = reddit;
        this.options = options;
    }

    public ArcanePlayer(UUID uuid, int id) {
        this.uuid = uuid;
        this.id = id;
        this.oldName = "";
        this.firstseen = new Timestamp(System.currentTimeMillis());
        this.lastleft = null;
        this.timezone = null;
        this.discord = 0;
        this.reddit = null;
        this.options = 0;
    }

    public UUID getUniqueID() {
        return uuid;
    }

    public int getId() {
        return id;
    }

    public String getOldName() {
        return oldName;
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

    public void setTimeZone(TimeZone timezone) {
        this.timezone = timezone;
    }

    public long getDiscord() {
        return discord;
    }

    public void setDiscord(long discord) {
        this.discord = discord;
    }

    public String getReddit() {
        if (reddit == null)
            return null;
        return "/u/" + reddit;
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