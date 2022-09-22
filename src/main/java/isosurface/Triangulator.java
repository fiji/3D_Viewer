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

package isosurface;

import java.util.List;

import ij.ImagePlus;

public interface Triangulator {

	/**
	 * This method must return a list of elements of class Point3f. Three
	 * subsequent points specify one triangle.
	 * 
	 * @param image the ImagePlus to be displayed
	 * @param threshold the isovalue of the surface to be generated.
	 * @param channels an array containing 3 booleans, indicating which of red,
	 *          green and blue to use for the Triangulation.
	 * @param resamplingF resampling factor
	 */
	public List getTriangles(ImagePlus image, int threshold, boolean[] channels,
		int resamplingF);
}
