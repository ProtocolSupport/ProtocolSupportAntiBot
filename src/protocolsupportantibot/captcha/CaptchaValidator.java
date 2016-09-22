package protocolsupportantibot.captcha;

import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketAdapter.AdapterParameteters;
import com.comphenix.protocol.events.PacketEvent;
import com.github.cage.Cage;
import com.github.cage.image.Painter;

import protocolsupport.api.events.PlayerDisconnectEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.ProtocolSupportAntiBot;
import protocolsupportantibot.Settings;
import protocolsupportantibot.bans.BanDataSource;
import protocolsupportantibot.utils.AbortableCountDownLatch.AbortedException;
import protocolsupportantibot.utils.Packets;

/*
 * Attempts to filter bots by requiring them to input captcha that is shown on the map
 */
public class CaptchaValidator implements Listener {

	protected final Map<InetSocketAddress, ValidatorInfo> validators = new ConcurrentHashMap<>();

	public CaptchaValidator() {
		//player ref init
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
			new AdapterParameteters().plugin(ProtocolSupportAntiBot.getInstance()).types(PacketType.Login.Server.SUCCESS)
		) {
			@Override
			public void onPacketSending(PacketEvent event) {
				validators.put(event.getPlayer().getAddress(), new ValidatorInfo(event.getPlayer()));
			}
		});
		//checker
		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
			new AdapterParameteters().plugin(ProtocolSupportAntiBot.getInstance()).types(PacketType.Play.Client.CHAT)
		) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				Player player = event.getPlayer();

				ValidatorInfo info = validators.get(player.getAddress());

				if (info == null) {
					return;
				}

				String message = event.getPacket().getStrings().read(0);
				if (message.startsWith("/")) {
					message = message.substring(1);
				}

				if (info.checkCaptcha(message)) {
					info.succces();
				} else {
					if (info.getTries() >= Settings.captchaMaxTries) {
						info.fail();
					} else {
						player.sendMessage(Settings.captchaFailTryMessage);
					}
				}
			}
		});
	}

	private static BufferedImage generateCaptchaImage(String val) {
		return new Cage(new Painter(128, 128, null, null, null, null), null, null, null, Cage.DEFAULT_COMPRESS_RATIO, null, null).drawImage(val);
	}

	private static byte[] toMinecraftMapData(BufferedImage image) {
		byte[] data = new byte[128 * 128];
		for (int x = 0; x < image.getWidth() && x < 128; x++) {
			for (int y = 0; y < image.getHeight() && y < 128; y++) {
				data[y * 128 + x] = MinecraftMapColor.byRgb(image.getRGB(x, y)).getMapDataId();
			}
		}
		return data;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onFinishLogin(PlayerLoginFinishEvent event) throws InterruptedException, ExecutionException, InvocationTargetException {
		if (event.isLoginDenied() || !Settings.captchaEnabled || Bukkit.getOfflinePlayer(event.getUUID()).hasPlayedBefore()) {
			validators.remove(event.getAddress());
			return;
		}

		ValidatorInfo info = validators.get(event.getAddress());

		byte[] mapdata = toMinecraftMapData(generateCaptchaImage(info.generateCaptcha()));

		ProtocolLibrary.getProtocolManager().sendServerPacket(info.player, Packets.createSetSlotPacket(36, new ItemStack(Material.MAP, 1, (short) 1)));
		ProtocolLibrary.getProtocolManager().sendServerPacket(info.player, Packets.createMapDataPacket(1, mapdata));

		info.player.sendMessage(Settings.captchaStartMessage);

		try {
			if (!info.waitConfirm(Settings.captchaMaxWait, TimeUnit.SECONDS)) {
				BanDataSource.getInstance().ban(event.getAddress().getAddress());
				info.player.kickPlayer(Settings.captchaFailMessage);
			} else {
				if (info.isSuccess()) {
					info.player.sendMessage(Settings.captchaSuccessMessage);
				} else {
					BanDataSource.getInstance().ban(event.getAddress().getAddress());
					event.denyLogin(Settings.captchaFailTryBanMessage.replace("{MAXTRIES}", String.valueOf(Settings.captchaMaxTries)));
				}
			}
		} catch (AbortedException e) {
		}

		validators.remove(event.getAddress());
	}

	@EventHandler
	public void onDisconnect(PlayerDisconnectEvent event) {
		ValidatorInfo info = validators.remove(event.getAddress());
		if (info != null) {
			info.interrupt();
		}
	}

}
