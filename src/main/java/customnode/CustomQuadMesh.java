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

package customnode;

import java.util.Arrays;
import java.util.List;

import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.QuadArray;
import org.scijava.java3d.utils.geometry.GeometryInfo;
import org.scijava.java3d.utils.geometry.NormalGenerator;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

public class CustomQuadMesh extends CustomTriangleMesh {

	public CustomQuadMesh(final List<Point3f> mesh) {
		super(mesh);
	}

	public CustomQuadMesh(final List<Point3f> mesh, final Color3f color,
		final float trans)
	{
		super(mesh, color, trans);
	}

	public void addQuads(final Point3f[] v) {
		if (v.length % 4 != 0) throw new IllegalArgumentException(
			"Number must be a multiple of 4");
		addVertices(v);
	}

	private final Point3f[] fourPoints = new Point3f[4];

	public void addQuad(final Point3f p1, final Point3f p2, final Point3f p3,
		final Point3f p4)
	{
		fourPoints[0] = p1;
		fourPoints[1] = p2;
		fourPoints[2] = p3;
		fourPoints[3] = p4;
		addVertices(fourPoints);
	}

	@Override
	protected GeometryArray createGeometry() {
		if (mesh == null || mesh.size() < 4) return null;
		final List<Point3f> tri = mesh;
		final int nValid = tri.size();
		final int nAll = 2 * nValid;

		final Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		final Color3f colors[] = new Color3f[nValid];
		Arrays.fill(colors, color);

		final GeometryArray ta =
			new QuadArray(nAll, GeometryArray.COORDINATES | GeometryArray.COLOR_3 |
				GeometryArray.NORMALS);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		final GeometryInfo gi = new GeometryInfo(ta);
		// generate normals
		final NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		final GeometryArray result = gi.getGeometryArray();
		result.setValidVertexCount(nValid);

		result.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		result.setCapability(Geometry.ALLOW_INTERSECT);

		return result;
	}
}
