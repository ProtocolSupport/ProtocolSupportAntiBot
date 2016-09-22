package protocolsupportantibot.bans;

import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ExecutionException;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import protocolsupport.api.events.PlayerLoginFinishEvent;
import protocolsupportantibot.Settings;

/* Kicks banned addresses
 *
 * this listener should handle login finish first
 */
public class BanChecker implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onFinishLogin(PlayerLoginFinishEvent event) throws InterruptedException, ExecutionException, InvocationTargetException {
		if (event.isLoginDenied()) {
			return;
		}

		if (BanDataSource.getInstance().isBanned(event.getAddress().getAddress())) {
			event.denyLogin(Settings.tempBanMessage);
		}
	}

}
