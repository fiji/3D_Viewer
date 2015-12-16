
package orthoslice;

import ij.ImagePlus;

import java.util.Arrays;
import java.util.BitSet;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.Group;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Switch;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import voltex.VolumeRenderer;

public class MultiOrthoslice extends VolumeRenderer {

	/**
	 * For each axis, a boolean array indicating if the slice should be visible
	 */
	private final boolean[][] slices = new boolean[3][];

	/** The dimensions in x-, y- and z- direction */
	private final int[] dimensions = new int[3];

	/** The visible children of the axis Switch in VolumeRenderer */
	private final BitSet whichChild = new BitSet(6);

	/**
	 * @param img The image stack
	 * @param color The color this Orthoslice should use
	 * @param tr The transparency of this Orthoslice
	 * @param channels A boolean[] array which indicates which color channels to
	 *          use (only affects RGB images). The length of the array must be 3.
	 */
	public MultiOrthoslice(final ImagePlus img, final Color3f color,
		final float tr, final boolean[] channels)
	{
		super(img, color, tr, channels);
		getVolume().setAlphaLUTFullyOpaque();
		dimensions[0] = img.getWidth();
		dimensions[1] = img.getHeight();
		dimensions[2] = img.getStackSize();

		for (int i = 0; i < 3; i++) {
			// by default, show only the middle slice
			slices[i] = new boolean[dimensions[i]];
			slices[i][dimensions[i] / 2] = true;;
			whichChild.set(i, true);
			whichChild.set(i + 3, true);
		}
	}

	/**
	 * Returns whether the textures are transparent or not.
	 */
	public boolean getTexturesOpaque() {
		return appCreator.getOpaqueTextures();
	}

	/**
	 * Makes the textures transparent or not.
	 */
	public void setTexturesOpaque(final boolean opaque) {
		final boolean before = appCreator.getOpaqueTextures();
		if (before != opaque) {
			appCreator.setOpaqueTextures(opaque);
			fullReload();
		}
	}

	/**
	 * Overwrites loadAxis() in VolumeRenderer to skip the slices for which the
	 * visibility flag is not set.
	 * 
	 * @param axis Must be one of X_AXIS, Y_AXIS or Z_AXIS in VolumeRendConstants.
	 * @param index The index within the axis
	 * @param front the front group
	 * @param back the back group
	 */
	@Override
	protected void loadAxis(final int axis, final int index, final Group front,
		final Group back)
	{
		if (slices[axis][index]) super.loadAxis(axis, index, front, back);
	}

	/**
	 * Override eyePtChanged() in VolumeRenderer to always show all slices.
	 * 
	 * @param view
	 */
	@Override
	public void eyePtChanged(final View view) {
		axisSwitch.setWhichChild(Switch.CHILD_MASK);
		axisSwitch.setChildMask(whichChild);
	}

	public int getSliceCount(final int axis) {
		return dimensions[axis];
	}

	public void setVisible(final int axis, final boolean[] b) {
		// cache existing children in the front group
		final BranchGroup[] cachedFrontGroups = new BranchGroup[dimensions[axis]];
		final int axisFront = axisIndex[axis][FRONT];
		final Group frontGroup = (Group) axisSwitch.getChild(axisFront);
		int groupIndex = 0;
		for (int i = 0; i < slices[axis].length; i++)
			if (slices[axis][i]) cachedFrontGroups[i] =
				(BranchGroup) frontGroup.getChild(groupIndex++);
		frontGroup.removeAllChildren();

		// cache existing children in the back group
		final BranchGroup[] cachedBackGroups = new BranchGroup[dimensions[axis]];
		final int axisBack = axisIndex[axis][BACK];
		final Group backGroup = (Group) axisSwitch.getChild(axisBack);
		groupIndex = backGroup.numChildren() - 1;
		for (int i = 0; i < slices[axis].length; i++)
			if (slices[axis][i]) cachedBackGroups[i] =
				(BranchGroup) backGroup.getChild(groupIndex--);
		backGroup.removeAllChildren();

		for (int i = 0; i < slices[axis].length; i++) {
			slices[axis][i] = b[i];

			if (!slices[axis][i]) continue;

			// see if we have something in the cache
			BranchGroup frontShapeGroup = cachedFrontGroups[i];
			BranchGroup backShapeGroup = cachedBackGroups[i];

			// if not cached, create it
			if (frontShapeGroup == null || backShapeGroup == null) {
				final GeometryArray quadArray = geomCreator.getQuad(axis, i);
				final Appearance a = appCreator.getAppearance(axis, i);

				final Shape3D frontShape = new Shape3D(quadArray, a);
				frontShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
				frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
				frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

				frontShapeGroup = new BranchGroup();
				frontShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
				frontShapeGroup.setCapability(Group.ALLOW_CHILDREN_READ);
				frontShapeGroup.addChild(frontShape);

				final Shape3D backShape = new Shape3D(quadArray, a);
				backShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
				backShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
				backShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

				backShapeGroup = new BranchGroup();
				backShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
				backShapeGroup.setCapability(Group.ALLOW_CHILDREN_READ);
				backShapeGroup.addChild(backShape);
			}

			// add the groups to the appropriate axis
			frontGroup.addChild(frontShapeGroup);
			backGroup.insertChild(backShapeGroup, 0);
		}
	}

	/** Hide/show the whole set of slices in the given axis. */
	public void setVisible(final int axis, final boolean b) {
		final boolean[] bs = new boolean[dimensions[axis]];
		Arrays.fill(bs, b);
		setVisible(axis, bs);
	}

	/**
	 * Show a slice every
	 * 
	 * @param interval slices, and hide the rest. Starts by showing slice at
	 * @param offset , and counts slices up to
	 * @param range .
	 */
	public void setVisible(final int axis, final int interval, final int offset,
		final int range)
	{
		final boolean[] bs = new boolean[dimensions[axis]];
		for (int i = offset, k = 0; k < (dimensions[axis] - offset) && k < range; ++k, i +=
			interval)
			bs[i] = true;
		setVisible(axis, bs);
	}

	/**
	 * Translate the visibility state along the given axis.
	 * 
	 * @param axis
	 */
	public void translateVisibilityState(final int axis, final int shift) {
		final boolean[] bs = new boolean[dimensions[axis]];
		final int first = 0;
		final int len = dimensions[axis];
		for (int i = 0; i < dimensions[axis]; ++i) {
			final int target = i + shift;
			if (target < 0 || target > dimensions[axis]) continue;
			bs[target] = slices[axis][i];
		}
		setVisible(axis, bs);
	}
}
