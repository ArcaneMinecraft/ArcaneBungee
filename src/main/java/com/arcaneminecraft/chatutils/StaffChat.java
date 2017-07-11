package com.arcaneminecraft.chatutils;

import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.arcaneminecraft.ArcaneCommons;
import com.arcaneminecraft.ColorPalette;

final class StaffChat implements ChatTogglable, CommandExecutor {
	private final ArcaneChatUtils plugin;
	private static final String PERMISSION_NODE = "arcane.command.a";
	private static final String TAG = "Staff";
	private final HashSet<Player> toggled = new HashSet<>();
	
	StaffChat(ArcaneChatUtils plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean isToggled(Player p) {
		return toggled.contains(p);
	}

	@Override
	public void runToggled(Player p, String msg) {
		broadcast(p, ChatColor.translateAlternateColorCodes('&',msg));
	}

	@Override
	public void removePlayer(Player p) {
		toggled.remove(p);
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission(PERMISSION_NODE)) {
			sender.sendMessage(ArcaneCommons.noPermissionMsg(label));
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("a")) {
			if (args.length == 0) {
				if (!(sender instanceof Player)) {
					sender.sendMessage(ArcaneCommons.tag(TAG, "Usage: /a <message>"));
					return true;
				}
				sender.sendMessage(ArcaneCommons.tag(TAG, "Your toggle is currently "
						+ (toggled.contains(sender)? ColorPalette.POSITIVE + "on": ColorPalette.NEGATIVE + "off")
						+ ColorPalette.CONTENT + ". Usage: /a <message>"));
				return true;
			}
			broadcast(sender, ChatColor.translateAlternateColorCodes('&',String.join(" ", args)));
			return true;
		}
		
		if (cmd.getName().equalsIgnoreCase("atoggle")) {
			if (!(sender instanceof Player)) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "You must be a player."));
				return true;
			}
			Player p = (Player) sender;
			
			if (toggled.add(p))
			{
				sender.sendMessage(ArcaneCommons.tag(TAG, "Staff chat has been toggled " + ColorPalette.POSITIVE + "on" + ColorPalette.CONTENT + "."));
			} else {
				toggled.remove(p);
				sender.sendMessage(ArcaneCommons.tag(TAG, "Staff chat has been toggled " + ColorPalette.NEGATIVE + "off" + ColorPalette.CONTENT + "."));
			}
			
			return true;
		}
		
		return false;
	}
	
	private void broadcast (CommandSender sender, String msg) {
		String send = ColorPalette.HEADING + "Staff // "
				+ ColorPalette.RESET + sender.getName()
				+ ": "
				+ ChatColor.GOLD + msg;
		
		
		plugin.getServer().broadcast(send, PERMISSION_NODE);
	}
}
