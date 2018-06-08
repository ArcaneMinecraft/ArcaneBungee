package com.arcaneminecraft.bungee;

public enum BungeeCommandUsage {
    AFK             ("/afk"),
    APPLY           ("/apply"),
    GREYLIST        ("/greylist", "arcane.command.greylist"),
    REPLY           ("/reply <private message ...>"),
    LINKS           ("/links"),
    NEWS            ("/news"),
    PING            ("/ping [player]"),
    SEEN            ("/seen <player>"),
    SLAP            ("/slap <player>", "arcane.command.slap"),
    FINDPLAYER      ("/findplayer <part of name>"),
    FIRSTSEEN       ("/firstseen [player]"),
    STAFFCHAT       ("/a <staff message ...>", "arcane.command.a"),
    STAFFCHATTOGGLE ("/atoggle", "arcane.command.a");

    private final String usage;
    private final String permission;

    BungeeCommandUsage(String usage){
        this.usage = usage;
        this.permission = null;
    }

    BungeeCommandUsage(String usage, String permission){
        this.usage = usage;
        this.permission = permission;
    }

    public String getUsage() {
        return usage;
    }

    public String getPermission() {
        return permission;
    }
}
