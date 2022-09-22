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

import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Tuple3d;

import ij3d.ContentNode;

public class CustomMeshNode extends ContentNode {

	private CustomMesh mesh;

	protected Point3f min, max, center;

	protected CustomMeshNode() {}

	public CustomMeshNode(final CustomMesh mesh) {
		this.mesh = mesh;
		calculateMinMaxCenterPoint();
		addChild(mesh);
	}

	public CustomMesh getMesh() {
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
	public void channelsUpdated(final boolean[] channels) {
		// do nothing
	}

	@Override
	public void lutUpdated(final int[] r, final int[] g, final int[] b,
		final int[] a)
	{
		// do nothing
	}

	@Override
	public void colorUpdated(final Color3f color) {
		mesh.setColor(color);
	}

	@Override
	public void eyePtChanged(final View view) {
		// do nothing
	}

	@Override
	public float getVolume() {
		return mesh.getVolume();
	}

	@Override
	public void shadeUpdated(final boolean shaded) {
		mesh.setShaded(shaded);
	}

	@Override
	public void thresholdUpdated(final int threshold) {
		// do nothing
	}

	@Override
	public void transparencyUpdated(final float transparency) {
		mesh.setTransparency(transparency);
	}

	private void calculateMinMaxCenterPoint() {
		min = new Point3f();
		max = new Point3f();
		center = new Point3f();
		mesh.calculateMinMaxCenterPoint(min, max, center);
	}

	@Override
	public void restoreDisplayedData(final String path, final String name) {
		mesh.restoreDisplayedData(path, name);
	}

	@Override
	public void swapDisplayedData(final String path, final String name) {
		mesh.swapDisplayedData(path, name);
	}

	@Override
	public void clearDisplayedData() {
		mesh.clearDisplayedData();
	}
}
