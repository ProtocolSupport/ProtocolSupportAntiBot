package protocolsupportantibot.protocolvalidator;

import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import protocolsupportantibot.utils.AbortableCountDownLatch;

final class ValidatorInfo {

	final Player player;
	public ValidatorInfo(Player player) {
		this.player = player;
	}

	private final AbortableCountDownLatch confirmations = new AbortableCountDownLatch(3);

	private boolean settings = false;
	private boolean transaction = false;
	private boolean teleport = false;

	public void confirmSettings() {
		if (!settings) {
			settings = true;
			confirmations.countDown();
		}
	}

	public void confirmTransaction() {
		if (!transaction) {
			transaction = true;
			confirmations.countDown();
		}
	}

	public void confirmTeleport() {
		if (!teleport) {
			teleport = true;
			confirmations.countDown();
		}
	}

	public boolean waitConfirm(long timeout, TimeUnit unit) throws InterruptedException {
		return confirmations.await(timeout, unit);
	}

	public void interrupt() {
		confirmations.interrupt();
	}

}