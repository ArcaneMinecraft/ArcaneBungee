package com.arcaneminecraft.chatutils;

import java.util.HashMap;
import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.arcaneminecraft.ArcaneCommons;
import com.arcaneminecraft.ColorPalette;

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
		broadcast(p, getRadius(p), msg);
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
		
		if (cmd.getName().equalsIgnoreCase("localradius")) {
			if (args.length == 0) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "Usage: /localradius <radius>"));
			}
			
			Integer r;
			try {
				r = Integer.parseInt(args[0]);
			} catch (NumberFormatException e) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "The given radius '" + args[0] + "' is not a valid number."));
				return true;
			}
			if (r > DIST_MAX) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "The messaging radius is capped at " + DIST_MAX + " blocks."));
			}
			
			radiusMap.put(p, r);
			sender.sendMessage(ArcaneCommons.tag(TAG, "Your messaging radius is now set to " + r + "."));
			return true;
		}
		
		int r = getRadius(p);
		
		if (cmd.getName().equalsIgnoreCase("local")) {
			if (args.length == 0) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "Your toggle is currently "
						+ (toggled.contains(sender)? ColorPalette.POSITIVE + "on": ColorPalette.NEGATIVE + "off")
						+ ColorPalette.CONTENT + ". Your chat radius is "
						+ ColorPalette.FOCUS + r + ColorPalette.CONTENT +". Usage: /l <message>"));
				return true;
			}
			broadcast(p, r, String.join(" ", args));
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
	
	private void broadcast (Player p, int r, String msg) {
		String send = CHAT_TAG + " <" + p.getDisplayName() + "> " + ChatColor.GRAY + ChatColor.ITALIC + msg;
		World w = p.getWorld();
		Location l = p.getLocation();
		// Who to send the message to?
		for (Player recipient : plugin.getServer().getOnlinePlayers()) {
			if (recipient.getWorld().equals(w)
					&& recipient.getLocation().distanceSquared(l) <= r*r)
				recipient.sendMessage(send);
		}
	}
}
