
package ij3d;

import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ByteProcessor;

import java.awt.image.IndexColorModel;

import javax.vecmath.Color3f;

public class ColorTable {

	public static Color3f getColor(final String name) {
		for (int i = 0; i < colors.length; i++) {
			if (colorNames[i].equals(name)) {
				return colors[i];
			}
		}
		return null;
	}

	public static String[] colorNames = new String[] { "None", "Black", "White",
		"Red", "Green", "Blue", "Cyan", "Magenta", "Yellow" };

	public static Color3f[] colors = { null, new Color3f(0, 0, 0),
		new Color3f(1f, 1f, 1f), new Color3f(1f, 0, 0), new Color3f(0, 1f, 0),
		new Color3f(0, 0, 1f), new Color3f(0, 1f, 1f), new Color3f(1f, 0, 1f),
		new Color3f(1f, 1f, 0) };

	public static boolean isRedCh(final String color) {
		return color.equals("White") || color.equals("Red") ||
			color.equals("Magenta") || color.equals("Yellow");
	}

	public static boolean isGreenCh(final String color) {
		return color.equals("White") || color.equals("Green") ||
			color.equals("Cyan") || color.equals("Yellow");
	}

	public static boolean isBlueCh(final String color) {
		return color.equals("White") || color.equals("Blue") ||
			color.equals("Cyan") || color.equals("Magenta");
	}

	public static String getColorName(final Color3f col) {
		for (int i = 1; i < colors.length; i++) {
			if (colors[i].equals(col)) return colorNames[i];
		}
		return "None";
	}

	public static int getHistogramMax(final ImagePlus imp) {
		final int d = imp.getStackSize();
		final int[] hist = new int[256];
		for (int i = 0; i < d; i++) {
			final int[] h = imp.getStack().getProcessor(i + 1).getHistogram();
			for (int j = 0; j < hist.length; j++) {
				hist[j] += h[j];
			}
		}
		int max = -1, maxIndex = -1;
		for (int j = 0; j < hist.length; j++) {
			if (hist[j] > max) {
				max = hist[j];
				maxIndex = j;
			}
		}
		return maxIndex;
	}

	public static IndexColorModel getOpaqueIndexedColorModel(final ImagePlus imp,
		final boolean[] ch)
	{

		final IndexColorModel cmodel =
			(IndexColorModel) imp.getProcessor().getColorModel();
		final int N = cmodel.getMapSize();
		final byte[] r = new byte[N];
		final byte[] g = new byte[N];
		final byte[] b = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		for (int i = 0; i < N; i++) {
			r[i] = ch[0] ? r[i] : 0;
			g[i] = ch[1] ? g[i] : 0;
			b[i] = ch[2] ? b[i] : 0;
		}
		final IndexColorModel c = new IndexColorModel(8, N, r, g, b);
		return c;
	}

	public static IndexColorModel getIndexedColorModel(final ImagePlus imp,
		final boolean[] ch)
	{

		final IndexColorModel cmodel =
			(IndexColorModel) imp.getProcessor().getColorModel();
		final int N = cmodel.getMapSize();
		final byte[] r = new byte[N];
		final byte[] g = new byte[N];
		final byte[] b = new byte[N];
		final byte[] a = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		// index in cmodel which has most pixels:
		// this is asumed to be the background value
		final int histoMax = getHistogramMax(imp);
		final int[] sumInt = new int[N];
		int maxInt = 0;
		for (int i = 0; i < N; i++) {
			sumInt[i] = 0;
			if (ch[0]) sumInt[i] += (r[i] & 0xff);
			if (ch[1]) sumInt[i] += (g[i] & 0xff);
			if (ch[2]) sumInt[i] += (b[i] & 0xff);
			maxInt = sumInt[i] > maxInt ? sumInt[i] : maxInt;
		}

		final float scale = 255f / maxInt;
		for (int i = 0; i < N; i++) {
			final byte meanInt = (byte) (scale * sumInt[i]);
			r[i] = ch[0] ? r[i] : 0;
			g[i] = ch[1] ? g[i] : 0;
			b[i] = ch[2] ? b[i] : 0;
			a[i] = meanInt;
		}
		a[histoMax] = (byte) 0;
		final IndexColorModel c = new IndexColorModel(8, N, r, g, b, a);
		return c;
	}

	public static IndexColorModel getOpaqueAverageGrayColorModel(
		final ImagePlus imp, final boolean[] ch)
	{

		final IndexColorModel cmodel =
			(IndexColorModel) imp.getProcessor().getColorModel();
		final int N = cmodel.getMapSize();
		final byte[] r = new byte[N];
		final byte[] g = new byte[N];
		final byte[] b = new byte[N];
		final byte[] a = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		final int[] sumInt = new int[N];
		int maxInt = 0;
		for (int i = 0; i < N; i++) {
			sumInt[i] = 0;
			if (ch[0]) sumInt[i] += (r[i] & 0xff);
			if (ch[1]) sumInt[i] += (g[i] & 0xff);
			if (ch[2]) sumInt[i] += (b[i] & 0xff);
			maxInt = sumInt[i] > maxInt ? sumInt[i] : maxInt;
		}

		final float scale = 255f / maxInt;
		for (int i = 0; i < N; i++) {
			final byte meanInt = (byte) (scale * sumInt[i]);
			final float colFac = (float) sumInt[i] / (float) maxInt;
			r[i] = meanInt;
			g[i] = meanInt;
			b[i] = meanInt;
		}
		final IndexColorModel c = new IndexColorModel(8, N, r, g, b);
		return c;

	}

	public static IndexColorModel getAverageGrayColorModel(final ImagePlus imp,
		final boolean[] ch)
	{

		final IndexColorModel cmodel =
			(IndexColorModel) imp.getProcessor().getColorModel();
		final int N = cmodel.getMapSize();
		final byte[] r = new byte[N];
		final byte[] g = new byte[N];
		final byte[] b = new byte[N];
		final byte[] a = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		// index in cmodel which has most pixels:
		// this is asumed to be the background value
		final int histoMax = getHistogramMax(imp);
		final int[] sumInt = new int[N];
		int maxInt = 0;
		for (int i = 0; i < N; i++) {
			sumInt[i] = 0;
			if (ch[0]) sumInt[i] += (r[i] & 0xff);
			if (ch[1]) sumInt[i] += (g[i] & 0xff);
			if (ch[2]) sumInt[i] += (b[i] & 0xff);
			maxInt = sumInt[i] > maxInt ? sumInt[i] : maxInt;
		}

		final float scale = 255f / maxInt;
		for (int i = 0; i < N; i++) {
			final byte meanInt = (byte) (scale * sumInt[i]);
			final float colFac = (float) sumInt[i] / (float) maxInt;
			r[i] = meanInt;
			g[i] = meanInt;
			b[i] = meanInt;
			a[i] = meanInt;
		}
		a[histoMax] = (byte) 0;
		final IndexColorModel c = new IndexColorModel(8, N, r, g, b, a);
		return c;

	}

	public static ImagePlus
		adjustChannels(final ImagePlus imp, final boolean[] ch)
	{

		final int w = imp.getWidth(), h = imp.getHeight();
		final int d = imp.getStackSize();
		final int[] weight = new int[3];
		final IndexColorModel cmodel =
			(IndexColorModel) imp.getProcessor().getColorModel();
		final int N = cmodel.getMapSize();
		final byte[] r = new byte[N];
		final byte[] g = new byte[N];
		final byte[] b = new byte[N];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		float sum = 0;
		for (int i = 0; i < 3; i++) {
			if (ch[i]) {
				weight[i] = 1;
				sum++;
			}
		}

		final ImageStack res = new ImageStack(w, h);
		for (int z = 0; z < d; z++) {
			final byte[] bytes =
				(byte[]) imp.getStack().getProcessor(z + 1).getPixels();
			final byte[] newB = new byte[w * h];
			for (int i = 0; i < w * h; i++) {
				final int index = bytes[i] & 0xff;
				final int value =
					(weight[0] * (r[index] & 0xff) + weight[1] * (g[index] & 0xff) + weight[2] *
						(b[index] & 0xff));
				newB[i] = (byte) (value / sum);

			}
			res.addSlice("", new ByteProcessor(w, h, newB, null));
		}
		final ImagePlus newImage = new ImagePlus(imp.getTitle(), res);
		newImage.setCalibration(imp.getCalibration());
		return newImage;
	}

	public static void debug(final IndexColorModel cmodel) {
		final byte[] r = new byte[256];
		final byte[] g = new byte[256];
		final byte[] b = new byte[256];
		cmodel.getReds(r);
		cmodel.getGreens(g);
		cmodel.getBlues(b);
		for (int i = 0; i < 256; i++) {
			System.out.println((r[i] & 0xff) + "\t" + (g[i] & 0xff) + "\t" +
				(b[i] & 0xff));
		}
	}
}
