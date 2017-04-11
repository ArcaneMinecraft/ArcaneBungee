package com.arcaneminecraft.chatutils;

import org.bukkit.entity.Player;

public interface ChatTogglable {
	public boolean isToggled(Player p);
	public void runToggled(Player p, String msg);
}
