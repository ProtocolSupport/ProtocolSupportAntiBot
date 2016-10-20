package protocolsupportantibot.fakespawn;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.bukkit.WorldType;
import org.bukkit.craftbukkit.v1_10_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerQuitEvent;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.EnumWrappers.Difficulty;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;

import net.minecraft.server.v1_10_R1.EntityPlayer;
import protocolsupport.api.Connection;
import protocolsupport.api.Connection.PacketReceiveListener;
import protocolsupport.api.Connection.PacketSendListener;
import protocolsupport.api.ProtocolSupportAPI;
import protocolsupport.api.events.ConnectionCloseEvent;
import protocolsupport.api.events.ConnectionOpenEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.protocolvalidator.EntityIdPool;
import protocolsupportantibot.utils.Packets;

/*
 * Spawns player in fake client side only world
 */
public class FakeWorldSpawn implements Listener {

	protected final Map<InetAddress, Integer> playerRealId = Collections.synchronizedMap(new IdentityHashMap<>());

	protected final EntityIdPool idPool = new EntityIdPool();

	public FakeWorldSpawn() {
		idPool.claimPool();
	}

	private static final String settings_packet_key = "psab_settings_packet_key";

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

	//replace entity id with real one
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLoginEvent(PlayerLoginEvent event) {
		Integer realId = playerRealId.remove(event.getAddress());
		if (realId == null) {
			event.disallow(Result.KICK_OTHER, "Internal error: failed to get real id");
			return;
		}
		Player player = event.getPlayer();
		EntityPlayer entityplayer = ((CraftPlayer) player).getHandle();
		entityplayer.h(realId);
	}

	//receive cached client settings on real join
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) throws IllegalAccessException, InvocationTargetException {
		Connection connection = ProtocolSupportAPI.getConnection(event.getPlayer());
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
		playerRealId.put(event.getAddress().getAddress(), playerId);

		Connection connection = ProtocolSupportAPI.getConnection(event.getAddress());
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
		Integer realId = playerRealId.remove(event.getConnection().getAddress().getAddress());
		if (realId != null) {
			idPool.returnId(realId);
		}
	}

}
