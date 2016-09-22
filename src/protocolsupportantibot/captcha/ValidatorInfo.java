package protocolsupportantibot.captcha;

import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.bukkit.entity.Player;

import protocolsupportantibot.utils.AbortableCountDownLatch;

final class ValidatorInfo {

	final Player player;
	public ValidatorInfo(Player player) {
		this.player = player;
	}

	private String captcha;
	private int tries;

	public String generateCaptcha() {
		captcha = String.valueOf(100 + ThreadLocalRandom.current().nextInt(900));
		return captcha;
	}

	private final AbortableCountDownLatch countdown = new AbortableCountDownLatch(1);
	private boolean success = false;

	public boolean checkCaptcha(String s) {
		tries++;
		return s.equalsIgnoreCase(captcha);
	}

	public void succces() {
		success = true;
		countdown.countDown();
	}

	public void fail() {
		countdown.countDown();
	}

	public boolean isSuccess() {
		return success;
	}

	public int getTries() {
		return tries;
	}

	public boolean waitConfirm(long timeout, TimeUnit unit) throws InterruptedException {
		return countdown.await(timeout, unit);
	}

	public void interrupt() {
		countdown.interrupt();
	}

}