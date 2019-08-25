package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.DiscordCommandExecutor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class RollCommand extends Command implements TabExecutor, DiscordCommandExecutor {
    private final SecureRandom random = new SecureRandom();

    public RollCommand() {
        super(BungeeCommandUsage.ROLL.getName(), BungeeCommandUsage.ROLL.getPermission(), BungeeCommandUsage.ROLL.getAliases());
        registerDiscordCommand("roll", "r");
    }

    private int roll(int max) {
        return random.nextInt(max) + 1;
    }

    private List<Integer> rolled(int repeat, int max) {
        List<Integer> ret = new ArrayList<>(repeat);
        // TODO
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        // TODO
    }

    // TODO make discord executor
    // TODO hook this into the main class
}
