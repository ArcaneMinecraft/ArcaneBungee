package com.arcaneminecraft.chatutils;

public enum Usage {
    AFK             ("/afk"),
    APPLY           ("/apply"),
    GREYLIST        ("/greylist", "arcane.command.greylist"),
    LINKS           ("/links"),
    NEWS            ("/news"),
    PING            ("/ping [player]"),
    SEEN            ("/seen <player>"),
    FINDPLAYER      ("/findplayer <part of name>"),
    FIRSTSEEN       ("/firstseen [player]"),
    STAFFCHAT       ("/a <staff message ...>", "arcane.command.a"),
    STAFFCHATTOGGLE ("/atoggle", "arcane.command.a");

    private final String usage;
    private final String permission;

    Usage(String usage){
        this.usage = usage;
        this.permission = null;
    }

    Usage(String usage, String permission){
        this.usage = usage;
        this.permission = permission;
    }

    String getUsage() {
        return usage;
    }

    String getPermission() {
        return permission;
    }
}
