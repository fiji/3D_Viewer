
package octree;

import javax.media.j3d.Appearance;
import javax.media.j3d.BranchGroup;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.Group;
import javax.media.j3d.Shape3D;

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
