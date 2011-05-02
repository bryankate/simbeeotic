/**
 * @(#)ColorInt.java
 *
 * A class that performs a variety of useful operations on colors.
 *
 * @author Tianen Li and Andrew Zhou
 * @version 1.00 2011/4/30
 */

package harvard.robobees.simbeeotic.model.sensor.camera;
public class ColorInt {

	// gets the red component of an rgb value
	public static int getRed(int rgb) {
        return (rgb >> 16) & 0xFF;
    }

	// gets the green component of an rgb value
    public static int getGreen(int rgb) {
        return (rgb >> 8) & 0xFF;
    }

	// gets the blue component of an rgb value
    public static int getBlue(int rgb) {
        return rgb & 0xFF;
    }

	// gets the alpha component of an rgb value
    public static int getAlpha(int rgb) {
        return (rgb >> 24) & 0xFF;
    }

	// given the red, green, blue, and alpha components of an rgb value
	// (each ranging from 0 to 255), combines them into the rgb value
    public static int convert(int red, int green, int blue, int alpha) {
        return (alpha << 24) | (red << 16) | (green << 8) | blue;
    }

    // Given an rgb value color, takes the norm of the color's red, green, and blue components.
    // Used to find the gradients in the image because taking the rgb values as is would overweight
    // the red component
	public static int normalize (int color) {
		int red = getRed(color);
		int green = getGreen(color);
		int blue = getBlue(color);
		return (int)Math.round(Math.sqrt(red*red + green*green + blue*blue));
	}

	// normalizes every single value in an array of rgb colors
	public static int[][] normalizeArr (int colors[][]) {
		int[][] normalized = new int[colors.length][colors[0].length];
		for (int i = 0; i < colors.length; i++) {
			for (int j = 0; j < colors[i].length; j++) {
				normalized[i][j] = normalize(colors[i][j]);
			}
		}
		return normalized;
	}

}