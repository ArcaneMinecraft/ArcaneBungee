package com.arcaneminecraft.chatutils;

import java.util.HashSet;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import com.arcaneminecraft.ArcaneCommons;

final class GlobalToggle implements CommandExecutor, Listener {
	private final HashSet<Player> global = new HashSet<>();
	private final ChatTogglable staff;
	private final ChatTogglable local;
	
	GlobalToggle(StaffChat staff, LocalChat local) {
		this.local = local;
		this.staff = staff;
	}
	
	// CommandExecutor: global
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(ArcaneCommons.noConsoleMsg());
			return true;
		}
		Player p = (Player)sender;
		
		String msg = String.join(" ", args);
		global.add(p);
		p.chat(msg);
		return true;
	}
	
	// Listener
	@EventHandler (priority=EventPriority.HIGH)
	public void detectChat (AsyncPlayerChatEvent e)
	{
		String msg = e.getMessage();
		Player p = e.getPlayer();
		
		// If chatted with /global
		if (global.remove(p)) {
			return;
		}
		
		
		if (staff.isToggled(p)) {
			e.setCancelled(true);
			staff.runToggled(p, msg);
			return;
		}
		if (local.isToggled(p)) {
			e.setCancelled(true);
			local.runToggled(p, msg);
			return;
		}
	}
	
	@EventHandler (priority=EventPriority.MONITOR)
	public void detectLeave (PlayerQuitEvent e) {
		Player p = e.getPlayer();
		local.removePlayer(p);
		staff.removePlayer(p);
	}
}
