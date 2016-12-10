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

package customnode;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TriangleArray;
import org.scijava.java3d.utils.geometry.GeometryInfo;
import org.scijava.java3d.utils.geometry.NormalGenerator;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point2d;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;

import isosurface.MeshProperties;

public class CustomTriangleMesh extends CustomMesh {

	private double volume = 0.0;

	public CustomTriangleMesh(final List<Point3f> mesh) {
		this(mesh, DEFAULT_COLOR, 0);
	}

	public CustomTriangleMesh(final List<Point3f> mesh, final Color3f col,
		final float trans)
	{
		super(mesh, col, trans);
		if (mesh != null) {
			final Point3d center = new Point3d();
			final double[][] inertia = new double[3][3];
			volume = MeshProperties.compute(mesh, center, inertia);
		}
	}

	public void setMesh(final List<Point3f> mesh) {
		this.mesh = mesh;
		update();
	}

	public void addTriangles(final Point3f[] v) {
		if (v.length % 3 != 0) throw new IllegalArgumentException(
			"Number must be a multiple of 3");
		addVertices(v);
	}

	private final Point3f[] threePoints = new Point3f[3];

	public void addTriangle(final Point3f p1, final Point3f p2, final Point3f p3)
	{
		threePoints[0] = p1;
		threePoints[1] = p2;
		threePoints[2] = p3;
		addVertices(threePoints);
	}

	private final int[] threeIndices = new int[3];

	public void removeTriangle(final int index) {
		final int offs = 3 * index;
		threeIndices[0] = offs;
		threeIndices[1] = offs + 1;
		threeIndices[2] = offs + 2;
		removeVertices(threeIndices);
	}

	public void removeTriangles(final int[] indices) {
		Arrays.sort(indices);
		final int[] vIndices = new int[indices.length * 3];
		for (int i = 0, j = 0; i < indices.length; i++) {
			final int index = indices[i];
			final int offs = 3 * index;
			vIndices[j++] = offs;
			vIndices[j++] = offs + 1;
			vIndices[j++] = offs + 2;
		}
		removeVertices(vIndices);
	}

	@Override
	protected GeometryArray createGeometry() {
		if (mesh == null || mesh.size() < 3) return null;
		final List<Point3f> tri = mesh;
		final int nValid = tri.size();
		final int nAll = 2 * nValid;

		final Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		final Color3f colors[] = new Color3f[nValid];
		if (null == color) {
			// Vertex-wise colors are not stored
			// so they have to be retrieved from the geometry:
			for (int i = 0; i < colors.length; ++i) {
				colors[i] = new Color3f(DEFAULT_COLOR);
			}
			final GeometryArray gaOld = (GeometryArray) getGeometry();
			if (null != gaOld) gaOld.getColors(0, colors);
		}
		else {
			Arrays.fill(colors, color);
		}

		final GeometryArray ta =
			new TriangleArray(nAll, GeometryArray.COORDINATES |
				GeometryArray.COLOR_3 | GeometryArray.NORMALS);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		final GeometryInfo gi = new GeometryInfo(ta);
// 		gi.recomputeIndices();
		// generate normals
		final NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);
		// stripify
// 		Stripifier st = new Stripifier();
// 		st.stripify(gi);
		final GeometryArray result = gi.getGeometryArray();
		result.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		result.setCapability(Geometry.ALLOW_INTERSECT);
		result.setValidVertexCount(nValid);

		return result;
	}

	private final Point2d p2d = new Point2d();

	private boolean roiContains(final Point3f p, final Transform3D volToIP,
		final Canvas3D canvas, final Polygon polygon)
	{
		final Point3d locInImagePlate = new Point3d(p);
		volToIP.transform(locInImagePlate);
		canvas.getPixelLocationFromImagePlate(locInImagePlate, p2d);
		return polygon.contains(p2d.x, p2d.y);
	}

	public void retain(final Canvas3D canvas, final Polygon polygon) {
		final Transform3D volToIP = new Transform3D();
		canvas.getImagePlateToVworld(volToIP);
		volToIP.invert();

		final Transform3D toVWorld = new Transform3D();
		this.getLocalToVworld(toVWorld);
		volToIP.mul(toVWorld);

		final ArrayList<Point3f> f = new ArrayList<Point3f>();
		for (int i = 0; i < mesh.size(); i += 3) {
			final Point3f p1 = mesh.get(i);
			final Point3f p2 = mesh.get(i + 1);
			final Point3f p3 = mesh.get(i + 2);
			if (roiContains(p1, volToIP, canvas, polygon) ||
				roiContains(p2, volToIP, canvas, polygon) ||
				roiContains(p3, volToIP, canvas, polygon))
			{
				f.add(p1);
				f.add(p2);
				f.add(p3);
			}
		}
		mesh.clear();
		mesh.addAll(f);
		update();
	}

	@Override
	public float getVolume() {
		return (float) volume;
	}
}
