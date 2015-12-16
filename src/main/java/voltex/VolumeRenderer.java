
package voltex;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.Group;
import org.scijava.java3d.Node;
import org.scijava.java3d.OrderedGroup;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Switch;
import org.scijava.java3d.TextureUnitState;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import ij.IJ;
import ij.ImagePlus;
import ij3d.AxisConstants;

/**
 * This class is the main class for creating a volume rendering. It consists of
 * a root branch group, which holds a Switch of OrderedGroups. Each OrderedGroup
 * holds the shapes for one direction. One shape consists of a rectangular
 * geometry on which a texture from the given image data is mapped.
 *
 * @author Benjamin Schmid
 */
public class VolumeRenderer implements AxisConstants {

	/** The image data which is rendered by this VolumeRenderer */
	protected final VoltexVolume volume;

	/** The root BranchGroup whose subtree contains the volume rendering */
	protected final BranchGroup root;

	/** The AppearanceCreator, which creates the textures. */
	protected final AppearanceCreator appCreator;
	/** The GeometryCreator, which creates the corresponding geomentries. */
	protected final GeometryCreator geomCreator;

	/** A Switch, which holds 6 OrderedGroups, one for each direction */
	protected final Switch axisSwitch;
	/** The index in the switch, given the direction */
	protected final int[][] axisIndex = new int[3][2];

	/** The current axis of view */
	private int curAxis = Z_AXIS;
	/** The current direction of view */
	private int curDir = FRONT;

	/**
	 * Constructor. Initializes this VolumeRenderer with the given image, color,
	 * transparency and channels. This does not yet start the rendering. To do so,
	 * fullReload() needs to be called.
	 *
	 * @param img the image stack to be rendered.
	 * @param color the color in which this rendering should be displayed
	 * @param tr the transparency value for this volume rendering
	 * @param channels the rgb channels which should be used. This must be a
	 *          boolean[] array of length three, one for red, green and blue
	 *          respectively.
	 */
	public VolumeRenderer(final ImagePlus img, final Color3f color,
		final float tr, final boolean[] channels)
	{

		this.volume = new VoltexVolume(img);
		volume.setChannels(channels);
		appCreator = new AppearanceCreator(volume, color, tr);
		geomCreator = new GeometryCreator(volume);

		axisIndex[X_AXIS][FRONT] = 0;
		axisIndex[X_AXIS][BACK] = 1;
		axisIndex[Y_AXIS][FRONT] = 2;
		axisIndex[Y_AXIS][BACK] = 3;
		axisIndex[Z_AXIS][FRONT] = 4;
		axisIndex[Z_AXIS][BACK] = 5;

		axisSwitch = new Switch();
		axisSwitch.setCapability(Switch.ALLOW_SWITCH_READ);
		axisSwitch.setCapability(Switch.ALLOW_SWITCH_WRITE);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_READ);
		axisSwitch.setCapability(Group.ALLOW_CHILDREN_WRITE);
		for (int i = 0; i < 6; i++)
			axisSwitch.addChild(getOrderedGroup());

		root = new BranchGroup();
		root.addChild(axisSwitch);
		root.setCapability(BranchGroup.ALLOW_DETACH);
		root.setCapability(Node.ALLOW_LOCAL_TO_VWORLD_READ);
	}

	public void disableTextures() {
		final int[] size = new int[] { volume.xDim, volume.yDim, volume.zDim };

		final Appearance empty = new Appearance();
		for (int axis = 0; axis < 3; axis++) {
			for (int i = 0; i < size[axis]; i++) {
				final Group frontGroup =
					(Group) axisSwitch.getChild(axisIndex[axis][FRONT]);
				final Group backGroup =
					(Group) axisSwitch.getChild(axisIndex[axis][BACK]);

				final Appearance app = getAppearance(axis, i);
				app.getTextureUnitState(0).setTexture(null);
				((Shape3D) ((Group) frontGroup.getChild(i)).getChild(0))
					.setAppearance(empty);
				((Shape3D) ((Group) backGroup.getChild(i)).getChild(0))
					.setAppearance(empty);
			}
		}
	}

	public void enableTextures() {
		final int[] size = new int[] { volume.xDim, volume.yDim, volume.zDim };
		for (int axis = 0; axis < 3; axis++) {
			for (int i = 0; i < size[axis]; i++) {
				final Appearance app = appCreator.getAppearance(axis, i);
				final Group frontGroup =
					(Group) axisSwitch.getChild(axisIndex[axis][FRONT]);
				((Shape3D) ((Group) frontGroup.getChild(i)).getChild(0))
					.setAppearance(app);
				final Group backGroup =
					(Group) axisSwitch.getChild(axisIndex[axis][BACK]);
				((Shape3D) ((Group) backGroup.getChild(size[axis] - i - 1)).getChild(0))
					.setAppearance(app);
			}
		}
	}

	/**
	 * Returns the root BranchGroup below which the whole volume rendering is
	 * organized.
	 * 
	 * @return
	 */
	public BranchGroup getVolumeNode() {
		return root;
	}

	/**
	 * Returns the image data of this rendering.
	 * 
	 * @return
	 */
	public VoltexVolume getVolume() {
		return volume;
	}

	/**
	 * Get the Appearance object for the Shape3D of the specified axis; Note that
	 * both front and back shapes share the same Appearance.
	 */
	public Appearance getAppearance(final int axis, final int index) {
		final Group frontGroup =
			(Group) axisSwitch.getChild(axisIndex[axis][FRONT]);
		return ((Shape3D) ((Group) frontGroup.getChild(index)).getChild(0))
			.getAppearance();
	}

	/**
	 * Create a mask to this volume.
	 */
	public Mask createMask() {
		final Mask mask = new Mask(volume, root);

		final Group frontGroup = null;
		final Group backGroup = null;

		final int[] size = new int[] { volume.xDim, volume.yDim, volume.zDim };

		for (int axis = 0; axis < 3; axis++) {
			for (int i = 0; i < size[axis]; i++) {
				final Appearance app = getAppearance(axis, i);
				app.setTextureUnitState(1, new TextureUnitState(appCreator.getTexture(
					axis, i, mask), mask.getMaskAttributes(), appCreator.getTg(axis)));
			}
		}
		return mask;
	}

	/**
	 * Call this method when the direction of view changed.
	 */
	private final Vector3d eyeVec = new Vector3d();

	public void eyePtChanged(final View view) {
		final Point3d eyePt = getViewPosInLocal(view, root);
		if (eyePt != null) {
			final Point3d volRefPt = volume.volRefPt;
			eyeVec.sub(eyePt, volRefPt);

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

			if ((axis != curAxis) || (dir != curDir)) {
				curAxis = axis;
				curDir = dir;
				axisSwitch.setWhichChild(axisIndex[curAxis][curDir]);
			}
		}
	}

	/**
	 * Fully removes all the data and reloads all the textures.
	 */
	public void fullReload() {
		for (int i = 0; i < axisSwitch.numChildren(); i++) {
			((Group) axisSwitch.getChild(i)).removeAllChildren();
		}
		loadAxis(Z_AXIS);
		loadAxis(Y_AXIS);
		loadAxis(X_AXIS);
		axisSwitch.setWhichChild(axisIndex[curAxis][curDir]);
	}

	/**
	 * Set the threshold. Pixel value below the threshold are not rendered.
	 * 
	 * @param threshold
	 */
	public void setThreshold(final int threshold) {
		float value = threshold / 255f;
		value = Math.min(1f, value);
		value = Math.max(0.1f, value);
		appCreator.setThreshold(value);
	}

	/**
	 * Set the transparency for this rendering
	 * 
	 * @param transparency
	 */
	public void setTransparency(final float transparency) {
		appCreator.setTransparency(transparency);
	}

	/**
	 * Set the displayed color channels for this volume rendering. This affects
	 * only RGB images. A boolean[] array is required of length 3, one value for
	 * red, green and blue.
	 * 
	 * @param channels
	 */
	public void setChannels(final boolean[] channels) {
		if (volume.setChannels(channels)) {
			appCreator.updateTextureMode();
			fullReload();
		}
	}

	/**
	 * Update the lookup tables for this volume rendering.
	 */
	public void
		setLUTs(final int[] r, final int[] g, final int[] b, final int[] a)
	{
		if (volume.setLUTs(r, g, b, a)) {
			appCreator.updateTextureMode();
			fullReload();
		}
	}

	/**
	 * Set the color for this volume rendering
	 * 
	 * @param color
	 */
	public void setColor(final Color3f color) {
		if (volume.setAverage(color != null)) {
			appCreator.updateTextureMode();
			fullReload();
		}
		final Color3f c = color != null ? color : new Color3f(1f, 1f, 1f);
		appCreator.setColor(c);
	}

	/**
	 * Load a specific axis (both front and back direction) This method is
	 * protected, so that it can eventually be overridden by subclasses like
	 * Orthoslice.
	 *
	 * @param axis
	 */
	protected void loadAxis(final int axis) {
		int rSize = 0; // number of tex maps to create
		Group frontGroup = null;
		Group backGroup = null;

		frontGroup = (Group) axisSwitch.getChild(axisIndex[axis][FRONT]);
		backGroup = (Group) axisSwitch.getChild(axisIndex[axis][BACK]);
		String m = "Loading ";

		switch (axis) {
			case Z_AXIS:
				rSize = volume.zDim;
				m += "x axis";
				break;
			case Y_AXIS:
				rSize = volume.yDim;
				m += "y axis";
				break;
			case X_AXIS:
				rSize = volume.xDim;
				m += "z axis";
				break;
		}
		IJ.showStatus(m);

		for (int i = 0; i < rSize; i++) {
			IJ.showProgress(i + 1, rSize);
			loadAxis(axis, i, frontGroup, backGroup);
		}
	}

	/**
	 * Load a specific slice of the specified axis (both front and back direction)
	 * and adds it to the specified Groups. This method is protected, so that it
	 * can eventually be overridden by subclasses like Orthoslice.
	 *
	 * @param axis
	 * @param index
	 * @param front
	 * @param back
	 */
	protected void loadAxis(final int axis, final int index, final Group front,
		final Group back)
	{

		final GeometryArray quadArray = geomCreator.getQuad(axis, index);
		final Appearance a = appCreator.getAppearance(axis, index);

		final Shape3D frontShape = new Shape3D(quadArray, a);
		frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		frontShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);

		final BranchGroup frontShapeGroup = new BranchGroup();
		frontShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
		frontShapeGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		frontShapeGroup.addChild(frontShape);
		front.addChild(frontShapeGroup);

		final Shape3D backShape = new Shape3D(quadArray, a);
		backShape.setCapability(Shape3D.ALLOW_APPEARANCE_READ);
		backShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);

		final BranchGroup backShapeGroup = new BranchGroup();
		backShapeGroup.setCapability(BranchGroup.ALLOW_DETACH);
		backShapeGroup.setCapability(Group.ALLOW_CHILDREN_READ);
		backShapeGroup.addChild(backShape);
		back.insertChild(backShapeGroup, 0);
	}

	private static Transform3D parentInv = new Transform3D();
	private static Point3d viewPosition = new Point3d();
	private static Transform3D t = new Transform3D();

	/**
	 * return the eye's position in <node>'s coordinate space
	 */
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

	private Group getOrderedGroup() {
		final OrderedGroup og = new OrderedGroup();
		og.setCapability(Group.ALLOW_CHILDREN_READ);
		og.setCapability(Group.ALLOW_CHILDREN_WRITE);
		og.setCapability(Group.ALLOW_CHILDREN_EXTEND);
		return og;
	}

}
