package mysticcaixas;

import java.sql.SQLException;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.scheduler.BukkitRunnable;

public class CratesListener implements Listener {
	
	@EventHandler
	public void onJoin(PlayerJoinEvent e) {
		if (!(CratesManager.isCratesLoaded(e.getPlayer())))
			new BukkitRunnable() {
				@Override
				public void run() {
					try {
						CratesManager.loadPlayerCrates(e.getPlayer().getName());
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
			}.runTaskAsynchronously(MysticCaixas.getInstance());
	}
	
	@EventHandler
	public void onClickBlock(PlayerInteractEvent e) {
		if (!(MysticCaixas.isSystemEnabled())) return;
		if (e.getAction() != Action.RIGHT_CLICK_BLOCK) return;
		Crate c = CratesManager.getCrateByBlock(e.getClickedBlock());
		if (c == null) return;
		c.openInventory(e.getPlayer());
	}
	
	@EventHandler
	public void onClickInventory(InventoryClickEvent e) {
		if (!(MysticCaixas.isSystemEnabled())) return;
		if (e.getClickedInventory() == null) return;
		if (e.getCurrentItem() == null) return;
		if ((e.getClickedInventory().getType() != InventoryType.CHEST) 
				&& (e.getClickedInventory().getType() != InventoryType.PLAYER)) return;
		Crate c = CratesManager.getCrateByTitle(e.getInventory().getTitle());
		if ((c == null) && !(e.getInventory().getTitle().startsWith("Girando"))) return;
		
		e.setCancelled(true);
		
		if (e.getCurrentItem().equals(CratesManager.rollItem) 
				&& (e.getClick() == ClickType.LEFT)) {
			Player p = (Player) e.getWhoClicked();
			int amt = CratesManager.getCrateAmount(p.getName(), c.getId());
			if (amt < 1) {
				p.closeInventory();
				Messenger.send(p, "sem-chaves");
				return;
			}
			
			int vazios = 0;
			for (int i = 0; i < 36; i++) {
				if (p.getInventory().getItem(i) == null)
					vazios++;
				if (vazios == 3)
					break;
			}
			if (vazios < 3) {
				p.closeInventory();
				Messenger.send(p, "sem-espaco");
				return;
			}
			
			try {
				CratesManager.setCrateAmount(p.getName(), c.getId(), (amt - 1));
			} 
			catch (SQLException ex) {
				ex.printStackTrace();
				p.closeInventory();
				Messenger.send(p, "sistema-indisponivel");
				return;
			}
			
			c.startRoll(p);
		}
	}
}