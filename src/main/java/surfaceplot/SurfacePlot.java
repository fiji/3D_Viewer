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

package surfaceplot;

import java.awt.Color;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.IndexedQuadArray;
import org.scijava.java3d.Material;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.java3d.utils.geometry.GeometryInfo;
import org.scijava.java3d.utils.geometry.NormalGenerator;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;

import ij.IJ;
import ij3d.Volume;

/**
 * This class displays an image stack as a surface plot.
 *
 * @author Benjamin Schmid
 */
public final class SurfacePlot extends Shape3D {

	/** The image data */
	private final Volume volume;

	/** The currently displayed slice */
	private int slice = 1;

	/** Pixel width in real world dimensions */
	private float pw = 1;
	/** Pixel height in real world dimensions */
	private float ph = 1;

	/** The maximum intensity value */
	private int maxVal = -1;

	/** The maximum z value */
	private float maxZ = -1;

	/** The factor by which the intensity values are multiplied */
	private float zFactor = 1;

	/** The color of this surface plot */
	private Color3f color = null;

	/** The geometry array */
	private final IndexedQuadArray[] geometry;
	/** The appearance */
	private final Appearance appearance;

	/**
	 * Constructs a SurfacePlot from the given image data, color and transparency
	 * of the specified slice.
	 * 
	 * @param vol
	 * @param color
	 * @param transp
	 * @param slice
	 */
	public SurfacePlot(final Volume vol, final Color3f color, final float transp,
		final int slice)
	{
		this.volume = vol;
		this.slice = slice;
		this.color = color;
		pw = (float) volume.pw;
		ph = (float) volume.ph;

		calculateMax();
		calculateZFactor();

		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);

		geometry = new IndexedQuadArray[volume.zDim];
		geometry[slice] = createGeometry(slice, color);
		appearance = createAppearance(transp);
		setGeometry(geometry[slice]);
		setAppearance(appearance);
		new Thread() {

			@Override
			public void run() {
				for (int g = 0; g < volume.zDim; g++) {
					if (g != slice) {
						geometry[g] = createGeometry(g, color);
						IJ.showProgress(g + 1, volume.zDim);
					}
				}
			}
		}.start();
	}

	/**
	 * Sets the currently displayed slice.
	 * 
	 * @param slice
	 */
	public void setSlice(final int slice) {
		this.slice = slice;
		setGeometry(geometry[slice - 1]);
	}

	/**
	 * Returns the currently displayed slice.
	 */
	public int getSlice() {
		return slice;
	}

	/**
	 * Change the transparency of this surface plot.
	 * 
	 * @param t
	 */
	public void setTransparency(final float t) {
		final TransparencyAttributes tr = appearance.getTransparencyAttributes();
		final int mode =
			t == 0f ? TransparencyAttributes.NONE : TransparencyAttributes.FASTEST;
		tr.setTransparencyMode(mode);
		tr.setTransparency(t);
	}

	/**
	 * Change the displayed channels of this surface plot. This has only an effect
	 * if color images are displayed.
	 * 
	 * @param ch
	 */
	public void setChannels(final boolean[] ch) {
		if (!volume.setChannels(ch)) return;
		calculateMax();
		calculateZFactor();
		geometry[slice] = createGeometry(slice, color);
		setGeometry(geometry[slice]);
		new Thread() {

			@Override
			public void run() {
				for (int g = 0; g < volume.zDim; g++) {
					if (g != slice) {
						geometry[g] = createGeometry(g, color);
						IJ.showProgress(g + 1, volume.zDim);
					}
				}
			}
		}.start();
	}

	/**
	 * Changes the color of this surface plot. If null, the z value is color
	 * coded.
	 * 
	 * @param color
	 */
	public void setColor(final Color3f color) {
		for (int g = 0; g < geometry.length; g++) {
			final int N = geometry[g].getVertexCount();
			final Color3f colors[] = new Color3f[N];
			final Point3f coord = new Point3f();
			for (int i = 0; i < N; i++) {
				geometry[g].getCoordinate(i, coord);
				colors[i] =
					color != null ? color : new Color3f(Color.getHSBColor(coord.z / maxZ,
						1, 1));
			}
			geometry[g].setColors(0, colors);
		}
	}

	/**
	 * Shade the surface or not.
	 * 
	 * @param b
	 */
	public void setShaded(final boolean b) {
		final PolygonAttributes pa = appearance.getPolygonAttributes();
		if (b) pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	}

	/**
	 * Store the minimum, maximum and center coordinate in the given points.
	 * 
	 * @param min
	 * @param max
	 * @param center
	 */
	void calculateMinMaxCenterPoint(final Point3d min, final Point3d max,
		final Point3d center)
	{

		min.x = 0;
		min.y = 0;
		min.z = 0;
		max.x = volume.xDim * pw;
		max.y = volume.yDim * ph;
		max.z = maxZ;
		center.x = max.x / 2;
		center.y = max.y / 2;
		center.z = max.z / 2;
	}

	/**
	 * Calculate the maximum intensity value in the image data.
	 */
	private void calculateMax() {
		maxVal = 0;
		for (int z = 0; z < volume.zDim; z++) {
			for (int y = 0; y < volume.yDim; y++) {
				for (int x = 0; x < volume.xDim; x++) {
					final int v = (0xff & volume.getAverage(x, y, z));
					if (v > maxVal) maxVal = v;
				}
			}
		}
	}

	/**
	 * Automatically calculate a suitable z-factor. The intensity values are
	 * multiplied with this factor.
	 */
	private void calculateZFactor() {
		final float realW = volume.xDim * pw;
		final float realH = volume.yDim * ph;
		maxZ = realW > realH ? realW : realH;
		zFactor = maxZ / maxVal;
	}

	/**
	 * Create the appearance.
	 * 
	 * @return
	 */
	private static Appearance createAppearance(final float transparency) {
		final Appearance app = new Appearance();
		app.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		final PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		app.setPolygonAttributes(polyAttrib);

		final ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
// 		colorAttrib.setColor(color);
		app.setColoringAttributes(colorAttrib);

		final TransparencyAttributes tr = new TransparencyAttributes();
		final int mode =
			transparency == 0f ? TransparencyAttributes.NONE
				: TransparencyAttributes.FASTEST;
		tr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		tr.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		tr.setTransparencyMode(mode);
		tr.setTransparency(transparency);
		app.setTransparencyAttributes(tr);

		final Material material = new Material();
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.5f, 0.5f, 0.5f);
		material.setDiffuseColor(0.1f, 0.1f, 0.1f);
		app.setMaterial(material);
		return app;
	}

	/**
	 * Create the geometry for the specified slice
	 * 
	 * @param g
	 * @return
	 */
	private IndexedQuadArray createGeometry(final int g, final Color3f color) {

		final int nQuads = (volume.xDim - 1) * (volume.yDim - 1);
		final int nIndices = volume.xDim * volume.yDim;
		final int nVertices = nQuads * 4;

		final IndexedQuadArray ta =
			new IndexedQuadArray(nIndices, GeometryArray.COORDINATES |
				GeometryArray.COLOR_3 | GeometryArray.NORMALS, nVertices);

		final Point3f[] coords = new Point3f[nIndices];
		final Color3f colors[] = new Color3f[nIndices];
		for (int i = 0; i < nIndices; i++) {
			final float y = ph * (i / volume.xDim);
			final float x = pw * (i % volume.xDim);
			final float v =
				zFactor *
					(0xff & volume.getAverage(i % volume.xDim, i / volume.xDim, g));
			coords[i] = new Point3f(x, y, v);
			final int c = volume.loadWithLUT(i % volume.xDim, i / volume.xDim, g);
			colors[i] =
				new Color3f(((c >> 16) & 0xff) / 255f, ((c >> 8) & 0xff) / 255f,
					(c & 0xff) / 255f);
		}
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		final int[] indices = new int[nVertices];
		int index = 0;
		for (int y = 0; y < volume.yDim - 1; y++) {
			for (int x = 0; x < volume.xDim - 1; x++) {
				indices[index++] = y * volume.xDim + x;
				indices[index++] = (y + 1) * volume.xDim + x;
				indices[index++] = (y + 1) * volume.xDim + x + 1;
				indices[index++] = y * volume.xDim + x + 1;
			}
		}
		ta.setCoordinateIndices(0, indices);
		ta.setColorIndices(0, indices);

		// initialize the geometry info here
		final GeometryInfo gi = new GeometryInfo(ta);
		// generate normals
		final NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		final IndexedQuadArray result =
			(IndexedQuadArray) gi.getIndexedGeometryArray();
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(Geometry.ALLOW_INTERSECT);

		return result;
	}
}
