package com.arcaneminecraft.bungee;

import com.arcaneminecraft.bungee.channel.DiscordBot;
import net.dv8tion.jda.core.entities.Message;

public interface DiscordCommandExecutor {
    default String getDiscordPrefix() {
        return DiscordBot.getInstance().getListener().getPrefix();
    }

    default void registerDiscordCommand(String command, String... aliases) {
        DiscordBot.getInstance().getListener().registerCommand(command, this, aliases);
    }

    default String getDiscordUsage() {
        return null;
    };

    default String getDiscordDescription() {
        return null;
    };

    boolean executeDiscordCommand(Message trigger, String[] args);
}
