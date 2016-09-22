package protocolsupportantibot.captcha;

import org.bukkit.Color;

public enum MinecraftMapColor {

	COLOR1(127, 178, 56),
	COLOR2(247, 233, 163),
	COLOR3(199, 199, 199),
	COLOR4(255, 0, 0),
	COLOR5(160, 160, 255),
	COLOR6(167, 167, 167),
	COLOR7(0, 124, 0),
	COLOR8(255, 255, 255),
	COLOR9(164, 168, 184),
	COLOR10(151, 109, 77),
	COLOR11(112, 112, 112),
	COLOR12(64, 64, 255),
	COLOR13(143, 119, 72),
	COLOR14(255, 252, 245),
	COLOR15(216, 127, 51),
	COLOR16(178, 76, 216),
	COLOR17(102, 153, 216),
	COLOR18(229, 229, 51),
	COLOR19(127, 204, 25),
	COLOR20(242, 127, 165),
	COLOR21(76, 76, 76),
	COLOR22(153, 153, 153),
	COLOR23(76, 127, 153),
	COLOR24(127, 63, 178),
	COLOR25(51, 76, 178),
	COLOR26(102, 76, 51),
	COLOR27(102, 127, 51),
	COLOR28(153, 51, 51),
	COLOR29(25, 25, 25),
	COLOR30(250, 238, 77),
	COLOR31(92, 219, 213),
	COLOR32(74, 128, 255),
	COLOR33(0, 217, 58),
	COLOR34(129, 86, 49),
	COLOR35(112, 2, 0);

	private final int rgb;

	MinecraftMapColor(int red, int green, int blue) {
		this.rgb = Color.fromBGR(red, green, blue).asRGB();
	}

	public byte getMapDataId() {
		return (byte) (ordinal() + 1);
	}

	public static MinecraftMapColor byRgb(int rgb) {
		double minDiff = Integer.MAX_VALUE;
		MinecraftMapColor closestColor = null;
		for (MinecraftMapColor mapcolor : MinecraftMapColor.values()) {
			double diff = ColorCompare.getDifference(rgb, mapcolor.rgb);
			if (diff < minDiff) {
				minDiff = diff;
				closestColor = mapcolor;
			}
		}
		return closestColor;
	}

}
