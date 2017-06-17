package protocolsupportantibot.fakespawn;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.Difficulty;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;

import net.minecraft.server.v1_12_R1.EntityPlayer;
import protocolsupport.api.Connection;
import protocolsupport.api.Connection.PacketReceiveListener;
import protocolsupport.api.Connection.PacketSendListener;
import protocolsupport.api.events.ConnectionCloseEvent;
import protocolsupport.api.events.ConnectionOpenEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupport.api.events.PlayerSyncLoginEvent;
import protocolsupportantibot.protocolvalidator.EntityIdPool;
import protocolsupportantibot.utils.Packets;

/*
 * Spawns player in fake client side only world
 */
public class FakeWorldSpawn implements Listener {

	protected final EntityIdPool idPool = new EntityIdPool();

	public FakeWorldSpawn() {
		idPool.claimPool();
	}

	private static final String settings_packet_key = "psab_settings_packet_key";
	private static final String real_id_key = "pasb_real_id_key";

	@EventHandler(priority = EventPriority.LOWEST)
	public void onConnectionOpen(ConnectionOpenEvent event) {
		final Connection connection = event.getConnection();
		connection.addPacketReceiveListener(new PacketReceiveListener() {
			@Override
			public boolean onPacketReceiving(Object packetObj) {
				PacketContainer container = PacketContainer.fromPacket(packetObj);
				if (container.getType() == PacketType.Play.Client.SETTINGS) {
					connection.addMetadata(settings_packet_key, packetObj);
					connection.removePacketReceiveListener(this);
				};
				return true;
			}
		});
		connection.addPacketSendListener(new PacketSendListener() {
			@Override
			public boolean onPacketSending(Object packetObj) {
				PacketContainer container = PacketContainer.fromPacket(packetObj);
				if (container.getType() == PacketType.Play.Server.LOGIN) {
					int realDimId = container.getIntegers().read(1);
					Difficulty realDiff = container.getDifficulties().read(0);
					WorldType realWType = container.getWorldTypeModifier().read(0);
					NativeGameMode realGameMode = container.getGameModes().read(0);
					connection.sendPacket(Packets.createRespawnPacket(realDimId == 0 ? -1 : 0, realDiff, realWType, realGameMode));
					connection.sendPacket(Packets.createRespawnPacket(realDimId, realDiff, realWType, realGameMode));
					connection.removePacketSendListener(this);
					return false;
				}
				return true;
			}
		});
	}

	//replace entity id with real one and receive real client settings
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLoginEvent(PlayerSyncLoginEvent event) {
		Connection connection = event.getConnection();

		Integer realId = (Integer) event.getConnection().removeMetadata(real_id_key);
		if (realId == null) {
			event.denyLogin("Internal error: failed to get real id");
			return;
		}
		Player player = event.getPlayer();
		EntityPlayer entityplayer = ((CraftPlayer) player).getHandle();
		entityplayer.h(realId);

		Object packet = connection.removeMetadata(settings_packet_key);
		if (packet != null) {
			connection.receivePacket(packet);
		}
	}

	//spawn player in a fake world
	@EventHandler(priority = EventPriority.LOW)
	public void onFinishLogin(PlayerLoginFinishEvent event) throws InterruptedException, ExecutionException, InvocationTargetException {
		if (event.isLoginDenied()) {
			return;
		}

		int playerId = idPool.getId();

		Connection connection = event.getConnection();

		connection.addMetadata(real_id_key, playerId);

		connection.sendPacket(Packets.createJoinGamePacket(playerId, 60));
		connection.sendPacket(LobbySchematic.chunkdata != null ? LobbySchematic.chunkdata.getHandle() : Packets.createEmptyChunkPacket());
		connection.sendPacket(Packets.createTeleportPacket(8, LobbySchematic.chunkdata != null ? 6 : 1000, 8, 0, 33));
	}


	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		idPool.returnId(event.getPlayer().getEntityId());
	}

	@EventHandler
	public void onDisconnect(ConnectionCloseEvent event) {
		Integer realId = (Integer) event.getConnection().removeMetadata(real_id_key);
		if (realId != null) {
			idPool.returnId(realId);
		}
	}

}
