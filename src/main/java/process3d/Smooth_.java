/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

package process3d;

import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;

/**
 * Smooth_es an ImagePlus, either uniformly or by Gaussian blur. The user can
 * specify if for Gaussian smoothing, the dimensions of the pixels should be
 * taken into account when calculating the kernel. The user also specifies the
 * radius / std dev.
 */
public class Smooth_ extends Smooth implements PlugInFilter {

	private ImagePlus image;

	@Override
	public void run(final ImageProcessor ip) {

		final GenericDialog gd = new GenericDialog("Smooth_");
		gd.addChoice("Method", new String[] { "Uniform", "Gaussian" }, "Gaussian");
		gd.addNumericField("sigma", 1.0f, 3);
		gd.addCheckbox("Use calibration", true);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		final boolean useGaussian = gd.getNextChoice().equals("Gaussian");
		final float sigma = (float) gd.getNextNumber();
		final boolean useCalibration = gd.getNextBoolean();

		final ImagePlus smoothed =
			smooth(image, useGaussian, sigma, useCalibration);
		smoothed.show();
	}

	@Override
	public int setup(final String arg, final ImagePlus img) {
		this.image = img;
		return DOES_8G | DOES_16 | DOES_32 | NO_CHANGES;
	}
}
