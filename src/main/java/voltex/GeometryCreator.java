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

import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.QuadArray;

import ij3d.AxisConstants;

/**
 * This class is a helper class whose main task is to create Geometry objects
 * for a specified axis and direction. The returned GeometryArray objects are
 * QuadArray objects.
 *
 * @author Benjamin Schmid
 */
public class GeometryCreator implements AxisConstants {

	/** Temporary array of the coordinates of one rectangle */
	private final double[] quadCoords = new double[12];

	/** Image data to be displayed */
	private VoltexVolume volume;

	/**
	 * Initializes this GeometryCreator with the given volume
	 * 
	 * @param volume
	 */
	public GeometryCreator(final VoltexVolume volume) {
		setVolume(volume);
	}

	/**
	 * Change the image data for this GeometryCreator
	 * 
	 * @param volume
	 */
	public void setVolume(final VoltexVolume volume) {
		this.volume = volume;
	}

	/**
	 * Returns the QuadArray for the specified axis and slice index.
	 * 
	 * @param direction
	 * @param index
	 * @return
	 */
	public GeometryArray getQuad(final int direction, final int index) {
		calculateQuad(direction, index);
		final QuadArray quadArray = new QuadArray(4, GeometryArray.COORDINATES);

		quadArray.setCoordinates(0, quadCoords);
		quadArray.setCapability(Geometry.ALLOW_INTERSECT);
		quadArray.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		return quadArray;
	}

	/**
	 * Returns the coordinates of the rectangle of the specified axis and slice
	 * index as a double array.
	 * 
	 * @param direction
	 * @param index
	 * @return
	 */
	public double[] getQuadCoords(final int direction, final int index) {
		calculateQuad(direction, index);
		return quadCoords;
	}

	/**
	 * Calculate the quad coordinates for the given axis and index and store them
	 * in the field.
	 * 
	 * @param direction
	 * @param index
	 */
	private void calculateQuad(final int direction, final int index) {
		switch (direction) {
			case X_AXIS:
				setCoordsY();
				setCoordsZ();
				setCurCoordX(index);
				break;
			case Y_AXIS:
				setCoordsX();
				setCoordsZ();
				setCurCoordY(index);
				break;
			case Z_AXIS:
				setCoordsX();
				setCoordsY();
				setCurCoordZ(index);
				break;
		}
	}

	private void setCurCoordX(final int i) {
		final double curX = i * volume.pw + volume.minCoord.x;
		quadCoords[0] = curX;
		quadCoords[3] = curX;
		quadCoords[6] = curX;
		quadCoords[9] = curX;
	}

	private void setCurCoordY(final int i) {
		final double curY = i * volume.ph + volume.minCoord.y;
		quadCoords[1] = curY;
		quadCoords[4] = curY;
		quadCoords[7] = curY;
		quadCoords[10] = curY;
	}

	private void setCurCoordZ(final int i) {
		final double curZ = i * volume.pd + volume.minCoord.z;
		quadCoords[2] = curZ;
		quadCoords[5] = curZ;
		quadCoords[8] = curZ;
		quadCoords[11] = curZ;
	}

	private void setCoordsX() {
		// lower left
		quadCoords[1] = volume.minCoord.y;
		quadCoords[2] = volume.minCoord.z;
		// lower right
		quadCoords[4] = volume.maxCoord.y;
		quadCoords[5] = volume.minCoord.z;
		// upper right
		quadCoords[7] = volume.maxCoord.y;
		quadCoords[8] = volume.maxCoord.z;
		// upper left
		quadCoords[10] = volume.minCoord.y;
		quadCoords[11] = volume.maxCoord.z;
	}

	private void setCoordsY() {
		// lower left
		quadCoords[0] = volume.minCoord.x;
		quadCoords[2] = volume.minCoord.z;
		// lower right
		quadCoords[3] = volume.minCoord.x;
		quadCoords[5] = volume.maxCoord.z;
		// upper right
		quadCoords[6] = volume.maxCoord.x;
		quadCoords[8] = volume.maxCoord.z;
		// upper left
		quadCoords[9] = volume.maxCoord.x;
		quadCoords[11] = volume.minCoord.z;
	}

	private void setCoordsZ() {
		// lower left
		quadCoords[0] = volume.minCoord.x;
		quadCoords[1] = volume.minCoord.y;
		// lower right
		quadCoords[3] = volume.maxCoord.x;
		quadCoords[4] = volume.minCoord.y;
		// upper right
		quadCoords[6] = volume.maxCoord.x;
		quadCoords[7] = volume.maxCoord.y;
		// upper left
		quadCoords[9] = volume.minCoord.x;
		quadCoords[10] = volume.maxCoord.y;
	}
}
