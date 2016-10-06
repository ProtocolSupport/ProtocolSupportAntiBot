package protocolsupportantibot.utils;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.font.GlyphVector;
import java.awt.image.BufferedImage;
import java.util.concurrent.ThreadLocalRandom;

/*
 * Original implementation by Cage
 * https://akiraly.github.io/cage/
 *
 * Modifications:
 * - background is always white
 * - foreground is always black
 * - removed various postprocess effects, except for outline, it is always enabled
 * - always uses fast glyph painter (no antialiasing and such)
 */
public class CaptchaPainter {

	private final int width;
	private final int height;

	public CaptchaPainter(final int width, final int height) {
		this.width = width;
		this.height = height;
	}

	public BufferedImage draw(final Font font, final String text) {
		BufferedImage img = this.createImage();
		final Graphics2D g = img.createGraphics();
		try {
			final Graphics2D g2 = this.configureGraphics(g, font);
			this.draw(g2, text);
		} finally {
			g.dispose();
		}
		return img;
	}

	protected BufferedImage createImage() {
		return new BufferedImage(this.width, this.height, BufferedImage.TYPE_INT_ARGB_PRE);
	}

	protected Graphics2D configureGraphics(final Graphics2D g2, final Font font) {
		this.configureGraphicsQuality(g2);
		g2.setColor(Color.BLACK);
		g2.setBackground(Color.WHITE);
		g2.setFont(font);
		g2.clearRect(0, 0, this.width, this.height);
		return g2;
	}

	protected void configureGraphicsQuality(final Graphics2D g2) {
		g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
		g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		g2.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
		g2.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);
		g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		g2.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
		g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
	}

	protected void draw(final Graphics2D g, final String text) {
		final GlyphVector vector = g.getFont().createGlyphVector(g.getFontRenderContext(), text);
		final Rectangle bounds = vector.getPixelBounds(null, 0.0f, this.height);
		final float bw = (float) bounds.getWidth();
		final float bh = (float) bounds.getHeight();
		final float wr = (this.width / bw) * ((ThreadLocalRandom.current().nextFloat() / 20.0f) + 0.89f);
		final float hr = (this.height / bh) * ((ThreadLocalRandom.current().nextFloat() / 20.0f) + 0.68f);
		g.translate((this.width - (bw * wr)) / 2.0f, (this.height - (bh * hr)) / 2.0f);
		g.scale(wr, hr);
		final float bx = (float) bounds.getX();
		final float by = (float) bounds.getY();
		g.draw(vector.getOutline(
			((Math.signum(ThreadLocalRandom.current().nextFloat() - 0.5f) * 1.0f * this.width) / 200.0f) - bx,
			(((Math.signum(ThreadLocalRandom.current().nextFloat() - 0.5f) * 1.0f * this.height) / 70.0f) + this.height) - by
		));
		g.drawGlyphVector(vector, -bx, this.height - by);
	}

}
