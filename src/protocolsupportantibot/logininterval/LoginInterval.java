package protocolsupportantibot.logininterval;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import protocolsupport.api.events.PlayerDisconnectEvent;
import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.ProtocolSupportAntiBot;
import protocolsupportantibot.Settings;
import protocolsupportantibot.utils.Packets;

/*
 * Adds a small delay between players login, so even if bots will pass through, server won't be overloaded instantly
 */
public class LoginInterval implements Listener {

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
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerLoginEvent(PlayerLoginFinishEvent event) {
		if (event.isLoginDenied()) {
			return;
		}

		event.getConnection().sendPacket(Packets.createChatPacket(Settings.loginIntervalMessage.replace("{PLAYERS}", String.valueOf(queue.size()))));

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
		synchronized (lock) {
			CountDownLatch countdown = queue.remove(event.getAddress());
			if (countdown != null) {
				countdown.countDown();
			}
		}
	}

}
