package protocolsupportantibot.logininterval;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketAdapter.AdapterParameteters;

import protocolsupport.api.events.PlayerDisconnectEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.ProtocolSupportAntiBot;
import protocolsupportantibot.Settings;

/*
 * Adds a small delay between players login, so even if bots will pass through, server won't be overloaded instantly
 */
public class LoginInterval implements Listener {

	protected final Map<InetSocketAddress, Player> players = new ConcurrentHashMap<>();

	protected final Map<InetSocketAddress, CountDownLatch> queue = new LinkedHashMap<>();
	protected final Object lock = new Object();

	public LoginInterval() {
		new Thread() {
			@Override
			public void run() {
				try {
					while (ProtocolSupportAntiBot.getInstance().isEnabled()) {
						synchronized (lock) {
							Iterator<CountDownLatch> iterator = queue.values().iterator();
							if (iterator.hasNext()) {
								iterator.next().countDown();
								iterator.remove();
							}
						}
						Thread.sleep(Settings.loginInterval);
					}
				} catch (InterruptedException e) {
				}
			}
		}.start();

		ProtocolLibrary.getProtocolManager().addPacketListener(new PacketAdapter(
			new AdapterParameteters().plugin(ProtocolSupportAntiBot.getInstance()).types(PacketType.Login.Server.SUCCESS)
		) {
			@Override
			public void onPacketSending(PacketEvent event) {
				players.put(event.getPlayer().getAddress(), event.getPlayer());
			}
		});
	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerLoginEvent(PlayerLoginFinishEvent event) {
		if (event.isLoginDenied()) {
			return;
		}

		Player player = players.get(event.getAddress());
		if (player == null) {
			return;
		}

		player.sendMessage(Settings.loginIntervalMessage.replace("{PLAYERS}", String.valueOf(queue.size())));

		CountDownLatch countdown = new CountDownLatch(1);
		synchronized (lock) {
			queue.put(event.getAddress(), countdown);	
		}
		try {
			countdown.await();
		} catch (InterruptedException e) {
		}
	}

	@EventHandler
	public void onPlayerDisconnect(PlayerDisconnectEvent event) {
		players.remove(event.getAddress());
		synchronized (lock) {
			CountDownLatch countdown = queue.remove(event.getAddress());
			if (countdown != null) {
				countdown.countDown();
			}
		}
	}

}
