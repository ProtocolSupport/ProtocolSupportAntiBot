package protocolsupportantibot.protocolvalidator;

import java.util.concurrent.TimeUnit;

import protocolsupportantibot.utils.AbortableCountDownLatch;

final class ValidatorInfo {

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