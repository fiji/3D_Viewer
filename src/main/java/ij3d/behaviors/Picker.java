
package ij3d.behaviors;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.PickInfo;
import org.scijava.java3d.SceneGraphPath;
import org.scijava.java3d.utils.pickfast.PickCanvas;
import org.scijava.java3d.utils.pickfast.PickTool;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import ij.IJ;
import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.DefaultUniverse;
import ij3d.ImageCanvas3D;
import ij3d.Volume;
import vib.BenesNamedPoint;
import vib.PointList;
import voltex.VoltexGroup;

/**
 * This class is a helper class which implements functions for picking.
 *
 * @author Benjamin Schmid
 */
public class Picker {

	private final DefaultUniverse univ;
	private final ImageCanvas3D canvas;

	/**
	 * Constructs a new Picker
	 * 
	 * @param univ
	 */
	public Picker(final DefaultUniverse univ) {
		this.univ = univ;
		this.canvas = (ImageCanvas3D) univ.getCanvas();
	}

	/**
	 * Deletes a landmark point of the specified Content at the given mouse
	 * position
	 * 
	 * @param c
	 * @param e
	 */
	public void deletePoint(final Content c, final MouseEvent e) {
		if (c == null) {
			IJ.error("Selection required");
			return;
		}
		final Point3d p3d = getPickPointGeometry(c, e);
		if (p3d == null) return;
		final PointList pl = c.getPointList();
		final float tol = c.getLandmarkPointSize();
		final int ind = pl.indexOfPointAt(p3d.x, p3d.y, p3d.z, tol);
		if (ind != -1) {
			pl.remove(ind);
		}
	}

	private int movingIndex = -1;

	/**
	 * Moves the picked landmark point to the position specified by the
	 * MouseEvent.
	 * 
	 * @param c
	 * @param e
	 */
	public synchronized void movePoint(final Content c, final MouseEvent e) {
		if (c == null) {
			IJ.error("Selection required");
			return;
		}
		final Point3d p3d = getPickPointGeometry(c, e);
		if (p3d == null) return;

		final PointList pl = c.getPointList();
		if (movingIndex == -1) movingIndex =
			pl.indexOfPointAt(p3d.x, p3d.y, p3d.z, c.getLandmarkPointSize());
		if (movingIndex != -1) {
			pl.placePoint(pl.get(movingIndex), p3d.x, p3d.y, p3d.z);
		}
	}

	/**
	 * Stop moving.
	 */
	public synchronized void stopMoving() {
		movingIndex = -1;
	}

	/**
	 * Adds a landmark point specfied by the canvas position
	 * 
	 * @param c
	 * @param x position in the canvas
	 * @param y position in the canvas
	 */
	public void addPoint(final Content c, final int x, final int y) {
		if (c == null) {
			IJ.error("Selection required");
			return;
		}
		final Point3d p3d = getPickPointGeometry(c, x, y);
		if (p3d == null) return;
		final PointList pl = c.getPointList();
		final float tol = c.getLandmarkPointSize();
		final BenesNamedPoint bnp = pl.pointAt(p3d.x, p3d.y, p3d.z, tol);
		if (bnp == null) {
			pl.add(p3d.x, p3d.y, p3d.z);
		}
	}

	/**
	 * Adds a landmark point specfied by the position of the MouseEvent.
	 * 
	 * @param c
	 * @param e
	 */
	public void addPoint(final Content c, final MouseEvent e) {
		if (c == null) {
			IJ.error("Selection required");
			return;
		}
		final Point3d p3d = getPickPointGeometry(c, e);
		if (p3d == null) return;
		final PointList pl = c.getPointList();
		final float tol = c.getLandmarkPointSize();
		final BenesNamedPoint bnp = pl.pointAt(p3d.x, p3d.y, p3d.z, tol);
		if (bnp == null) {
			pl.add(p3d.x, p3d.y, p3d.z);
		}
	}

	/**
	 * Get the picked point using geometry picking. The pick line is specified by
	 * the given Point3d and Vector3d.
	 * 
	 * @param c
	 * @param origin
	 * @param dir
	 * @return
	 */
	public Point3d getPickPointGeometry(final Content c, final Point3d origin,
		final Vector3d dir)
	{
		final PickTool pickTool = new PickTool(c);
		pickTool.setShapeRay(origin, dir);

		pickTool.setMode(PickInfo.PICK_GEOMETRY);
		pickTool.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		try {
			final PickInfo[] result = pickTool.pickAllSorted();
			if (result == null || result.length == 0) return null;

			for (int i = 0; i < result.length; i++) {
				final Point3d intersection = result[i].getClosestIntersectionPoint();
				if (c.getType() != ContentConstants.VOLUME) return intersection;

				final float v = getVolumePoint(c, intersection);
				if (v > 20) return intersection;
			}
			return null;
		}
		catch (final Exception ex) {
			return null;
		}
	}

	/**
	 * Get the picked point, using geometry picking, for the specified canvas
	 * position.
	 * 
	 * @param c
	 * @param e
	 * @return
	 */
	public Point3d getPickPointGeometry(final Content c, final MouseEvent e) {
		return getPickPointGeometry(c, e.getX(), e.getY());
	}

	/**
	 * Get the picked point, using geometry picking, for the specified canvas
	 * position.
	 * 
	 * @param c
	 * @param e
	 * @return
	 */
	public Point3d
		getPickPointGeometry(final Content c, final int x, final int y)
	{
		final PickCanvas pickCanvas = new PickCanvas(canvas, c);
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3.0f);
		pickCanvas.setShapeLocation(x, y);
		try {
			final PickInfo[] result = pickCanvas.pickAllSorted();
			if (result == null || result.length == 0) return null;

			for (int i = 0; i < result.length; i++) {
				final Point3d intersection = result[i].getClosestIntersectionPoint();
				if (c.getType() != ContentConstants.VOLUME) return intersection;

				final float v = getVolumePoint(c, intersection);
				if (v > 20) return intersection;
			}
			return null;
		}
		catch (final Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public List<Map.Entry<Point3d, Float>> getPickPointColumn(final Content c,
		final int x, final int y)
	{
		if (c.getType() != ContentConstants.VOLUME) return null;
		final PickCanvas pickCanvas = new PickCanvas(canvas, c);
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3.0f);
		pickCanvas.setShapeLocation(x, y);
		try {
			final PickInfo[] result = pickCanvas.pickAllSorted();
			if (result == null || result.length == 0) return null;

			final ArrayList<Map.Entry<Point3d, Float>> list =
				new ArrayList<Map.Entry<Point3d, Float>>();
			for (int i = 0; i < result.length; i++) {
				final Point3d intersection = result[i].getClosestIntersectionPoint();
				list.add(new Map.Entry<Point3d, Float>() {

					@Override
					public Point3d getKey() {
						return intersection;
					}

					@Override
					public Float getValue() {
						return getVolumePoint(c, intersection);
					}

					@Override
					public Float setValue(final Float f) {
						throw new UnsupportedOperationException();
					}
				});
			}
			return list;
		}
		catch (final Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	public int[] getPickedVertexIndices(final BranchGroup bg, final int x,
		final int y)
	{
		final PickCanvas pickCanvas = new PickCanvas(univ.getCanvas(), bg);
		pickCanvas.setTolerance(3f);
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.CLOSEST_GEOM_INFO);
		pickCanvas.setShapeLocation(x, y);
		try {
			final PickInfo result = pickCanvas.pickClosest();
			if (result == null) return null;

			final PickInfo.IntersectionInfo info = result.getIntersectionInfos()[0];
			return info.getVertexIndices();
		}
		catch (final Exception ex) {
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Get the Content at the specified canvas position
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public Content getPickedContent(final int x, final int y) {
		final PickCanvas pickCanvas = new PickCanvas(canvas, univ.getScene());
		pickCanvas.setMode(PickInfo.PICK_GEOMETRY);
		pickCanvas.setFlags(PickInfo.SCENEGRAPHPATH |
			PickInfo.CLOSEST_INTERSECTION_POINT);
		pickCanvas.setTolerance(3);
		pickCanvas.setShapeLocation(x, y);
		try {
			final PickInfo[] result = pickCanvas.pickAllSorted();
			if (result == null) return null;
			for (int i = 0; i < result.length; i++) {
				final SceneGraphPath path = result[i].getSceneGraphPath();
				Content c = null;
				for (int j = path.nodeCount() - 1; j >= 0; j--)
					if (path.getNode(j) instanceof Content) c = (Content) path.getNode(j);

				if (c == null) continue;

				if (c.getType() != ContentConstants.VOLUME &&
					c.getType() != ContentConstants.ORTHO) return c;

				final Point3d intersection = result[i].getClosestIntersectionPoint();

				final float v = getVolumePoint(c, intersection);
				if (v > 20) return c;
			}
			return null;
		}
		catch (final Exception ex) {
			return null;
		}
	}

	private static float getVolumePoint(final Content c, final Point3d p) {

		final Volume v = ((VoltexGroup) c.getContent()).getRenderer().getVolume();

		final int ix = (int) Math.round(p.x / v.pw);
		final int iy = (int) Math.round(p.y / v.ph);
		final int iz = (int) Math.round(p.z / v.pd);
		if (ix < 0 || ix >= v.xDim || iy < 0 || iy >= v.yDim || iz < 0 ||
			iz >= v.zDim) return 0;
		return (v.getAverage(ix, iy, iz) & 0xff);
	}
}
