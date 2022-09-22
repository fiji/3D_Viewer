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

package ij3d;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Group;
import org.scijava.java3d.Node;
import org.scijava.java3d.Switch;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3f;

import ij.IJ;
import ij.ImagePlus;
import ij.io.FileInfo;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij3d.pointlist.PointListDialog;
import vib.PointList;

public class Content extends BranchGroup implements UniverseListener,
	ContentConstants, AxisConstants
{

	private final HashMap<Integer, Integer> timepointToSwitchIndex;
	private final TreeMap<Integer, ContentInstant> contents;
	private int currentTimePoint;
	private final Switch contentSwitch;
	private boolean showAllTimepoints = false;
	private final String name;
	private boolean showPointList = false;

	private final boolean swapTimelapseData;

	public Content(final String name) {
		this(name, 0);
	}

	public Content(final String name, final int tp) {
		this.name = name;
		this.swapTimelapseData = false;
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(Node.ENABLE_PICK_REPORTING);
		timepointToSwitchIndex = new HashMap<Integer, Integer>();
		contents = new TreeMap<Integer, ContentInstant>();
		final ContentInstant ci = new ContentInstant(name + "_#" + tp);
		ci.timepoint = tp;
		contents.put(tp, ci);
		timepointToSwitchIndex.put(tp, 0);
		contentSwitch = new Switch();
		contentSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		contentSwitch.setCapability(Group.ALLOW_CHILDREN_WRITE);
		contentSwitch.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		contentSwitch.addChild(ci);
		addChild(contentSwitch);
	}

	public Content(final String name,
		final TreeMap<Integer, ContentInstant> contents)
	{
		this(name, contents, false);
	}

	public Content(final String name,
		final TreeMap<Integer, ContentInstant> contents,
		final boolean swapTimelapseData)
	{
		this.name = name;
		this.swapTimelapseData = swapTimelapseData;
		setCapability(BranchGroup.ALLOW_DETACH);
		setCapability(Node.ENABLE_PICK_REPORTING);
		this.contents = contents;
		timepointToSwitchIndex = new HashMap<Integer, Integer>();
		contentSwitch = new Switch();
		contentSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		contentSwitch.setCapability(Group.ALLOW_CHILDREN_WRITE);
		contentSwitch.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		for (final int i : contents.keySet()) {
			final ContentInstant c = contents.get(i);
			c.timepoint = i;
			timepointToSwitchIndex.put(i, contentSwitch.numChildren());
			contentSwitch.addChild(c);
		}
		addChild(contentSwitch);
	}

	// replace if timepoint is already present
	public void addInstant(final ContentInstant ci) {
		final int timepoint = ci.timepoint;
		contents.put(timepoint, ci);
		if (!contents.containsKey(timepoint)) {
			timepointToSwitchIndex.put(timepoint, contentSwitch.numChildren());
			contentSwitch.addChild(ci);
		}
		else {
			final int switchIdx = timepointToSwitchIndex.get(timepoint);
			contentSwitch.setChild(ci, switchIdx);
		}
	}

	public void removeInstant(final int timepoint) {
		if (!contents.containsKey(timepoint)) return;
		final int sIdx = timepointToSwitchIndex.get(timepoint);
		contentSwitch.removeChild(sIdx);
		contents.remove(timepoint);
		timepointToSwitchIndex.remove(timepoint);
		// update the following switch indices.
		for (int i = sIdx; i < contentSwitch.numChildren(); i++) {
			final ContentInstant ci = (ContentInstant) contentSwitch.getChild(i);
			final int tp = ci.getTimepoint();
			timepointToSwitchIndex.put(tp, i);
		}
	}

	public ContentInstant getCurrent() {
		return contents.get(currentTimePoint);
	}

	public ContentInstant getInstant(final int i) {
		return contents.get(i);
	}

	public TreeMap<Integer, ContentInstant> getInstants() {
		return contents;
	}

	public void showTimepoint(final int tp) {
		showTimepoint(tp, false);
	}

	public void showTimepoint(final int tp, final boolean force) {
		if (tp == currentTimePoint && !force) return;
		final ContentInstant old = getCurrent();
		if (old != null && !showAllTimepoints) {
			if (swapTimelapseData) old.swapDisplayedData();
			if (!showAllTimepoints) {
				final ContentInstant next = contents.get(tp);
				if (next != null) next.showPointList(showPointList);
			}
			getCurrent().showPointList(false);
		}
		currentTimePoint = tp;
		if (showAllTimepoints) return;
		final ContentInstant next = getCurrent();
		if (next != null && swapTimelapseData) next.restoreDisplayedData();

		final Integer idx = timepointToSwitchIndex.get(tp);
		if (idx == null) contentSwitch.setWhichChild(Switch.CHILD_NONE);
		else contentSwitch.setWhichChild(idx);
	}

	public void setShowAllTimepoints(final boolean b) {
		this.showAllTimepoints = b;
		if (b) {
			contentSwitch.setWhichChild(Switch.CHILD_ALL);
			return;
		}
		final Integer idx = timepointToSwitchIndex.get(currentTimePoint);
		if (idx == null) contentSwitch.setWhichChild(Switch.CHILD_NONE);
		else contentSwitch.setWhichChild(idx);
	}

	public boolean getShowAllTimepoints() {
		return showAllTimepoints;
	}

	public int getNumberOfInstants() {
		return contents.size();
	}

	public boolean isVisibleAt(final int tp) {
		return contents.containsKey(tp);
	}

	public int getStartTime() {
		return contents.firstKey();
	}

	public int getEndTime() {
		return contents.lastKey();
	}

	// ==========================================================
	// From here begins the 'Content Instant interface', i.e.
	// methods which are delegated to the individual
	// ContentInstants.
	//
	public void displayAs(final int type) {
		for (final ContentInstant c : contents.values())
			c.displayAs(type);
	}

	public static int getDefaultThreshold(final ImagePlus imp, final int type) {
		return ContentInstant.getDefaultThreshold(imp, type);
	}

	public static int getDefaultResamplingFactor(final ImagePlus imp,
		final int type)
	{
		return ContentInstant.getDefaultResamplingFactor(imp, type);
	}

	public void display(final ContentNode node) {
		for (final ContentInstant c : contents.values())
			c.display(node);
	}

	public ImagePlus exportTransformed() {
		return getCurrent().exportTransformed();
	}

	/* ************************************************************
	 * setters - visibility flags
	 *
	 * ***********************************************************/

	public void setVisible(final boolean b) {
		for (final ContentInstant c : contents.values())
			c.setVisible(b);
	}

	public void showBoundingBox(final boolean b) {
		for (final ContentInstant c : contents.values())
			c.showBoundingBox(b);
	}

	public void showCoordinateSystem(final boolean b) {
		for (final ContentInstant c : contents.values())
			c.showCoordinateSystem(b);
	}

	public void setSelected(final boolean selected) {
		// TODO really all?
		for (final ContentInstant c : contents.values())
			c.setSelected(selected);
	}

	/* ************************************************************
	 * point list
	 *
	 * ***********************************************************/
	public void setPointListDialog(final PointListDialog p) {
		for (final ContentInstant c : contents.values())
			c.setPointListDialog(p);
	}

	public void showPointList(final boolean b) {
		getCurrent().showPointList(b);
		this.showPointList = b;
	}

	protected final static Pattern startFramePattern = Pattern
		.compile("(?s)(?m).*?^(# frame:? (\\d+)\n).*");

	public void loadPointList() {
		String dir = null, fileName = null;
		final ImagePlus image = contents.firstEntry().getValue().image;
		if (image != null) {
			final FileInfo fi = image.getFileInfo();
			dir = fi.directory;
			fileName = fi.fileName + ".points";
		}
		final OpenDialog od =
			new OpenDialog("Open points annotation file", dir, fileName);
		if (od.getFileName() == null) return;

		final File file = new File(od.getDirectory(), od.getFileName());
		try {
			String fileContents = readFile(new FileInputStream(file));
			Matcher matcher = startFramePattern.matcher(fileContents);
			if (matcher.matches()) {
				// empty point lists
				for (final Integer frame : contents.keySet())
					contents.get(frame).setPointList(new PointList());
				while (matcher.matches()) {
					final int frame = Integer.parseInt(matcher.group(2));
					fileContents = fileContents.substring(matcher.end(1));
					matcher = startFramePattern.matcher(fileContents);
					final ContentInstant ci = contents.get(frame);
					if (ci == null) continue;
					final String pointsForFrame =
						matcher.matches() ? fileContents.substring(0, matcher.start(1))
							: fileContents;
					final PointList points = PointList.parseString(pointsForFrame);
					if (points != null) ci.setPointList(points);
				}
			}
			else {
				// fall back to old-style one-per-frame point lists
				final PointList points = PointList.parseString(fileContents);
				if (points != null) getCurrent().setPointList(points);
			}
			showPointList(true);
		}
		catch (final IOException e) {
			IJ.error("Could not read point list from " + file);
		}
	}

	String readFile(final InputStream in) throws IOException {
		final ByteArrayOutputStream out = new ByteArrayOutputStream();
		final byte[] buffer = new byte[1024];
		for (;;) {
			final int count = in.read(buffer);
			if (count < 0) break;
			out.write(buffer, 0, count);
		}
		in.close();
		out.close();
		return out.toString("UTF-8");
	}

	public void savePointList() {
		String dir = OpenDialog.getDefaultDirectory();
		String fileName = getName();
		final ImagePlus image = contents.firstEntry().getValue().image;
		if (image != null) {
			final FileInfo fi = image.getFileInfo();
			dir = fi.directory;
			fileName = fi.fileName;
		}
		final SaveDialog sd =
			new SaveDialog("Save points annotation file as...", dir, fileName,
				".points");
		if (sd.getFileName() == null) return;

		final File file = new File(sd.getDirectory(), sd.getFileName());
		if (file.exists() &&
			!IJ.showMessageWithCancel("File exists", "Overwrite " + file + "?")) return;
		try {
			final PrintStream out = new PrintStream(file);
			for (final Integer frame : contents.keySet()) {
				final ContentInstant ci = contents.get(frame);
				if (ci.getPointList().size() != 0) {
					out.println("# frame " + frame);
					ci.savePointList(out);
				}
			}
			out.close();
		}
		catch (final IOException e) {
			IJ.error("Could not save points to " + file);
		}
	}

	/**
	 * @deprecated
	 * @param p
	 */
	@Deprecated
	public void addPointListPoint(final Point3d p) {
		getCurrent().addPointListPoint(p);
	}

	/**
	 * @deprecated
	 * @param i
	 * @param pos
	 */
	@Deprecated
	public void setListPointPos(final int i, final Point3d pos) {
		getCurrent().setListPointPos(i, pos);
	}

	public float getLandmarkPointSize() {
		return getCurrent().getLandmarkPointSize();
	}

	public void setLandmarkPointSize(final float r) {
		for (final ContentInstant c : contents.values())
			c.setLandmarkPointSize(r);
	}

	public Color3f getLandmarkColor() {
		return getCurrent().getLandmarkColor();
	}

	public void setLandmarkColor(final Color3f color) {
		for (final ContentInstant c : contents.values())
			c.setLandmarkColor(color);
	}

	public PointList getPointList() {
		return getCurrent().getPointList();
	}

	/**
	 * @deprecated
	 * @param i
	 */
	@Deprecated
	public void deletePointListPoint(final int i) {
		getCurrent().deletePointListPoint(i);
	}

	/* ************************************************************
	 * setters - transform
	 *
	 **************************************************************/
	public void toggleLock() {
		for (final ContentInstant c : contents.values())
			c.toggleLock();
	}

	public void setLocked(final boolean b) {
		for (final ContentInstant c : contents.values())
			c.setLocked(b);
	}

	public void applyTransform(final double[] matrix) {
		applyTransform(new Transform3D(matrix));
	}

	public void applyTransform(final Transform3D transform) {
		for (final ContentInstant c : contents.values())
			c.applyTransform(transform);
	}

	public void applyRotation(final int axis, final double degree) {
		final Transform3D t = new Transform3D();
		switch (axis) {
			case X_AXIS:
				t.rotX(deg2rad(degree));
				break;
			case Y_AXIS:
				t.rotY(deg2rad(degree));
				break;
			case Z_AXIS:
				t.rotZ(deg2rad(degree));
				break;
		}
		applyTransform(t);
	}

	public void applyTranslation(final float dx, final float dy, final float dz) {
		final Transform3D t = new Transform3D();
		t.setTranslation(new Vector3f(dx, dy, dz));
		applyTransform(t);
	}

	public void setTransform(final double[] matrix) {
		setTransform(new Transform3D(matrix));
	}

	public void setTransform(final Transform3D transform) {
		for (final ContentInstant c : contents.values())
			c.setTransform(transform);
	}

	public void setRotation(final int axis, final double degree) {
		final Transform3D t = new Transform3D();
		switch (axis) {
			case X_AXIS:
				t.rotX(deg2rad(degree));
				break;
			case Y_AXIS:
				t.rotY(deg2rad(degree));
				break;
			case Z_AXIS:
				t.rotZ(deg2rad(degree));
				break;
		}
		setTransform(t);
	}

	public void setTranslation(final float dx, final float dy, final float dz) {
		final Transform3D t = new Transform3D();
		t.setTranslation(new Vector3f(dx, dy, dz));
		setTransform(t);
	}

	private double deg2rad(final double deg) {
		return deg * Math.PI / 180.0;
	}

	/* ************************************************************
	 * setters - attributes
	 *
	 * ***********************************************************/

	public void setChannels(final boolean[] channels) {
		for (final ContentInstant c : contents.values())
			c.setChannels(channels);
	}

	public void
		setLUT(final int[] r, final int[] g, final int[] b, final int[] a)
	{
		for (final ContentInstant c : contents.values())
			c.setLUT(r, g, b, a);
	}

	public void setThreshold(final int th) {
		for (final ContentInstant c : contents.values())
			c.setThreshold(th);
	}

	public void setShaded(final boolean b) {
		for (final ContentInstant c : contents.values())
			c.setShaded(b);
	}

	public void setSaturatedVolumeRendering(final boolean b) {
		for (final ContentInstant c : contents.values())
			c.setSaturatedVolumeRendering(b);
	}

	public void applySurfaceColors(final ImagePlus img) {
		for (final ContentInstant c : contents.values())
			c.applySurfaceColors(img);
	}

	public void setColor(final Color3f color) {
		for (final ContentInstant c : contents.values())
			c.setColor(color);
	}

	public synchronized void setTransparency(final float transparency) {
		for (final ContentInstant c : contents.values())
			c.setTransparency(transparency);
	}

	/* ************************************************************
	 * UniverseListener interface
	 *
	 *************************************************************/
	@Override
	public void transformationStarted(final View view) {}

	@Override
	public void contentAdded(final Content c) {}

	@Override
	public void contentRemoved(final Content c) {
		for (final ContentInstant co : contents.values()) {
			co.contentRemoved(c);
		}
	}

	@Override
	public void canvasResized() {}

	@Override
	public void contentSelected(final Content c) {}

	@Override
	public void contentChanged(final Content c) {}

	@Override
	public void universeClosed() {
		for (final ContentInstant c : contents.values()) {
			c.universeClosed();
		}
	}

	@Override
	public void transformationUpdated(final View view) {
		eyePtChanged(view);
	}

	@Override
	public void transformationFinished(final View view) {
		eyePtChanged(view);
		// apply same transformation to all other time points
		// in case this content was transformed
		final ContentInstant curr = getCurrent();
		if (curr == null || !curr.selected) return;
		final Transform3D t = new Transform3D();
		final Transform3D r = new Transform3D();
		curr.getLocalTranslate(t);
		curr.getLocalRotate(r);

		for (final ContentInstant c : contents.values()) {
			if (c == getCurrent()) continue;
			c.getLocalRotate().setTransform(r);
			c.getLocalTranslate().setTransform(t);
			c.transformationFinished(view);
		}
	}

	public void eyePtChanged(final View view) {
		for (final ContentInstant c : contents.values())
			c.eyePtChanged(view);
	}

	/* *************************************************************
	 * getters
	 *
	 **************************************************************/
	@Override
	public String getName() {
		return name;
	}

	public int getType() {
		return getCurrent().getType();
	}

	public ContentNode getContent() {
		return getCurrent().getContent();
	}

	public void getMin(final Point3d min) {
		min.set(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE);
		final Point3d tmp = new Point3d();
		for (final ContentInstant c : contents.values()) {
			c.getContent().getMin(tmp);
			if (tmp.x < min.x) min.x = tmp.x;
			if (tmp.y < min.y) min.y = tmp.y;
			if (tmp.z < min.z) min.z = tmp.z;
		}
	}

	public void getMax(final Point3d max) {
		max.set(Double.MIN_VALUE, Double.MIN_VALUE, Double.MIN_VALUE);
		final Point3d tmp = new Point3d();
		for (final ContentInstant c : contents.values()) {
			c.getContent().getMax(tmp);
			if (tmp.x > max.x) max.x = tmp.x;
			if (tmp.y > max.y) max.y = tmp.y;
			if (tmp.z > max.z) max.z = tmp.z;
		}
	}

	public ImagePlus getImage() {
		return getCurrent().getImage();
	}

	public boolean[] getChannels() {
		return getCurrent().getChannels();
	}

	public void getRedLUT(final int[] l) {
		getCurrent().getRedLUT(l);
	}

	public void getGreenLUT(final int[] l) {
		getCurrent().getGreenLUT(l);
	}

	public void getBlueLUT(final int[] l) {
		getCurrent().getBlueLUT(l);
	}

	public void getAlphaLUT(final int[] l) {
		getCurrent().getAlphaLUT(l);
	}

	public Color3f getColor() {
		return getCurrent().getColor();
	}

	public boolean isShaded() {
		return getCurrent().isShaded();
	}

	public boolean isSaturatedVolumeRendering() {
		return getCurrent().isSaturatedVolumeRendering();
	}

	public int getThreshold() {
		return getCurrent().getThreshold();
	}

	public float getTransparency() {
		return getCurrent().getTransparency();
	}

	public int getResamplingFactor() {
		return getCurrent().getResamplingFactor();
	}

	public TransformGroup getLocalRotate() {
		return getCurrent().getLocalRotate();
	}

	public TransformGroup getLocalTranslate() {
		return getCurrent().getLocalTranslate();
	}

	public void getLocalRotate(final Transform3D t) {
		getCurrent().getLocalRotate(t);
	}

	public void getLocalTranslate(final Transform3D t) {
		getCurrent().getLocalTranslate(t);
	}

	public boolean isLocked() {
		return getCurrent().isLocked();
	}

	public boolean isVisible() {
		return getCurrent().isVisible();
	}

	public boolean hasCoord() {
		return getCurrent().hasCoord();
	}

	public boolean hasBoundingBox() {
		return getCurrent().hasBoundingBox();
	}

	public boolean isPLVisible() {
		return getCurrent().isPLVisible();
	}
}
