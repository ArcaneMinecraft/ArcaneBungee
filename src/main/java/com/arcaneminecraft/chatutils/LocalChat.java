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

class LocalChat implements ChatTogglable, CommandExecutor {
	private final ArcaneChatUtils plugin;
	private static final String TAG = "Local";
	private final HashMap<Player,Integer> toggled = new HashMap<>();

	private static final String G_TAG = "§g";

	LocalChat(ArcaneChatUtils plugin) {
		this.plugin = plugin;
	}

	@Override
	public boolean isToggled(Player p) {
		return toggled.containsKey(p);
	}

	@Override
	public void runToggled(Player p, String msg) {
		
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		return false;
	}
}
