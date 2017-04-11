package com.arcaneminecraft.chatutils;

import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public interface ChatTogglable {
	
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
