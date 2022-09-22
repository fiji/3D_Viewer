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

package octree;

import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.QuadArray;

import ij3d.AxisConstants;

public class GeometryCreator implements AxisConstants {

	private final float[] quadCoords = new float[12];
	private static GeometryCreator instance;

	private GeometryCreator() {}

	public static GeometryCreator instance() {
		if (instance == null) instance = new GeometryCreator();
		return instance;
	}

	public GeometryArray getQuad(final CubeData cdata, final int index) {
		calculateQuad(cdata, index);
		final QuadArray quadArray = new QuadArray(4, GeometryArray.COORDINATES);

		quadArray.setCoordinates(0, quadCoords);
//		quadArray.setCapability(QuadArray.ALLOW_INTERSECT);
		return quadArray;
	}

	public float[] getQuadCoordinates(final CubeData cdata, final int index) {
		calculateQuad(cdata, index);
		return quadCoords;
	}

	private void calculateQuad(final CubeData cdata, final int index) {
		switch (cdata.axis) {
			case X_AXIS:
				setCoordsY(cdata);
				setCoordsZ(cdata);
				setCurCoordX(index, cdata);
				break;
			case Y_AXIS:
				setCoordsX(cdata);
				setCoordsZ(cdata);
				setCurCoordY(index, cdata);
				break;
			case Z_AXIS:
				setCoordsX(cdata);
				setCoordsY(cdata);
				setCurCoordZ(index, cdata);
				break;
		}
	}

	private void setCurCoordX(final int i, final CubeData cdata) {
		final float curX = i * cdata.cal[0] + cdata.min[0];
		quadCoords[0] = curX;
		quadCoords[3] = curX;
		quadCoords[6] = curX;
		quadCoords[9] = curX;
	}

	private void setCurCoordY(final int i, final CubeData cdata) {
		final float curY = i * cdata.cal[1] + cdata.min[1];
		quadCoords[1] = curY;
		quadCoords[4] = curY;
		quadCoords[7] = curY;
		quadCoords[10] = curY;
	}

	private void setCurCoordZ(final int i, final CubeData cdata) {
		final float curZ = i * cdata.cal[2] + cdata.min[2];
		quadCoords[2] = curZ;
		quadCoords[5] = curZ;
		quadCoords[8] = curZ;
		quadCoords[11] = curZ;
	}

	private void setCoordsX(final CubeData cdata) {
		// lower left
		quadCoords[1] = cdata.min[1];
		quadCoords[2] = cdata.min[2];
		// lower right
		quadCoords[4] = cdata.max[1];
		quadCoords[5] = cdata.min[2];
		// upper right
		quadCoords[7] = cdata.max[1];
		quadCoords[8] = cdata.max[2];
		// upper left
		quadCoords[10] = cdata.min[1];
		quadCoords[11] = cdata.max[2];
	}

	private void setCoordsY(final CubeData cdata) {
		// lower left
		quadCoords[0] = cdata.min[0];
		quadCoords[2] = cdata.min[2];
		// lower right
		quadCoords[3] = cdata.min[0];
		quadCoords[5] = cdata.max[2];
		// upper right
		quadCoords[6] = cdata.max[0];
		quadCoords[8] = cdata.max[2];
		// upper left
		quadCoords[9] = cdata.max[0];
		quadCoords[11] = cdata.min[2];
	}

	private void setCoordsZ(final CubeData cdata) {
		// lower left
		quadCoords[0] = cdata.min[0];
		quadCoords[1] = cdata.min[1];
		// lower right
		quadCoords[3] = cdata.max[0];
		quadCoords[4] = cdata.min[1];
		// upper right
		quadCoords[6] = cdata.max[0];
		quadCoords[7] = cdata.max[1];
		// upper left
		quadCoords[9] = cdata.min[0];
		quadCoords[10] = cdata.max[1];
	}
}
