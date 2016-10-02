package protocolsupportantibot.protocolvalidator;

import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import protocolsupportantibot.utils.AbortableCountDownLatch;

final class ValidatorInfo {

	final Player player;
	public ValidatorInfo(Player player) {
		this.player = player;
	}

	private final AbortableCountDownLatch confirmations = new AbortableCountDownLatch(2);

	private boolean settings = false;
	private boolean transaction = false;

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

	public boolean waitConfirm(long timeout, TimeUnit unit) throws InterruptedException {
		return confirmations.await(timeout, unit);
	}

	public void interrupt() {
		confirmations.interrupt();
	}

}