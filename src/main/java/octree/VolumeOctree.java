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
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Properties;

import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.Group;
import org.scijava.java3d.Node;
import org.scijava.java3d.OrderedGroup;
import org.scijava.java3d.Switch;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.View;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import ij3d.AxisConstants;
import ij3d.Content;
import ij3d.UniverseListener;

public class VolumeOctree implements UniverseListener, AxisConstants {

	public static final int SIZE = 128;

	static final int DETAIL_AXIS = 6;

	private static final int[][] axisIndex = new int[3][2];

	private int[][] sortingIndices;

	private final Switch axisSwitch;

	private final String imageDir;

	private final Cube rootCube;
	private final BranchGroup rootBranchGroup;
	private final UpdaterThread updater;

	int curAxis = Z_AXIS;
	int curDir = BACK;

	private final int maxLevel;
	private final int xdim, ydim, zdim;
	final float pw, ph, pd;
	private final Point3d refPt;

	/* This flag is set here and the cubes check it repeatedly
	 * when they are updating, to be able to cancel.
	 */
	boolean stopUpdating = false;

	public VolumeOctree(final String imageDir, final Canvas3D canvas)
		throws RuntimeException
	{
		this.imageDir = imageDir;

		axisIndex[X_AXIS][FRONT] = 0;
		axisIndex[X_AXIS][BACK] = 1;
		axisIndex[Y_AXIS][FRONT] = 2;
		axisIndex[Y_AXIS][BACK] = 3;
		axisIndex[Z_AXIS][FRONT] = 4;
		axisIndex[Z_AXIS][BACK] = 5;

		axisSwitch = new Switch();
		axisSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_WRITE);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_EXTEND);

		for (int i = 0; i < 7; i++) {
			axisSwitch.addChild(newOrderedGroup());
		}

		rootBranchGroup = new BranchGroup();
		rootBranchGroup.setCapability(BranchGroup.ALLOW_DETACH);
		rootBranchGroup.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);

		final Properties props = new Properties();

		try {
			props.load(new FileInputStream(new File(imageDir, "props.txt")));

			xdim = Integer.parseInt(props.getProperty("width"));
			ydim = Integer.parseInt(props.getProperty("height"));
			zdim = Integer.parseInt(props.getProperty("depth"));
			maxLevel = Integer.parseInt(props.getProperty("level"));

			pw = Float.parseFloat(props.getProperty("pixelWidth"));
			ph = Float.parseFloat(props.getProperty("pixelHeight"));
			pd = Float.parseFloat(props.getProperty("pixelDepth"));

			rootCube = new Cube(this, imageDir, 0, 0, 0, maxLevel);
			rootCube.createChildren();

			refPt = new Point3d(xdim * pw / 2, ydim * ph / 2, zdim * pd / 2);
		}
		catch (final Exception e) {
			throw new RuntimeException("Error in property file.", e);
		}

		addEmptyGroups(curAxis, curDir);
		createSortingIndices();

		updater = new UpdaterThread(canvas);
		updater.run();
	}

// 	public void update() {
// 	}
//
// 	public void cancel() {
// 	}

	public BranchGroup getRootBranchGroup() {
		return rootBranchGroup;
	}

	public Cube getRootCube() {
		return rootCube;
	}

	public float realWorldXDim() {
		return xdim * pw;
	}

	public float realWorldYDim() {
		return ydim * ph;
	}

	public float realWorldZDim() {
		return zdim * pd;
	}

	public void displayInitial() {
		final int[] axis = new int[] { X_AXIS, Y_AXIS, Z_AXIS };

		for (int ai = 0; ai < 3; ai++) {
			final CubeData cdata = new CubeData(rootCube);
			cdata.prepareForAxis(axis[ai]);
			cdata.show();

			Arrays.sort(cdata.shapes);
			final OrderedGroup fg = getOrderedGroup(axisIndex[axis[ai]][FRONT]);
			final OrderedGroup bg = getOrderedGroup(axisIndex[axis[ai]][BACK]);
			for (int i = 0; i < SIZE; i++) {
				fg.addChild(cdata.shapes[i].duplicate().group);
				bg.addChild(cdata.shapes[i].duplicate().group);
			}
		}
		rootBranchGroup.addChild(axisSwitch);
		setWhichChild(axisIndex[curAxis][curDir]);
	}

	final void removeAllCubes() {
		// remove the old data
		final OrderedGroup og = getOrderedGroup(DETAIL_AXIS);
		for (int i = og.numChildren() - 1; i >= 0; i--) {
			final BranchGroup child = (BranchGroup) og.getChild(i);
			child.detach();
			child.removeAllChildren();
		}
	}

	private final void createSortingIndices() {
		final int[] axis = new int[] { X_AXIS, Y_AXIS, Z_AXIS };
		final int[] dir = new int[] { FRONT, BACK };
		sortingIndices = new int[6][];

		final List<Cube> cubes = new ArrayList<Cube>();
		for (final int a : axis) {
			cubes.clear();
			rootCube.collectCubes(cubes, a);
			final ShapeGroup[] shapes = new ShapeGroup[cubes.size() * SIZE];
			int i = 0;
			for (final Cube c : cubes) {
				for (final ShapeGroup sg : c.cdata.shapes) {
					shapes[i] = sg;
					shapes[i].indexInParent = i;
					i++;
				}
			}
			Arrays.sort(shapes);

			final int aif = axisIndex[a][FRONT];
			final int aib = axisIndex[a][BACK];
			sortingIndices[aif] = new int[shapes.length];
			sortingIndices[aib] = new int[shapes.length];
			for (i = 0; i < shapes.length; i++) {
				final int j = shapes.length - 1 - i;
				sortingIndices[aif][i] = shapes[i].indexInParent;
				sortingIndices[aib][j] = shapes[i].indexInParent;
			}
		}
	}

	private final void addEmptyGroups(final int axis, final int dir) {
		final List<Cube> cubes = new ArrayList<Cube>();
		rootCube.collectCubes(cubes, axis);

		final ShapeGroup[] shapes = new ShapeGroup[cubes.size() * SIZE];
		int i = 0;
		for (final Cube c : cubes)
			for (final ShapeGroup sg : c.cdata.shapes)
				shapes[i++] = sg;
		final OrderedGroup og = getOrderedGroup(DETAIL_AXIS);
		for (i = 0; i < shapes.length; i++)
			og.addChild(shapes[i].group);
	}

	final void axisChanged(final Point3d eyePosInLocal) {
		System.out.println("**** AXIS CHANGED ****");
		rootCube.hideSelf();
		rootCube.hideSubtree();
		rootCube.prepareForAxis(curAxis, eyePosInLocal);
		getOrderedGroup(DETAIL_AXIS).setChildIndexOrder(
			sortingIndices[axisIndex[curAxis][curDir]]);
		System.out.println("**** AXIS CHANGED DONE ****");
	}

	private final Transform3D toVWorld = new Transform3D();

	final void volumeToIP(final Canvas3D canvas, final Transform3D ret) {
		canvas.getImagePlateToVworld(ret);
		ret.invert();
		rootBranchGroup.getLocalToVworld(toVWorld);
		ret.mul(toVWorld);
	}

	private final Transform3D volToIP = new Transform3D();

	final void updateCubes(final Canvas3D canvas, final Point3d eyePosInLocal,
		final boolean axisChanged)
	{
		volumeToIP(canvas, volToIP);
		// update cubes
		updater.submit(volToIP, eyePosInLocal, axisChanged);
	}

	private final BitSet bitset = new BitSet(6);

	final void setCombinedWhichChild(final int child) {
		axisSwitch.setWhichChild(Switch.CHILD_MASK);
		bitset.clear();
		bitset.set(DETAIL_AXIS, true);
		bitset.set(child, true);
		axisSwitch.setChildMask(bitset);
	}

	final void setWhichChild(final int child) {
		axisSwitch.setWhichChild(child);
	}

	final OrderedGroup getOrderedGroup(final int i) {
		return (OrderedGroup) axisSwitch.getChild(i);
	}

	static final BranchGroup newBranchGroup() {
		final BranchGroup bg = new BranchGroup();
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		return bg;
	}

	final int countDetailShapes() {
		return getOrderedGroup(DETAIL_AXIS).numChildren();
	}

	/*
	 * private methods
	 */
	private final int countInitialShapes() {
		int sum = 0;
		for (int i = 0; i < 6; i++) {
			final OrderedGroup og = getOrderedGroup(i);
			sum += og.numChildren();
		}
		return sum;
	}

	private static final OrderedGroup newOrderedGroup() {
		final OrderedGroup og = new OrderedGroup();
		og.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		og.setCapability(Group.ALLOW_CHILDREN_WRITE);
		og.setCapability(OrderedGroup.ALLOW_CHILD_INDEX_ORDER_WRITE);
		return og;
	}

	/*
	 * UniverseListener interface
	 */
	private final Vector3d eyeVec = new Vector3d();

	@Override
	public void transformationUpdated(final View view) {
		final Point3d eyePt = getViewPosInLocal(view, rootBranchGroup);
		if (eyePt == null) return;
		eyeVec.sub(eyePt, refPt);

		// select the axis with the greatest magnitude
		int axis = X_AXIS;
		double value = eyeVec.x;
		double max = Math.abs(eyeVec.x);
		if (Math.abs(eyeVec.y) > max) {
			axis = Y_AXIS;
			value = eyeVec.y;
			max = Math.abs(eyeVec.y);
		}
		if (Math.abs(eyeVec.z) > max) {
			axis = Z_AXIS;
			value = eyeVec.z;
			max = Math.abs(eyeVec.z);
		}

		// select the direction based on the sign of the magnitude
		final int dir = value > 0.0 ? FRONT : BACK;
		final Canvas3D canvas = view.getCanvas3D(0);

		if ((axis != curAxis) || (dir != curDir)) {
			curAxis = axis;
			curDir = dir;
			updateCubes(canvas, eyePt, true);
		}
		else {
			updateCubes(canvas, eyePt, false);
		}
	}

	@Override
	public void transformationStarted(final View view) {}

	@Override
	public void transformationFinished(final View view) {}

	@Override
	public void contentAdded(final Content c) {}

	@Override
	public void contentRemoved(final Content c) {}

	@Override
	public void contentChanged(final Content c) {}

	@Override
	public void contentSelected(final Content c) {}

	@Override
	public void canvasResized() {}

	@Override
	public void universeClosed() {}

	private static Transform3D parentInv = new Transform3D();
	private static Point3d viewPosition = new Point3d();
	private static Transform3D t = new Transform3D();

	private static Point3d getViewPosInLocal(final View view, final Node node) {
		if (node == null) return null;
		if (!node.isLive()) return null;
		// get viewplatforms's location in virutal world
		final Canvas3D canvas = view.getCanvas3D(0);
		canvas.getCenterEyeInImagePlate(viewPosition);
		canvas.getImagePlateToVworld(t);
		t.transform(viewPosition);

		// get parent transform
		node.getLocalToVworld(parentInv);
		parentInv.invert();

		// transform the eye position into the parent's coordinate system
		parentInv.transform(viewPosition);

		return viewPosition;
	}

	private class UpdaterThread {

		private final Canvas3D canvas;

		private final Transform3D nextT = new Transform3D();
		private final Point3d nextEyePosInLocal = new Point3d();

		private final Transform3D runningT = new Transform3D();
		private final Point3d runningEyePosInLocal = new Point3d();

		private Thread thread;

		/* This flag is set to true if a new update was submitted */
		private boolean available = false;

		/* This flag is set when a axis-change task was submitted */
		private boolean axisChanged = false;

		public UpdaterThread(final Canvas3D canvas) {
			this.canvas = canvas;
		}

		/*
		 * Should be called when the transformation was changed
		 * but the axis stay the same.
		 */
		public synchronized void submit(final Transform3D t,
			final Point3d eyePosInLocal, final boolean axisChanged)
		{
			if (axisChanged) {
				this.axisChanged = axisChanged;
				System.out.println("SUBMIT AXIS CHANGE");
			}
			nextT.set(t);
			nextEyePosInLocal.set(eyePosInLocal);
			available = true;
			stopUpdating = true;
			notify();
		}

		private synchronized void fetchNext() {
			if (!available) {
				try {
					wait();
				}
				catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}
			runningT.set(nextT);
			runningEyePosInLocal.set(nextEyePosInLocal);
			available = false;
		}

		public void run() {
			thread = new Thread() {

				@Override
				public void run() {
					while (true) {
						fetchNext();
						setWhichChild(axisIndex[curAxis][curDir]);
						if (axisChanged) {
							axisChanged = false;
							axisChanged(runningEyePosInLocal);
							setWhichChild(DETAIL_AXIS);
						}
						System.out.println("updateCubes");
						stopUpdating = false;
						rootCube.update(canvas, runningT);
						setWhichChild(DETAIL_AXIS);
						System.out.println("updateCubes finished");
					}
				}
			};
			thread.setPriority(Thread.MIN_PRIORITY);
			thread.start();
		}

		// TODO cancel thread
	}
}
