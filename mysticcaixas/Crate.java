package mysticcaixas;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;

public class Crate {
	
	private static final int[] WIN_SLOTS = {21, 22, 23};
	
	private String id;
	private String inventoryTitle;
	private Block block;
	private ArrayList<ItemChance> items = new ArrayList<>();
	private List<String> commands = new ArrayList<>();
	private HashMap<ItemStack, List<String>> items_command = new HashMap<>();
	
	public Crate(String id, String inventoryTitle, Block block) {
		this.id = id;
		this.inventoryTitle = inventoryTitle;
		this.block = block;
	}
	
	public String getId() {
		return id;
	}
	
	public String getInventoryTitle() {
		return inventoryTitle;
	}

	public ArrayList<ItemChance> getItems() {
		return items;
	}

	public List<String> getCommands() {
		return commands;
	}
	
	public void addItemCommands(ItemStack item, List<String> commands) {
		items_command.put(item, commands);
	}

	protected void setCommands(List<String> commands) {
		this.commands = commands;
	}
	
	public boolean hasCommands() {
		return !(commands.isEmpty());
	}
	
	public Block getBlock() {
		return block;
	}
	
	protected void setBlock(Block b) {
		this.block = b;
	}
	
	protected void openInventory(Player p) {
		Inventory inv = Bukkit.createInventory(p, 45, inventoryTitle);
		
		int rollSlot = MysticCaixas.getInstance().getConfig().getInt("item-girar.slot");
		inv.setItem(rollSlot, CratesManager.rollItem);
		
		ItemStack info = CratesManager.loadItem(MysticCaixas.getInstance()
				.getConfig(), "item-info");
		
		int amount = CratesManager.getCrateAmount(p.getName(), id);
		
		List<String> lore = new ArrayList<>();
		ItemMeta meta = info.getItemMeta();
		
		for (String line : info.getItemMeta().getLore())
			lore.add(line.replace("@quantidade", Integer.toString(amount)));
		
		meta.setLore(lore);
		info.setItemMeta(meta);
		
		int infoSlot = MysticCaixas.getInstance().getConfig().getInt("item-info.slot");
		inv.setItem(infoSlot, info);
		
		p.openInventory(inv);
	}
	
	protected void startRoll(Player p) {
		Inventory inv = Bukkit.createInventory(p, 45, "Girando " + id);
		
		Random r = new Random();
		FileConfiguration cfg = MysticCaixas.getInstance().getConfig();
		Sound s = Sound.valueOf(cfg.getString("vidros.som.nome"));
		
		BukkitTask changeGlass = new BukkitRunnable() {
			@Override
			public void run() {
				for (int i = 0; i < 45; i++) {
					if ((i < 21) || (i > 23))
						inv.setItem(i, new ItemStack(Material.STAINED_GLASS_PANE, 
								1, (short) r.nextInt(15)));
				}
				p.playSound(p.getLocation(), s, 
						(float)cfg.getDouble("vidros.som.volume"), 
						(float)cfg.getDouble("vidros.som.pitch"));
			}
		}.runTaskTimer(MysticCaixas.getInstance(), 0, cfg.getInt("vidros.velocidade"));
		
		new BukkitRunnable() {
			int l = 0;
			@Override
			public void run() {
				ItemStack win = null;
				float chance = r.nextInt(100) + 1;
				for (int i = 0; i < items.size(); i++) {
					ItemChance ic = items.get(i);
					if (chance <= ic.getChance()) {
						win = ic.getItem();
						break;
					}
				}
				if (win == null)
					win = CratesManager.defaultItem;
				
				inv.setItem(WIN_SLOTS[l], win);
				
				l++;
				if (l == 3) {
					changeGlass.cancel();
					cancel();
					
					new BukkitRunnable() {
						@Override
						public void run() {
							if (!(p.isOnline())) return;
							if (!(hasCommands())) {
								for (int i : WIN_SLOTS) {
									ItemStack item = inv.getItem(i);
									if (p.getInventory().firstEmpty() == -1) {
										Messenger.send(p, "perdeu-itens");
										break;
									}
									if (items_command.containsKey(item)) {
										for (String cmd : items_command.get(item)) {
											Bukkit.dispatchCommand
													(Bukkit.getConsoleSender(), cmd
													.replace("@player", p.getName()));
										}
									}
									else {
										p.getInventory().addItem(item);
									}
								}
							} 
							else {
								for (String cmd : getCommands())
									Bukkit.dispatchCommand(Bukkit.getConsoleSender(), 
											cmd.replace("@player", p.getName()));
							}
							
							p.closeInventory();
							Messenger.send(p, "aproveite");
						}
					}.runTaskLater(MysticCaixas.getInstance(), 20);
				}
			}
		}.runTaskTimer(MysticCaixas.getInstance(), 0, 20);
		
		p.openInventory(inv);
	}
	
	public static class ItemChance {
		private ItemStack item;
		private int chance;
		
		public ItemChance(ItemStack item, int chance) {
			this.item = item;
			this.chance = chance;
		}
		
		public ItemStack getItem() {
			return item;
		}
		
		public int getChance() {
			return chance;
		}
	}
}