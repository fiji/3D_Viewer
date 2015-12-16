
package voltex;

import ij.IJ;
import ij.ImagePlus;
import ij3d.Volume;

import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import org.scijava.java3d.ImageComponent;
import org.scijava.java3d.ImageComponent2D;
import org.scijava.vecmath.Point3d;

/**
 * This class encapsulates an image stack and provides various methods for
 * retrieving data. It is possible to control the loaded color channels of RGB
 * images, and to specify whether or not to average several channels (and merge
 * them in this way into one byte per pixel). Depending on these settings, and
 * on the type of image given at construction time, the returned data type is
 * one of INT_DATA or BYTE_DATA.
 *
 * @author Benjamin Schmid
 */
public class VoltexVolume extends Volume {

	/** The textures' size. These are powers of two. */
	int xTexSize, yTexSize, zTexSize;

	/** The texGenScale */
	float xTexGenScale, yTexGenScale, zTexGenScale;

	/** The mid point in the data */
	final Point3d volRefPt = new Point3d();

	/** The ColorModel used for 8-bit textures */
	protected static final ColorModel greyCM = createGreyColorModel();

	/** The ColorModel used for RGB textures */
	protected static final ColorModel rgbCM = createRGBColorModel();

	protected ComponentCreator compCreator;

	private final ImageUpdater updater = new ImageUpdater();

	private byte[][] xy;
	private byte[][] xz;
	private byte[][] yz;

	private ImageComponent2D[] xyComp;
	private ImageComponent2D[] xzComp;
	private ImageComponent2D[] yzComp;

	/**
	 * Initializes this Volume with the specified image. All channels are used.
	 * 
	 * @param imp
	 */
	public VoltexVolume(final ImagePlus imp) {
		this(imp, new boolean[] { true, true, true });
	}

	/**
	 * Initializes this Volume with the specified image and channels.
	 * 
	 * @param imp
	 * @param ch A boolean[] array of length three, which indicates whether the
	 *          red, blue and green channel should be read. This has only an
	 *          effect when reading color images.
	 */
	public VoltexVolume(final ImagePlus imp, final boolean[] ch) {
		setImage(imp, ch);
	}

	@Override
	public void setImage(final ImagePlus imp, final boolean[] ch) {
		super.setImage(imp, ch);
		// tex size is next power of two greater than max - min
		// regarding pixels
		xTexSize = powerOfTwo(xDim);
		yTexSize = powerOfTwo(yDim);
		zTexSize = powerOfTwo(zDim);

		final float xSpace = (float) pw;
		final float ySpace = (float) ph;
		final float zSpace = (float) pd;

		// xTexSize is the pixel dim of the file in x-dir, e.g. 256
		// xSpace is the normalised length of a pixel
		xTexGenScale = (float) (1.0 / (xSpace * xTexSize));
		yTexGenScale = (float) (1.0 / (ySpace * yTexSize));
		zTexGenScale = (float) (1.0 / (zSpace * zTexSize));

		// the min and max coords are for the usable area of the texture,
		volRefPt.x = (maxCoord.x + minCoord.x) / 2;
		volRefPt.y = (maxCoord.y + minCoord.y) / 2;
		volRefPt.z = (maxCoord.z + minCoord.z) / 2;

		initDataType();
		initVoltexLoader();
		createImageComponents();
		updateData();
	}

	@Override
	public void clear() {
		super.clear();
		xy = null;
		xz = null;
		yz = null;
		xyComp = null;
		xzComp = null;
		yzComp = null;
	}

	@Override
	public void swap(final String path) {
		super.swap(path);
		xy = null;
		xz = null;
		yz = null;
		xyComp = null;
		xzComp = null;
		yzComp = null;
	}

	@Override
	public void restore(final String path) {
		final ImagePlus imp = IJ.openImage(path + ".tif");
		try {
			setImage(IJ.openImage(path + ".tif"), channels);
		}
		catch (final NullPointerException e) {
			throw new IllegalArgumentException("Cannot load image from " + path);
		}
		catch (final RuntimeException e) {
			System.out.println("Cannot load " + path);
			throw e;
		}
	}

	private void createImageComponents() {
		for (int z = 0; z < zDim; z++)
			xyComp[z] = compCreator.createImageComponent(xy[z], xTexSize, yTexSize);
		for (int y = 0; y < yDim; y++)
			xzComp[y] = compCreator.createImageComponent(xz[y], xTexSize, zTexSize);
		for (int x = 0; x < xDim; x++)
			yzComp[x] = compCreator.createImageComponent(yz[x], yTexSize, zTexSize);
	}

	public void updateData() {
		for (int z = 0; z < zDim; z++) {
			loadZ(z, xy[z]);
			xyComp[z].updateData(updater, 0, 0, xTexSize, yTexSize);
		}
		for (int y = 0; y < yDim; y++) {
			loadY(y, xz[y]);
			xzComp[y].updateData(updater, 0, 0, xTexSize, zTexSize);
		}
		for (int x = 0; x < xDim; x++) {
			loadX(x, yz[x]);
			yzComp[x].updateData(updater, 0, 0, yTexSize, zTexSize);
		}
	}

	public ImageComponent2D getImageComponentZ(final int index) {
		return xyComp[index];
	}

	public ImageComponent2D getImageComponentY(final int index) {
		return xzComp[index];
	}

	public ImageComponent2D getImageComponentX(final int index) {
		return yzComp[index];
	}

	public void setNoCheckNoUpdate(final int x, final int y, final int z,
		final int v)
	{
		voltexLoader.setNoCheckNoUpdate(x, y, z, v);
	}

	@Override
	public boolean setSaturatedVolumeRendering(final boolean b) {
		if (super.setSaturatedVolumeRendering(b) && dataType == INT_DATA) {
			((VoltexIntLoader) voltexLoader).setLoader((IntLoader) loader);
			updateData();
			return true;
		}
		return false;
	}

	@Override
	public boolean setAverage(final boolean average) {
		if (super.setAverage(average)) {
			initVoltexLoader();
			createImageComponents();
			updateData();
			return true;
		}
		return false;
	}

	/**
	 * Sets the channels which are to be used in this volume rendering. Returns
	 * true if the channel settings has changed.
	 */
	@Override
	public boolean setChannels(final boolean[] ch) {
		if (super.setChannels(ch)) {
			initVoltexLoader();
			createImageComponents();
			updateData();
			return true;
		}
		return false;
	}

	/**
	 * Set the lookup tables for this volume rendering. Returns true if the data
	 * type of the textures has changed.
	 */
	@Override
	public boolean setLUTs(final int[] r, final int[] g, final int[] b,
		final int[] a)
	{
		final boolean ret = super.setLUTs(r, g, b, a);
		if (ret) {
			initVoltexLoader();
			createImageComponents();
		}
		updateData();
		return ret;
	}

	/**
	 * Set the alpha channel to fully opaque. Returns true if the data type of the
	 * textures have changed.
	 */
	@Override
	public boolean setAlphaLUTFullyOpaque() {
		final boolean ret = super.setAlphaLUTFullyOpaque();
		if (ret) {
			initVoltexLoader();
			createImageComponents();
		}
		updateData();
		return ret;
	}

	private VoltexLoader voltexLoader;

	/**
	 * Init the loader, based on the currently set data type, which is either
	 * INT_DATA or BYTE_DATA.
	 */
	protected void initVoltexLoader() {
		switch (dataType) {
			case BYTE_DATA:
				voltexLoader = new VoltexByteLoader((ByteLoader) loader);
				compCreator = new GreyComponentCreator();
				break;
			case INT_DATA:
				voltexLoader = new VoltexIntLoader((IntLoader) loader);
				compCreator = new ColorComponentCreator();
				break;
		}
	}

	/**
	 * Calculate the next power of two to the given value.
	 * 
	 * @param value
	 * @return
	 */
	protected static int powerOfTwo(final int value) {
		int retval = 1;
		while (retval < value) {
			retval *= 2;
		}
		return retval;
	}

	/**
	 * Loads a xy-slice at the given z position.
	 * 
	 * @param z
	 * @param dst must be an byte[] array of the correct length. (If the data type
	 *          is INT_DATA, it must be 4 times as long).
	 */
	private void loadZ(final int z, final byte[] dst) {
		voltexLoader.loadZ(z, dst);
	}

	/**
	 * Loads a xz-slice at the given y position.
	 * 
	 * @param y
	 * @param dst must be an byte[] array of the correct length. (If the data type
	 *          is INT_DATA, it must be 4 times as long).
	 */
	private void loadY(final int y, final byte[] dst) {
		voltexLoader.loadY(y, dst);
	}

	/**
	 * Loads a yz-slice at the given x position.
	 * 
	 * @param x
	 * @param dst must be an byte[] array of the correct length. (If the data type
	 *          is INT_DATA, it must be 4 times as long).
	 */
	private void loadX(final int x, final byte[] dst) {
		voltexLoader.loadX(x, dst);
	}

	private static final ColorModel createGreyColorModel() {
		final byte[] r = new byte[256], g = new byte[256], b = new byte[256];
		for (int i = 0; i < 256; i++)
			r[i] = (byte) i;
		return new IndexColorModel(8, 256, r, g, b);
	}

	private static final ColorModel createRGBColorModel() {
		final ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
		final int[] nBits = { 8, 8, 8, 8 };
		return new ComponentColorModel(cs, nBits, true, false,
			Transparency.TRANSLUCENT, DataBuffer.TYPE_BYTE);
	}

	/* **********************************************************************
	 * The ImageUpdater which is needed for dynamically updating the textures
	 ***********************************************************************/

	private class ImageUpdater implements ImageComponent2D.Updater {

		@Override
		public void updateData(final ImageComponent2D comp, final int x,
			final int y, final int w, final int h)
		{}
	}

	/* **********************************************************************
	 * The ComponentCreator interface and implementing classes
	 ***********************************************************************/

	/**
	 * Abstract class which defines the interface for creating the different
	 * ImageComponents.
	 */
	private abstract class ComponentCreator {

		ComponentCreator() {
			xyComp = new ImageComponent2D[zDim];
			xzComp = new ImageComponent2D[yDim];
			yzComp = new ImageComponent2D[xDim];
		}

		/**
		 * Create the ImageComponent2D out of the specified pixel array, width and
		 * height
		 */
		abstract ImageComponent2D createImageComponent(byte[] pix, int w, int h);
	}

	/**
	 * Creates an ImageComponent2D for 8-bit textures.
	 */
	private final class GreyComponentCreator extends ComponentCreator {

		@Override
		ImageComponent2D createImageComponent(final byte[] pix, final int w,
			final int h)
		{
			final DataBufferByte db = new DataBufferByte(pix, w * h, 0);
			final SampleModel smod = greyCM.createCompatibleSampleModel(w, h);
			final WritableRaster raster = Raster.createWritableRaster(smod, db, null);

			final BufferedImage bImage =
				new BufferedImage(greyCM, raster, false, null);
			final ImageComponent2D bComp =
				new ImageComponent2D(ImageComponent.FORMAT_CHANNEL8, w, h, true, true);
			bComp.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
			bComp.set(bImage);
			return bComp;
		}
	}

	/**
	 * Loads the ImageComponent2Ds for RGBA-textures.
	 */
	private final class ColorComponentCreator extends ComponentCreator {

		@Override
		ImageComponent2D createImageComponent(final byte[] pix, final int w,
			final int h)
		{
			final int[] bandOffset = { 0, 1, 2, 3 };

			final DataBufferByte db = new DataBufferByte(pix, w * h * 4, 0);
			final WritableRaster raster =
				Raster.createInterleavedRaster(db, w, h, w * 4, 4, bandOffset, null);

			final BufferedImage bImage =
				new BufferedImage(rgbCM, raster, false, null);
			final ImageComponent2D bComp =
				new ImageComponent2D(ImageComponent.FORMAT_RGBA, w, h, true, true);
			bComp.setCapability(ImageComponent.ALLOW_IMAGE_WRITE);
			bComp.set(bImage);
			return bComp;
		}
	}

	/* **********************************************************************
	 * The Loader interface and the implementing classes
	 * *********************************************************************/

	/**
	 * Abstract interface for the loader classes.
	 */
	protected interface VoltexLoader extends Loader {

		/**
		 * Loads an xy-slice, with the given z value (x changes fastest) and stores
		 * the data in the provided object
		 */
		void loadZ(int z, byte[] dst);

		/**
		 * Loads an xz-slice, with the given y value (x changes fastest) and stores
		 * the data in the provided object
		 */
		void loadY(int y, byte[] dst);

		/**
		 * Loads an yz-slice, with the given x value (y changes fastest) and stores
		 * the data in the provided object
		 */
		void loadX(int x, byte[] dst);

		/**
		 * Only set the values, without updating the ImageComponent2Ds.
		 */
		void setNoCheckNoUpdate(int x, int y, int z, int v);
	}

	/**
	 * This class is used if the data type is BYTE_DATA.
	 */
	private final class VoltexByteLoader implements VoltexLoader {

		private final ByteLoader l;

		public VoltexByteLoader(final ByteLoader l) {
			this.l = l;
			xy = new byte[zDim][xTexSize * yTexSize];
			xz = new byte[yDim][xTexSize * zTexSize];
			yz = new byte[xDim][yTexSize * zTexSize];
		}

		@Override
		public int load(final int x, final int y, final int z) {
			return l.load(x, y, z);
		}

		@Override
		public int loadWithLUT(final int x, final int y, final int z) {
			return l.load(x, y, z);
		}

		@Override
		public void setNoCheck(final int x, final int y, final int z, int v) {
			l.setNoCheck(x, y, z, v);
			v = l.loadWithLUT(x, y, z);
			xy[z][y * xTexSize + x] = (byte) v;
			xz[y][z * xTexSize + x] = (byte) v;
			yz[x][z * yTexSize + y] = (byte) v;
			xyComp[z].updateData(updater, x, y, 1, 1);
			xzComp[y].updateData(updater, x, z, 1, 1);
			yzComp[x].updateData(updater, y, z, 1, 1);
		}

		@Override
		public void setNoCheckNoUpdate(final int x, final int y, final int z,
			final int v)
		{
			l.setNoCheck(x, y, z, v);
		}

		@Override
		public void set(final int x, final int y, final int z, final int v) {
			if (x >= 0 && x < xDim && y >= 0 && y < yDim && z >= 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}

		@Override
		public void loadZ(final int z, final byte[] d) {
			for (int y = 0; y < yDim; y++) {
				int offs = y * xTexSize;
				for (int x = 0; x < xDim; x++)
					d[offs++] = (byte) l.loadWithLUT(x, y, z);
			}
		}

		@Override
		public void loadY(final int y, final byte[] d) {
			for (int z = 0; z < zDim; z++) {
				int offs = z * xTexSize;
				for (int x = 0; x < xDim; x++)
					d[offs++] = (byte) l.loadWithLUT(x, y, z);
			}
		}

		@Override
		public void loadX(final int x, final byte[] d) {
			for (int z = 0; z < zDim; z++) {
				int offs = z * yTexSize;
				for (int y = 0; y < yDim; y++)
					d[offs++] = (byte) l.loadWithLUT(x, y, z);
			}
		}
	}

	/**
	 * This class is used when the data type is INT_DATA.
	 */
	private final class VoltexIntLoader implements VoltexLoader {

		protected IntLoader l;

		VoltexIntLoader(final IntLoader l) {
			this.l = l;
			xy = new byte[zDim][4 * xTexSize * yTexSize];
			xz = new byte[yDim][4 * xTexSize * zTexSize];
			yz = new byte[xDim][4 * yTexSize * zTexSize];
		}

		public void setLoader(final IntLoader l) {
			this.l = l;
		}

		@Override
		public int load(final int x, final int y, final int z) {
			return l.load(x, y, z);
		}

		@Override
		public int loadWithLUT(final int x, final int y, final int z) {
			return l.load(x, y, z);
		}

		@Override
		public void setNoCheckNoUpdate(final int x, final int y, final int z,
			final int v)
		{
			l.setNoCheck(x, y, z, v);
		}

		@Override
		public void setNoCheck(final int x, final int y, final int z, int v) {
			l.setNoCheck(x, y, z, v);
			v = l.loadWithLUT(x, y, z);

			final int a = (v & 0xff000000) >> 24;
			final int r = (v & 0xff0000) >> 16;
			final int g = (v & 0xff00) >> 8;
			final int b = (v & 0xff);

			int i = 4 * (y * xTexSize + x);
			xy[z][i++] = (byte) r;
			xy[z][i++] = (byte) g;
			xy[z][i++] = (byte) b;
			xy[z][i++] = (byte) a;
			xyComp[z].updateData(updater, x, y, 1, 1);

			i = 4 * (z * xTexSize + x);
			xz[y][i++] = (byte) r;
			xz[y][i++] = (byte) g;
			xz[y][i++] = (byte) b;
			xz[y][i++] = (byte) a;
			xzComp[y].updateData(updater, x, z, 1, 1);

			i = 4 * (z * yTexSize + y);
			yz[x][i++] = (byte) r;
			yz[x][i++] = (byte) g;
			yz[x][i++] = (byte) b;
			yz[x][i++] = (byte) a;
			yzComp[x].updateData(updater, y, z, 1, 1);
		}

		@Override
		public void set(final int x, final int y, final int z, final int v) {
			if (x >= 0 && x < xDim && y >= 0 && y < yDim && z >= 0 && z < zDim) {
				setNoCheck(x, y, z, v);
			}
		}

		@Override
		public void loadZ(final int zValue, final byte[] dst) {
			for (int y = 0; y < yDim; y++) {
				int offsDst = y * xTexSize * 4;
				for (int x = 0; x < xDim; x++) {
					final int c = l.loadWithLUT(x, y, zValue);
					final int a = (c & 0xff000000) >> 24;
					final int r = (c & 0xff0000) >> 16;
					final int g = (c & 0xff00) >> 8;
					final int b = c & 0xff;
					dst[offsDst++] = (byte) r;
					dst[offsDst++] = (byte) g;
					dst[offsDst++] = (byte) b;
					dst[offsDst++] = (byte) a;
				}
			}
		}

		@Override
		public void loadY(final int yValue, final byte[] dst) {
			for (int z = 0; z < zDim; z++) {
				int offsDst = z * xTexSize * 4;
				for (int x = 0; x < xDim; x++) {
					final int c = l.loadWithLUT(x, yValue, z);
					final int a = (c & 0xff000000) >> 24;
					final int r = (c & 0xff0000) >> 16;
					final int g = (c & 0xff00) >> 8;
					final int b = c & 0xff;
					dst[offsDst++] = (byte) r;
					dst[offsDst++] = (byte) g;
					dst[offsDst++] = (byte) b;
					dst[offsDst++] = (byte) a;
				}
			}
		}

		@Override
		public void loadX(final int xValue, final byte[] dst) {
			for (int z = 0; z < zDim; z++) {
				int offsDst = z * yTexSize * 4;
				for (int y = 0; y < yDim; y++) {
					final int c = l.loadWithLUT(xValue, y, z);
					final int a = (c & 0xff000000) >> 24;
					final int r = (c & 0xff0000) >> 16;
					final int g = (c & 0xff00) >> 8;
					final int b = c & 0xff;
					dst[offsDst++] = (byte) r;
					dst[offsDst++] = (byte) g;
					dst[offsDst++] = (byte) b;
					dst[offsDst++] = (byte) a;
				}
			}
		}
	}
}
