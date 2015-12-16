
package ij3d.shapes;

import org.scijava.java3d.utils.geometry.Text2D;

import java.awt.Font;
import java.text.DecimalFormat;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.LineArray;
import org.scijava.java3d.OrientedShape3D;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

public class BoundingBox extends BranchGroup {

	private final Point3f min, max;

	public BoundingBox(final Point3d min, final Point3d max) {
		this(new Point3f(min), new Point3f(max));
	}

	public BoundingBox(final Point3f min, final Point3f max) {
		this(min, max, new Color3f(1, 0, 0));
	}

	public BoundingBox(final Point3d minp, final Point3d maxp, final Color3f color)
	{
		this(new Point3f(minp), new Point3f(maxp), color);
	}

	public BoundingBox(final Point3f minp, final Point3f maxp, final Color3f color)
	{
		setCapability(BranchGroup.ALLOW_DETACH);
//		setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		min = minp;
		max = maxp;

		min.x -= 0;
		min.y -= 0;
		min.z -= 0;
		max.x += 0;
		max.y += 0;
		max.z += 0;
		final Point3f[] p = new Point3f[8];
		p[0] = new Point3f(min.x, min.y, max.z);
		p[1] = new Point3f(max.x, min.y, max.z);
		p[2] = new Point3f(max.x, max.y, max.z);
		p[3] = new Point3f(min.x, max.y, max.z);
		p[4] = new Point3f(min.x, min.y, min.z);
		p[5] = new Point3f(max.x, min.y, min.z);
		p[6] = new Point3f(max.x, max.y, min.z);
		p[7] = new Point3f(min.x, max.y, min.z);

		final Shape3D shape = new Shape3D();
		shape.setName("BB");

		final float lx = max.x - min.x;
		final float ly = max.y - min.y;
		final float lz = max.z - min.z;
		final float max = Math.max(lx, Math.max(ly, lz));
		float min = Math.min(lx, Math.min(ly, lz));
		if (min == 0 || max / min > 100) min = max;
		double tmp = 0.00001f;
		while (min / tmp > 5)
			tmp *= 10;

		if (min / tmp < 2) tmp = tmp / 2;

		final float tickDistance = (float) tmp;

		final float tickSize = max / 50;

		final Color3f c = color;
		final float td = tickDistance;
		final float ts = tickSize;

		final float fx = tickDistance - (this.min.x % tickDistance);
		final float fy = tickDistance - (this.min.y % tickDistance);
		final float fz = tickDistance - (this.min.z % tickDistance);

		shape.addGeometry(makeLine(p[0], p[1], c, td, 0f, ts, true));
		shape.addGeometry(makeLine(p[1], p[2], c, td, 0f, ts, true));
		shape.addGeometry(makeLine(p[2], p[3], c, td, 0f, ts, true));
		shape.addGeometry(makeLine(p[3], p[0], c, td, 0f, ts, true));

		shape.addGeometry(makeLine(p[4], p[5], c, td, fx, ts, false));
		shape.addGeometry(makeLine(p[5], p[6], c, td, 0f, ts, true));
		shape.addGeometry(makeLine(p[6], p[7], c, td, 0f, ts, true));
		shape.addGeometry(makeLine(p[4], p[7], c, td, fy, ts, false));

		shape.addGeometry(makeLine(p[4], p[0], c, td, fz, ts, false));
		shape.addGeometry(makeLine(p[1], p[5], c, td, 0f, ts, true));
		shape.addGeometry(makeLine(p[2], p[6], c, td, 0f, ts, true));
		shape.addGeometry(makeLine(p[3], p[7], c, td, 0f, ts, true));

		shape.setAppearance(createAppearance(color));
		addChild(shape);

		final float fontsize = 2 * tickSize;
		final DecimalFormat df = new DecimalFormat("#.##");

		// x text
		float v = this.min.x + fx;
		Point3f pos =
			new Point3f(v, this.min.y - 1.5f * tickSize, this.min.z - 1.5f * tickSize);
		addText(df.format(v), pos, fontsize, color);
		v = this.min.x + fx + tickDistance;
		pos =
			new Point3f(v, this.min.y - 1.5f * tickSize, this.min.z - 1.5f * tickSize);
		addText(df.format(v), pos, fontsize, color);

		// y text
		v = this.min.y + fy;
		pos =
			new Point3f(this.min.x - 1.5f * tickSize, v, this.min.z - 1.5f * tickSize);
		addText(df.format(v), pos, fontsize, color);
		v = this.min.y + fy + tickDistance;
		pos =
			new Point3f(this.min.x - 1.5f * tickSize, v, this.min.z - 1.5f * tickSize);
		addText(df.format(v), pos, fontsize, color);

		// z text
		v = this.min.z + fz;
		pos =
			new Point3f(this.min.x - 1.5f * tickSize, this.min.y - 1.5f * tickSize, v);
		addText(df.format(v), pos, fontsize, color);
		v = this.min.z + fz + tickDistance;
		pos =
			new Point3f(this.min.x - 1.5f * tickSize, this.min.y - 1.5f * tickSize, v);
		addText(df.format(v), pos, fontsize, color);
	}

	private void addText(final String s, final Point3f pos, final float fontsize,
		final Color3f c)
	{
		final Transform3D translation = new Transform3D();
		translation.rotX(Math.PI);
		translation.setTranslation(new Vector3f(pos));
		final TransformGroup tg = new TransformGroup(translation);
		final OrientedShape3D textShape = new OrientedShape3D();
		textShape.setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_POINT);
		textShape.setAlignmentAxis(0.0f, 1.0f, 0.0f);
		textShape.setRotationPoint(new Point3f(0, 0, 0));
		textShape.setConstantScaleEnable(true);
		final Text2D t2d = new Text2D(s, c, "Helvetica", 24, Font.PLAIN);
		t2d.setRectangleScaleFactor(0.03f);
		textShape.setGeometry(t2d.getGeometry());
		textShape.setAppearance(t2d.getAppearance());

		tg.addChild(textShape);
		addChild(tg);
	}

	private Appearance createAppearance(final Color3f color) {
		final Appearance a = new Appearance();
		final PolygonAttributes pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		a.setPolygonAttributes(pa);

		final ColoringAttributes ca = new ColoringAttributes();
		ca.setColor(color);
		a.setColoringAttributes(ca);

		return a;
	}

	private Geometry makeLine(final Point3f start, final Point3f end,
		final Color3f color, final float tickDistance, final float first,
		final float tickSize, final boolean noTicks)
	{
		final float lineLength = start.distance(end);
		final int nTicks =
			(int) Math.floor((lineLength - first) / tickDistance) + 1;

		final int n = noTicks ? 2 : nTicks * 6 + 2;

		final Point3f[] coords = new Point3f[n];
		int i = 0;
		coords[i++] = start;
		coords[i++] = end;
		if (!noTicks) {
			final Point3f p = new Point3f();
			final Vector3f dir = new Vector3f();
			dir.sub(end, start);
			dir.normalize();
			final float fx = first * dir.x;
			final float fy = first * dir.y;
			final float fz = first * dir.z;
			dir.scale(tickDistance);
			for (int t = 0; t < nTicks; t++) {
				p.x = start.x + fx + t * dir.x;
				p.y = start.y + fy + t * dir.y;
				p.z = start.z + fz + t * dir.z;

				coords[i++] = new Point3f(p.x - tickSize, p.y, p.z);
				coords[i++] = new Point3f(p.x + tickSize, p.y, p.z);
				coords[i++] = new Point3f(p.x, p.y - tickSize, p.z);
				coords[i++] = new Point3f(p.x, p.y + tickSize, p.z);
				coords[i++] = new Point3f(p.x, p.y, p.z - tickSize);
				coords[i++] = new Point3f(p.x, p.y, p.z + tickSize);
			}
		}

		final LineArray ga =
			new LineArray(coords.length, GeometryArray.COORDINATES |
				GeometryArray.COLOR_3);
		ga.setCoordinates(0, coords);
		final Color3f[] col = new Color3f[coords.length];
		for (i = 0; i < col.length; i++)
			col[i] = color;
		ga.setColors(0, col);
		return ga;
	}

	@Override
	public String toString() {
		return "[BoundingBox (" + min.x + ", " + min.y + ", " + min.z + ") - (" +
			max.x + ", " + max.y + ", " + max.z + ")]";
	}
}
