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

import java.util.List;

import org.scijava.vecmath.Matrix4f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

public class Tube extends Primitive {

	public static final int DEFAULT_PARALLELS = 12;

	public Tube(final List<Point3f> points, final float r) {
		this(points, r, DEFAULT_PARALLELS);
	}

	public Tube(final List<Point3f> points, final float r, final int parallels) {
		super(makeVertices(points, r, parallels), makeFaces(points.size(),
			parallels));
	}

	private static Point3f[] makeVertices(final List<Point3f> points,
		final float r, final int parallels)
	{
		final Point3f[] p = new Point3f[points.size() * parallels];

		// first set of parallels
		Point3f p0 = points.get(0);
		Point3f p1 = points.get(1);
		for (int i = 0; i < parallels; i++) {
			final double a = (i - 6) * (2 * Math.PI) / 12;
			final double c = r * Math.cos(a);
			final double s = r * Math.sin(a);
			p[i] = new Point3f((float) c, (float) s, 0);
		}
		Matrix4f ry = new Matrix4f();
		float ay = (float) Math.atan2((p1.x - p0.x), (p1.z - p0.z));
		ry.rotY(ay);

		Matrix4f rx = new Matrix4f();
		float ax = -(float) Math.asin((p1.y - p0.y) / p1.distance(p0));
		rx.rotX(ax);

		rx.mul(ry, rx);
		for (int i = 0; i < parallels; i++) {
			final Point3f pi = p[i];
			rx.transform(pi);
			pi.add(p0);
		}

		// between
		Point3f p2;
		for (int pi = 1; pi < points.size() - 1; pi++) {
			p0 = points.get(pi - 1);
			p1 = points.get(pi);
			p2 = points.get(pi + 1);

			final Vector3f p0p1 = new Vector3f();
			p0p1.sub(p1, p0);
			final Vector3f p1p2 = new Vector3f();
			p1p2.sub(p2, p1);
			p0p1.normalize();
			p1p2.normalize();
			final Vector3f plane = new Vector3f();
			plane.add(p0p1);
			plane.add(p1p2);
			plane.normalize();

			final Vector3f transl = new Vector3f();
			transl.sub(p1, p0);

			// project onto plane
			for (int i = 0; i < parallels; i++) {
				final int idx0 = ((pi - 1) * parallels + i);
				final int idx1 = (pi * parallels + i);
				p[idx1] = new Point3f(p[idx0]);
				p[idx1].add(transl);
				p[idx1] = intersect(p[idx0], p[idx1], plane, p1);
			}
		}

		// last set of parallels
		p0 = points.get(points.size() - 2);
		p1 = points.get(points.size() - 1);
		final int offset = (points.size() - 1) * parallels;
		for (int i = 0; i < parallels; i++) {
			final double a = (i - 6) * (2 * Math.PI) / 12;
			final double c = r * Math.cos(a);
			final double s = r * Math.sin(a);
			p[offset + i] = new Point3f((float) c, (float) s, 0);
		}
		ry = new Matrix4f();
		ay = (float) Math.atan2((p1.x - p0.x), (p1.z - p0.z));
		ry.rotY(ay);

		rx = new Matrix4f();
		ax = -(float) Math.asin((p1.y - p0.y) / p1.distance(p0));
		rx.rotX(ax);

		rx.mul(ry, rx);
		for (int i = 0; i < parallels; i++) {
			final Point3f pi = p[offset + i];
			rx.transform(pi);
			pi.add(p1);
		}
		return p;
	}

	private static int[] makeFaces(final int nPts, final int parallels) {
		int idx = 0;
		final int[] faces = new int[2 * 3 * parallels * (nPts - 1)];
		for (int pi = 0; pi < nPts - 1; pi++) {
			final int offs0 = pi * parallels;
			final int offs1 = (pi + 1) * parallels;
			for (int i = 0; i < parallels; i++) {
				faces[idx++] = offs0 + i;
				faces[idx++] = offs1 + i;
				faces[idx++] = offs1 + (i + 1) % parallels;
				faces[idx++] = offs0 + i;
				faces[idx++] = offs1 + (i + 1) % parallels;
				faces[idx++] = offs0 + (i + 1) % parallels;
			}
		}
		return faces;
	}

	private static Point3f intersect(final Point3f p1, final Point3f p2,
		final Vector3f n, final Point3f p3)
	{
		// http://paulbourke.net/geometry/planeline/
		final Vector3f v1 = new Vector3f();
		v1.sub(p3, p1);
		final Vector3f v2 = new Vector3f();
		v2.sub(p2, p1);
		final float u = (n.dot(v1)) / (n.dot(v2));
		final Point3f res = new Point3f();
		res.scaleAdd(u, v2, p1);
		return res;
	}
}
