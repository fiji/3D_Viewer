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

package octree;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.Group;
import org.scijava.java3d.Shape3D;

public class ShapeGroup implements Comparable {

	float pos;
	Shape3D shape;
	BranchGroup group;
	BranchGroup child;

	/* This is only used for creating the sorting indices for
	 * the parent OrderedGroup. Not very nice...
	 */
	int indexInParent;

	public ShapeGroup() {
		group = new BranchGroup();
		group.setCapability(Group.ALLOW_CHILDREN_WRITE);
		group.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		child = new BranchGroup();
		child.setCapability(BranchGroup.ALLOW_DETACH);
	}

	public void prepareForAxis(final float pos) {
		this.pos = pos;
	}

	public void show(final CubeData cdata, final int index) {
		shape =
			new Shape3D(createGeometry(cdata, index), createAppearance(cdata, index));
		child.addChild(shape);
		group.addChild(child);
	}

	public void hide() {
		child.detach();
		child.removeAllChildren();
		shape = null;
	}

	private static GeometryArray createGeometry(final CubeData cdata,
		final int index)
	{
		final GeometryArray arr = GeometryCreator.instance().getQuad(cdata, index);
		return arr;
	}

	private static Appearance createAppearance(final CubeData cdata,
		final int index)
	{
		return AppearanceCreator.instance().getAppearance(cdata, index);
	}

	@Override
	public int compareTo(final Object o) {
		final ShapeGroup sg = (ShapeGroup) o;
		if (pos < sg.pos) return -1;
		if (pos > sg.pos) return +1;
		return 0;
	}

	/*
	 * Used in displayInitial.
	 */
	public ShapeGroup duplicate() {
		final ShapeGroup ret = new ShapeGroup();
		if (shape != null) {
			ret.shape = new Shape3D(shape.getGeometry(), shape.getAppearance());
			ret.group.addChild(ret.shape);
		}
		ret.pos = pos;
		return ret;
	}
}
