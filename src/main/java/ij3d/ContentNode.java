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

package ij3d;

import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Tuple3d;

/**
 * Abstract class which has to be extended by classes that want to define a new
 * type of Content.
 *
 * @author Benjamin Schmid
 */
public abstract class ContentNode extends BranchGroup {

	/**
	 * Default constructor. Ensures that this ContentNode is detachable.
	 */
	public ContentNode() {
		super();
		setCapability(BranchGroup.ALLOW_DETACH);
	}

	/**
	 * Returns the volume of this ContentNode.
	 * 
	 * @return
	 */
	public abstract float getVolume();

	/**
	 * Stores the minimum coordinate of this ContentNode in the specified Tuple3d
	 * 
	 * @param min
	 */
	public abstract void getMin(Tuple3d min);

	/**
	 * Stores the maximum coordinate of this ContentNode in the specified Tuple3d
	 * 
	 * @param min
	 */
	public abstract void getMax(Tuple3d max);

	/**
	 * Stores the center coordinate of this ContentNode in the specified Tuple3d
	 * 
	 * @param min
	 */
	public abstract void getCenter(Tuple3d center);

	/**
	 * This method is called when the channels of the Content have changed.
	 */
	public abstract void channelsUpdated(boolean[] channels);

	/**
	 * This method is called when the color lookup tables of the Content have
	 * changed.
	 */
	public abstract void lutUpdated(int[] r, int[] g, int[] b, int[] a);

	/**
	 * This method is called when the threshold of the Content has changed.
	 */
	public abstract void thresholdUpdated(int threshold);

	/**
	 * This method is called when the color of the Content has changed.
	 */
	public abstract void colorUpdated(Color3f color);

	/**
	 * This method is called when the transparency of the Content has changed.
	 */
	public abstract void transparencyUpdated(float transparency);

	/**
	 * This method is called when the 'shade' attribute of the Content has
	 * changed.
	 */
	public abstract void shadeUpdated(boolean shaded);

	/**
	 * This method is called when the view direction has changed.
	 * 
	 * @param view
	 */
	public abstract void eyePtChanged(View view);

	public abstract void swapDisplayedData(String path, String name);

	public abstract void clearDisplayedData();

	public abstract void restoreDisplayedData(String path, String name);
}
