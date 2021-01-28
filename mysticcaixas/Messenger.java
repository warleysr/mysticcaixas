package mysticcaixas;

import java.io.File;
import java.util.HashMap;

import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Messenger {
	
	private static final HashMap<String, String> MESSAGES = new HashMap<>();
	
	protected static void loadMessages() {
		File f = new File(MysticCaixas.getInstance().getDataFolder(), "mensagens.yml");
		if (!(f.exists()))
			MysticCaixas.getInstance().saveResource("mensagens.yml", false);
		FileConfiguration fc = YamlConfiguration.loadConfiguration(f);
		for (String key : fc.getKeys(false)) {
			if (fc.isList(key)) {
				String msg = "";
				for (String s : fc.getStringList(key))
					msg += s + "\n";
				MESSAGES.put(key, msg.replace('&', '\u00a7'));
			} else
				MESSAGES.put(key, fc.getString(key).replace('&', '\u00a7'));
		}
	}
	
	public static void send(CommandSender sender, String... keys) {
		for (String key : keys)
			sender.sendMessage(get(key));
	}
	
	public static String get(String key) {
		return MESSAGES.containsKey(key) ? MESSAGES.get(key) : key + " - Erro. Contate um staff.";
	}

}
