/**
 * ArcaneChatUtilPlugin.java
 * Close-chat function for the Arcane Survival server.
 * @author Morios (Mark Talrey)
 * @version 3.2.0 for Minecraft 1.9.*
 */

package util;

import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.plugin.java.JavaPlugin;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;

import org.bukkit.World.Spigot;

import org.bukkit.entity.Player;
import org.bukkit.Location;

import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.UUID;
import java.util.Collection;

public final class ArcaneChatUtilsPlugin extends JavaPlugin
{
	private static final int DIST_DEF = 40;
	private static int DIST_MAX = 500;
	
	private static final String FORMAT_LOCAL_PRE = "§A(local) ";
	private static final String FORMAT_LOCAL = "Local Chat: ";
	private static final String FORMAT_WHITE = "§F";
	private static final String FORMAT_GRAY = "§7";
	private static final String FORMAT_ITALIC = "§o";
	private static final String FORMAT_AFK = "§7";
	private static final String TAG_AFK = "§5[AFK] §r§f";
	private static final String TAG_GLOBAL = "§g";
	
	private static final String LOCAL_SHORT = "l";
	private static final String LOCAL = "local";
	private static final String LOCAL_TOGGLE = "ltoggle";
	private static final String LOCAL_G_SHORT = "g";
	private static final String LOCAL_GLOBAL = "global";
	private static final String AFK = "afk";
	private static final String UNAFK = "unafk";
	
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
		
	
	private HashMap<UUID, Boolean> afkState = new HashMap<>();
	private HashMap<UUID, Integer> ltogState = new HashMap<>();
	
	@Override
	public boolean onCommand (CommandSender sender, Command cmd, String label, String[] args)
	{
		boolean ret = true;
		
		if (cmd.getName().equals("test"))
		{
			ret = testFunction(args, sender);
		}
		else if (cmd.getName().equals(LOCAL_SHORT))
		{
			ret = shoutFunction(args, sender);
		}
		else if (cmd.getName().equals(LOCAL))
		{
			ret = shoutFunction(args, sender);
		}
		else if (cmd.getName().equals(LOCAL_TOGGLE))
		{
			ret = shoutToggle(args, sender);
		}
		else if (cmd.getName().equals(LOCAL_GLOBAL))
		{
			ret = true;
		}
		else if (cmd.getName().equals(LOCAL_G_SHORT))
		{
			ret = true;
		}
		else if (cmd.getName().equals(AFK))
		{
			ret = enableAFK(args, sender);
		}
		else if (cmd.getName().equals(UNAFK))
		{
			// no longer needed, running a command clears your afk state already.
			ret = disableAFK(args, sender);
		}
		return ret;
	}
	
	private boolean testFunction (String[] args, CommandSender sender)
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
			
			for (Player him : Bukkit.getOnlinePlayers())
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
			
			for (Player him : Bukkit.getOnlinePlayers())
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
	
	private boolean enableAFK (String[] args, CommandSender sender)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage(FORMAT_LOCAL + "You must be a player.");
			return true;
		}
		Player pl = (Player)sender;
		
		if (afkState.get(pl.getUniqueId()) == null)
		{
			afkState.put(pl.getUniqueId(), false);
		}
		if (afkState.get(pl.getUniqueId()) == false)
		{
			afkState.put(pl.getUniqueId(), true);
		
			pl.setPlayerListName(TAG_AFK + pl.getPlayerListName());
		
			sender.sendMessage(FORMAT_AFK + "You are now AFK.");
		}
		else
		{
			sender.sendMessage(FORMAT_AFK + "You are still AFK.");
		}
		return true;
	}
	
	private boolean disableAFK (String[] args, CommandSender sender)
	{
		if (!(sender instanceof Player))
		{
			sender.sendMessage(FORMAT_LOCAL + "You must be a player.");
			return true;
		}
		Player pl = (Player)sender;
		
		if (afkState.get(pl.getUniqueId()) == null)
		{
			afkState.put(pl.getUniqueId(), false);
		}
		if (afkState.get(pl.getUniqueId()) == true)
		{
			_disableAFK(pl);
		}
		else
		{
			sender.sendMessage(FORMAT_AFK + "You are still not AFK.");
		}
		return true;
	}
	
	private void _disableAFK(Player pl)
	{
		String temp = pl.getPlayerListName();
		if (temp.isEmpty() || temp == null || temp.length() < 8)
		{
			getLogger().info("ArcaneChatUtils: empty player name? " + temp);
			temp = "I Am Error";
		}
		pl.setPlayerListName(temp.substring(8)); // magic number much? TAG_AFK is odd.
		afkState.put(pl.getUniqueId(), false);
		pl.sendRawMessage(FORMAT_AFK + "You are no longer AFK.");
	}
	
	@Override
	public void onEnable ()
	{
		//Bukkit.getLogger().info("AFK and Local enabled.");
		getServer().getPluginManager().registerEvents(new UtilListener(), this);
	}
	
	@Override public void onDisable()
	{
		// ze goggles
	}
	
	public final class UtilListener implements Listener
	{
		@EventHandler
		public void detectCommand (PlayerCommandPreprocessEvent pcpe)
		{
			Player pl = pcpe.getPlayer();
			UUID pID = pl.getUniqueId();
			String msg = pcpe.getMessage();
			
			if (msg.startsWith("/kill"))
			{
				if (msg.trim().equalsIgnoreCase("/kill"))
				{
					getServer().dispatchCommand(
						getServer().getConsoleSender(),
						"minecraft:kill " + pl.getUniqueId()
					);
				}
				else
				{
					if (pl.hasPermission("acu.murder")) return;
					// otherwise
					((CommandSender)pl).sendMessage("Sorry, this kind of murder is highly discouraged.");
				}
				pcpe.setCancelled(true);
			}
			else if (msg.startsWith("/minecraft:kill"))
			{
				if (msg.trim().equalsIgnoreCase("/minecraft:kill"))
				{
					getServer().dispatchCommand(
						getServer().getConsoleSender(), "minecraft:kill" + pl.getUniqueId()
					);
				}
				else
				{
					if (pl.hasPermission("acu.murder")) return;
					((CommandSender)pl).sendMessage("Sorry, this kind of murder is highly discouraged.");
				}
				pcpe.setCancelled(true);
			}
			else if (msg.startsWith("/" + LOCAL_GLOBAL))
			{
				pl.chat(msg.replaceFirst("/" + LOCAL_GLOBAL+" ",TAG_GLOBAL));
				pcpe.setCancelled(true);
			}
			else if (msg.startsWith("/" + LOCAL_G_SHORT))
			{
				pl.chat(msg.replaceFirst("/" + LOCAL_G_SHORT+" ",TAG_GLOBAL));
				pcpe.setCancelled(true);
			}
			
			if (afkState.get(pID) == null)
			{
				afkState.put(pID, false);
			}
			if (afkState.get(pID) == true)
			{
				_disableAFK(pl);
			}
		}
		
		@EventHandler
		public void detectChat (AsyncPlayerChatEvent pce)
		{
			Player pl = pce.getPlayer();
			UUID pID = pl.getUniqueId();
			String msg = pce.getMessage();
			
			if (afkState.get(pID) == null)
			{
				afkState.put(pID, false);
			}
			if (afkState.get(pID) == true)
			{
				_disableAFK(pl);
			}
			
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
		
		@EventHandler
		public void detectMotion (PlayerMoveEvent pme)
		{
			Player pl = pme.getPlayer();
			UUID pID = pl.getUniqueId();
			
			if (afkState.get(pID) == null)
			{
				afkState.put(pID, false);
			}
			if (afkState.get(pID) == true)
			{
				_disableAFK(pl);
			}
		}
		
		@EventHandler
		public void detectDiscon (PlayerQuitEvent pqe)
		{
			Player pl = pqe.getPlayer();
			UUID pID = pl.getUniqueId();
			
			if (afkState.get(pID) == null)
			{
				afkState.put(pID, false);
			}
			if (afkState.get(pID) == true)
			{
				_disableAFK(pl);
			}
		}
	}
}
