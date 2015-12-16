
package isosurface;

import customnode.CustomTriangleMesh;
import ij.IJ;
import ij3d.Content;
import ij3d.ContentInstant;
import ij3d.ContentNode;

import java.awt.Color;
import java.util.List;

import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Tuple3d;

import marchingcubes.MCTriangulator;

public class MeshGroup extends ContentNode {

	private final CustomTriangleMesh mesh;
	private final Triangulator triangulator = new MCTriangulator();
	private final ContentInstant c;
	private Point3f min, max, center;

	public MeshGroup(final Content c) {
		this(c.getCurrent());
	}

	public MeshGroup(final ContentInstant c) {
		super();
		this.c = c;
		Color3f color = c.getColor();
		final List tri =
			triangulator.getTriangles(c.getImage(), c.getThreshold(),
				c.getChannels(), c.getResamplingFactor());
		if (color == null) {
			final int value =
				c.getImage().getProcessor().getColorModel().getRGB(c.getThreshold());
			color = new Color3f(new Color(value));
		}
		mesh = new CustomTriangleMesh(tri, color, c.getTransparency());
		calculateMinMaxCenterPoint();
		addChild(mesh);
	}

	public CustomTriangleMesh getMesh() {
		return mesh;
	}

	@Override
	public void getMin(final Tuple3d min) {
		min.set(this.min);
	}

	@Override
	public void getMax(final Tuple3d max) {
		max.set(this.max);
	}

	@Override
	public void getCenter(final Tuple3d center) {
		center.set(this.center);
	}

	@Override
	public void eyePtChanged(final View view) {
		// do nothing
	}

	@Override
	public void thresholdUpdated(final int threshold) {
		if (c.getImage() == null) {
			IJ.error("Mesh was not calculated of a grayscale "
				+ "image. Can't change threshold");
			return;
		}
		final List tri =
			triangulator.getTriangles(c.getImage(), c.getThreshold(),
				c.getChannels(), c.getResamplingFactor());
		mesh.setMesh(tri);
	}

	@Override
	public void lutUpdated(final int[] r, final int[] g, final int[] b,
		final int[] a)
	{
		// TODO
	}

	@Override
	public void channelsUpdated(final boolean[] channels) {
		if (c.getImage() == null) {
			IJ.error("Mesh was not calculated of a grayscale "
				+ "image. Can't change channels");
			return;
		}
		final List tri =
			triangulator.getTriangles(c.getImage(), c.getThreshold(),
				c.getChannels(), c.getResamplingFactor());
		mesh.setMesh(tri);
	}

	public void calculateMinMaxCenterPoint() {
		min = new Point3f();
		max = new Point3f();
		center = new Point3f();
		if (mesh != null) {
			mesh.calculateMinMaxCenterPoint(min, max, center);
		}
	}

	@Override
	public float getVolume() {
		if (mesh == null) return -1;
		return mesh.getVolume();
	}

	@Override
	public void shadeUpdated(final boolean shaded) {
		mesh.setShaded(shaded);
	}

	@Override
	public void colorUpdated(Color3f newColor) {
		if (newColor == null) {
			final int val =
				c.getImage().getProcessor().getColorModel().getRGB(c.getThreshold());
			newColor = new Color3f(new Color(val));
		}
		mesh.setColor(newColor);
	}

	@Override
	public void transparencyUpdated(final float transparency) {
		mesh.setTransparency(transparency);
	}

	@Override
	public void restoreDisplayedData(final String path, final String name) {
		mesh.restoreDisplayedData(path, name);
	}

	@Override
	public void clearDisplayedData() {
		mesh.clearDisplayedData();
	}

	@Override
	public void swapDisplayedData(final String path, final String name) {
		mesh.swapDisplayedData(path, name);
	}
}
