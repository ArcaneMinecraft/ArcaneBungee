package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.DiscordCommandExecutor;
import com.arcaneminecraft.bungee.channel.DiscordBot;
import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.entities.Message;

import java.util.Map;

public class DHelpCommand implements DiscordCommandExecutor {
    public DHelpCommand() {
        registerDiscordCommand("help", "h", "?");
    }

    @Override
    public String getDiscordDescription() {
        return "Shows this view";
    }

    @Override
    public boolean executeDiscordCommand(Message m, String[] args) {
        EmbedBuilder embed = new EmbedBuilder()
                .setDescription("**Hello!** These are Arcane commands on Discord so far.  Message the staff if you need help.\n" +
                        "More commands are in the works. If there's any we must have right now, you may suggest them!\n\n" +
                        "**Come build with us!**\n" +
                        "Arcane v" + ArcaneBungee.getInstance().getDescription().getVersion() + "\n"
                )
                .setThumbnail("https://arcaneminecraft.com/res/img/icon/512.png")
                .setColor(0xFFAA00);

        for (Map.Entry<String, DiscordCommandExecutor> e : DiscordBot.getInstance().getListener().getCommandMap().entrySet()) {
            String prefix = e.getValue().getDiscordPrefix();
            String name = prefix + e.getKey();
            String usage = e.getValue().getDiscordUsage();

            String desc = e.getValue().getDiscordDescription();

            embed.addField(name,
                    (usage == null ? "" : "__Usage:__ " + prefix + usage)
                    + (desc == null ? "" : desc),true);
        }

        m.getChannel().sendMessage(embed.build()).complete();
        return true;
    }
}
