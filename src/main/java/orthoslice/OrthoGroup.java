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

package orthoslice;

import ij.ImagePlus;
import ij3d.Content;
import ij3d.ContentInstant;
import vib.NaiveResampler;
import voltex.VoltexGroup;

/**
 * This class extends VoltexGroup. Instead of a whole volume, it shows only
 * three orthogonal slices, one in xy-, xz-, and yz-direction. Each of the
 * slices can be shown or hidden. Additionally, this class offers methods to
 * alter the position of each of the three slices.
 *
 * @author Benjamin Schmid
 */
public class OrthoGroup extends VoltexGroup {

	/**
	 * Construct a OrthoGroup from the given Content.
	 * 
	 * @param c
	 */
	public OrthoGroup(final Content c) {
		this(c.getCurrent());
	}

	/**
	 * Construct a OrthoGroup from the given ContentInstant.
	 * 
	 * @param c
	 */
	public OrthoGroup(final ContentInstant c) {
		super();
		this.c = c;
		final ImagePlus imp =
			c.getResamplingFactor() == 1 ? c.getImage() : NaiveResampler.resample(c
				.getImage(), c.getResamplingFactor());
		renderer =
			new Orthoslice(imp, c.getColor(), c.getTransparency(), c.getChannels());
		renderer.fullReload();
		calculateMinMaxCenterPoint();
		addChild(renderer.getVolumeNode());
	}

	/**
	 * Alter the slice index of the given direction.
	 * 
	 * @param axis
	 * @param v
	 */
	public void setSlice(final int axis, final int v) {
		((Orthoslice) renderer).setSlice(axis, v);
	}

	/**
	 * Get the slice index of the specified axis
	 * 
	 * @param axis
	 * @return
	 */
	public int getSlice(final int axis) {
		return ((Orthoslice) renderer).getSlice(axis);
	}

	/**
	 * Alter the slice index of the given direction.
	 * 
	 * @param axis
	 */
	public void decrease(final int axis) {
		((Orthoslice) renderer).decrease(axis);
	}

	/**
	 * Alter the slice index of the given direction.
	 * 
	 * @param axis
	 */
	public void increase(final int axis) {
		((Orthoslice) renderer).increase(axis);
	}

	/**
	 * Returns true if the slice in the given direction is visible.
	 */
	public boolean isVisible(final int i) {
		return ((Orthoslice) renderer).isVisible(i);
	}

	/**
	 * Set the specified slice visible
	 * 
	 * @param axis
	 * @param b
	 */
	public void setVisible(final int axis, final boolean b) {
		((Orthoslice) renderer).setVisible(axis, b);
	}
}
