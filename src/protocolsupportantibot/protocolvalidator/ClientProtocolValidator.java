package protocolsupportantibot.protocolvalidator;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;

import protocolsupport.api.Connection;
import protocolsupport.api.Connection.PacketReceiveListener;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.events.ConnectionCloseEvent;
import protocolsupport.api.events.ConnectionOpenEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.Settings;
import protocolsupportantibot.utils.AbortableCountDownLatch.AbortedException;
import protocolsupportantibot.utils.Packets;

/*
 * Attempts to filter bots that don't implement some request->response minecraft client mechanic
 * TODO: do more advanced checks
 */
public class ClientProtocolValidator implements Listener {

	private static final String validator_info_key = "pasb_protocol_validator_info_key";
	private static final String validator_listener_key = "pasb_protocol_validator_listener_key";

	@EventHandler(priority = EventPriority.LOWEST)
	public void onConnectionOpen(ConnectionOpenEvent event) {
		final Connection connection = event.getConnection();
		final ValidatorInfo validator = new ValidatorInfo();
		PacketReceiveListener listener = packetObj -> {
			PacketContainer container = PacketContainer.fromPacket(packetObj);
			if (container.getType() == PacketType.Play.Client.SETTINGS) {
				validator.confirmSettings();
			} else if (container.getType() == PacketType.Play.Client.TRANSACTION) {
				validator.confirmTransaction();
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

		if (event.isLoginDenied() || !Settings.protocolValidatorEnabled || Bukkit.getOfflinePlayer(event.getUUID()).hasPlayedBefore()) {
			cleanupConnection(connection);
			return;
		}

		ValidatorInfo validator = (ValidatorInfo) connection.getMetadata(validator_info_key);

		connection.sendPacket(Packets.createTransactionPacket());

		connection.sendPacket(Packets.createChatPacket(Settings.protocolValidatorStartMessage));

		try {
			if (!validator.waitConfirm(Settings.protocolValidatorMaxWait, TimeUnit.SECONDS)) {
				event.denyLogin(Settings.protocolValidatorFailMessage);
			} else {
				connection.sendPacket(Packets.createChatPacket(Settings.protocolValidatorSuccessMessage));
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
