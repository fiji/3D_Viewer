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

package ij3d.behaviors;

import org.scijava.java3d.Transform3D;

/**
 * @author Benjamin Schmid
 */
public interface BehaviorCallback {

	public static final int ROTATE = 0;
	public static final int TRANSLATE = 0;

	/**
	 * Called when the transformation of a Content or the view has changed.
	 * 
	 * @param type
	 * @param t
	 */
	public void transformChanged(int type, Transform3D t);

}
