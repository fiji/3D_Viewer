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

package ij3d;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeMap;

import org.scijava.vecmath.Color3f;

import customnode.CustomMesh;
import customnode.CustomMeshNode;
import customnode.CustomMultiMesh;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.io.FileInfo;
import ij.process.ColorProcessor;
import ij.process.ImageConverter;
import ij.process.StackConverter;

public class ContentCreator {

	private static final boolean SWAP_TIMELAPSE_DATA = false;

	public static Content createContent(final String name, final ImagePlus image,
		final int type)
	{
		final int resf = Content.getDefaultResamplingFactor(image, type);
		return createContent(name, image, type, resf, 0);
	}

	public static Content createContent(final String name, final ImagePlus image,
		final int type, final int resf)
	{
		return createContent(name, image, type, resf, 0);
	}

	public static Content createContent(final String name, final ImagePlus image,
		final int type, final int resf, final int tp)
	{
		final int thr = Content.getDefaultThreshold(image, type);
		return createContent(name, image, type, resf, tp, null, thr, new boolean[] {
			true, true, true });
	}

	public static Content createContent(final String name, final ImagePlus image,
		final int type, final int resf, final int tp, final Color3f color,
		final int thresh, final boolean[] channels)
	{

		return createContent(name, getImages(image), type, resf, tp, color, thresh,
			channels);
	}

	public static Content createContent(final String name, final File file,
		final int type, final int resf, final int tp, final Color3f color,
		final int thresh, final boolean[] channels)
	{

		return createContent(name, getImages(file), type, resf, tp, color, thresh,
			channels);
	}

	public static Content createContent(final String name,
		final ImagePlus[] images, final int type, final int resf, int tp,
		final Color3f color, final int thresh, final boolean[] channels)
	{

		final TreeMap<Integer, ContentInstant> instants =
			new TreeMap<Integer, ContentInstant>();
		final boolean timelapse = images.length > 1;
		final boolean shouldSwap = SWAP_TIMELAPSE_DATA && timelapse;
		for (final ImagePlus imp : images) {
			final ContentInstant content = new ContentInstant(name);
			content.image = imp;
			content.color = color;
			content.threshold = thresh;
			content.channels = channels;
			content.resamplingF = resf;
			content.timepoint = tp;
			content
				.showCoordinateSystem(UniverseSettings.showLocalCoordinateSystemsByDefault);
			content.displayAs(type);
			content.compile();
			if (shouldSwap) {
				content.clearOriginalData();
				content.swapDisplayedData();
			}
			instants.put(tp++, content);
		}
		return new Content(name, instants, shouldSwap);
	}

	public static Content createContent(final CustomMesh mesh, final String name)
	{
		return createContent(mesh, name, 0);
	}

	public static Content createContent(final CustomMesh mesh, final String name,
		final int tp)
	{
		final Content c = new Content(name, tp);
		final ContentInstant content = c.getInstant(tp);
		content.color = mesh.getColor();
		content.transparency = mesh.getTransparency();
		content.shaded = mesh.isShaded();
		content
			.showCoordinateSystem(UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.display(new CustomMeshNode(mesh));
		return c;
	}

	public static Content createContent(final CustomMultiMesh node,
		final String name)
	{
		return createContent(node, name, 0);
	}

	public static Content createContent(final CustomMultiMesh node,
		final String name, final int tp)
	{
		final Content c = new Content(name, tp);
		final ContentInstant content = c.getInstant(tp);
		content.color = null;
		content.transparency = 0f;
		content.shaded = true;
		content
			.showCoordinateSystem(UniverseSettings.showLocalCoordinateSystemsByDefault);
		content.display(node);
		return c;
	}

	/**
	 * Get an array of images of the specified image; if the image is a
	 * hyperstack, it is splitted into several individual images, otherwise, it
	 * the returned array contains the given image only.
	 * 
	 * @param imp
	 * @return
	 */
	public static ImagePlus[] getImages(final ImagePlus imp) {
		final ImagePlus[] ret = new ImagePlus[imp.getNFrames()];
		int i = 0;
		for (final ImagePlus frame : HyperStackIterator.getIterable(imp))
			ret[i++] = frame;
		return ret;
	}

	/**
	 * If <code>file</code> is a regular file, it is opened using IJ.openImage(),
	 * and then given to getImages(ImagePlus); If <code>file</code> however is a
	 * directory, all the files in it are sorted alphabetically and then loaded,
	 * failing silently if an image can not be opened by IJ.openImage().
	 * 
	 * @param file
	 * @return
	 */
	public static ImagePlus[] getImages(final File file) {
		final ArrayList<ImagePlus> images = new ArrayList<ImagePlus>();
		for (final ImagePlus frame : FileIterator.getIterable(file))
			images.add(frame);
		return images.toArray(new ImagePlus[] {});
	}

	public static void convert(final ImagePlus image) {
		final int imaget = image.getType();
		if (imaget == ImagePlus.GRAY8 || imaget == ImagePlus.COLOR_256) return;
		final int s = image.getStackSize();
		switch (imaget) {
			case ImagePlus.GRAY16:
			case ImagePlus.GRAY32:
				if (s == 1) new ImageConverter(image).convertToGray8();
				else new StackConverter(image).convertToGray8();
				break;
		}
	}

	private static class FileIterator implements Iterator<ImagePlus>,
		Iterable<ImagePlus>
	{

		public static Iterable<ImagePlus> getIterable(final File file) {
			if (!file.isDirectory()) {
				return HyperStackIterator.getIterable(IJ.openImage(file
					.getAbsolutePath()));
			}
			return new FileIterator(file);
		}

		private final String directory;
		private final String[] names;
		private int nextIndex = 0;

		/**
		 * file may be a single file or a directory.
		 */
		private FileIterator(final File file) {
			if (!file.isDirectory()) {
				names = new String[] { file.getName() };
				directory = file.getParentFile().getAbsolutePath();
				return;
			}
			// get the file names
			directory = file.getAbsolutePath();
			names = file.list();
			Arrays.sort(names);
		}

		@Override
		public Iterator<ImagePlus> iterator() {
			return this;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasNext() {
			return nextIndex < names.length;
		}

		@Override
		public ImagePlus next() {
			if (nextIndex == names.length) return null;

			final File f = new File(directory, names[nextIndex]);
			nextIndex++;
			try {
				return IJ.openImage(f.getAbsolutePath());
			}
			catch (final Exception e) {
				return null;
			}
		}
	}

	private static class HyperStackIterator implements Iterator<ImagePlus>,
		Iterable<ImagePlus>
	{

		public static Iterable<ImagePlus> getIterable(final ImagePlus image) {
			return new HyperStackIterator(image);
		}

		private final ImagePlus image;
		private final int nChannels;
		private final int nSlices;
		private final int nFrames;
		private final int w;
		private final int h;
		private int nextFrame = 0;

		private HyperStackIterator(final ImagePlus image) {
			this.image = image;
			nChannels = image.getNChannels();
			nSlices = image.getNSlices();
			nFrames = image.getNFrames();
			System.out.println("nFrames = " + nFrames);
			w = image.getWidth();
			h = image.getHeight();
		}

		@Override
		public Iterator<ImagePlus> iterator() {
			return this;
		}

		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean hasNext() {
			return nextFrame < nFrames;
		}

		@Override
		public ImagePlus next() {
			if (nextFrame == nFrames) return null;

			final ImageStack oldStack = image.getStack();
			final String oldTitle = image.getTitle();
			final FileInfo fi = image.getFileInfo();
			final ImageStack newStack = new ImageStack(w, h);
			newStack.setColorModel(oldStack.getColorModel());
			for (int j = 0; j < nSlices; j++) {
				final int index = image.getStackIndex(1, j + 1, nextFrame + 1);
				Object pixels;
				if (nChannels > 1) {
					image.setPositionWithoutUpdate(1, j + 1, nextFrame + 1);
					pixels = new ColorProcessor(image.getImage()).getPixels();
				}
				else {
					pixels = oldStack.getPixels(index);
				}
				newStack.addSlice(oldStack.getSliceLabel(index), pixels);
			}
			final ImagePlus ret =
				new ImagePlus(oldTitle + " (frame " + nextFrame + ")", newStack);
			ret.setCalibration(image.getCalibration().copy());
			ret.setFileInfo((FileInfo) fi.clone());
			nextFrame++;
			return ret;
		}
	}
}
