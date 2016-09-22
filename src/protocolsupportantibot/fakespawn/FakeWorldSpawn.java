package protocolsupportantibot.fakespawn;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketAdapter.AdapterParameteters;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.EnumWrappers.Difficulty;
import com.comphenix.protocol.wrappers.EnumWrappers.NativeGameMode;

import net.minecraft.server.v1_10_R1.EntityPlayer;
import protocolsupport.api.events.PlayerDisconnectEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.ProtocolSupportAntiBot;
import protocolsupportantibot.protocolvalidator.EntityIdPool;
import protocolsupportantibot.utils.Packets;

/*
 * Spawns player in fake client side only world
 */
public class FakeWorldSpawn implements Listener {

	protected final Map<InetAddress, Integer> playerRealId = Collections.synchronizedMap(new IdentityHashMap<>());
	protected final Map<InetSocketAddress, PacketContainer> playerSettings = new ConcurrentHashMap<>();
	protected final Map<InetSocketAddress, Player> players = new ConcurrentHashMap<>();

	protected final EntityIdPool idPool = new EntityIdPool();

	public FakeWorldSpawn() {
		idPool.claimPool();

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
			new AdapterParameteters().plugin(ProtocolSupportAntiBot.getInstance()).types(PacketType.Login.Server.SUCCESS)
		) {
			@Override
			public void onPacketSending(PacketEvent event) {
				players.put(event.getPlayer().getAddress(), event.getPlayer());
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
			new AdapterParameteters().plugin(ProtocolSupportAntiBot.getInstance()).types(PacketType.Play.Client.SETTINGS)
		) {
			@Override
			public void onPacketReceiving(PacketEvent event) {
				playerSettings.put(event.getPlayer().getAddress(), event.getPacket());
			}
		});

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
				new AdapterParameteters().plugin(ProtocolSupportAntiBot.getInstance()).types(PacketType.Play.Server.LOGIN)
			) {
				@Override
				public void onPacketSending(PacketEvent event) {
					if (players.containsKey(event.getPlayer().getAddress())) {
						return;
					}

					event.setCancelled(true);

					int realDimId = event.getPacket().getIntegers().read(1);
					Difficulty realDiff = event.getPacket().getDifficulties().read(0);
					WorldType realWType = event.getPacket().getWorldTypeModifier().read(0);
					NativeGameMode realGameMode = event.getPacket().getGameModes().read(0);

					try {
						ProtocolLibrary.getProtocolManager().sendServerPacket(
							event.getPlayer(), Packets.createRespawnPacket(realDimId == 0 ? -1 : 0, realDiff, realWType, realGameMode)
						);
						ProtocolLibrary.getProtocolManager().sendServerPacket(
							event.getPlayer(), Packets.createRespawnPacket(realDimId, realDiff, realWType, realGameMode)
						);
					} catch (InvocationTargetException e) {
						e.printStackTrace();
					}
				}
			}
		);
	}

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

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerJoin(PlayerJoinEvent event) throws IllegalAccessException, InvocationTargetException {
		PacketContainer container = playerSettings.remove(event.getPlayer().getAddress());
		if (container != null) {
			ProtocolLibrary.getProtocolManager().recieveClientPacket(event.getPlayer(), container);
		}
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(PlayerQuitEvent event) {
		idPool.returnId(event.getPlayer().getEntityId());
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onFinishLogin(PlayerLoginFinishEvent event) throws InterruptedException, ExecutionException, InvocationTargetException {
		if (event.isLoginDenied()) {
			return;
		}

		Player player = players.get(event.getAddress());
		if (player == null) {
			return;
		}

		int playerId = idPool.getId();
		playerRealId.put(event.getAddress().getAddress(), playerId);

		ProtocolLibrary.getProtocolManager().sendServerPacket(player, Packets.createJoinGamePacket(playerId, 60));
		ProtocolLibrary.getProtocolManager().sendServerPacket(player, LobbySchematic.chunkdata != null ? LobbySchematic.chunkdata : Packets.createEmptyChunkPacket());
		ProtocolLibrary.getProtocolManager().sendServerPacket(player, Packets.createTeleportPacket(8, LobbySchematic.chunkdata != null ? 6 : 1000, 8, 0, 33));

		players.remove(event.getAddress());
	}

	@EventHandler
	public void onDisconnect(PlayerDisconnectEvent event) {
		players.remove(event.getAddress());
		playerSettings.remove(event.getAddress());
		Integer realId = playerRealId.remove(event.getAddress().getAddress());
		if (realId != null) {
			idPool.returnId(realId);
		}
	}

}
