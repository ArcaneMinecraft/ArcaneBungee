/**
 * ArcaneChatUtilPlugin.java
 * Close-chat function for the Arcane Survival server.
 * @author Morios (Mark Talrey)
 * @version 3.3.0 for Minecraft 1.9.*
 */

package com.arcaneminecraft.chatutils;

import org.bukkit.plugin.java.JavaPlugin;

public final class ArcaneChatUtils extends JavaPlugin
{
	private Badge badge;
	
	@Override
	public void onEnable () {
		badge = new Badge(this);
		getCommand("badge").setExecutor(badge);
		getCommand("badgetoggle").setExecutor(badge);
		getCommand("badgeadmin").setExecutor(badge);
		getServer().getPluginManager().registerEvents(badge, this);
		
		LocalChat lc = new LocalChat(this);
		getCommand("local").setExecutor(lc);
		getCommand("localtoggle").setExecutor(lc);
		getCommand("localradius").setExecutor(lc);
		
		StaffChat sc = new StaffChat(this);
		getCommand("a").setExecutor(sc);
		getCommand("atoggle").setExecutor(sc);
		
		GlobalToggle gtog = new GlobalToggle(sc, lc);
		getCommand("global").setExecutor(gtog);
		getServer().getPluginManager().registerEvents(gtog, this);

		Tell tell = new Tell(this);
		getCommand("tell").setExecutor(tell);
		getCommand("reply").setExecutor(tell);
	}
	
	@Override
	public void onDisable() {
		badge.saveConfig();
	}
}
