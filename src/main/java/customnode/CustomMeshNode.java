
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
