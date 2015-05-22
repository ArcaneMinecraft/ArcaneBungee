/**
 * ArcaneChatUtilPlugin.java
 * Close-chat function for the Arcane Survival server.
 * @author Morios (Mark Talrey)
 * @version 2.3 for Minecraft 1.8.*
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
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashMap;
import java.util.UUID;
import java.util.Collection;

public final class ArcaneChatUtilsPlugin extends JavaPlugin
{
	private static final int DIST_MAX = 40;
	
	private static final String FORMAT_LOCAL_PRE = "§A(local) ";
	private static final String FORMAT_LOCAL = "Local Chat: ";
	private static final String FORMAT_WHITE = "§F";
	private static final String FORMAT_GRAY = "§7";
	private static final String FORMAT_ITALIC = "§o";
	private static final String FORMAT_AFK = "§7";
	private static final String TAG_AFK = "§5[AFK] §r§f";
	
	private static final String LOCAL_SHORT = "l";
	private static final String LOCAL = "local";
	private static final String AFK = "afk";
	private static final String UNAFK = "unafk";
	
	private static final String LOCAL_HELP = 
		FORMAT_LOCAL + 
		"Use /local or /l. Either enter a message afterward, " + 
		"or use -r and a whole number to specify the range of the message. " +
		"The default range (radius) of a message is " + DIST_MAX + " blocks.";

	
	private HashMap<UUID, Boolean> afkState = new HashMap<>();
	
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
					
					if (center.distance(him.getLocation()) <= Integer.parseInt(args[1]))
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
				
				if (center.distance(him.getLocation()) <= DIST_MAX)
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
		
		pl.setPlayerListName(temp.substring(8, temp.length()));
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
		public void detectChat (PlayerChatEvent pce)
		{
			Player pl = pce.getPlayer();
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
	}
}
