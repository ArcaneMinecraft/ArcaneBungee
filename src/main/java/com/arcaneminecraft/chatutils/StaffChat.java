package com.arcaneminecraft.chatutils;

import java.util.HashSet;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.arcaneminecraft.ArcaneCommons;
import com.arcaneminecraft.ColorPalette;

class StaffChat implements CommandExecutor {
	private final ArcaneChatUtils plugin;
	private static final String PERMISSION_NODE = "arcane.staffchat";
	private static final String TAG = "Staff";
	private final HashSet<CommandSender> toggled = new HashSet<>();
	
	StaffChat(ArcaneChatUtils plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission(PERMISSION_NODE)) {
			sender.sendMessage(ArcaneCommons.noPermissionMsg());
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
			broadcast(sender, args);
			return true;
		}
		
    	if (cmd.getName().equalsIgnoreCase("atoggle") && sender.hasPermission("simonplugin.a")) {
    		if (!(sender instanceof Player)) {
    			sender.sendMessage(ArcaneCommons.tag(TAG, "You must be a player."));
    			return true;
    		}
    		
    		if (toggled.add(sender))
    		{
    			sender.sendMessage(ArcaneCommons.tag(TAG, "Staff chat has been toggled " + ColorPalette.POSITIVE + "on" + ColorPalette.CONTENT + "."));
    		} else {
    			toggled.remove(sender);
    			sender.sendMessage(ArcaneCommons.tag(TAG, "Staff chat has been toggled " + ColorPalette.NEGATIVE + "off" + ColorPalette.CONTENT + "."));
    		}
    		
    		return true;
    	}
    	
    	return false;
	}
	
	private void broadcast (CommandSender sender, String[] args) {
		String msg = ColorPalette.HEADING + "Staff // "
				+ ColorPalette.RESET + sender.getName()
				+ ": "
				+ ChatColor.GOLD + ChatColor.translateAlternateColorCodes('&',String.join(" ", args));
		
		
		plugin.getServer().broadcast(msg, PERMISSION_NODE);
	}
	
	public final class ToggleListener implements Listener {
		// Global
		@EventHandler (priority=EventPriority.LOW)
		public void detectChat (AsyncPlayerChatEvent e)
		{
			String msg = e.getMessage();
			CommandSender p = e.getPlayer();
			// if the player's admin chat is toggled on
			if (toggled.contains(p))
			{
				if (msg.startsWith("§g")) {
					e.setMessage(msg.substring(2));
				}
				else
				{
					e.setCancelled(true);
					
					String[] chat = { msg };
					broadcast(p,chat);
				}
			}
		}
	}
}
