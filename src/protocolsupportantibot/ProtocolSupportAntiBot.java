package protocolsupportantibot;

import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import protocolsupportantibot.bans.BanChecker;
import protocolsupportantibot.captcha.CaptchaValidator;
import protocolsupportantibot.fakespawn.FakeWorldSpawn;
import protocolsupportantibot.fakespawn.LobbySchematic;
import protocolsupportantibot.logininterval.LoginInterval;
import protocolsupportantibot.protocolvalidator.ClientProtocolValidator;
import protocolsupportantibot.utils.ProtocolLibPacketSender;

public class ProtocolSupportAntiBot extends JavaPlugin implements Listener {

	private static ProtocolSupportAntiBot instance;

	public static ProtocolSupportAntiBot getInstance() {
		return instance;
	}

	public ProtocolSupportAntiBot() {
		instance = this;
	}

	@Override
	public void onEnable() {
		try {
			ProtocolLibPacketSender.init();
			Settings.load();
			getCommand("protocolsupportantibot").setExecutor(new Commands());
			saveResource(LobbySchematic.name, false);
			LobbySchematic.load();
			getServer().getPluginManager().registerEvents(new BanChecker(), this);
			getServer().getPluginManager().registerEvents(new FakeWorldSpawn(), this);
			getServer().getPluginManager().registerEvents(new ClientProtocolValidator(), this);
			getServer().getPluginManager().registerEvents(new CaptchaValidator(), this);
			getServer().getPluginManager().registerEvents(new LoginInterval(), this);
		} catch (Exception e) {
			throw new RuntimeException("Error when enabling", e);
		}
	}

}
