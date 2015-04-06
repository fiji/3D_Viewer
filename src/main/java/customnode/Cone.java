
package customnode;

import javax.vecmath.Matrix4f;
import javax.vecmath.Point3f;

public class Cone extends Primitive {

	public static final int DEFAULT_PARALLELS = 12;

	public Cone(final Point3f from, final Point3f to, final float r) {
		this(from, to, r, DEFAULT_PARALLELS);
	}

	public Cone(final Point3f from, final Point3f to, final float r,
		final int parallels)
	{
		super(makeVertices(from, to, r, parallels), makeFaces(parallels));
	}

	private static Point3f[] makeVertices(final Point3f from, final Point3f to,
		final float r, final int parallels)
	{
		final Point3f[] p = new Point3f[parallels + 2];
		p[0] = new Point3f(from);
		p[1] = new Point3f(to);

		for (int i = 0; i < parallels; i++) {
			final double a = (i - 6) * (2 * Math.PI) / 12;
			final double c = r * Math.cos(a);
			final double s = r * Math.sin(a);
			p[i + 2] = new Point3f((float) c, (float) s, 0);
		}
		final Matrix4f ry = new Matrix4f();
		final float ay = (float) Math.atan2((to.x - from.x), (to.z - from.z));
		ry.rotY(ay);

		final Matrix4f rx = new Matrix4f();
		final float ax = -(float) Math.asin((to.y - from.y) / from.distance(to));
		rx.rotX(ax);

		rx.mul(ry, rx);
		for (int i = 2; i < p.length; i++) {
			final Point3f pi = p[i];
			rx.transform(pi);
			pi.add(from);
		}
		return p;
	}

	private static int[] makeFaces(final int parallels) {
		int idx = 0;
		final int[] faces = new int[2 * 3 * parallels];
		for (int i = 0; i < parallels; i++) {
			faces[idx++] = 2 + i;
			faces[idx++] = 2 + (i + 1) % parallels;
			faces[idx++] = 0;
			faces[idx++] = 2 + i;
			faces[idx++] = 1;
			faces[idx++] = 2 + (i + 1) % parallels;
		}
		return faces;
	}
}
