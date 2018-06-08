package com.arcaneminecraft.bungee;

public enum BungeeCommandUsage {
    AFK             ("/afk"),
    APPLY           ("/apply"),
    GREYLIST        ("/greylist",   null,                   "arcane.command.greylist"),
    REPLY           ("/reply",      "<private message ...>"),
    LINKS           ("/links"),
    NEWS            ("/news"),
    PING            ("/ping",       "[player]"),
    SEEN            ("/seen",       "<player>"),
    SLAP            ("/slap",       "<player>",             "arcane.command.slap"),
    FINDPLAYER      ("/findplayer", "<part of name>"),
    FIRSTSEEN       ("/firstseen",  "[player]"),
    STAFFCHAT       ("/a",          "<staff message ...>",  "arcane.command.a"),
    STAFFCHATTOGGLE ("/atoggle",    null,                   "arcane.command.a");

    private final String command;
    private final String usage;
    private final String permission;

    BungeeCommandUsage(String command){
        this.command = command;
        this.usage = command;
        this.permission = null;
    }

    BungeeCommandUsage(String command, String usage){
        this.command = command;
        this.usage = command + " " + usage;
        this.permission = null;
    }

    BungeeCommandUsage(String command, String usage, String permission){
        this.command = command;
        this.usage = usage == null ? command : command + " " + usage;
        this.permission = permission;
    }

    public String getUsage() {
        return usage;
    }

    public String getPermission() {
        return permission;
    }
}
