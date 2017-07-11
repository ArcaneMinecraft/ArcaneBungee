package com.arcaneminecraft.chatutils;

import java.util.HashMap;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.arcaneminecraft.ArcaneCommons;
import com.arcaneminecraft.ColorPalette;
import com.arcaneminecraft.TextComponentURL;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

final class LocalChat implements ChatTogglable, CommandExecutor {
	private final ArcaneChatUtils plugin;
	private static final String TAG = "Local";
	private static final String CHAT_TAG = ChatColor.GREEN + "(local)";
	private static final int DIST_MAX = 500;
	private static final int DIST_DEFAULT = 40;
	private final HashSet<Player> toggled = new HashSet<>();
	private final HashMap<Player,Integer> radiusMap = new HashMap<>();

	LocalChat(ArcaneChatUtils plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean isToggled(Player p) {
		return toggled.contains(p);
	}

	@Override
	public void runToggled(Player p, String msg) {
		broadcastLocal(p, getRadius(p), StringUtils.split(msg));
	}

	@Override
	public void removePlayer(Player p) {
		toggled.remove(p);
		radiusMap.remove(p);
	}
	
	private int getRadius(Player p) {
		Integer rt = radiusMap.get(p);
		return (rt == null) ? DIST_DEFAULT : rt;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ArcaneCommons.noConsoleMsg());
			return true;
		}
		Player p = (Player) sender;
		
		int r = getRadius(p);
		
		if (cmd.getName().equalsIgnoreCase("localradius")) {
			if (args.length == 0) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "Your chat radius is "
						+ ColorPalette.FOCUS + r + ColorPalette.CONTENT +". Usage: /localradius <radius>"));
				return true;
			}
			
			try {
				r = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "The given radius '" + args[0] + "' is not a valid number."));
				return true;
			}
			if (r > DIST_MAX) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "The messaging radius is capped at " + DIST_MAX + " blocks."));
				return true;
			}
			
			radiusMap.put(p, r);
			sender.sendMessage(ArcaneCommons.tag(TAG, "Your messaging radius is now set to " + r + "."));
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("local")) {
			if (args.length == 0) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "Your toggle is currently "
						+ (toggled.contains(sender)? ColorPalette.POSITIVE + "on": ColorPalette.NEGATIVE + "off")
						+ ColorPalette.CONTENT + ". Your chat radius is "
						+ ColorPalette.FOCUS + r + ColorPalette.CONTENT +". Usage: /l <message>"));
				return true;
			}
			broadcastLocal(p, r, args);
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("localtoggle")) {
			if (toggled.add(p)){
				sender.sendMessage(ArcaneCommons.tag(TAG, "Local chat has been toggled " + ColorPalette.POSITIVE + "on" + ColorPalette.CONTENT + "."));
			} else {
				toggled.remove(p);
				sender.sendMessage(ArcaneCommons.tag(TAG, "Local chat has been toggled " + ColorPalette.NEGATIVE + "off" + ColorPalette.CONTENT + "."));
			}
			
			return true;
		}
		return false;
	}
	
	private void broadcastLocal (Player p, int r, String[] msg) {
		// 1. Get all the recipients
		HashSet<Player> recipients = new HashSet<>();
		
		World w = p.getWorld();
		Location l = p.getLocation();
		for (Player recipient : plugin.getServer().getOnlinePlayers()) {
			if (recipient.getWorld().equals(w)
					&& recipient.getLocation().distanceSquared(l) <= r*r)
				recipients.add(recipient);
		}
		
		// 2. Create message
		TextComponent send = new TextComponent();
		send.setColor(ChatColor.GRAY);
		
		// Beginning: tag
		TextComponent a = new TextComponent();
		
		TextComponent b = new TextComponent(CHAT_TAG);
		
		a.addExtra(b);
		
		// name
		b = new TextComponent(" <" + p.getDisplayName() + "> ");
		b.setColor(ChatColor.WHITE);
		a.addExtra(b);
		
		// Add a click action only to the beginning
		a.setClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/l "));
		
		// Hover event to show list of players who received the message
		String list = "";
		for (Player rp : recipients)
			list += ", " + rp.getName();
		
		list.substring(2);
		a.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
				new ComponentBuilder("Recipient" + (list.length() == 1 ? "" : "s") + ": " + list).create()));
		
		send.addExtra(a);
		
		// Later: message
		a = TextComponentURL.activate(msg);
		a.setItalic(true);
		send.addExtra(a);
		
		// Send Messages
		for (Player rp : recipients)
			rp.spigot().sendMessage(send);
	}
}
