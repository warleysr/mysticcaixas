package mysticcaixas;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;

public class MysticCaixas extends JavaPlugin {
	
	private static MysticCaixas instance; 
	private static Connection conn;
	private static Location crates;
	
	@Override
	public void onEnable() {
		instance = this;
		
		saveDefaultConfig();
		reloadConfig();
		
		crates = CratesManager.getLocation(getConfig(), "local");
		
		boolean ok = startConnection();
		if (!(ok)) {
			Bukkit.getConsoleSender().sendMessage("\u00a7c*** Falha ao conectar MySQL ***");
			setEnabled(false);
			return;
		}
		
		try {
			PreparedStatement st = conn.prepareStatement("CREATE TABLE IF NOT EXISTS "
					+ "mysticcaixas (jogador VARCHAR(16), caixa VARCHAR(16), quantidade INT);");
			st.executeUpdate();
		} catch (SQLException e) {
			e.printStackTrace();
			setEnabled(false);
			return;
		}
		
		CratesManager.loadCrates();
		Messenger.loadMessages();
		
		Bukkit.getPluginManager().registerEvents(new CratesListener(), this);
		
		CratesCommandExecutor executor = new CratesCommandExecutor();
		for (String cmd : executor.getCommands())
			getCommand(cmd).setExecutor(executor);
		
		if (getConfig().getBoolean("mysql.reconectar", false)) {
			new BukkitRunnable() {
				@Override
				public void run() {
					if (!(validateConnection())) {
						if (!(startConnection())) {
							setEnabled(false);
							cancel();
						}
					}
				}
			}.runTaskTimerAsynchronously(this, 3600, 3600);
		}
	}
	
	public static MysticCaixas getInstance() {
		return instance;
	}
	
	protected static Connection getConnection() {
		return conn;
	}
	
	protected static boolean startConnection() {
		try {
			FileConfiguration cfg = instance.getConfig();
			conn = DriverManager.getConnection("jdbc:mysql://" 
					+ cfg.getString("mysql.host") 
					+ ":" + cfg.getInt("mysql.port") 
					+ "/" + cfg.getString("mysql.db"), 
					cfg.getString("mysql.user"), 
					cfg.getString("mysql.pass"));
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected static boolean validateConnection() {
		try {
			return conn.isValid(3);
		} catch (SQLException e) {
			e.printStackTrace();
			return false;
		}
	}
	
	protected static Location getCratesLocation() {
		return crates;
	}
	
	protected static void setCratesLocation(Location newLoc) {
		crates = newLoc;
	}
	
	public static boolean isSystemEnabled() {
		return instance.getConfig().getBoolean("enabled");
	}
	
	protected static void setSystemEnabled(boolean enabled) {
		instance.getConfig().set("enabled", enabled);
		instance.saveConfig();
	}
	
}
