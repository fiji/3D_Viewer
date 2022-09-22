/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2022 Fiji developers.
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

package surfaceplot;

import java.awt.Component;
import java.awt.Scrollbar;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;

import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Tuple3d;

import ij.ImagePlus;
import ij.gui.StackWindow;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.ContentNode;
import ij3d.Volume;
import vib.NaiveResampler;

/**
 * This class extends ContentNode to render a Content as a surface plot. By
 * default, the current slice of the image stack is rendered. When the slider in
 * the original image window is dragged, the surface plot is automatically
 * updated.
 *
 * @author Benjamin Schmid
 */
public class SurfacePlotGroup extends ContentNode implements AdjustmentListener
{

	/** The actual surface plot object */
	private SurfacePlot surfacep;

	/** The content which is displayed */
	private ContentInstant c;

	/** The min coordinate */
	private final Point3d min = new Point3d();
	/** The max coordinate */
	private final Point3d max = new Point3d();
	/** The center coordinate */
	private final Point3d center = new Point3d();

	/**
	 * Constructs a surface plot for the given ContentInstant.
	 * 
	 * @param c
	 */
	public SurfacePlotGroup(final Content c) {
		this(c.getCurrent());
	}

	/**
	 * Constructs a surface plot for the given Content.
	 * 
	 * @param c
	 */
	public SurfacePlotGroup(final ContentInstant c) {
		super();
		this.c = c;
		final int res = c.getResamplingFactor();
		final ImagePlus imp =
			res == 1 ? c.getImage() : NaiveResampler.resample(c.getImage(), res, res,
				1);
		final Volume volume = new Volume(imp);
		volume.setChannels(c.getChannels());
		surfacep =
			new SurfacePlot(volume, c.getColor(), c.getTransparency(), c.getImage()
				.getSlice() - 1);

		surfacep.calculateMinMaxCenterPoint(min, max, center);
		addChild(surfacep);
		if (c.getImage().getStackSize() == 1) return;
		final StackWindow win = (StackWindow) c.getImage().getWindow();
		if (win == null) return;
		final Component[] co = win.getComponents();
		for (int i = 0; i < co.length; i++) {
			if (co[i] instanceof Scrollbar) {
				((Scrollbar) co[i]).addAdjustmentListener(this);
			}
		}
	}

	/**
	 * Set the slice to display.
	 * 
	 * @param slice index starting at 1
	 */
	public void setSlice(final int slice) {
		surfacep.setSlice(slice);
	}

	/**
	 * Returns the currently displayed slice.
	 */
	public int getSlice() {
		return surfacep.getSlice();
	}

	/**
	 * Implements AdjustmentListener interface to automatically update the surface
	 * plot when the slice slider in the stack window is dragged.
	 * 
	 * @param e
	 */
	@Override
	public void adjustmentValueChanged(final AdjustmentEvent e) {
		surfacep.setSlice(((Scrollbar) e.getSource()).getValue());
	}

	/**
	 * @see ContentNode#getMax getMax
	 */
	@Override
	public void getMax(final Tuple3d max) {
		max.set(this.max);
	}

	/**
	 * @see ContentNode#getMin getMin
	 */
	@Override
	public void getMin(final Tuple3d min) {
		min.set(this.min);
	}

	/**
	 * @see ContentNode#getCenter getCenter
	 */
	@Override
	public void getCenter(final Tuple3d center) {
		center.set(this.center);
	}

	/**
	 * @see ContentNode#eyePtChanged(View) eyePtChanged
	 */
	@Override
	public void eyePtChanged(final View view) {
		// do nothing
	}

	/**
	 * @see ContentNode#thresholdUpdated thresholdUpdated
	 */
	@Override
	public void thresholdUpdated(final int threshold) {
		// TODO
	}

	/**
	 * @see ContentNode#lutUpdated(int[], int[], int[], int[]) lutUpdated
	 */
	@Override
	public void lutUpdated(final int[] r, final int[] g, final int[] b,
		final int[] a)
	{
		// TODO
	}

	/**
	 * @see ContentNode#channelsUpdated channelsUpdated
	 */
	@Override
	public void channelsUpdated(final boolean[] channels) {
		surfacep.setChannels(channels);
	}

	/**
	 * @see ContentNode#getVolume() getVolume
	 */
	@Override
	public float getVolume() {
		if (surfacep == null) return -1;
		// TODO
		return 0f;
	}

	/**
	 * @see ContentNode#colorUpdated colorUpdated
	 */
	@Override
	public void colorUpdated(final Color3f color) {
		surfacep.setColor(color);
	}

	/**
	 * @see ContentNode#transparencyUpdated transparencyUpdated
	 */
	@Override
	public void transparencyUpdated(final float transparency) {
		surfacep.setTransparency(transparency);
	}

	/**
	 * @see ContentNode#shadeUpdated shadeUpdated
	 */
	@Override
	public void shadeUpdated(final boolean shaded) {
		surfacep.setShaded(shaded);
	}

	@Override
	public void restoreDisplayedData(final String path, final String name) {
		// TODO not implemented yet
	}

	@Override
	public void clearDisplayedData() {
		// TODO not implemented yet
	}

	@Override
	public void swapDisplayedData(final String path, final String name) {
		// TODO not implemented yet
	}
}
