package protocolsupportantibot;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import protocolsupportantibot.fakespawn.LobbySchematic;

public class Commands implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
		if (!sender.hasPermission("protocolsupportantibot.admin")) {
			sender.sendMessage(ChatColor.RED + "You have no power here");
			return true;
		}
		switch (args[0].toLowerCase()) {
			case "reload": {
				Settings.load();
				sender.sendMessage(ChatColor.AQUA + "Configs reloaded");
				return true;
			}
			case "captcha": {
				if (Settings.captchaEnabled) {
					Settings.captchaEnabled = false;
					sender.sendMessage(ChatColor.AQUA + "Disabled captcha");
				} else {
					Settings.captchaEnabled = true;
					sender.sendMessage(ChatColor.AQUA + "Enabled captcha");
				}
				return true;
			}
			case "lobby": {
				LobbySchematic.load();
				sender.sendMessage(ChatColor.AQUA + "Lobby schematic reloaded");
				return true;
			}
		}
		return false;
	}

}
