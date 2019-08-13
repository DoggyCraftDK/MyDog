package dk.fido2603.mydog;

import dk.fido2603.mydog.LevelFactory.Level;
import dk.fido2603.mydog.listeners.DamageListener;
import dk.fido2603.mydog.listeners.WolfMainListener;
import dk.fido2603.mydog.utils.ParticleUtils;
import net.milkbowl.vault.economy.Economy;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

/*           THIS PLUGIN IS HEAVILY INSPIRED BY           */
/*                   DOGONFIRE'S MYHORSE                  */
/*														  */
/*          https://github.com/DogOnFire/MyHorse          */
public class MyDog extends JavaPlugin
{
	public static MyDog 						instance;
	public static boolean						pluginEnabled							= false;

	public boolean								vaultEnabled							= false;

	public static Server						server									= null;
	public boolean								debug									= false;
	private static MyDog						plugin;
	private static FileConfiguration			config									= null;
	private static PermissionsManager			permissionsManager						= null;
	private static DogManager					dogManager								= null;
	private static LevelFactory					levelFactory							= null;
	private static ParticleUtils				particleUtils							= null;

	public boolean								randomCollarColor						= true;
	public boolean								useLevels								= true;
	public List<String>							dogNames								= Arrays.asList("Fido", "Queen", "King", "Doggy", "Charlie", "Max", "Milo", "Ollie", "Toby", "Teddy", "Molly", "Rosie", "Bella");

	public Map<Integer, Level>					dogLevels								= new HashMap<Integer, Level>();

	private static Economy						economy									= null;
	private Commands							commands								= null;
	private String								chatPrefix								= "MyDog";
	public String								serverName								= "Your Server";

	public static MyDog instance()
	{
		return instance;
	}

	public static PermissionsManager getPermissionsManager()
	{
		return permissionsManager;
	}

	public static DogManager getDogManager()
	{
		return dogManager;
	}

	public static LevelFactory getLevelFactory()
	{
		return levelFactory;
	}

	public static ParticleUtils getParticleUtils()
	{
		return particleUtils;
	}

	public static Economy getEconomy()
	{
		return economy;
	}

	public String getChatPrefix()
	{
		return chatPrefix;
	}

	public void sendInfo(Player player, String message)
	{
		if (player == null)
		{
			log(message);
		}
		else
		{
			player.sendMessage(message);
		}
	}

	public void onDisable()
	{
		saveSettings();
		reloadSettings();

		pluginEnabled = false;
	}

	@Override
	public void onEnable()
	{
		WolfMainListener tameListener = null;
		DamageListener damageListener = null;
		
		plugin = this;
		instance = this;
		server = getServer();
		config = getConfig();

		this.commands = new Commands(this);

		pluginEnabled = true;

		tameListener = new WolfMainListener(this);
		damageListener = new DamageListener(this);
		dogManager = new DogManager(this);
		levelFactory = new LevelFactory(this);

		PluginManager pm = getServer().getPluginManager();

		// Check for Vault
		if (pm.getPlugin("Vault") != null && pm.getPlugin("Vault").isEnabled())
		{
			this.vaultEnabled = true;

			log("Vault detected.");

			RegisteredServiceProvider<Economy> economyProvider = plugin.getServer().getServicesManager().getRegistration(Economy.class);
			if (economyProvider != null)
			{
				economy = economyProvider.getProvider();
			}
			else
			{
				plugin.log("Vault not found.");
			}
		}
		else
		{
			log("Vault not found.");
		}

		permissionsManager = new PermissionsManager(this);
		particleUtils = new ParticleUtils(this);

		getServer().getPluginManager().registerEvents(tameListener, this);
		getServer().getPluginManager().registerEvents(damageListener, this);

		reloadSettings();
		saveSettings();

		permissionsManager.load();
	}

	public void log(String message)
	{
		plugin.getLogger().info(message);
	}

	public void logDebug(String message)
	{
		if (this.debug)
		{
			plugin.getLogger().info("[Debug] " + message);
		}
	}

	public void reloadSettings()
	{
		reloadConfig();
		loadSettings();
	}

	public void loadSettings()
	{
		config = getConfig();

		this.debug = config.getBoolean("Settings.Debug", false);
		this.serverName = config.getString("Settings.ServerName", "Your Server");
		this.chatPrefix = config.getString("Settings.ChatPrefix", "MyDog");
		this.randomCollarColor = config.getBoolean("DogSettings.RandomCollarColor", true);
		this.useLevels = config.getBoolean("DogSettings.UseLevels", true);
		if (config.contains("DogSettings.DogNames"))
		{
			this.dogNames = config.getStringList("DogSettings.DogNames");
		}

		// Levels
		if (config.getConfigurationSection("DogSettings.Levels") != null)
		{
			for (String level : config.getConfigurationSection("DogSettings.Levels").getKeys(false))
			{
				if (config.getConfigurationSection("DogSettings.Levels." + level) != null)
				{
					Integer exp = config.getInt("DogSettings.Levels." + level + ".Experience");
					double health = config.getInt("DogSettings.Levels." + level + ".Health");
					double damage = config.getInt("DogSettings.Levels." + level + ".Damage");

					this.dogLevels.put(Integer.parseInt(level), getLevelFactory().newLevel(Integer.parseInt(level), exp, health, damage));
				}
			}
		}
		else
		{
			// Put levels into the hashmap
			// Level format - [level, experience]
			this.dogLevels.put(2, getLevelFactory().newLevel(2, 10, 21, 5));
			this.dogLevels.put(3, getLevelFactory().newLevel(3, 100, 22, 6));
			this.dogLevels.put(4, getLevelFactory().newLevel(4, 200, 23, 7));
			this.dogLevels.put(5, getLevelFactory().newLevel(5, 500, 24, 8));
			this.dogLevels.put(6, getLevelFactory().newLevel(6, 1000, 26, 11));
			this.dogLevels.put(7, getLevelFactory().newLevel(7, 2000, 29, 13));
			this.dogLevels.put(8, getLevelFactory().newLevel(8, 3000, 31, 15));
			this.dogLevels.put(9, getLevelFactory().newLevel(9, 4000, 33, 17));
			this.dogLevels.put(10, getLevelFactory().newLevel(10, 5000, 36, 20));	
		}

		dogManager.load();
	}

	public void saveSettings()
	{
		config.set("Settings.ServerName", this.serverName);
		config.set("Settings.Debug", Boolean.valueOf(this.debug));
		config.set("Settings.ChatPrefix", this.chatPrefix);
		config.set("DogSettings.RandomCollarColor", this.randomCollarColor);
		config.set("DogSettings.UseLevels", this.useLevels);
		config.set("DogSettings.DogNames", this.dogNames);

		// Levels
		for (Integer level : this.dogLevels.keySet())
		{
			Level levelObject = this.dogLevels.get(level);
			config.set("DogSettings.Levels." + level + ".Experience", levelObject.exp);
			config.set("DogSettings.Levels." + level + ".Health", levelObject.health);
			config.set("DogSettings.Levels." + level + ".Damage", levelObject.damage);
		}

		saveConfig();
		dogManager.save();
	}

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args)
	{
		return this.commands.onCommand(sender, cmd, label, args);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args)
	{
		return this.commands.onTabComplete(sender, cmd, alias, args);
	}
}