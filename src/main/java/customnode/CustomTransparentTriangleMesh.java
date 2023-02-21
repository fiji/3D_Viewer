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

package customnode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.Material;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.java3d.TriangleArray;
import org.scijava.java3d.utils.geometry.GeometryInfo;
import org.scijava.java3d.utils.geometry.NormalGenerator;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Color4f;
import org.scijava.vecmath.Point3f;

public class CustomTransparentTriangleMesh extends CustomTriangleMesh {

	private final double volume = 0.0;

	public CustomTransparentTriangleMesh(final List<Point3f> mesh) {
		this(mesh, DEFAULT_COLOR, 0);
	}

	public CustomTransparentTriangleMesh(final List<Point3f> mesh,
		final Color3f col, final float trans)
	{
		super(mesh, col, trans);
	}

	@Override
	public float getVolume() {
		return (float) volume;
	}

	@Override
	public void setColor(final Color3f color) {
		this.color = color != null ? color : DEFAULT_COLOR;
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;
		final int N = ga.getVertexCount();
		final Color4f colors[] = new Color4f[N];
		for (int i = 0; i < N; i++) {
			colors[i] = new Color4f(this.color.x, this.color.y, this.color.z, 1);
		}
		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	public void setTransparency(final float t) {
		// do nothing
	}

	public void setTransparentColor(final List<Color4f> color) {
		this.color = null;
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;
		final int N = ga.getValidVertexCount();
		if (color.size() != N) throw new IllegalArgumentException("list of size " +
			N + " expected");
		final Color4f[] colors = new Color4f[N];
		color.toArray(colors);
		ga.setColors(0, colors);
		changed = true;
	}

	@Override
	protected GeometryArray createGeometry() {
		if (mesh == null || mesh.size() < 3) return null;
		final List<Point3f> tri = mesh;
		final int nValid = tri.size();
		final int nAll = 2 * nValid;

		final Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		final Color4f colors[] = new Color4f[nValid];
		if (null == color) {
			// Vertex-wise colors are not stored
			// so they have to be retrieved from the geometry:
			for (int i = 0; i < colors.length; ++i) {
				colors[i] =
					new Color4f(DEFAULT_COLOR.x, DEFAULT_COLOR.y, DEFAULT_COLOR.z, 1);
			}
			final GeometryArray gaOld = (GeometryArray) getGeometry();
			if (null != gaOld) gaOld.getColors(0, colors);
		}
		else {
			Arrays.fill(colors, new Color4f(color.x, color.y, color.z, 1));
		}

		final GeometryArray ta =
			new TriangleArray(nAll, GeometryArray.COORDINATES |
				GeometryArray.COLOR_4 | GeometryArray.NORMALS);

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

	@Override
	protected Appearance createAppearance() {
		final Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		final PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		if (this.shaded) polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		appearance.setPolygonAttributes(polyAttrib);

		final ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		if (null != color) // is null when colors are vertex-wise
		colorAttrib.setColor(color);
		appearance.setColoringAttributes(colorAttrib);

		final TransparencyAttributes tr = new TransparencyAttributes();
		final int mode = TransparencyAttributes.FASTEST;
		tr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		tr.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		tr.setTransparencyMode(mode);
		tr.setTransparency(transparency);
		appearance.setTransparencyAttributes(tr);

		final Material material = new Material();
		material.setCapability(Material.ALLOW_COMPONENT_WRITE);
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.1f, 0.1f, 0.1f);
		material.setDiffuseColor(0.1f, 0.1f, 0.1f);
		appearance.setMaterial(material);
		return appearance;
	}

	public static void main(final String[] args) {
		final ij3d.Image3DUniverse u = new ij3d.Image3DUniverse();
		u.show();

		final List<Point3f> pts = new ArrayList<Point3f>();
		pts.add(new Point3f(0, 0, 0));
		pts.add(new Point3f(1, 0, 0));
		pts.add(new Point3f(1, 1, 0));

		final CustomTransparentTriangleMesh m =
			new CustomTransparentTriangleMesh(pts);
		final List<Color4f> cols = new ArrayList<Color4f>();
		cols.add(new Color4f(0, 1, 1, 0));
		cols.add(new Color4f(1, 0, 1, 0.5f));
		cols.add(new Color4f(1, 1, 0, 1));

		m.setTransparentColor(cols);

		u.addCustomMesh(m, "lkjl");
	}
}
