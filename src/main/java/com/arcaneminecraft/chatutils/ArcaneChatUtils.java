/**
 * ArcaneChatUtilPlugin.java
 * Close-chat function for the Arcane Survival server.
 * @author Morios (Mark Talrey)
 * @version 3.3.0 for Minecraft 1.9.*
 */

package com.arcaneminecraft.chatutils;

import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.entity.Player;
import org.bukkit.Location;

import org.bukkit.scheduler.BukkitScheduler;

import net.md_5.bungee.api.ChatColor;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.UUID;

public final class ArcaneChatUtils extends JavaPlugin
{
	private static final int DIST_DEF = 40;
	private static final int AFK_COUNTDOWN = 300; // 5 minute countdown to being afk
	private static int DIST_MAX = 500;
	
	private static final String FORMAT_LOCAL_PRE = "§A(local) ";
	private static final String FORMAT_LOCAL = "Local Chat: ";
	private static final String FORMAT_WHITE = ChatColor.WHITE.toString();
	private static final String FORMAT_GRAY = ChatColor.GRAY.toString();
	private static final String FORMAT_ITALIC = ChatColor.ITALIC.toString();
	private static final String FORMAT_AFK = "§7";
	private static final String TAG_AFK = "§5[AFK] §r§f";
	private static final String TAG_GLOBAL = "§g";
	
	private static final String LOCAL_G_SHORT = "g";
	private static final String LOCAL_GLOBAL = "global";
	
	private static final String LOCAL_HELP = 
		"§2" + FORMAT_LOCAL + "§r" + FORMAT_GRAY + FORMAT_ITALIC + 
		"Use /local or /l. Sends a message only to players within a radius.\n" + 
		"§2-h§r§7§o displays this help message.\n" +
		"§2-r NUMBER§r§7§o will set the broadcast radius to NUMBER. Must be positive and " +
		"less than " + DIST_MAX + ", larger values will be treated as this maximum.\n" +
		"The default radius (if §2-r§r§7§o is not used) is " + DIST_DEF + ".";
		
	private static final String LTOGG_HELP = 
		"§2" + FORMAT_LOCAL + "§r" + FORMAT_GRAY + FORMAT_ITALIC + 
		"/ltoggle turns on or off automatic local chatting.\n" +
		"§2-h§r§7§o displays this help message.\n" +
		"§2-r NUMBER§r§7§o will set the broadcast radius to NUMBER. Must be positive and " +
		"less than " + DIST_MAX + ", larger values will be treated as this maximum.\n" +
		"The default radius (if §2-r§r§7§o is not used) is " + DIST_DEF + ".";
		
	private static final String LOCAL_RANGE =
		"§2" + FORMAT_LOCAL + "§r" + FORMAT_GRAY + FORMAT_ITALIC + 
		"(warning) The message radius is capped at " + DIST_MAX + " blocks.";
		
	
	
	private HashMap<UUID, Integer> afkState = new HashMap<>(); // counts down toward [AFK] every second
	private HashMap<UUID, Integer> ltogState = new HashMap<>();
	
	@Override
	public void onEnable ()
	{
		// Command stuff
		
		LocalChat lc = new LocalChat(this);
		getCommand("local").setExecutor(lc);
		getCommand("ltoggle").setExecutor(lc);
		
		StaffChat sc = new StaffChat(this);
		getCommand("a").setExecutor(sc);
		getCommand("atoggle").setExecutor(sc);
		
		GlobalToggle gtog = new GlobalToggle(sc, lc);
		getCommand("global").setExecutor(gtog);
		getServer().getPluginManager().registerEvents(gtog, this);

		Tell tell = new Tell(this);
		getCommand("tell").setExecutor(tell);
		getCommand("reply").setExecutor(tell);
		
		
		//Bukkit.getLogger().info("AFK and Local enabled.");
		getServer().getPluginManager().registerEvents(new UtilListener(), this);
		
		BukkitScheduler scheduler = getServer().getScheduler();
		int ret = scheduler.scheduleSyncRepeatingTask(this, new Runnable() {
			@Override
			public void run()
			{
				// This gives warning: Iterator is a raw type. References to generic type Iterator<E> should be parameterized
				Iterator it = afkState.entrySet().iterator();
			//  ^
				while (it.hasNext())
				{
					// This gives a warning: Type safety: Unchecked cast from Object to Map.Entry<UUID,Integer>
					Map.Entry<UUID,Integer> pair = (Map.Entry<UUID,Integer>)it.next();
					//								^
					if (pair.getValue() > 1)
					{
						afkState.put(pair.getKey(), pair.getValue()-1);
						//getLogger().info("ticked down to: " + (pair.getValue()-1));
					}
					else if (pair.getValue() == 1)
					{
						Player pl = getServer().getPlayer(pair.getKey());
						pl.setPlayerListName(TAG_AFK + pl.getPlayerListName());
						pl.sendRawMessage(FORMAT_AFK + "You are now AFK.");
						afkState.put(pair.getKey(), 0);
					}
				}
			}
		}, 0L, 20L); // run every 20 ticks (~1 Hz)
		if (ret < 0) getLogger().info("Failed to set up AFK timer.");
	}
	
	@Override
	public boolean onCommand (CommandSender sender, Command cmd, String label, String[] args)
	{
		if (cmd.getName().equals("test"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage("You must be a player.");
				return true;
			}
			Player pl = (Player)sender;
			
			Location me = pl.getLocation();
			me.getWorld().strikeLightning(me);
			
			return true;
		}
		if (cmd.getName().equals("local"))
		{
			return shoutFunction(args, sender);
		}
		if (cmd.getName().equals("ltoggle"))
		{
			return shoutToggle(args, sender);
		}
		if (cmd.getName().equals("global"))
		{
			return true;
		}
		if (cmd.getName().equals("afk"))
		{
			if (!(sender instanceof Player))
			{
				sender.sendMessage(FORMAT_LOCAL + "You must be a player.");
				return true;
			}
			Player pl = (Player)sender;
			
			if (afkState.get(pl.getUniqueId()) == null)
			{
				afkState.put(pl.getUniqueId(), AFK_COUNTDOWN);
			}
			if (afkState.get(pl.getUniqueId()) >= 0)
			{
				afkState.put(pl.getUniqueId(), 0);
			
				pl.setPlayerListName(TAG_AFK + pl.getPlayerListName());
			
				sender.sendMessage(FORMAT_AFK + "You are now AFK.");
			}
			else
			{
				// this shouldn't actually happen, now should it?
				// yup, it wouldn't ~Simon
				sender.sendMessage(FORMAT_AFK + "You are still AFK.");
			}
			return true;
		}
		return false;
	}
	
	private boolean shoutFunction (String[] args, CommandSender sender)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage(FORMAT_LOCAL + "You must be a player.");
			return true;
		}
		if (args.length == 0)
		{
			sender.sendMessage(FORMAT_LOCAL + "You must specify a message. Use -h for details.");
			return false;
		}
		else if (args[0].equals("-h"))
		{
			sender.sendMessage(LOCAL_HELP);
			return true;
		}
		else if (args[0].equals("-r"))
		{
			Player pl = (Player)sender;
			Location center = pl.getLocation();
			
			int rad = DIST_DEF;
			try { rad = Integer.parseInt(args[1]); }
			catch (Exception e)
			{
				sender.sendMessage(FORMAT_LOCAL +
					"There was an error with the radius, falling back to the default.\n" +
					"Did you format your number correctly? See -h for help."
				);
			}
			
			if (rad > DIST_MAX)
			{
				sender.sendMessage(LOCAL_RANGE);
				rad = DIST_MAX;
			}
			
			StringBuilder msg = new StringBuilder();
			for (int i=2; i<args.length; i++)
			{
				msg.append(args[i] + " ");
			}
			if (msg.toString().trim().isEmpty())
			{
				sender.sendMessage("No message given. Use -h for details.");
			}
			
			for (Player him : getServer().getOnlinePlayers())
			{
				try {
					if (!(center.getWorld().equals(him.getLocation().getWorld()))) continue;
					
					// moving to a less intensive function (doesn't need to do square root)
					if (center.distanceSquared(him.getLocation()) <= (rad^2))
					{
						him.sendRawMessage(
							FORMAT_LOCAL_PRE + 
							FORMAT_WHITE + 
							"<" + pl.getDisplayName() + "> " + 
							FORMAT_GRAY + FORMAT_ITALIC +
							msg
						);
					}
				}
				catch (NumberFormatException nfe)
				{
					sender.sendMessage("You must specify an integer for range. Use -h for details.");
				}
			}
		}
		else
		{
			Player pl = (Player)sender;
			Location center = pl.getLocation();
			
			StringBuilder msg = new StringBuilder();
			for (int i=0; i<args.length; i++)
			{
				msg.append(args[i] + " ");
			}
			if (msg.toString().trim().isEmpty())
			{
				sender.sendMessage("No message given.");
			}
			
			for (Player him : getServer().getOnlinePlayers())
			{
				if (!(center.getWorld().equals(him.getLocation().getWorld()))) continue;
				
				if (center.distance(him.getLocation()) <= DIST_DEF)
				{
					him.sendRawMessage(
						FORMAT_LOCAL_PRE + 
						FORMAT_WHITE + 
						"<" + pl.getDisplayName() + "> " + 
						FORMAT_GRAY + FORMAT_ITALIC +
						msg
					);
				}
			}
		}	
		return true;
	}
	
	private boolean shoutToggle (String[] args, CommandSender sender)
	{
		int dist = DIST_DEF;
		
		if (!(sender instanceof Player))
		{
			sender.sendMessage(FORMAT_LOCAL + "You must be a player.");
			return true;
		}
		
		if (args.length != 0)
		{
			if (args[0].equals("-h"))
			{
				sender.sendMessage(LTOGG_HELP);
				return true;
			}
			else if (args[0].equals("-r"))
			{
				try { dist = Integer.parseInt(args[1]); }
				catch (Exception e)
				{
					sender.sendMessage(FORMAT_LOCAL +
						"There was an error with the radius, falling back to the default.\n" +
						"Did you format your number correctly? See -h for help."
					);
				}
			}
			
			if (dist > DIST_MAX)
			{
				sender.sendMessage(LOCAL_RANGE);
				dist = DIST_MAX;
			}
		}
		Player pl = (Player)sender;
		UUID me = pl.getUniqueId();
		
		if (ltogState.get(me) == null || ltogState.get(me) == 0)
		{
			ltogState.put(me,dist);
			sender.sendMessage(FORMAT_GRAY + "Local chat toggled on.");
		}
		else
		{
			ltogState.put(me,0);
			sender.sendMessage(FORMAT_GRAY + "Local chat toggled off.");
		}
		return true;
	}
	
	public final class UtilListener implements Listener
	{
		@EventHandler
		public void detectChat (AsyncPlayerChatEvent pce)
		{
			Player pl = pce.getPlayer();
			UUID pID = pl.getUniqueId();
			String msg = pce.getMessage();
			
			if (afkState.get(pID) == null) afkState.put(pID, AFK_COUNTDOWN);
			
			int prevState = afkState.get(pID);
			afkState.put(pID, AFK_COUNTDOWN);
			
			if (prevState == 0)
			{
				// TODO: FIX
				//_disableAFK(pl);
			}
			
			// if the player's local chat is toggled on
			if ((ltogState.get(pID) != null) && (ltogState.get(pID) > 0))
			{
				if (!msg.startsWith(TAG_GLOBAL))
				{
					pce.setCancelled(true);
				
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
