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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Group;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Tuple3d;

public class CustomMultiMesh extends CustomMeshNode {

	private List<CustomMesh> customMeshes;

	public CustomMultiMesh() {
		setCapabilities(this);
		customMeshes = new ArrayList<CustomMesh>();
		calculateMinMaxCenterPoint();
	}

	public CustomMultiMesh(final CustomMesh customMesh) {
		setCapabilities(this);
		customMeshes = new ArrayList<CustomMesh>();
		customMeshes.add(customMesh);
		calculateMinMaxCenterPoint();
		final BranchGroup bg = new BranchGroup();
		setCapabilities(bg);

		bg.addChild(customMesh);
		addChild(bg);
	}

	public CustomMultiMesh(final List<CustomMesh> meshes) {
		customMeshes = meshes;
		calculateMinMaxCenterPoint();
		for (final CustomMesh m : customMeshes) {
			final BranchGroup bg = new BranchGroup();
			setCapabilities(bg);
			bg.addChild(m);
			addChild(bg);
		}
	}

	private static void setCapabilities(final BranchGroup bg) {
		bg.setCapability(BranchGroup.ALLOW_DETACH);
		bg.setCapability(Group.ALLOW_CHILDREN_WRITE);
		bg.setCapability(Group.ALLOW_CHILDREN_EXTEND);
	}

	public void remove(final int i) {
		customMeshes.remove(i);
		calculateMinMaxCenterPoint();
		final BranchGroup bg = (BranchGroup) getChild(i);
		removeChild(i);
		bg.removeAllChildren();
	}

	public void remove(final CustomMesh mesh) {
		customMeshes.remove(mesh);
		calculateMinMaxCenterPoint();
		final BranchGroup bg = (BranchGroup) mesh.getParent();
		removeChild(bg);
		bg.removeAllChildren();
	}

	public void add(final CustomMesh mesh) {
		customMeshes.add(mesh);
		calculateMinMaxCenterPoint();
		final BranchGroup bg = new BranchGroup();
		setCapabilities(bg);
		bg.addChild(mesh);
		addChild(bg);
	}

	public int size() {
		return customMeshes.size();
	}

	public CustomMesh getMesh(final int i) {
		return customMeshes.get(i);
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
	public void channelsUpdated(final boolean[] channels) {
		// do nothing
	}

	@Override
	public void colorUpdated(final Color3f color) {
		for (final CustomMesh mesh : customMeshes)
			mesh.setColor(color);
	}

	@Override
	public void eyePtChanged(final View view) {
		// do nothing
	}

	@Override
	public float getVolume() {
		float vol = 0f;
		for (final CustomMesh mesh : customMeshes)
			vol += mesh.getVolume();
		return vol;
	}

	@Override
	public void shadeUpdated(final boolean shaded) {
		for (final CustomMesh mesh : customMeshes)
			mesh.setShaded(shaded);
	}

	@Override
	public void thresholdUpdated(final int threshold) {
		// do nothing
	}

	@Override
	public void transparencyUpdated(final float transparency) {
		for (final CustomMesh mesh : customMeshes)
			mesh.setTransparency(transparency);
	}

	private static void adjustMinMax(final Point3f p, final Point3f min,
		final Point3f max)
	{
		if (p.x < min.x) min.x = p.x;
		if (p.y < min.y) min.y = p.y;
		if (p.z < min.z) min.z = p.z;

		if (p.x > max.x) max.x = p.x;
		if (p.y > max.y) max.y = p.y;
		if (p.z > max.z) max.z = p.z;
	}

	private void calculateMinMaxCenterPoint() {
		min = new Point3f();
		max = new Point3f();
		center = new Point3f();
		if (customMeshes.isEmpty()) return;

		customMeshes.get(0).calculateMinMaxCenterPoint(min, max, center);
		final Point3f mint = new Point3f();
		final Point3f maxt = new Point3f();
		final int n = customMeshes.size();
		if (n == 1) return;
		for (int i = 1; i < n; i++) {
			final CustomMesh mesh = customMeshes.get(i);
			mesh.calculateMinMaxCenterPoint(mint, maxt, center);
			adjustMinMax(mint, min, max);
			adjustMinMax(maxt, min, max);
		}
		center.sub(max, min);
		center.scale(0.5f);
	}

	@Override
	public void restoreDisplayedData(final String path, final String name) {
		HashMap<String, CustomMesh> contents = null;
		customMeshes = null;
		try {
			contents = WavefrontLoader.load(path);
			for (int i = 0; i < contents.size(); i++) {
				final CustomMesh cm = contents.get(name + "###" + i);
				customMeshes.add(cm);
				cm.update();
			}

		}
		catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void swapDisplayedData(final String path, final String name) {
		final HashMap<String, CustomMesh> contents =
			new HashMap<String, CustomMesh>();
		for (int i = 0; i < customMeshes.size(); i++)
			contents.put(name + "###" + i, customMeshes.get(i));

		try {
			WavefrontExporter.save(contents, path + ".obj");
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
	}
}
