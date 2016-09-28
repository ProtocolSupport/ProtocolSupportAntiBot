package protocolsupportantibot.protocolvalidator;

import java.lang.reflect.InvocationTargetException;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketAdapter.AdapterParameteters;
import com.comphenix.protocol.events.PacketEvent;

import protocolsupport.api.events.PlayerDisconnectEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.ProtocolSupportAntiBot;
import protocolsupportantibot.Settings;
import protocolsupportantibot.utils.AbortableCountDownLatch.AbortedException;
import protocolsupportantibot.utils.Packets;
import protocolsupportantibot.utils.ProtocolLibPacketSender;

/*
 * Attempts to filter bots that don't implement some request->response minecraft client mechanic
 * TODO: do more advanced checks
 */
public class ClientProtocolValidator implements Listener {

	protected final Map<InetSocketAddress, ValidatorInfo> validators = new ConcurrentHashMap<>();

	public ClientProtocolValidator() {
		ProtocolLibrary.getProtocolManager().addPacketListener(
			new PacketAdapter(
				new AdapterParameteters()
				.plugin(ProtocolSupportAntiBot.getInstance())
				.types(PacketType.Login.Server.SUCCESS)
			) {
				@Override
				public void onPacketSending(PacketEvent event) {
					validators.put(event.getPlayer().getAddress(), new ValidatorInfo(event.getPlayer()));
				}
			}
		);

		ProtocolLibrary.getProtocolManager().addPacketListener(
			new PacketAdapter(
				new AdapterParameteters().plugin(ProtocolSupportAntiBot.getInstance())
				.types(PacketType.Play.Client.SETTINGS, PacketType.Play.Client.TRANSACTION, PacketType.Play.Client.TELEPORT_ACCEPT)
			) {
				@Override
				public void onPacketReceiving(PacketEvent event) {
					ValidatorInfo validator = validators.get(event.getPlayer().getAddress());

					if (validator == null) {
						return;
					}

					PacketType type = event.getPacketType();
					if (type == PacketType.Play.Client.TELEPORT_ACCEPT) {
						validator.confirmTeleport();
					} else if (type == PacketType.Play.Client.SETTINGS) {
						validator.confirmSettings();
					} else if (type == PacketType.Play.Client.TRANSACTION) {
						validator.confirmTransaction();
					}
				}
			}
		);
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onFinishLogin(PlayerLoginFinishEvent event) throws InterruptedException, ExecutionException, InvocationTargetException {
		if (event.isLoginDenied() || !Settings.protocolValidatorEnabled || Bukkit.getOfflinePlayer(event.getUUID()).hasPlayedBefore()) {
			validators.remove(event.getAddress());
			return;
		}

		ValidatorInfo playervalidator = validators.get(event.getAddress());

		ProtocolLibPacketSender.sendServerPacket(playervalidator.player, Packets.createTransactionPacket());
		playervalidator.player.sendMessage(Settings.protocolValidatorStartMessage);

		try {
			if (!playervalidator.waitConfirm(Settings.protocolValidatorMaxWait, TimeUnit.SECONDS)) {
				event.denyLogin(Settings.protocolValidatorFailMessage);
			} else {
				playervalidator.player.sendMessage(Settings.protocolValidatorSuccessMessage);
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
