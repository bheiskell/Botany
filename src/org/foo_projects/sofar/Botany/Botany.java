package org.foo_projects.sofar.Botany;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;

import org.foo_projects.sofar.util.ChunkList.ChunkList;

import com.bekvon.bukkit.residence.Residence;
import com.bekvon.bukkit.residence.protection.ClaimedResidence;

import com.massivecraft.factions.entity.BoardColls;
import com.massivecraft.factions.entity.Faction;
import com.massivecraft.mcore.ps.PS;

import com.palmergames.bukkit.towny.object.TownyUniverse;

import com.sk89q.worldguard.bukkit.WGBukkit;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;

public final class Botany extends JavaPlugin {
	private long conf_blocks = 1;
	private int conf_ticks = 1;

	private boolean conf_saplings = true;
	private boolean conf_cacti = true;

	private boolean conf_protect = true;

	private boolean have_factions = false;
	private boolean have_factions_old = false;
	private boolean have_towny = false;
	private boolean have_worldguard = false;
	private boolean have_residence = false;

	private Map<Material, Long> stat_planted = new HashMap<Material, Long>();

	private ChunkList chunkList;

	// main plant grow probability matrix - hashed over biome
	private class plantMatrix {
		public Material target_type;
		public byte     target_data;
		public Material base_type;
		public byte     base_data;
		public Material scan_type;
		public byte     scan_data;
		public double   density;
		public long     radius;

		public plantMatrix(Material tt, byte td, Material bt, byte bd, Material st, byte sd, double d, long r) {
			target_type = tt;
			target_data = td;
			base_type = bt;
			base_data = bd;
			scan_type = st;
			scan_data = sd;
			density = d;
			radius = r;
		}

		/*
		 * if we ever get serialization to work.... :
		 * 
		public plantMatrix(Map<String, Object> map) {
			target_type = Material.valueOf((String)map.get("target_type"));
			target_data = (byte)map.get("target_data");
			base_type = Material.valueOf((String)map.get("base_type"));
			base_data = (byte)map.get("base_data");
			scan_type = Material.valueOf((String)map.get("scan_type"));
			scan_data = (byte)map.get("scan_data");
			density = Double.parseDouble((String)map.get("density"));
			radius = Long.parseLong((String)map.get("radius"));
		}

		@Override
		public Map<String, Object> serialize() {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("target_type", target_type.toString());
			map.put("Target_data", target_data);
			map.put("base_type", base_type.toString());
			map.put("base_data", base_data);
			map.put("scan_type", scan_type.toString());
			map.put("scan_data", scan_data);
			map.put("density", String.format("%.3f", density));
			map.put("radius", String.format("%l", radius));
			return map;
		}
		*/
	}

	// contains our biome - plant probability matrix
	private static Map<Biome, List<plantMatrix>> matrix = new HashMap<Biome, List<plantMatrix>>();
	// used to fill our plant prob. matrix at startup

	private void mapadd(Biome biome, Material tt, byte td, Material bt, byte bd, Material st, byte sd, double d, long r) {
		plantMatrix pm = new plantMatrix(tt, td, bt, bd, st, sd, d, r);
		List<plantMatrix> pml;
		if (matrix.containsKey(biome)) {
			pml = matrix.get(biome);
		} else {
			pml = new ArrayList<plantMatrix>();
		}
		pml.add(pm);
		matrix.put(biome, pml);
	}

	private Biome BiomeReduce(Biome b) {
		switch (b) {
		case SWAMPLAND:
		case SWAMPLAND_MOUNTAINS:
			return Biome.SWAMPLAND;
		case FOREST:
		case FOREST_HILLS:
			return Biome.FOREST;
		case BIRCH_FOREST:
		case BIRCH_FOREST_HILLS:
		case BIRCH_FOREST_MOUNTAINS:
		case BIRCH_FOREST_HILLS_MOUNTAINS:
			return Biome.BIRCH_FOREST;
		case TAIGA:
		case TAIGA_MOUNTAINS:
		case COLD_TAIGA:
		case COLD_TAIGA_HILLS:
		case MEGA_TAIGA:
		case MEGA_TAIGA_HILLS:
		case TAIGA_HILLS:
		case COLD_TAIGA_MOUNTAINS:
		case MEGA_SPRUCE_TAIGA:
		case MEGA_SPRUCE_TAIGA_HILLS:
			return Biome.TAIGA;
		case JUNGLE:
		case JUNGLE_HILLS:
		case JUNGLE_EDGE:
		case JUNGLE_MOUNTAINS:
		case JUNGLE_EDGE_MOUNTAINS:
			return Biome.JUNGLE;
		case SAVANNA:
		case SAVANNA_PLATEAU:
		case SAVANNA_MOUNTAINS:
		case SAVANNA_PLATEAU_MOUNTAINS:
			return Biome.SAVANNA;
		case MESA:
		case MESA_PLATEAU_FOREST:
		case MESA_PLATEAU:
		case MESA_BRYCE:
		case MESA_PLATEAU_FOREST_MOUNTAINS:
		case MESA_PLATEAU_MOUNTAINS:
			return Biome.MESA;
		case DESERT:
		case DESERT_HILLS:
		case DESERT_MOUNTAINS:
			return Biome.DESERT;
		case BEACH:
		case STONE_BEACH:
		case COLD_BEACH:
			return Biome.BEACH;
		case OCEAN:
		case DEEP_OCEAN:
		case FROZEN_OCEAN:
			return Biome.OCEAN;
		case RIVER:
		case FROZEN_RIVER:
			return Biome.RIVER;
		case SMALL_MOUNTAINS:
		case EXTREME_HILLS:
		case EXTREME_HILLS_PLUS:
		case EXTREME_HILLS_MOUNTAINS:
		case EXTREME_HILLS_PLUS_MOUNTAINS:
			return Biome.EXTREME_HILLS;
		case ROOFED_FOREST:
		case ROOFED_FOREST_MOUNTAINS:
			return Biome.ROOFED_FOREST;
		case ICE_PLAINS:
		case ICE_MOUNTAINS:
			return Biome.ICE_PLAINS;
		case MUSHROOM_ISLAND:
		case MUSHROOM_SHORE:
			return Biome.MUSHROOM_ISLAND;

		case PLAINS:
		case HELL:
		case SKY:
		case SUNFLOWER_PLAINS:
		case FLOWER_FOREST:
		case ICE_PLAINS_SPIKES:

			return b;
		}
		return b;
	}

	@SuppressWarnings("deprecation")
	/* needed to make sure we don't think that a red_rose:9 is a different flower than :0 */
	private byte getSimpleData(Block b) {
		switch(b.getType()) {
		case LONG_GRASS:
			return (byte) (b.getData() % 3);
		case DOUBLE_PLANT:
			return (byte) (b.getData() % 8);
		case LEAVES:
			return (byte) (b.getData() % 4);
		case LEAVES_2:
			return (byte) (b.getData() % 2);
		case SAPLING:
			return (byte) (b.getData() % 6);
		case RED_ROSE:
			return (byte) (b.getData() % 9);
		default:
			return 0;
		}
	}

	@SuppressWarnings("deprecation")
	private void setData(Block b, byte d) {
		b.setData(d);
	}

	private boolean isProtected(Block block) {
		if (!conf_protect)
			return false;

		if (have_factions_old) {
			com.massivecraft.factions.Faction faction = com.massivecraft.factions.Board.getFactionAt(new com.massivecraft.factions.FLocation(block));
			if (!faction.isNone())
				return true;
		}
		if (have_factions) {
			Faction faction = BoardColls.get().getFactionAt(PS.valueOf(block.getLocation()));
			if (!faction.isNone())
				return true;
		}
		if (have_towny) {
			if (TownyUniverse.getTownBlock(block.getLocation()) != null)
				return true;
		}
		if (have_worldguard) {
			RegionManager rm = WGBukkit.getRegionManager(block.getWorld());
			if (rm == null)
				return false;
			ApplicableRegionSet set = rm.getApplicableRegions(block.getLocation());
			return (set.size() > 0);
		}
		if (have_residence) {
			ClaimedResidence res = Residence.getResidenceManager().getByLoc(block.getLocation());
			if (res != null)
				return true;
		}

		return false;
	}

	private void growAt(World world, int x, int z) {
		Block b = world.getHighestBlockAt(x, z);

		if (isProtected(b))
			return;

		// verify this block is empty
		if (b.getType() != Material.AIR)
			return;

		// do we plant in this biome?
		if (!matrix.containsKey(BiomeReduce(b.getBiome())))
			return;

		List<plantMatrix> pml = matrix.get(BiomeReduce(b.getBiome()));

		for (plantMatrix pm: pml) {
			int count = 0;

			// are these plant types enabled?
			if (pm.target_type == Material.SAPLING && (!conf_saplings))
				continue;
			if (pm.target_type == Material.CACTUS && (!conf_cacti))
				continue;

			Block base = b.getRelative(BlockFace.DOWN);

			// check if base is OK for this material
			if ((base.getType() != pm.base_type) || (getSimpleData(base) != pm.base_data))
				continue;

			// determine density of plant in radius
			for (long xx = b.getX() - pm.radius; xx < b.getX() + pm.radius; xx++) {
				for (long zz = b.getZ() - pm.radius; zz < b.getZ() + pm.radius; zz++) {
					Block h = world.getHighestBlockAt((int)xx, (int)zz);
					if (h.getType() == Material.AIR)
						h = h.getRelative(BlockFace.DOWN);

					if (((h.getType() == pm.scan_type) && (getSimpleData(h) == pm.scan_data)) ||
						((h.getType() == pm.target_type) && (getSimpleData(h) == pm.target_data)))
						count++;
				}
			}

			// The cast to double here is critical!
			if (((double)count / (pm.radius * pm.radius)) < pm.density) {
				// plant the thing
				b.setType(pm.target_type);
				setData(b, pm.target_data);
				if (pm.target_type == Material.DOUBLE_PLANT) {
					Random rnd = new Random();
					Block tb = b.getRelative(BlockFace.UP);
					tb.setType(Material.DOUBLE_PLANT);
					/* top half seems to be (8 & orientation of planting) - make it random */
					setData(tb, (byte)(8 & rnd.nextInt(4)));
				}

				if (stat_planted.get(pm.target_type) == null)
					stat_planted.put(pm.target_type, (long)1);
				else
					stat_planted.put(pm.target_type, stat_planted.get(pm.target_type) + 1);

				return;
			}
		}
	}

	private class BotanyRunnable implements Runnable {
		@Override
		public void run() {
			if (chunkList.isEmpty())
				return;
			for (World w: chunkList.listWorlds()) {
				for (int j = 0; j < conf_blocks; j++) {
					Chunk c = chunkList.getRandom(w);
					Random rnd = new Random();
					int x = rnd.nextInt(16);
					int z = rnd.nextInt(16);
					growAt(c.getWorld(), c.getX() * 16 + x, c.getZ() * 16 + z);
				}
			}
		}
	}

	class BotanyCommand implements CommandExecutor {
		public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
			String msg = "unknown command";
			String helpmsg = "\n" + 
					"/botany help - display this help message\n" +
					"/botany stats - display statistics\n" +
					"/botany list - display enabled worlds\n" +
					"/botany blocks <int> - set number of block attempts per cycle\n" +
					"/botany enable <world> - enable for world\n" +
					"/botany disable <world> - enable for world\n" +
					"/botany scan - scan the area in your current biome for plants";

command:
			if (split.length >= 1) {
				switch (split[0]) {
					case "blocks":
						if (split.length == 2) {
							conf_blocks = Long.parseLong(split[1]);
							getConfig().set("blocks", conf_blocks);
							saveConfig();
						}
						msg = "number of blocks set to " + conf_blocks;
						break;
					case "test":
						if (split.length != 7) {
							msg = "test requires 6 parameters: world x1 z1 x2 z2 blocks";
							break;
						};
						for (World w: chunkList.listWorlds()) {
							if (w.getName().equals(split[1])) {
								int x1 = Integer.parseInt(split[2]);
								int z1 = Integer.parseInt(split[3]);
								int x2 = Integer.parseInt(split[4]);
								int z2 = Integer.parseInt(split[5]);
								if (x1 > x2) {
									int t = x1;
									x1 = x2;
									x2 = t;
								}
								if (z1 > z2) {
									int t = z1;
									z1 = z2;
									z2 = t;
								}
								for (long i = 0; i < Long.parseLong(split[6]); i++)
									for (int x = x1; x <= x2; x++)
										for (int z = z1; z <= z2; z++)
											growAt(w, x, z);
								msg = "test cycle finished";
								break command;
							}
						}
						msg = "Invalid world name - world must be enabled already";
						break;
					case "enable":
						if (split.length != 2) {
							msg = "enable requires a world name";
							break;
						};
						if (!chunkList.enableWorld(split[1])) {
							msg = "unable to enable for world \"" + split[1] + "\"";
						} else {
							msg = "enabled for world \"" + split[1] + "\"";
							List<String> worldStringList = getConfig().getStringList("worlds");
							if (worldStringList.indexOf(split[1]) == -1) {
								worldStringList.add(split[1]);
								getConfig().set("worlds", worldStringList);
								saveConfig();
							}
						}
						break;
					case "disable":
						if (split.length != 2) {
							msg = "disable requires a world name";
							break;
						};
						if (!chunkList.disableWorld(split[1])) {
							msg = "unable to disable for world \"" + split[1] + "\"";
						} else {
							msg = "disabled for world \"" + split[1] + "\"";
							List<String> worldStringList = getConfig().getStringList("worlds");
							worldStringList.remove(split[1]);
							getConfig().set("worlds", worldStringList);
							saveConfig();
						}
						break;
					case "list":
						msg = "plugin enabled for worlds:\n";
						if (chunkList.isEmpty()) {
							msg = "Not enabled for any worlds\n";
							break;
						}
						for (World w: chunkList.listWorlds())
							msg += "- " + w.getName() + "\n";
						break;
					case "stats":
						msg = "Planting statistics:\n";
						for (Material m: stat_planted.keySet())
							msg += m.toString() + " - " + stat_planted.get(m) + "\n";
						break;
					case "scan":
						if (!(sender instanceof Player)) {
							msg = "You must be a player to issue this command!\n";
							break;
						}

						Player player = (Player)sender;
						Block block = player.getLocation().getBlock();
						Biome biome = block.getBiome();
						World world = player.getWorld();
						long area = 0;
						Map <String,Long> plants = new HashMap<String,Long>();

						for (int x = block.getX() - 100; x < block.getX() + 100; x++) {
							for (int z = block.getZ() - 100; z < block.getZ() + 100; z++) {
								Block scan = world.getHighestBlockAt(x, z);
								if (!scan.getBiome().equals(biome))
									continue;
								if (scan.getType() == Material.AIR)
									scan = scan.getRelative(BlockFace.DOWN);

								area++;

								Material mat = scan.getType();
								switch (mat) {
								case LONG_GRASS:
								case DOUBLE_PLANT:
								case CACTUS:
								case LEAVES:
								case LEAVES_2:
								case SAPLING:
								case RED_ROSE:
								case YELLOW_FLOWER:
								case DEAD_BUSH:
								case SUGAR_CANE:
									String name = mat.toString() + ":" + getSimpleData(scan);
									if (plants.get(name) != null)
										plants.put(name, plants.get(name) + 1);
									else
										plants.put(name, (long)1);
									break;
								default:
									break;
								}
							}
						}
						msg = "Scan results:\n";
						msg += "Biome: " + biome.toString() + " Area: " + area + " range: " + 100 + "\n";
						for (String name: plants.keySet())
							msg += biome.toString() + "," + name + "," + plants.get(name) + "," + (String.format("%.3f",  (float)plants.get(name) / (float)area)) + "\n";
						break;
					case "help":
					default:
						msg = helpmsg;
						break;
				}
			} else {
				msg = helpmsg;
			}

			if (!(sender instanceof Player)) {
				getLogger().info(msg);
			} else {
				Player player = (Player) sender;
				player.sendMessage(msg);
			}
			return true;
		}
	}

	public void onEnable() {
		// config data handling
		saveDefaultConfig();

		// setup
		this.chunkList = new ChunkList(this);

		conf_blocks = getConfig().getInt("blocks");
		conf_ticks = getConfig().getInt("ticks");

		conf_saplings = getConfig().getBoolean("saplings");
		conf_cacti = getConfig().getBoolean("cacti");

		getLogger().info("blocks: " + conf_blocks + " ticks: " + conf_ticks);

		List<String> worldStringList = getConfig().getStringList("worlds");

		/* populate chunk cache for each world */
		for (int i = 0; i < worldStringList.size(); i++)
			chunkList.enableWorld(worldStringList.get(i));

		/* Detect protection plugins like WG, factions, etc. */
		if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("Factions")) {
			try {
				/* this is an old API thing */
				new com.massivecraft.factions.FLocation();
			} catch (NoClassDefFoundError e) {
				have_factions = true;
			}
			if (!have_factions)
				have_factions_old = true;
		}

		if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("Towny"))
			have_towny = true;

		if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("WorldGuard"))
			have_worldguard = true;

		if (org.bukkit.Bukkit.getPluginManager().isPluginEnabled("Residence"))
			have_residence = true;

		getLogger().info("Protection plugins: " +
						(have_factions | have_factions_old ? "+" : "-") + "Factions, " +
						(have_towny ? "+" : "-") + "Towny, " +
						(have_worldguard ? "+" : "-") + "WorldGuard, " +
						(have_residence ? "+" : "-") + "Residence"
						);

		conf_protect = getConfig().getBoolean("protect");
		getLogger().info("protection is " + (conf_protect ? "on" : "off"));

		// long array of plants for each biome
		mapadd(Biome.BEACH,   Material.LONG_GRASS,    (byte)1, Material.GRASS, (byte)0, Material.LONG_GRASS,    (byte)1, 0.01,   16);

		mapadd(Biome.SAVANNA, Material.LONG_GRASS,    (byte)1, Material.GRASS, (byte)0, Material.LONG_GRASS,    (byte)1, 0.6,     8);
		mapadd(Biome.SAVANNA, Material.SAPLING,       (byte)4, Material.GRASS, (byte)0, Material.LEAVES_2,      (byte)0, 0.1,    32);
		mapadd(Biome.SAVANNA, Material.DOUBLE_PLANT,  (byte)2, Material.GRASS, (byte)0, Material.DOUBLE_PLANT,  (byte)2, 0.01,   16);

		mapadd(Biome.PLAINS,  Material.LONG_GRASS,    (byte)1, Material.GRASS, (byte)0, Material.LONG_GRASS,    (byte)1, 0.3,     8);
		mapadd(Biome.PLAINS,  Material.YELLOW_FLOWER, (byte)0, Material.GRASS, (byte)0, Material.YELLOW_FLOWER, (byte)0, 0.01,   16);
		mapadd(Biome.PLAINS,  Material.RED_ROSE,      (byte)0, Material.GRASS, (byte)0, Material.RED_ROSE,      (byte)0, 0.01,   16);
		mapadd(Biome.PLAINS,  Material.DOUBLE_PLANT,  (byte)2, Material.GRASS, (byte)0, Material.DOUBLE_PLANT,  (byte)2, 0.01,   16);

		mapadd(Biome.DESERT,  Material.CACTUS,        (byte)0, Material.SAND,  (byte)0, Material.CACTUS,        (byte)0, 0.0004, 32);
		mapadd(Biome.DESERT,  Material.LONG_GRASS,    (byte)0, Material.SAND,  (byte)0, Material.LONG_GRASS,    (byte)0, 0.0004, 32);

		getCommand("botany").setExecutor(new BotanyCommand());

		// schedule our planter
		BukkitScheduler scheduler = Bukkit.getServer().getScheduler();
		scheduler.scheduleSyncRepeatingTask(this, new BotanyRunnable(), 1L, conf_ticks);
	}
}
