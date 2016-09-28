package protocolsupportantibot;

import java.io.File;
import java.io.IOException;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.YamlConfiguration;

public class Settings {

	public static int loginInterval = 50;
	public static String loginIntervalMessage = ChatColor.AQUA + "Your connection is currently queued. Please wait for your turn. There are {PLAYERS} players before you in a queue.";

	public static boolean protocolValidatorEnabled = true;
	public static int protocolValidatorMaxWait = 10;
	public static String protocolValidatorStartMessage = ChatColor.AQUA + "Basic antibot check in progress, please wait";
	public static String protocolValidatorSuccessMessage = ChatColor.AQUA + "Basic bot check success";
	public static String protocolValidatorFailMessage = ChatColor.RED + "Failed basic antibot check";

	public static boolean captchaEnabled = true;
	public static int captchaMaxWait = 60;
	public static int captchaMaxTries = 3;
	public static String captchaStartMessage = ChatColor.AQUA + "Please write a number you see on the map to chat";
	public static String captchaSuccessMessage = ChatColor.AQUA + "Your captca code is valid";
	public static String captchaFailMessage = ChatColor.RED + "You didn't solve captcha in time, your address is temportally banned, please try joining again later";
	public static String captchaFailTryMessage = ChatColor.RED + "Wrong captcha, please try again";
	public static String captchaFailTryBanMessage = ChatColor.RED + "You failed to input valid captcha in {MAXTRIES} tries, your address is temporally banned, please try joining again later";

	public static long tempBanTime = 60;
	public static String tempBanMessage = ChatColor.RED + "Your address is temporally banned, please try again later";

	public static void load() {
		YamlConfiguration config = YamlConfiguration.loadConfiguration(getMainConfigFile());
		loginInterval = config.getInt("logininterval", loginInterval);
		protocolValidatorEnabled = config.getBoolean("protocolvalidator.enabled", protocolValidatorEnabled);
		protocolValidatorMaxWait = config.getInt("protocolvalidator.maxwait", protocolValidatorMaxWait);
		captchaEnabled = config.getBoolean("captcha.enabled", captchaEnabled);
		captchaMaxWait = config.getInt("captcha.maxwait", captchaMaxWait);
		captchaMaxTries = config.getInt("captcha.maxtries", captchaMaxTries);
		tempBanTime = config.getLong("tempban.time", tempBanTime);
		YamlConfiguration messages = YamlConfiguration.loadConfiguration(getMessagesConfigFile());
		loginIntervalMessage = messages.getString("logininterval.start", loginIntervalMessage);
		protocolValidatorStartMessage = messages.getString("protocolvalidator.start", protocolValidatorStartMessage);
		protocolValidatorSuccessMessage = messages.getString("protocolvalidator.success", protocolValidatorSuccessMessage);
		protocolValidatorFailMessage = messages.getString("protocolvalidator.fail", protocolValidatorFailMessage);
		captchaStartMessage = messages.getString("captcha.start", captchaStartMessage);
		captchaSuccessMessage = messages.getString("captcha.success", captchaSuccessMessage);
		captchaFailMessage = messages.getString("captcha.fail", captchaFailMessage);
		captchaFailTryMessage = messages.getString("captcha.tryfail", captchaFailTryMessage);
		captchaFailTryBanMessage = messages.getString("captcha.tryfailban", captchaFailTryBanMessage);
		tempBanMessage = messages.getString("tempban", tempBanMessage);
		save();
	}

	private static void save() {
		YamlConfiguration config = new YamlConfiguration();
		config.set("logininterval", loginInterval);
		config.set("protocolvalidator.enabled", protocolValidatorEnabled);
		config.set("protocolvalidator.maxwait", protocolValidatorMaxWait);
		config.set("captcha.enabled", captchaEnabled);
		config.set("captcha.maxwait", captchaMaxWait);
		config.set("captcha.maxtries", captchaMaxTries);
		config.set("tempban.time", tempBanTime);
		YamlConfiguration messages = new YamlConfiguration();
		messages.set("logininterval.start", loginIntervalMessage);
		messages.set("protocolvalidator.start", protocolValidatorStartMessage);
		messages.set("protocolvalidator.success", protocolValidatorSuccessMessage);
		messages.set("protocolvalidator.fail", protocolValidatorFailMessage);
		messages.set("captcha.start", captchaStartMessage);
		messages.set("captcha.success", captchaSuccessMessage);
		messages.set("captcha.fail", captchaFailMessage);
		messages.set("captcha.tryfail", captchaFailTryMessage);
		messages.set("captcha.tryfailban", captchaFailTryBanMessage);
		messages.set("tempban", tempBanMessage);
		try {
			config.save(getMainConfigFile());
			messages.save(getMessagesConfigFile());
		} catch (IOException e) {
		}
	}

	private static File getMainConfigFile() {
		return new File(ProtocolSupportAntiBot.getInstance().getDataFolder(), "config.yml");
	}

	private static File getMessagesConfigFile() {
		return new File(ProtocolSupportAntiBot.getInstance().getDataFolder(), "messages.yml");
	}

}
