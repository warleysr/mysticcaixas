package mysticcaixas;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class CratesCommandExecutor implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command command, String cmd, String[] args) {
		if (command.getName().equalsIgnoreCase("caixas")) {
			if (!(MysticCaixas.isSystemEnabled()))  {
				Messenger.send(sender, "sistema-indisponivel");
				return false;
			}
			if (!(sender instanceof Player)) {
				Messenger.send(sender, "somente-player");
				return false;
			}
			new BukkitRunnable() {
				@Override
				public void run() {
					((Player) sender).teleport(MysticCaixas.getCratesLocation());
					Messenger.send(sender, "bem-vindo");
				}
			}.runTaskAsynchronously(MysticCaixas.getInstance());
			
		} else if (command.getName().equalsIgnoreCase("setcaixas")) {
			if (!(sender instanceof Player)) {
				Messenger.send(sender, "somente-player");
				return false;
			}
			if (!(sender.hasPermission("mysticcaixas.admin"))) {
				Messenger.send(sender, "sem-permissao");
				return false;
			}
			Location loc = ((Player) sender).getLocation();
			MysticCaixas.getInstance().getConfig().set("local", 
					CratesManager.serializeLocation(loc));
			MysticCaixas.getInstance().saveConfig();
			MysticCaixas.setCratesLocation(loc);
			Messenger.send(sender, "local-definido");
			
		} else if (command.getName().equalsIgnoreCase("setcaixa")) {
			if (!(sender instanceof Player)) {
				Messenger.send(sender, "somente-player");
				return false;
			}
			if (!(sender.hasPermission("mysticcaixas.admin"))) {
				Messenger.send(sender, "sem-permissao");
				return false;
			}
			if (args.length == 0) {
				Messenger.send(sender, "uso-setcaixa");
				return false;
			}
			Player p  = (Player) sender;
			Crate c = CratesManager.getCrateById(args[0]);
			if (c == null) {
				Messenger.send(sender, "caixa-nao-existe");
				return false;
			}
			try {
				CratesManager.setCrateBlock(c, p.getTargetBlock(null, 5));
				p.sendMessage(Messenger.get("caixa-setada").replace("@caixa", c.getId()));
			} catch (IOException e) {
				Messenger.send(p, "erro-critico");
				e.printStackTrace();
			}
		} else if (command.getName().equalsIgnoreCase("chaves")) {
			if (!(sender.hasPermission("mysticcaixas.admin"))) {
				Messenger.send(sender, "sem-permissao");
				return false;
			}
			if (args.length == 0) {
				Messenger.send(sender, "uso-chaves");
				return false;
			}
			if (CratesManager.getCratesList(args[0]).isEmpty()) {
				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							CratesManager.loadPlayerCrates(args[0]);
							if (CratesManager.getCratesList(args[0]).isEmpty()) {
								Messenger.send(sender, "sem-caixas");
								return;
							}
							sendCratesInfo(sender, args[0]);
						} catch (SQLException e) {
							e.printStackTrace();
							Messenger.send(sender, "erro-critico");
						}
					}
				}.runTaskAsynchronously(MysticCaixas.getInstance());
			} else
				sendCratesInfo(sender, args[0]);
			
		} else if (command.getName().equalsIgnoreCase("darchaves")) {
			if (!(sender.hasPermission("mysticcaixas.admin"))) {
				Messenger.send(sender, "sem-permissao");
				return false;
			}
			if (args.length < 3) {
				Messenger.send(sender, "uso-darchaves");
				return false;
			}
			Crate c = CratesManager.getCrateById(args[1]);
			if (c == null) {
				Messenger.send(sender, "caixa-nao-existe");
				return false;
			}
			try {
				int give = Integer.parseInt(args[2]);
				if (give < 1) {
					Messenger.send(sender, "somente-numero-valido");
					return false;
				}
				int amt = CratesManager.getCrateAmount(args[0], c.getId());
				
				new BukkitRunnable() {
					@Override
					public void run() {
						try {
							CratesManager.setCrateAmount(args[0], c.getId(), (amt + give));
							sender.sendMessage(Messenger.get("caixas-dadas")
									.replace("@player", args[0]).replace("@caixa", c.getId())
									.replace("@quantidade", Integer.toString(give)));
						} catch (SQLException e) {
							e.printStackTrace();
							Messenger.send(sender, "erro-critico");
						}
					}
				}.runTaskAsynchronously(MysticCaixas.getInstance());
			} catch (NumberFormatException e) {
				Messenger.send(sender, "somente-numero-valido");
			}
		} else if (command.getName().equalsIgnoreCase("desativarcaixas")) {
			if (!(sender.hasPermission("mysticcaixas.admin"))) {
				Messenger.send(sender, "sem-permissao");
				return false;
			}
			if (!(MysticCaixas.isSystemEnabled())) {
				Messenger.send(sender, "ja-desativado");
				return false;
			}
			MysticCaixas.setSystemEnabled(false);
			Messenger.send(sender, "desativado");
			
		} else if (command.getName().equalsIgnoreCase("ativarcaixas")) {
			if (!(sender.hasPermission("mysticcaixas.admin"))) {
				Messenger.send(sender, "sem-permissao");
				return false;
			}
			if (MysticCaixas.isSystemEnabled()) {
				Messenger.send(sender, "ja-ativado");
				return false;
			}
			MysticCaixas.setSystemEnabled(true);
			Messenger.send(sender, "ativado");
		}
		return false;
	}
	
	protected String[] getCommands() {
		return new String[]{"caixas", "setcaixa", "setcaixas", "chaves", 
				"darchaves", "desativarcaixas", "ativarcaixas"};
	}
	
	private void sendCratesInfo(CommandSender sender, String player) {
		String msg = Messenger.get("info-chaves-header")
				.replace("@player", player) + "\n";
		for (String crate : CratesManager.getCratesList(player)) {
			int amt = CratesManager.getCrateAmount(player, crate);
			msg += Messenger.get("info-chaves-body").replace("@caixa", crate)
					.replace("@quantidade", Integer.toString(amt)) + "\n";
		}
		msg += Messenger.get("info-chaves-footer");
		sender.sendMessage(msg);
	}

}
