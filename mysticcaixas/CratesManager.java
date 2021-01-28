package mysticcaixas;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import mysticcaixas.Crate.ItemChance;

public class CratesManager {
	
	private static final HashMap<String, Crate> CRATES = new HashMap<>();
	private static final HashMap<String, Integer> CRATES_INFO = new HashMap<>();
	private static final ArrayList<String> LOADED = new ArrayList<>();
	private static File f;
	private static FileConfiguration fc;
	protected static ItemStack rollItem, defaultItem;
	
	protected static void loadCrates() {
		f = new File(MysticCaixas.getInstance().getDataFolder(), "caixas.yml");
		if (!(f.exists()))
			MysticCaixas.getInstance().saveResource("caixas.yml", false);
		fc = YamlConfiguration.loadConfiguration(f);
		
		for (String id : fc.getKeys(false)) {
			Block b = getLocation(fc, id + ".local").getBlock();
			Crate c = new Crate(id, fc.getString(id + ".titulo").replace('&', '\u00a7'), b);
			for (String iid : fc.getConfigurationSection(id + ".itens").getKeys(false)) {
				ItemStack item = loadItem(fc, id + ".itens." + iid);
				int chance = fc.getInt(id + ".itens." + iid + ".chance");
				ItemChance ic = new ItemChance(item, chance);
				c.getItems().add(ic);
				if (fc.isList(id + ".itens." + iid + ".comandos"))
					c.addItemCommands(item, fc.getStringList
							(id + ".itens." + iid + ".comandos"));
			}
			
			if (fc.isList(id + ".comandos"))
				c.setCommands(fc.getStringList(id + ".comandos"));
			
			CRATES.put(fc.getString(id + ".local"), c);
		}
		
		rollItem = loadItem(MysticCaixas.getInstance().getConfig(), "item-girar");
		defaultItem = loadItem(MysticCaixas.getInstance().getConfig(), "item-padrao");
	}
	
	protected static void loadPlayerCrates(String player) throws SQLException {
		confirmConnection();
		PreparedStatement st = MysticCaixas.getConnection()
				.prepareStatement("SELECT * FROM mysticcaixas WHERE jogador = ?;");
		st.setString(1, player);
		
		ResultSet rs = st.executeQuery();
		while(rs.next()) {
			String crate = rs.getString("caixa");
			int amount = rs.getInt("quantidade");
			
			CRATES_INFO.put(player.toLowerCase() + "-" + crate, amount);
		}
		LOADED.add(player.toLowerCase());
	}
	
	protected static boolean isCratesLoaded(Player p) {
		return LOADED.contains(p.getName().toLowerCase());
	}
	
	public static int getCrateAmount(String player, String crateId) {
		String key = player.toLowerCase() + "-" + crateId;
		return CRATES_INFO.containsKey(key) ? CRATES_INFO.get(key) : 0;
	}
	
	protected static ArrayList<String> getCratesList(String player) {
		player = player.toLowerCase();
		ArrayList<String> crates = new ArrayList<>();
		for (String key : CRATES_INFO.keySet()) {
			if (key.startsWith(player))
				crates.add(key.split("-")[1]);
		}
		return crates;
	}
	
	protected static void setCrateAmount(String player, String crateId, int amount) 
			throws SQLException {
		confirmConnection();
		String key = player.toLowerCase() + "-" + crateId;
		if (!(CRATES_INFO.containsKey(key)))
			loadPlayerCrates(player);
		
		boolean inserting = false;
		if (!(CRATES_INFO.containsKey(key))) {
			PreparedStatement st = MysticCaixas.getConnection()
					.prepareStatement("INSERT INTO mysticcaixas (jogador, caixa, quantidade) "
							+ "VALUES (?, ?, ?);");
			st.setString(1, player);
			st.setString(2, crateId);
			st.setInt(3, amount);
			st.executeUpdate();
			
			inserting = true;
		}
		
		if (!(inserting)) {
			PreparedStatement st = MysticCaixas.getConnection()
					.prepareStatement("UPDATE mysticcaixas SET quantidade = ? "
							+ "WHERE jogador = ? AND caixa = ?;");
			st.setInt(1, amount);
			st.setString(2, player);
			st.setString(3, crateId);
			st.executeUpdate();
		}
		
		CRATES_INFO.put(key, amount);
	}
	
	public static Collection<Crate> getCrates() {
		return CRATES.values();
	}
	
	public static Crate getCrateById(String id) {
		for (Crate c : getCrates())
			if (c.getId().equalsIgnoreCase(id))
				return c;
		return null;
	}
	
	public static Crate getCrateByTitle(String inventoryTitle) {
		for (Crate c : getCrates())
			if (c.getInventoryTitle().equals(inventoryTitle))
				return c;
		return null;
	}
	
	public static Crate getCrateByBlock(Block b) {
		return CRATES.get(serializeLocation(b.getLocation()));
	}
	
	public static boolean isCrateInventory(String inventoryTitle) {
		return getCrateByTitle(inventoryTitle) != null;
	}
	
	protected static ItemStack loadItem(FileConfiguration fc, String path) {
		Material m = Material.getMaterial(fc.getString(path + ".tipo").toUpperCase());
		ItemStack is = new ItemStack(m, fc.getInt(path + ".quantidade", 1), 
				(short) fc.getInt(path + ".data", 0));
		
		ItemMeta meta = is.getItemMeta();
		if (fc.isConfigurationSection(path + ".encantamentos")) {
			for (String enc : fc.getConfigurationSection(path + ".encantamentos")
					.getKeys(false)) {
				Enchantment enchant = Enchantment.getByName(enc.toUpperCase());
				if (enchant == null) continue;
				meta.addEnchant(enchant, fc.getInt(path + ".encantamentos." + enc), true);
			}
		}
		if (fc.isList(path + ".lore")) {
			ArrayList<String> lore = new ArrayList<>();
			for (String line : fc.getStringList(path + ".lore"))
				lore.add(line.replace('&', '\u00a7'));
			meta.setLore(lore);
			
		}
		if (fc.isString(path + ".nome"))
			meta.setDisplayName(fc.getString(path + ".nome").replace('&', '\u00a7'));
		
		is.setItemMeta(meta);
		
		return is;
	}
	
	protected static Location getLocation(FileConfiguration fc, String path) {
		String[] args = fc.getString(path).split(",");
		return new Location(Bukkit.getWorld(args[0]), Double.valueOf(args[1]), 
				Double.valueOf(args[2]), Double.valueOf(args[3]), Float.valueOf(args[4]), 
				Float.valueOf(args[5]));
	}
	
	protected static String serializeLocation(Location loc) {
		return loc.getWorld().getName() + "," + loc.getX() + "," + loc.getY() + "," 
					+ loc.getZ() + "," + loc.getYaw() + "," + loc.getPitch();
	}
	
	protected static void setCrateBlock(Crate c, Block b) throws IOException {
		c.setBlock(b);
		fc.set(c.getId() + ".local", serializeLocation(b.getLocation()));
		fc.save(f);
		CRATES.remove(serializeLocation(c.getBlock().getLocation()));
		CRATES.put(serializeLocation(b.getLocation()), c);
	}
	
	private static void confirmConnection() {
		if (!(MysticCaixas.validateConnection()))
			MysticCaixas.startConnection();
	}

}
