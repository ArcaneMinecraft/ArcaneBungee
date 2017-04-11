package com.arcaneminecraft.chatutils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

import com.arcaneminecraft.ArcaneCommons;
import com.arcaneminecraft.ColorPalette;

class Tell implements CommandExecutor {
	private static final String TAG = "PM";
	private final ArcaneChatUtils plugin;
	private final Map<CommandSender,CommandSender> lastReceived = new HashMap<>();
	
	Tell(ArcaneChatUtils plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("tell")) {
			if (args.length < 2) {
				return false;
			}
			
			// Get recipient
			CommandSender p = plugin.getServer().getPlayer(args[0]);
			if (p == null) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "'" + ColorPalette.FOCUS + args[0] + ColorPalette.CONTENT + "' is not online!"));
				return true;
			}
			
			// Combine Message
			TextComponent msg = new TextComponent();
			for (int i = 1; i < args.length; i++) {
				msg.addExtra(" ");
				msg.addExtra(args[i]);
			}
			msg.setColor(ColorPalette.CONTENT);
			
			messanger(sender, p, msg);
			
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("reply")) {
			if (args.length == 0) {
				return false;
			}
			
			CommandSender p = lastReceived.get(sender);
			if (p == null || (p instanceof Player && !((Player)p).isOnline())) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "No player found!"));
				return true;
			}
			
			// Combine Message
			TextComponent msg = new TextComponent(" " + String.join(" ", args));
			msg.setColor(ColorPalette.CONTENT);
			
			messanger(sender, p, msg);
			
			return true;
		}
		
		return false;
	}

	private void messanger(CommandSender from, CommandSender to, TextComponent msg) {
		messageSender(from, to, msg, false); // send to "sender" as "To p: msg"
		messageSender(to, from, msg, true);
		
		// Update sender-receiver map
		lastReceived.put(to, from);
	}
	
	private void messageSender(CommandSender player, CommandSender name, TextComponent msg, boolean isReceiving) {
		TextComponent send = new TextComponent();
		
		TextComponent a = new TextComponent("> ");
		a.setColor(ChatColor.DARK_GRAY);
		send.addExtra(a);
		
		send.addExtra((isReceiving ? "From" : "To") + " ");
		send.addExtra(name.getName());
		
		a = new TextComponent(":");
		a.setColor(ChatColor.DARK_GRAY);
		send.addExtra(a);
		
		send.addExtra(msg);
		send.setColor(ColorPalette.HEADING);
		send.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + name.getName() + " "));

		// Send Messages
		if (player instanceof Player)
			((Player)player).spigot().sendMessage(send);
		else
			player.sendMessage(send.toPlainText());
	}
}
