/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2016 Fiji developers.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 * #L%
 */

package voltex;

import java.awt.Polygon;

import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point2d;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Tuple3d;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.ContentNode;
import vib.NaiveResampler;

/**
 * This class extends ContentNode to display a Content as a Volume Rendering.
 *
 * @author Benjamin Schmid
 */
public class VoltexGroup extends ContentNode {

	/** The VolumeRenderer behind this VoltexGroup */
	protected VolumeRenderer renderer;

	/** Reference to the Content which holds this VoltexGroup */
	protected ContentInstant c;

	/** The volume of this VoltexGroup */
	private float volume;

	/** The minimum coordinate of this VoltexGroup */
	private Point3d min;

	/** The maximum coordinate of this VoltexGroup */
	private Point3d max;

	/** The center point of this VoltexGroup */
	private Point3d center;

	/**
	 * This constructor only exists to allow subclasses to access the super
	 * constructor of BranchGroup.
	 */
	protected VoltexGroup() {
		super();
	}

	/**
	 * Initialize this VoltexGroup with the specified Content.
	 * 
	 * @param c
	 * @throws IllegalArgumentException if the specified Content has no image.
	 */
	public VoltexGroup(final Content c) {
		this(c.getCurrent());
	}

	/**
	 * Initialize this VoltexGroup with the specified ContentInstant.
	 * 
	 * @param c
	 * @throws IllegalArgumentException if the specified ContentInstant has no
	 *           image.
	 */
	public VoltexGroup(final ContentInstant c) {
		super();
		if (c.getImage() == null) throw new IllegalArgumentException(
			"VoltexGroup can only"
				+ "be initialized from a ContentInstant that holds an image.");
		this.c = c;
		final ImagePlus imp =
			c.getResamplingFactor() == 1 ? c.getImage() : NaiveResampler.resample(c
				.getImage(), c.getResamplingFactor());
		renderer =
			new VolumeRenderer(imp, c.getColor(), c.getTransparency(), c
				.getChannels());
		final int[] rLUT = new int[256];
		final int[] gLUT = new int[256];
		final int[] bLUT = new int[256];
		final int[] aLUT = new int[256];
		renderer.volume.getRedLUT(rLUT);
		renderer.volume.getGreenLUT(gLUT);
		renderer.volume.getBlueLUT(bLUT);
		renderer.volume.getAlphaLUT(aLUT);
		c.setLUT(rLUT, gLUT, bLUT, aLUT);
		renderer.fullReload();
		calculateMinMaxCenterPoint();
		addChild(renderer.getVolumeNode());
	}

	/**
	 * Update the volume rendering from the image (only if the resampling factor
	 * is 1.
	 */
	public void update() {
		if (c.getResamplingFactor() != 1) return;
		renderer.getVolume().updateData();
	}

	/**
	 * Get a reference VolumeRenderer which is used by this class
	 */
	public VolumeRenderer getRenderer() {
		return renderer;
	}

	public Mask createMask() {
		return renderer.createMask();
	}

	/**
	 * @see ContentNode#getMin(Tuple3d) getMin
	 */
	@Override
	public void getMin(final Tuple3d min) {
		min.set(this.min);
	}

	/**
	 * @see ContentNode#getMax (Tuple3d) getMax
	 */
	@Override
	public void getMax(final Tuple3d max) {
		max.set(this.max);
	}

	/**
	 * @see ContentNode#getCenter(Tuple3d) getCenter
	 */
	@Override
	public void getCenter(final Tuple3d center) {
		center.set(this.center);
	}

	/**
	 * @see ContentNode#thresholdUpdated(int) thresholdUpdated
	 */
	@Override
	public void thresholdUpdated(final int threshold) {
		renderer.setThreshold(threshold);
	}

	/**
	 * @see ContentNode#getVolume() getVolume
	 */
	@Override
	public float getVolume() {
		return volume;
	}

	/**
	 * @see ContentNode#eyePtChanged(View view) eyePtChanged
	 */
	@Override
	public void eyePtChanged(final View view) {
		renderer.eyePtChanged(view);
	}

	/**
	 * @see ContentNode#channelsUpdated channelsUpdated
	 */
	@Override
	public void channelsUpdated(final boolean[] channels) {
		renderer.setChannels(channels);
	}

	/**
	 * @see ContentNode#lutUpdated lutUpdated
	 */
	@Override
	public void lutUpdated(final int[] r, final int[] g, final int[] b,
		final int[] a)
	{
		renderer.setLUTs(r, g, b, a);
	}

	/**
	 * @see ContentNode#shadeUpdated shadeUpdated
	 */
	@Override
	public void shadeUpdated(final boolean shaded) {
		// do nothing
	}

	/**
	 * @see ContentNode#colorUpdated colorUpdated
	 */
	@Override
	public void colorUpdated(final Color3f color) {
		renderer.setColor(color);
	}

	/**
	 * @see ContentNode#transparencyUpdated transparencyUpdated
	 */
	@Override
	public void transparencyUpdated(final float transparency) {
		renderer.setTransparency(transparency);
	}

	/**
	 * Stores the matrix which transforms this VoltexGroup to the image plate in
	 * the specified Transform3D.
	 * 
	 * @param toImagePlate
	 */
	public void volumeToImagePlate(final Transform3D toImagePlate) {
		final Transform3D toVWorld = new Transform3D();
		renderer.getVolumeNode().getLocalToVworld(toVWorld);
		toImagePlate.mul(toVWorld);
	}

	/**
	 * Fills the projection of the specified ROI with the given fillValue. Does
	 * nothing if the given ROI is null. Works not only on the internally created
	 * image (the resampled one), but also on the original image.
	 * 
	 * @param canvas
	 * @param roi
	 * @param fillValue
	 */
	public void
		fillRoi(final Canvas3D canvas, final Roi roi, final byte fillValue)
	{
		if (roi == null) return;

		final Polygon p = roi.getPolygon();
		final Transform3D volToIP = new Transform3D();
		canvas.getImagePlateToVworld(volToIP);
		volToIP.invert();
		volumeToImagePlate(volToIP);

		final VoltexVolume vol = renderer.getVolume();
		final Point2d onCanvas = new Point2d();
		for (int z = 0; z < vol.zDim; z++) {
			for (int y = 0; y < vol.yDim; y++) {
				for (int x = 0; x < vol.xDim; x++) {
					volumePointInCanvas(canvas, volToIP, x, y, z, onCanvas);
					if (p.contains(onCanvas.x, onCanvas.y)) {
						vol.setNoCheckNoUpdate(x, y, z, fillValue);
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, vol.zDim);
		}
		vol.updateData();

		// also fill the original image
		final ImagePlus image = c.getImage();
		final int factor = c.getResamplingFactor();
		if (image == null || factor == 1) return;

		final ij3d.Volume volu = new ij3d.Volume(image);
		for (int z = 0; z < volu.zDim; z++) {
			for (int y = 0; y < volu.yDim; y++) {
				for (int x = 0; x < volu.xDim; x++) {
					volumePointInCanvas(canvas, volToIP, x / factor, y / factor, z /
						factor, onCanvas);
					if (p.contains(onCanvas.x, onCanvas.y)) {
						volu.set(x, y, z, fillValue);
					}
				}
			}
			IJ.showStatus("Filling...");
			IJ.showProgress(z, volu.zDim);
		}
	}

	/**
	 * Returns the 3D coordinates of the given x, y, z position on the 3D canvas.
	 * 
	 * @param canvas
	 * @param volToIP
	 * @param x
	 * @param y
	 * @param z
	 * @return
	 */
	private void volumePointInCanvas(final Canvas3D canvas,
		final Transform3D volToIP, final int x, final int y, final int z,
		final Point2d ret)
	{

		final VoltexVolume vol = renderer.volume;
		final double px = x * vol.pw;
		final double py = y * vol.ph;
		final double pz = z * vol.pd;
		final Point3d locInImagePlate = new Point3d(px, py, pz);

		volToIP.transform(locInImagePlate);

		canvas.getPixelLocationFromImagePlate(locInImagePlate, ret);
	}

	/**
	 * Calculate the minimum, maximum and center coordinate, together with the
	 * volume.
	 */
	protected void calculateMinMaxCenterPoint() {
		final ImagePlus imp = c.getImage();
		final int w = imp.getWidth(), h = imp.getHeight();
		final int d = imp.getStackSize();
		final Calibration cal = imp.getCalibration();
		min = new Point3d();
		max = new Point3d();
		center = new Point3d();
		min.x = w * (float) cal.pixelHeight;
		min.y = h * (float) cal.pixelHeight;
		min.z = d * (float) cal.pixelDepth;
		max.x = 0;
		max.y = 0;
		max.z = 0;

		float vol = 0;
		for (int zi = 0; zi < d; zi++) {
			final float z = zi * (float) cal.pixelDepth;
			final ImageProcessor ip = imp.getStack().getProcessor(zi + 1);

			final int wh = w * h;
			for (int i = 0; i < wh; i++) {
				final float v = ip.getf(i);
				if (v == 0) continue;
				vol += v;
				final float x = (i % w) * (float) cal.pixelWidth;
				final float y = (i / w) * (float) cal.pixelHeight;
				if (x < min.x) min.x = x;
				if (y < min.y) min.y = y;
				if (z < min.z) min.z = z;
				if (x > max.x) max.x = x;
				if (y > max.y) max.y = y;
				if (z > max.z) max.z = z;
				center.x += v * x;
				center.y += v * y;
				center.z += v * z;
			}
		}
		center.x /= vol;
		center.y /= vol;
		center.z /= vol;

		volume = (float) (vol * cal.pixelWidth * cal.pixelHeight * cal.pixelDepth);

	}

	@Override
	public void swapDisplayedData(final String path, final String name) {
		renderer.volume.swap(path + ".tif");
		renderer.disableTextures();
	}

	@Override
	public void clearDisplayedData() {
		renderer.volume.clear();
		renderer.disableTextures();
	}

	@Override
	public void restoreDisplayedData(final String path, final String name) {
		renderer.volume.restore(path + ".tif");
		renderer.enableTextures();
	}
}
