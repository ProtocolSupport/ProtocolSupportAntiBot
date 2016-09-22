package protocolsupportantibot.captcha;

import java.awt.Color;

public class ColorCompare {

	//http://www.brucelindbloom.com
	private static int[] rgb2lab(int R, int G, int B) {
		float r, g, b, X, Y, Z, fx, fy, fz, xr, yr, zr;
		float Ls, as, bs;
		float eps = 216.f / 24389.f;
		float k = 24389.f / 27.f;

		float Xr = 0.964221f;
		float Yr = 1.0f;
		float Zr = 0.825211f;

		r = R / 255.f;
		g = G / 255.f;
		b = B / 255.f;

		if (r <= 0.04045) {
			r = r / 12;
		} else {
			r = (float) Math.pow((r + 0.055) / 1.055, 2.4);
		}

		if (g <= 0.04045) {
			g = g / 12;
		} else {
			g = (float) Math.pow((g + 0.055) / 1.055, 2.4);
		}

		if (b <= 0.04045) {
			b = b / 12;
		} else {
			b = (float) Math.pow((b + 0.055) / 1.055, 2.4);
		}

		X = 0.436052025f * r + 0.385081593f * g + 0.143087414f * b;
		Y = 0.222491598f * r + 0.71688606f * g + 0.060621486f * b;
		Z = 0.013929122f * r + 0.097097002f * g + 0.71418547f * b;

		xr = X / Xr;
		yr = Y / Yr;
		zr = Z / Zr;

		if (xr > eps) {
			fx = (float) Math.pow(xr, 1 / 3.);
		} else {
			fx = (float) ((k * xr + 16.) / 116.);
		}

		if (yr > eps) {
			fy = (float) Math.pow(yr, 1 / 3.);
		} else {
			fy = (float) ((k * yr + 16.) / 116.);
		}

		if (zr > eps) {
			fz = (float) Math.pow(zr, 1 / 3.);
		} else {
			fz = (float) ((k * zr + 16.) / 116);
		}

		Ls = (116 * fy) - 16;
		as = 500 * (fx - fy);
		bs = 200 * (fy - fz);

		int[] lab = new int[3];
		lab[0] = (int) (2.55 * Ls + .5);
		lab[1] = (int) (as + .5);
		lab[2] = (int) (bs + .5);
		return lab;
	}

	public static double getDifference(int rgb1, int rgb2) {
		Color color1 = new Color(rgb1);
		Color color2 = new Color(rgb2);
		int[] lab1 = rgb2lab(color1.getRed(), color1.getGreen(), color1.getBlue());
		int[] lab2 = rgb2lab(color2.getRed(), color2.getGreen(), color2.getBlue());
		return Math.sqrt(Math.pow(lab2[0] - lab1[0], 2) + Math.pow(lab2[1] - lab1[1], 2) + Math.pow(lab2[2] - lab1[2], 2));
	}

}
