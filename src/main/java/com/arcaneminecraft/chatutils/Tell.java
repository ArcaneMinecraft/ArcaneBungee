package com.arcaneminecraft.chatutils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.arcaneminecraft.api.ArcaneCommons;
import com.arcaneminecraft.api.ColorPalette;
import com.arcaneminecraft.api.TextComponentURL;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;

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
			TextComponent msg = TextComponentURL.activate(args, 1);
			
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
			TextComponent msg = TextComponentURL.activate(args);
			
			messanger(sender, p, msg);
			
			return true;
		}
		
		return false;
	}

	private void messanger(CommandSender from, CommandSender to, TextComponent msg) {
		msg.setColor(ColorPalette.CONTENT);
		
		messageSender(from, to, msg, false); // send to "sender" as "To p: msg"
		messageSender(to, from, msg, true);
		
		// Update sender-receiver map
		lastReceived.put(to, from);
	}
	
	private void messageSender(CommandSender player, CommandSender name, TextComponent msg, boolean isReceiving) {
		TextComponent send = new TextComponent();
		
		// Beginning
		TextComponent a = new TextComponent();
		a.setColor(ColorPalette.HEADING);
		
		TextComponent b = new TextComponent("> ");
		b.setColor(ChatColor.DARK_GRAY);
		
		a.addExtra(b);
		a.addExtra((isReceiving ? "From" : "To") + " ");
		a.addExtra(name.getName());
		
		b = new TextComponent(": ");
		b.setColor(ChatColor.DARK_GRAY);
		a.addExtra(b);
		
		// Add a click action only to the beginning
		if (name instanceof Player)
			a.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/msg " + name.getName() + " "));
		else
			a.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/reply "));
		
		
		send.addExtra(a);
		
		// Message
		send.addExtra(msg);
		
		// Send Messages
		if (player instanceof Player)
			((Player)player).spigot().sendMessage(send);
		else
			player.sendMessage(send.toLegacyText());
	}
}
