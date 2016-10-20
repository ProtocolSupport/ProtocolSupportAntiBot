package protocolsupportantibot.captcha;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

import protocolsupport.api.Connection;
import protocolsupport.api.Connection.PacketReceiveListener;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.events.ConnectionCloseEvent;
import protocolsupport.api.events.ConnectionOpenEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.Settings;
import protocolsupportantibot.bans.BanDataSource;
import protocolsupportantibot.utils.AbortableCountDownLatch.AbortedException;
import protocolsupportantibot.utils.Packets;

/*
 * Attempts to filter bots by requiring them to input captcha that is shown on the map
 */
public class CaptchaValidator implements Listener {

	private static final String validator_info_key = "pasb_captcha_validator_info_key";
	private static final String validator_listener_key = "pasb_captcha_validator_listener_key";

	@EventHandler(priority = EventPriority.LOWEST)
	public void onConnectionOpen(ConnectionOpenEvent event) {
		final Connection connection = event.getConnection();
		final ValidatorInfo validator = new ValidatorInfo();
		PacketReceiveListener listener = packetObj -> {
			PacketContainer container = PacketContainer.fromPacket(packetObj);
			if (container.getType() == PacketType.Play.Client.CHAT) {
				String message = container.getStrings().read(0);
				if (message.startsWith("/")) {
					message = message.substring(1);
				}

				if (validator.checkCaptcha(message)) {
					validator.succces();
				} else {
					if (validator.getTries() >= Settings.captchaMaxTries) {
						validator.fail();
					} else {
						connection.sendPacket(Packets.createChatPacket(Settings.captchaFailTryMessage));
					}
				}
			}
			return true;
		};
		connection.addMetadata(validator_info_key, validator);
		connection.addMetadata(validator_listener_key, listener);
		connection.addPacketReceiveListener(listener);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onFinishLogin(PlayerLoginFinishEvent event) throws InterruptedException, ExecutionException, InvocationTargetException {
		Connection connection = ProtocolSupportAPI.getConnection(event.getAddress());

		if (event.isLoginDenied() || !Settings.captchaEnabled || Bukkit.getOfflinePlayer(event.getUUID()).hasPlayedBefore()) {
			cleanupConnection(connection);
			return;
		}

		ValidatorInfo validator = (ValidatorInfo) connection.getMetadata(validator_info_key);

		byte[] mapdata = MapCaptchaPainter.create(validator.generateCaptcha());

		connection.sendPacket(Packets.createSetSlotPacket(36, new ItemStack(Material.MAP, 1, (short) 1)));
		connection.sendPacket(Packets.createMapDataPacket(1, mapdata));

		connection.sendPacket(Packets.createChatPacket(Settings.captchaStartMessage));

		try {
			if (!validator.waitConfirm(Settings.captchaMaxWait, TimeUnit.SECONDS)) {
				BanDataSource.getInstance().ban(event.getAddress().getAddress());
				event.denyLogin(Settings.captchaFailMessage);
			} else {
				if (validator.isSuccess()) {
					connection.sendPacket(Packets.createChatPacket(Settings.captchaSuccessMessage));
				} else {
					BanDataSource.getInstance().ban(event.getAddress().getAddress());
					event.denyLogin(Settings.captchaFailTryBanMessage.replace("{MAXTRIES}", String.valueOf(Settings.captchaMaxTries)));
				}
			}
			cleanupConnection(connection);
		} catch (AbortedException e) {
		}
	}

	private void cleanupConnection(Connection connection) {
		connection.removeMetadata(validator_info_key);
		PacketReceiveListener listener = (PacketReceiveListener) connection.removeMetadata(validator_listener_key);
		if (listener != null) {
			connection.removePacketReceiveListener(listener);
		}
	}

	@EventHandler
	public void onDisconnect(ConnectionCloseEvent event) {
		ValidatorInfo info = (ValidatorInfo) event.getConnection().removeMetadata(validator_info_key);
		if (info != null) {
			info.interrupt();
		}
	}

}
