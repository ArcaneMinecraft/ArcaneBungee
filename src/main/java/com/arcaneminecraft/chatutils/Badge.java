package com.arcaneminecraft.chatutils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;

import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.arcaneminecraft.ArcaneCommons;
import com.arcaneminecraft.ColorPalette;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

final class Badge implements Listener, CommandExecutor {
	private final ArcaneChatUtils plugin;
	private final String TAG = "Badge";
	private final FileConfiguration config;
	private final File configFile;
	/** Current Badge */
	private final HashMap<UUID, String> badgeOn = new HashMap<>();
	/** Allowed Badges */
	private final HashMap<UUID, List<String>> tagAllowed = new HashMap<>();
	/** Presetted Badges*/
	private final HashMap<String, String> tagPreset = new HashMap<>();
	
	private static final String BADGE_TAG_PERMISSION = "arcane.badgeadmin";
	
	private static final String[][] BADGE_HELP = {
			{"badge", "show this screen", "Alias:\n /b"},
			{"badgetoggle", "Togggle your badge quicly", "Alias:\n /bt"},
			{"badge list", "list your badge collection"},
			{"badge use", "use last used or specified tag", "Usage: /badge use [badge]"},
			{"badge off", "remove your tag from chat"},
			{"Your tag appears by your name in chat for everyone."},
		};
	private static final String[][] BADGE_TAG_HELP = {
			{"badgeadmin", "show this screen", "Aliases:\n /ba\n /nt"},
			{"badgeadmin allow", "give new badge option", "Usage: /badgeadmin allow <badge> <player>"},
			{"badgeadmin disallow", "remove existing badge option", "Usage: /badgeadmin disallow <badge> <player>"},
			{"badgeadmin set", "set player custom tag", "Usage: /badgeadmin set <tag...> <player>\n Tag may contain multiple spaces."},
			{"badgeadmin clear", "clear player tag", "Usage: /badgeadmin clear <player>"},
			{"badgeadmin library add", "add new badge type", "Usage: /badgeadmin library add <badge> <tag...>"},
			{"badgeadmin library remove", "remove existing badge", "Usage: /badgeadmin library remove <badge>"},
		};
	
	//@SuppressWarnings("unchecked")
	@SuppressWarnings("unchecked")
	Badge (ArcaneChatUtils plugin) {
		this.plugin = plugin;
		this.configFile = new File(plugin.getDataFolder(), "badge.yml");
		
		// SaveDefaultConfig
	    if (!configFile.exists()) {            
	         plugin.saveResource("badge.yml", false);
	     }
		
		this.config = YamlConfiguration.loadConfiguration(configFile);
		
		ConfigurationSection cs = config.getConfigurationSection("badges");
		
		// Load default list of tags
		for (String tag : cs.getKeys(false)) {
			tagPreset.put(tag, ChatColor.translateAlternateColorCodes('&',cs.getString(tag)));
		}
		
		cs = config.getConfigurationSection("players");
		
		// Load player's badge status
		for (String uuid : cs.getKeys(false)) {
			UUID u = UUID.fromString(uuid);
			tagAllowed.put(u, cs.getStringList("badges"));
			String using = cs.getString("tag");
			if (using == null || using == "")
				continue;
			// If player has a tag applied already
			badgeOn.put(u, using);
		}
	}
	
	void saveConfig() {
        try {
            config.save(configFile);
        } catch (IOException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not save badge configuration to " + configFile, e);
        }
	}
	
	private OfflinePlayer getPlayerFromName(String pname) {
		Player p = plugin.getServer().getPlayer(pname);
		if (p != null)
			return p;
		
		@SuppressWarnings("deprecation")
		OfflinePlayer op = plugin.getServer().getOfflinePlayer(pname);
		if (op.hasPlayedBefore())
			return op;
		
		return null;
	}
	
	private String playerNotExistTagMsg(CommandSender sender, String pname) {
		return ArcaneCommons.tag(TAG, "Player \"" + ColorPalette.FOCUS + pname + ColorPalette.CONTENT + "\" does not exist.");
	}
	
	private BaseComponent getBadgeList(List<String> l) {
		BaseComponent ret = ArcaneCommons.tagTC(TAG);
		ret.addExtra("Available badges:");
		
		if (l == null) {
			TextComponent tc = new TextComponent(" none");
			tc.setItalic(true);
			ret.addExtra(tc);
			return ret;
		}
		
		for (String s : l) {
			String t = tagPreset.get(s);
			if (t == null) {
				l.remove(s);
				continue;
			}
			ret.addExtra(" ");
			TextComponent tc = new TextComponent('[' + s + ']');
			tc.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/badge use " + s));
			tc.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(t).create()));
			tc.setColor(ColorPalette.FOCUS);
			ret.addExtra(tc);
		}
		return ret;
	}
	
	private String setBadge(UUID u, String b) {
		// Set badge to configuration file
		config.set("players."+u.toString()+".tag", b);
		
		String c = ChatColor.translateAlternateColorCodes('&', b);
		badgeOn.put(u, c);
		
		return c;
	}
	
	private boolean removeBadge(UUID u) {
		config.set("players."+u.toString()+".tag", null);
		return badgeOn.remove(u) != null;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (cmd.getName().equalsIgnoreCase("badgeadmin")) {
			if (!sender.hasPermission(BADGE_TAG_PERMISSION)) {
				sender.sendMessage(ArcaneCommons.noPermissionMsg(label));
				return true;
			}
			
			if (args.length == 0) {
				ArcaneCommons.sendCommandMenu(sender, "Badge Tag Help", BADGE_TAG_HELP, new String[]{"blah"});
				return true;
			}
			
			// 4allow 4disallow 43set 3clear library<5add 4remove>
			//{"badgetag library add", "add new badge type", "Usage: /badgetag library add <Badge ID> <Badge Display>"},
			//{"badgetag library remove", "remove existing badge", "Usage: /badgetag library remove <Badge ID>"}
			if (args[0].equalsIgnoreCase("allow")) {
				if (args.length != 3) {
					sender.sendMessage(ArcaneCommons.tag(TAG, "Usage: /badgeadmin allow <badge> <player>"));
					return true;
				}
				
				String b = args[1].toLowerCase();
				OfflinePlayer p = getPlayerFromName(args[2]);
				UUID u = p.getUniqueId();
				if (u == null) {
					sender.sendMessage(playerNotExistTagMsg(sender,args[2]));
					return true;
				}
				
				List<String> l = tagAllowed.get(u);
				if (l == null) {
					l = new LinkedList<String>();
					tagAllowed.put(u, l);
				}
				if (l.contains(b)) {
					sender.sendMessage(ArcaneCommons.tag(TAG, ColorPalette.FOCUS + p.getName() + ColorPalette.CONTENT + " already has this badge."));
					return true;
				}
				
				String t = tagPreset.get(b);
				if (t == null) {
					sender.sendMessage(ArcaneCommons.tag(TAG, "\"" + ColorPalette.FOCUS + b + ColorPalette.CONTENT + "\" badge does not exist."));
					return true;
				}
				
				l.add(b);
				
				config.set("players."+u.toString()+".badges", l);
				sender.sendMessage(ArcaneCommons.tag(TAG, ColorPalette.FOCUS + p.getName() + ColorPalette.CONTENT + " can now use " + ChatColor.RESET + t + ColorPalette.CONTENT + "."));
				return true;
			}
			
			if (args[0].equalsIgnoreCase("disallow")) {
				if (args.length != 3) {
					sender.sendMessage(ArcaneCommons.tag(TAG, "Usage: /badgeadmin disallow <badge> <player>"));
					return true;
				}
				
				OfflinePlayer p = getPlayerFromName(args[2]);
				UUID u = p.getUniqueId();
				if (u == null) {
					sender.sendMessage(playerNotExistTagMsg(sender,args[2]));
					return true;
				}
				
				List<String> l = tagAllowed.get(u);
				String b = args[1].toLowerCase();
				if (l == null || l.remove(b)) {
					sender.sendMessage(ArcaneCommons.tag(TAG, ColorPalette.FOCUS + p.getName() + ColorPalette.CONTENT + " doesn't have the badge \"" + ColorPalette.FOCUS + b + ColorPalette.CONTENT + "\"."));
					return true;
				}
				
				config.set("players."+u.toString()+".badges", l);
				
				sender.sendMessage(ArcaneCommons.tag(TAG, ColorPalette.FOCUS + p.getName() + ColorPalette.CONTENT + " cannot use the badge \"" + ColorPalette.FOCUS + b + ColorPalette.CONTENT + "\" anymore."));
				return true;
			}
			
			if (args[0].equalsIgnoreCase("set")) {
				if (args.length < 3) {
					sender.sendMessage(ArcaneCommons.tag(TAG, "Usage: /badgeadmin set <tag...> <player>"));
					return true;
				}
				
				int plPos = args.length - 1;
				
				OfflinePlayer p = getPlayerFromName(args[plPos]);
				UUID u = p.getUniqueId();
				if (u == null) {
					sender.sendMessage(playerNotExistTagMsg(sender,args[plPos]));
					return true;
				}
				
				String b = "";
				for (int i = 1; i < plPos; i++) {
					b += " " + args[i];
				}
				b = b.substring(1);
				
				String c = setBadge(u, b);
				
				sender.sendMessage(ArcaneCommons.tag(TAG, ColorPalette.FOCUS + p.getName() + ColorPalette.CONTENT + "'s tag is now " + c + "."));
				return true;
			}
			
			if (args[0].equalsIgnoreCase("clear")) {
				if (args.length != 2) {
					sender.sendMessage(ArcaneCommons.tag(TAG, "Usage: /badgeadmin clear <player>"));
					return true;
				}
				
				OfflinePlayer p = getPlayerFromName(args[1]);
				UUID u = p.getUniqueId();
				if (u == null) {
					sender.sendMessage(playerNotExistTagMsg(sender,args[1]));
					return true;
				}
				
				if (!removeBadge(u)) {
					sender.sendMessage(ArcaneCommons.tag(TAG, ColorPalette.FOCUS + p.getName() + ColorPalette.CONTENT + " wasn't holding a tag."));
					return true;
				}
				
				sender.sendMessage(ArcaneCommons.tag(TAG, ColorPalette.FOCUS + p.getName() + ColorPalette.CONTENT + "'s tag is cleared successfully."));
				return true;
			}
			
			if (args[0].equalsIgnoreCase("library")) {
				// Books!... er, tags!
				// TODO
				
				String  s = "Usage: /badgeadmin library add <badge> <tag...>";
				s = "Usage: /badgeadmin library remove <badge>";
				
				config.set("players."+u.toString()+".tag", b);
				return true;
			}
			return false;
		}
		
		// Must be a player from this point on.
		if (!(sender instanceof Player)) {
			sender.sendMessage(ArcaneCommons.noConsoleMsg());
			return true;
		}
		
		UUID u = ((Player)sender).getUniqueId();
		
		if (cmd.getName().equalsIgnoreCase("badgetoggle")) {
			if (removeBadge(u)) {
				sender.sendMessage(ArcaneCommons.tag(TAG, "You put your badge away."));
				return true;
			}
			
			List<String> ls = tagAllowed.get(u);
			if (ls != null && ls.size() == 1) {
				// only one
				for (String s : ls) {
					String b = tagPreset.get(s);
					setBadge(u, b);
					sender.sendMessage(ArcaneCommons.tag(TAG, "Your tag is now " + ChatColor.RESET + b + ColorPalette.CONTENT + "."));
					return true;
				}
			}
			((Player)sender).spigot().sendMessage(getBadgeList(ls));
			return true;
		}
		
		// Command: badge
		
		//String b = tagAllowed.get(p).get(0);
		if (args.length == 0) {
			String b = badgeOn.get(u);
			ArcaneCommons.sendCommandMenu(sender, "Badge Help", BADGE_HELP, new String[]{"Currently using", b == null ? ChatColor.ITALIC + "none" : ChatColor.RESET + b});
			return true;
		}
		
		if (args[0].equalsIgnoreCase("off")) {
			if (removeBadge(u))
				sender.sendMessage(ArcaneCommons.tag(TAG, "You put your badge away."));
			else
				sender.sendMessage(ArcaneCommons.tag(TAG, "You didn't have any badge on."));
			return true;
		}
		
		if (args[0].equalsIgnoreCase("use")) {
			if (args.length >= 2) {
				if (tagAllowed.get(u).contains(args[1])) {
					String b = tagPreset.get(args[1]);
					setBadge(u, b);
					sender.sendMessage(ArcaneCommons.tag(TAG, "Your tag is now " + ChatColor.RESET + b + ColorPalette.CONTENT + "."));
					//config.set("players."+".asdf", value);
					return true;
				}
				
				sender.sendMessage(ArcaneCommons.tag(TAG, "You don't have that badge!"));
			}
		}
		
		// List
		List<String> ls = tagAllowed.get(u);
		((Player)sender).spigot().sendMessage(getBadgeList(ls));
		
		return true;
	}
	
	@EventHandler (priority=EventPriority.HIGHEST)
	public void addBadge(AsyncPlayerChatEvent e) {
		String badge = badgeOn.get(e.getPlayer().getUniqueId());
		if (badge != null) {
			e.setFormat(badge + ' ' + ChatColor.RESET + e.getFormat());
		}
	}
}
