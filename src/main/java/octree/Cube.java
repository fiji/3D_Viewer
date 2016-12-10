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

package octree;

import java.io.File;
import java.util.List;

import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Point2d;
import org.scijava.vecmath.Point3d;

import ij3d.AxisConstants;

public class Cube implements AxisConstants, Comparable<Cube> {

	public static final int RESOLUTION_SUFFICIENT = 0;
	public static final int RESOLUTION_UNSUFFICIENT = 1;
	public static final int OUTSIDE_CANVAS = 2;

	public static final double RES_THRESHOLD = VolumeOctree.SIZE * 4;// Math.sqrt(3);

	final int x, y, z, level;
	final String name;
	final String dir;

	private Cube[] children;
	private final Point3d midp;
	private final Point3d[] corners;
	private Point2d[] cornersInCanvas;
	private boolean visible = false;
	private boolean subtreeVisible = false;

	final CubeData cdata;

	final VolumeOctree octree;
	private double distSqFromEye;

	public Cube(final VolumeOctree oct, final String dir, final int x,
		final int y, final int z, final int l)
	{
		this.dir = dir + "/";
		this.octree = oct;
		this.x = x;
		this.y = y;
		this.z = z;
		this.level = l;
		this.name = x + "_" + y + "_" + z + "_" + l;
		if (new File(this.dir + name + ".info").exists()) {
			this.cdata = new CubeData(this);

			corners = new Point3d[8];
			cornersInCanvas = new Point2d[8];
			for (int i = 0; i < 8; i++) {
				cornersInCanvas[i] = new Point2d();
				corners[i] = new Point3d();
			}

			final float[] min = cdata.min;
			final float[] max = cdata.max;

			corners[0].set(min[0], min[1], min[2]);
			corners[7].set(max[0], max[1], max[2]);
			corners[1].set(max[0], min[1], min[2]);
			corners[2].set(min[0], max[1], min[2]);
			corners[3].set(max[0], max[1], min[2]);
			corners[4].set(min[0], min[1], max[2]);
			corners[5].set(max[0], min[1], max[2]);
			corners[6].set(min[0], max[1], max[2]);
			this.midp =
				new Point3d(min[0] + (max[0] - min[0]) / 2, min[1] + (max[1] - min[1]) /
					2, min[2] + (max[2] - min[2]) / 2);
		}
		else {
			// such an object is hopefully never used.
			corners = null;
			this.cdata = null;
			this.midp = null;
		}
	}

	public Cube createCube(final VolumeOctree oct, final String dir, final int x,
		final int y, final int z, final int l)
	{
		final String name = x + "_" + y + "_" + z + "_" + l;
		if (new File(dir, name + ".info").exists()) return new Cube(oct, dir, x, y,
			z, l);
		return null;
	}

	public Cube[] getChildren() {
		return children;
	}

	public void prepareForAxis(final int axis, final Point3d eyePosInLocal) {
		cdata.prepareForAxis(axis);
		if (children == null) return;

		for (final Cube c : children) {
			if (c != null) {
				c.prepareForAxis(axis, eyePosInLocal);
				c.calcDistSqFromEye(eyePosInLocal);
			}
		}
	}

	private void calcDistSqFromEye(final Point3d eyePosInLocal) {
		distSqFromEye = eyePosInLocal.distanceSquared(midp);
	}

	/**
	 * axis One of X_AXIS, Y_AXIS or Z_AXIS
	 */
	public void collectCubes(final List<Cube> cubes, final int axis) {
		cdata.prepareForAxis(axis);
		cubes.add(this);
		if (children == null) return;
		for (final Cube c : children)
			if (c != null) c.collectCubes(cubes, axis);
	}

	public void hideSelf() {
		if (this.visible) {
			this.cdata.hide();
			this.visible = false;
		}
	}

	public void hideSubtree() {
		if (this.subtreeVisible) {
			this.subtreeVisible = false;
			if (this.children == null) return;
			for (final Cube localCube : this.children)
				if (localCube != null) {
					localCube.hideSelf();
					localCube.hideSubtree();
				}
		}
	}

	private void showSelf() {
		if (!(this.visible)) {
			this.cdata.show();
			this.visible = true;
		}
	}

	public void update(final Canvas3D canvas, final Transform3D volToIP) {
		if (octree.stopUpdating) return;

		// give the renderer a chance
// 		try {
// 			Thread.sleep(50);
// 		} catch(InterruptedException e) {}
		final int i = checkResolution(canvas, volToIP);
		if (i == OUTSIDE_CANVAS) {
			hideSelf();
			hideSubtree();
			return;
		}
		if ((i == RESOLUTION_UNSUFFICIENT) && (this.children != null)) {
			this.subtreeVisible = true;
			hideSelf();
			for (final Cube localCube : this.children)
				if (localCube != null) localCube.update(canvas, volToIP);
		}
		else {
			hideSubtree();
			showSelf();
		}
	}

	public int checkResolution(final Canvas3D canvas, final Transform3D volToIP) {
		for (int i = 0; i < this.corners.length; ++i)
			volumePointInCanvas(canvas, volToIP, this.corners[i],
				this.cornersInCanvas[i]);
		if (outsideCanvas(canvas)) return OUTSIDE_CANVAS;

		double d2 = this.cornersInCanvas[0].distance(this.cornersInCanvas[7]);
		double d1 = this.cornersInCanvas[1].distance(this.cornersInCanvas[6]);
		if (d1 > d2) d2 = d1;
		d1 = this.cornersInCanvas[2].distance(this.cornersInCanvas[5]);
		if (d1 > d2) d2 = d1;
		d1 = this.cornersInCanvas[3].distance(this.cornersInCanvas[4]);
		if (d1 > d2) d2 = d1;

		return ((d2 <= RES_THRESHOLD) ? RESOLUTION_SUFFICIENT
			: RESOLUTION_UNSUFFICIENT);
	}

	public void createChildren() {
		if (level == 1) return;
		final int l = level >> 1;
		final int s = VolumeOctree.SIZE;
		children = new Cube[8];
		children[0] = createCube(octree, dir, x, y, z, l);
		children[1] = createCube(octree, dir, x + l * s, y, z, l);
		children[2] = createCube(octree, dir, x, y + l * s, z, l);
		children[3] = createCube(octree, dir, x + l * s, y + l * s, z, l);
		children[4] = createCube(octree, dir, x, y, z + l * s, l);
		children[5] = createCube(octree, dir, x + l * s, y, z + l * s, l);
		children[6] = createCube(octree, dir, x, y + l * s, z + l * s, l);
		children[7] = createCube(octree, dir, x + l * s, y + l * s, z + l * s, l);
		// children should create their children too
		for (final Cube cube : children)
			if (cube != null) cube.createChildren();
	}

	Point3d ptmp = new Point3d();

	private final void volumePointInCanvas(final Canvas3D canvas,
		final Transform3D volToIP, final Point3d p, final Point2d ret)
	{

		ptmp.set(p);
		volToIP.transform(ptmp);
		canvas.getPixelLocationFromImagePlate(ptmp, ret);
	}

	private final boolean outsideCanvas(final Canvas3D canvas) {
		// check if left
		boolean found = true;
		for (int i = 0; i < 8; i++) {
			if (cornersInCanvas[i].x >= 0) {
				found = false;
				break;
			}
		}
		if (found) return true;
		// top
		found = true;
		for (int i = 0; i < 8; i++) {
			if (cornersInCanvas[i].y >= 0) {
				found = false;
				break;
			}
		}
		if (found) return true;

		final int cw = canvas.getWidth(), ch = canvas.getHeight();
		// right
		found = true;
		for (int i = 0; i < 8; i++) {
			if (cornersInCanvas[i].x < cw) {
				found = false;
				break;
			}
		}
		if (found) return true;

		// left
		found = true;
		for (int i = 0; i < 8; i++) {
			if (cornersInCanvas[i].y < ch) {
				found = false;
				break;
			}
		}
		if (found) return true;
		return false;
	}

	@Override
	public int compareTo(final Cube other) {
		if (other == null) return -1;
		if (distSqFromEye < other.distSqFromEye) return -1;
		if (distSqFromEye > other.distSqFromEye) return +1;
		return 0;
	}

	@Override
	public String toString() {
		return name;
	}
}
