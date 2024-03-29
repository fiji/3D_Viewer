/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2023 Fiji developers.
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
/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* Copyright 2006, 2007 Mark Longair */

/*
  This file is part of the ImageJ plugin "Show_Colour_Surfaces".

  The ImageJ plugin "Simple Neurite Tracer" is free software; you
  can redistribute it and/or modify it under the terms of the GNU
  General Public License as published by the Free Software
  Foundation; either version 3 of the License, or (at your option)
  any later version.

  The ImageJ plugin "Simple Neurite Tracer" is distributed in the
  hope that it will be useful, but WITHOUT ANY WARRANTY; without
  even the implied warranty of MERCHANTABILITY or FITNESS FOR A
  PARTICULAR PURPOSE.  See the GNU General Public License for more
  details.

  In addition, as a special exception, the copyright holders give
  you permission to combine this program with free software programs or
  libraries that are released under the Apache Public License.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package isosurface;

import java.awt.image.IndexColorModel;
import java.util.HashMap;

import org.scijava.vecmath.Color3f;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.GUI;
import ij.gui.GenericDialog;
import ij.measure.Calibration;
import ij.plugin.PlugIn;
import ij.process.ByteProcessor;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Image3DUniverse;
import process3d.Smooth;
import vib.NaiveResampler;

/*
  This plugin should be used with 8-bit indexed colour images where
  each colour represents a different material.  Each of this materials
  will be displayed as a surface in the 3D viewer.
 */

public class Show_Colour_Surfaces implements PlugIn {

	/* If backgroundColorIndex is -1, then ask for the background colour.
	   If resampling is < 0, then automatically pick a resampling factor,
	         otherwise, use that parameter. */

	public void displayAsSurfaces(final Image3DUniverse univ,
		final ImagePlus image, final int backgroundColorIndex,
		final double smoothingSigma)
	{
		displayAsSurfaces(univ, image, backgroundColorIndex, smoothingSigma, -1);
	}

	public void displayAsSurfaces(final Image3DUniverse univ, ImagePlus image,
		int backgroundColorIndex, final double smoothingSigma,
		final int requestedResampling)
	{
		if (image == null) {
			IJ.error("Show_Colour_Surfaces.displayAsSurfaces was passed a null 'image'");
			return;
		}
		if (univ == null) {
			IJ.error("Show_Colour_Surfaces.displayAsSurfaces was passed a null 'univ'");
			return;
		}
		final int type = image.getType();
		if (type != ImagePlus.COLOR_256) {
			IJ.error("Show_Colour_Surfaces only works with 8-bit indexed color images.");
			return;
		}
		int width = image.getWidth();
		int height = image.getHeight();
		int depth = image.getStackSize();
		Calibration calibration = image.getCalibration();
		if (calibration == null) calibration = new Calibration(image);
		int maxSampleSide = Math.max(width, Math.max(height, depth));
		int resamplingFactor = 1;
		if (requestedResampling < 0) {
			System.out.println("resamplingFactor is now: " + resamplingFactor);
			while ((maxSampleSide / resamplingFactor) > 512) {
				resamplingFactor *= 2;
			}
		}
		else {
			resamplingFactor = requestedResampling;
		}
		if (resamplingFactor != 1) {
			image = NaiveResampler.resample(image, resamplingFactor);
			width = image.getWidth();
			height = image.getHeight();
			depth = image.getStackSize();
			calibration = image.getCalibration();
			if (calibration == null) calibration = new Calibration(image);
			maxSampleSide = Math.max(width, Math.max(height, depth));
		}

		final ImageStack stack = image.getStack();
		final IndexColorModel cm = (IndexColorModel) stack.getColorModel();
		if (cm == null) {
			IJ.error("The color model for this image stack was null");
			return;
		}
		final int colours = cm.getMapSize();
		final byte[] reds = new byte[colours];
		final byte[] greens = new byte[colours];
		final byte[] blues = new byte[colours];
		cm.getReds(reds);
		cm.getBlues(blues);
		cm.getGreens(greens);
		if (backgroundColorIndex < 0) {
			final GenericDialog gd = new GenericDialog("Show Colour Surfaces");
			gd.addNumericField("Index of background colour (from 0 to " +
				(colours - 1) + " inclusive):", 0, 3);
			gd.showDialog();
			if (gd.wasCanceled()) return;
			backgroundColorIndex = (int) gd.getNextNumber();
		}
		if (backgroundColorIndex < 0 || backgroundColorIndex >= colours) {
			IJ.error("The background colour must have an index from 0 to " +
				(colours - 1) + " inclusive");
			return;
		}
		final HashMap<Integer, Boolean> coloursUsedInImage = new HashMap();
		for (int c = 0; c < colours; ++c) {
			coloursUsedInImage.put(c, false);
		}
		for (int z = 0; z < depth; ++z) {
			final byte[] pixels = (byte[]) stack.getPixels(z + 1);
			for (int i = 0; i < pixels.length; ++i) {
				final int v = pixels[i] & 0xFF;
				coloursUsedInImage.put(v, true);
			}
		}
		for (int i = 0; i < colours; ++i) {
			final boolean used = coloursUsedInImage.get(i);
			if (!used) {
				System.out.println("Skipping colour index " + i +
					", since it's not used in the image");
				continue;
			}
			if (i == backgroundColorIndex) continue;
			final Color3f c =
				new Color3f((reds[i] & 0xFF) / 255.0f, (greens[i] & 0xFF) / 255.0f,
					(blues[i] & 0xFF) / 255.0f);
			final byte v = (byte) i;
			// Make a new ImagePlus with just this colour:
			final ImageStack newStack = new ImageStack(width, height);
			for (int z = 0; z < depth; ++z) {
				final byte[] originalPixels = (byte[]) stack.getPixels(z + 1);
				final byte[] newBytes = new byte[originalPixels.length];
				for (int j = 0; j < originalPixels.length; ++j) {
					if (originalPixels[j] == v) newBytes[j] = (byte) 255;
				}
				final ByteProcessor bp = new ByteProcessor(width, height);
				bp.setPixels(newBytes);
				newStack.addSlice("", bp);
			}
			ImagePlus colourImage =
				new ImagePlus("Image for colour index: " + i, newStack);
			colourImage.setCalibration(calibration);
			if (smoothingSigma > 0) {
				final ImagePlus smoothedColourImage =
					Smooth.smooth(colourImage, true, (float) smoothingSigma, true);
				smoothedColourImage.setTitle("Smoothed image for colour index: " + i);
				colourImage.close();
				colourImage = smoothedColourImage;
			}
			final boolean[] channels = { true, true, true };
			final Content content =
				univ.addContent(colourImage, c, colourImage.getTitle(), 40, // threshold
					channels, resamplingFactor, ContentConstants.SURFACE);
			content.setLocked(true);
			// c.setTransparency(0.5f);
			colourImage.close();
		}
	}

	@Override
	public void run(final String ignored) {
		final ImagePlus image = IJ.getImage();
		if (image == null) {
			IJ.error("There is no image to view.");
			return;
		}
		Calibration c = image.getCalibration();
		if (c == null) c = new Calibration(image);
		final double maxSampleSeparation =
			Math.max(Math.max(Math.abs(c.pixelWidth), Math.abs(c.pixelHeight)), Math
				.abs(c.pixelDepth));

		final int type = image.getType();
		if (type != ImagePlus.COLOR_256) {
			IJ.error("Show_Colour_Surfaces only works with 8-bit indexed color images.");
			return;
		}
		final ImageStack stack = image.getStack();

		final IndexColorModel cm = (IndexColorModel) stack.getColorModel();
		if (cm == null) {
			IJ.error("The color model for this image stack was null");
			return;
		}
		final int colours = cm.getMapSize();

		final int maxSampleSide =
			Math.max(Math.max(image.getWidth(), image.getHeight()), image
				.getStackSize());
		int suggestedResamplingFactor = 1;
		while ((maxSampleSide / suggestedResamplingFactor) > 512) {
			suggestedResamplingFactor *= 2;
		}

		final GenericDialog gd = new GenericDialog("Show Colour Surfaces");

		final String[] choices = new String[Image3DUniverse.universes.size() + 1];
		final String useNewString = "Create New 3D Viewer";
		choices[choices.length - 1] = useNewString;
		for (int i = 0; i < choices.length - 1; ++i) {
			final String contentsString =
				Image3DUniverse.universes.get(i).allContentsString();
			String shortContentsString;
			if (contentsString.length() == 0) shortContentsString = "[Empty]";
			else shortContentsString =
				contentsString.substring(0, Math.min(20, contentsString.length() - 1));
			choices[i] = "[" + i + "] containing " + shortContentsString;
		}
		gd.addChoice("Use 3D Viewer", choices, useNewString);
		gd.addNumericField("Resampling factor: ", suggestedResamplingFactor, 0);
		gd.addNumericField("Index of background colour (from 0 to " +
			(colours - 1) + " inclusive):", 0, 0);
		gd.addNumericField(
			"Radius of smoothing, or -1 for no smoothing (much faster)",
			maxSampleSeparation, 2);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		final String chosenViewer = gd.getNextChoice();
		int chosenIndex;
		for (chosenIndex = 0; chosenIndex < choices.length; ++chosenIndex)
			if (choices[chosenIndex].equals(chosenViewer)) break;
		final int resamplingFactor = (int) gd.getNextNumber();
		final int backgroundColorIndex = (int) gd.getNextNumber();
		final double smoothingSigma = gd.getNextNumber();

		Image3DUniverse univ;
		if (chosenIndex == choices.length - 1) {
			univ = new Image3DUniverse(512, 512);
			univ.show();
			GUI.center(univ.getWindow());
		}
		else univ = Image3DUniverse.universes.get(chosenIndex);
		displayAsSurfaces(univ, image, backgroundColorIndex, smoothingSigma,
			resamplingFactor);
	}
}
