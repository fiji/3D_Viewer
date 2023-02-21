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

package customnode;

import org.scijava.vecmath.Matrix4f;
import org.scijava.vecmath.Point3f;

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
