package com.arcaneminecraft.chatutils;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

class LocalChat implements CommandExecutor {
	private final ArcaneChatUtils plugin;
	private static final String TAG = "Local";
	private final HashMap<Player,Integer> toggled = new HashMap<>();

	private static final String G_TAG = "§g";

	LocalChat(ArcaneChatUtils plugin) {
		this.plugin = plugin;
	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		return false;
	}

	public final class UtilListener implements Listener
	{
		@EventHandler
		public void detectChat (AsyncPlayerChatEvent e)
		{
			Player p = e.getPlayer();
			String msg = e.getMessage();
			// if the player's local chat is toggled on
			if (toggled.get(p) != null)
			{
				if (!msg.startsWith(G_TAG))
				{
					e.setCancelled(true);

					String[] chat = { "-r", ltogState.get(pID) + "", msg };
					shoutFunction(chat, (CommandSender)pl);
				}
				else
				{
					pce.setMessage(msg.replace(TAG_GLOBAL,""));
				}
			}
		}
	}
}
