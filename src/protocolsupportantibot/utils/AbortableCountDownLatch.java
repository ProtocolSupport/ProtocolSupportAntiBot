package protocolsupportantibot.utils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AbortableCountDownLatch extends CountDownLatch {

	public AbortableCountDownLatch(int count) {
		super(count);
	}

	private volatile boolean aborted = false;

	@Override
	public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
		boolean result = super.await(timeout, unit);
		if (aborted) {
			throw new AbortedException();
		}
		return result;
	}

	public void interrupt() {
		aborted = true;
		for (int i = 0; i < getCount(); i++) {
			countDown();
		}
	}

	public static class AbortedException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}

}
