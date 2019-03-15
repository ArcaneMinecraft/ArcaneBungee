package com.arcaneminecraft.bungee.command;

import com.arcaneminecraft.api.ArcaneColor;
import com.arcaneminecraft.api.ArcaneText;
import com.arcaneminecraft.api.BungeeCommandUsage;
import com.arcaneminecraft.bungee.ArcaneBungee;
import com.arcaneminecraft.bungee.DiscordCommandExecutor;
import com.arcaneminecraft.bungee.TabCompletePreset;
import com.arcaneminecraft.bungee.module.DiscordUserModule;
import com.arcaneminecraft.bungee.module.MinecraftPlayerModule;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

import java.util.UUID;

public class DiscordCommand extends Command implements TabExecutor, DiscordCommandExecutor {
    private static final String DISCORD_LINK_PERMISSION = "arcane.command.discord.link";

    private final DiscordUserModule module = ArcaneBungee.getInstance().getDiscordUserModule();
    private final MinecraftPlayerModule mpModule = ArcaneBungee.getInstance().getMinecraftPlayerModule();


    public DiscordCommand() {
        super(BungeeCommandUsage.DISCORD.getName(), BungeeCommandUsage.DISCORD.getPermission(), BungeeCommandUsage.DISCORD.getAliases());
        registerDiscordCommand("link", "unlink");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (sender instanceof ProxiedPlayer && args.length != 0 && sender.hasPermission(DISCORD_LINK_PERMISSION)) {
            ProxiedPlayer p = (ProxiedPlayer) sender;
            if (args[0].equalsIgnoreCase("link")) {
                if (args.length < 3) {
                    // Get a link token
                    int token = module.linkToken(p.getUniqueId());
                    if (token == -1) {
                        BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.discord.link.exists", module.getUserTag(module.getDiscordId(p.getUniqueId())));
                        send.setColor(ArcaneColor.NEGATIVE);

                        p.sendMessage(ChatMessageType.SYSTEM, send);
                        return;
                    } else {
                        String commandString = getDiscordPrefix() + "link " + sender.getName() + " " + token;
                        TextComponent command = new TextComponent(commandString);
                        command.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, commandString));
                        command.setColor(ArcaneColor.FOCUS);

                        BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.discord.link", command);
                        send.setColor(ArcaneColor.CONTENT);

                        p.sendMessage(ChatMessageType.SYSTEM, send);
                        return;
                    }
                }

                StringBuilder nameBuilder = new StringBuilder();
                for (int i = 1; i < args.length - 1; i++) {
                    if (i != 1)
                        nameBuilder.append(' ');
                    nameBuilder.append(args[i]);
                }
                String name = nameBuilder.toString();
                boolean success;
                Member m = module.getMember(name);
                try {
                    int token = Integer.parseInt(args[args.length - 1]);
                    success = module.confirmLink(p.getUniqueId(), m.getUser().getIdLong(), token);
                } catch (NumberFormatException e) {
                    BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.discord.link.nan");
                    send.setColor(ArcaneColor.NEGATIVE);
                    p.sendMessage(ChatMessageType.SYSTEM, send);
                    return;
                }

                if (success) {
                    linkSuccess(m.getUser().getIdLong(), null, p.getUniqueId());
                } else {
                    BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.discord.link.failure");
                    send.setColor(ArcaneColor.NEGATIVE);
                    p.sendMessage(ChatMessageType.SYSTEM, send);
                }
                return;
            }

            if (args[0].equalsIgnoreCase("unlink")) {
                long id = module.unlink(p.getUniqueId());
                if (id != 0) {
                    unlinkSuccess(id, null, p.getUniqueId());
                } else {
                    BaseComponent send = ArcaneText.translatable(p.getLocale(), "commands.discord.unlink.failure");
                    send.setColor(ArcaneColor.NEGATIVE);
                    p.sendMessage(ChatMessageType.SYSTEM, send);
                }
                return;
            }
        }

        if (sender instanceof ProxiedPlayer) {
            ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, LinkCommands.singleLink(((ProxiedPlayer) sender).getLocale(), "Discord", LinkCommands.DISCORD));
            if (sender.hasPermission(DISCORD_LINK_PERMISSION)) {
                BaseComponent send = new TextComponent(" Other usage: /discord <link [username token]|unlink>");
                send.setColor(ArcaneColor.CONTENT);
                ((ProxiedPlayer) sender).sendMessage(ChatMessageType.SYSTEM, send);
            }
        } else {
            sender.sendMessage(LinkCommands.singleLink(null, "Discord", LinkCommands.DISCORD));
        }
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender sender, String[] args) {
        if (args.length == 1)
            return TabCompletePreset.argStartsWith(args, ImmutableSet.of("link","unlink"));
        return ImmutableList.of();
    }

    @Override
    public String getDiscordUsage() {
        return "link [<in-game name> <token>]\n" + getDiscordPrefix() + "unlink";
    }

    @Override
    public String getDiscordDescription() {
        return getDiscordPrefix() + "link: Links your Minecraft account with your Discord account.\n" +
                getDiscordPrefix() + "unlink: Unlinks your Discord account from your MC account.";
    }

    @Override
    public boolean executeDiscordCommand(Message m, String[] args) {
        long id = m.getAuthor().getIdLong();

        if (args[0].equalsIgnoreCase("unlink")) {
            UUID uuid = module.unlink(id);
            if (uuid == null) {
                String send = ":x: Your account was not linked yet.";
                m.getChannel().sendMessage(send).complete();
                return true;
            }

            unlinkSuccess(id, m.getChannel(), uuid);

            return true;
        }

        if (args.length < 3) {
            // Get a link token
            int token = module.linkToken(m.getAuthor().getIdLong());
            if (token == -1) {
                m.getChannel().sendMessage(
                        ":raised_hand: You're already linked to a Minecraft account named '"
                        + mpModule.getName(module.getMinecraftUuid(id))
                        + "'."
                ).complete();
                return true;
            }
            // Provide link token
            MessageChannel chan = m.getAuthor().openPrivateChannel().complete();
            chan.sendMessage(
                    "Your token: " + token + ". Run the following command ***in-game*** to complete linking:\n"
                    + "```\n/discord link " + module.getUserTag(id) + " " + token + "\n```"
            ).complete();

            if (m.getChannel() != chan)
                m.getChannel().sendMessage("Your link token has been sent via **private message**.").complete();
            return true;
        }

        UUID uuid = mpModule.getUUID(args[1]);
        boolean success;
        try {
            int token = Integer.parseInt(args[2]);
            success = module.confirmLink(id, uuid, token);
        } catch (NumberFormatException e) {
            // Token is not all numbers
            String send = ":x: The **token** must be **all numbers**!";
            m.getChannel().sendMessage(send).complete();
            return true;
        }

        if (success) {
            linkSuccess(id, m.getChannel(), uuid);
        } else {
            m.getChannel().sendMessage(
                    ":x: You've provided the wrong link token;  run `"
                    + getDiscordPrefix()
                    + "link` and try again."
            ).complete();
        }
        return true;
    }

    private void unlinkSuccess(long id, MessageChannel messageChannel, UUID uuid) {
        Member m = module.getMember(id);
        MessageChannel chan = messageChannel == null
                ? m.getUser().openPrivateChannel().complete()
                : messageChannel;

        String sendD = ":heavy_check_mark: You've successfully unlinked your account. So long :cry:";
        chan.sendMessage(sendD).complete();

        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);

        if (p != null) {
            BaseComponent send = ArcaneText.translatable(
                    p.getLocale(),
                    "commands.discord.unlink.success"
            );
            send.setColor(ArcaneColor.CONTENT);
            p.sendMessage(ChatMessageType.SYSTEM, send);
        }
    }

    private void linkSuccess(long id, MessageChannel messageChannel, UUID uuid) {
        Member m = module.getMember(id);
        MessageChannel chan = messageChannel == null
                ? m.getUser().openPrivateChannel().complete()
                : messageChannel;

        String sendD = ":white_check_mark: "
                + m.getAsMention()
                + "'s account has Successfully linked to '"
                + mpModule.getName(uuid)
                + "' Minecraft account!";
        chan.sendMessage(sendD).complete();

        ProxiedPlayer p = ProxyServer.getInstance().getPlayer(uuid);
        if (p != null) {
            BaseComponent discordTag = new TextComponent(module.getUserTag(id));
            discordTag.setColor(ArcaneColor.FOCUS);
            BaseComponent send = ArcaneText.translatable(
                    p.getLocale(),
                    "commands.discord.link.success",
                    discordTag
            );
            send.setColor(ArcaneColor.CONTENT);

            p.sendMessage(ChatMessageType.SYSTEM, send);
        }
    }
}

