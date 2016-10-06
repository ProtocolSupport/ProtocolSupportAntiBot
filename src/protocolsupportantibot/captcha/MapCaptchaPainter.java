package protocolsupportantibot.captcha;

import java.awt.Font;
import java.awt.image.BufferedImage;

import protocolsupportantibot.Settings;
import protocolsupportantibot.utils.CaptchaPainter;

public class MapCaptchaPainter {

	private final static CaptchaPainter painter = new CaptchaPainter(128, 128);
	private final static Font font = new Font("Monospaced", 1, 64);

	public static byte[] create(String val) {
		return toMinecraftMapData(generateCaptchaImage(val));
	}

	private static BufferedImage generateCaptchaImage(String val) {
		return painter.draw(font, val);
	}

	private static byte[] toMinecraftMapData(BufferedImage image) {
		byte[] data = new byte[128 * 128];
		for (int x = 0; x < 128; x++) {
			for (int y = 0; y < 128; y++) {
				data[y * 128 + x] = (byte) toMinecraftColorId(image.getRGB(x, y));
			}
		}
		return data;
	}

	private static final int toMinecraftColorId(int rgb) {
		return rgb == -1 ? 0 : Settings.captchaColorId;
	}

}
