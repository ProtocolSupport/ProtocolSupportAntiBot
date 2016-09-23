package protocolsupportantibot.bans;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import protocolsupport.api.events.PlayerLoginStartEvent;
import protocolsupportantibot.Settings;

/*
 * Kicks banned addresses
 */
public class BanChecker implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onFinishLogin(PlayerLoginStartEvent event) throws InterruptedException, ExecutionException, InvocationTargetException {
		if (event.isLoginDenied()) {
			return;
		}

		if (BanDataSource.getInstance().isBanned(event.getAddress().getAddress())) {
			event.denyLogin(Settings.tempBanMessage);
		}
	}

}
