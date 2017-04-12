package com.arcaneminecraft.chatutils;

import org.bukkit.entity.Player;

interface ChatTogglable {
	boolean isToggled(Player p);
	void runToggled(Player p, String msg);
	void removePlayer(Player p);
}
