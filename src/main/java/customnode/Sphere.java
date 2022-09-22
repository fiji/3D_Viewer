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

import org.scijava.vecmath.Point3f;

public class Sphere extends Primitive {

	public static final int DEFAULT_MERIDIANS = 12;
	public static final int DEFAULT_PARALLELS = 12;

	public Sphere(final Point3f center, final float radius) {
		this(center, radius, DEFAULT_MERIDIANS, DEFAULT_PARALLELS);
	}

	public Sphere(final Point3f center, final float radius, final int meridians,
		final int parallels)
	{
		super(makeVertices(center, radius, meridians, parallels), makeFaces(
			meridians, parallels));
	}

	private static int[] makeFaces(final int meridians, final int parallels) {
		final int lv = (parallels - 2) * meridians + 2;
		final int N = 3 * (meridians + meridians + 2 * (parallels - 1) * meridians);
		final int[] faces = new int[N];
		int i = 0;
		// south pole:
		int offs = 1;
		for (int m = 0; m < meridians; m++) {
			faces[i++] = 0;
			faces[i++] = offs + m; // plus one because of pole
			faces[i++] = offs + ((m + 1) % meridians);
		}
		// middle part:
		for (int p = 1; p < parallels - 2; p++) {
			offs = 1 + (p - 1) * meridians;
			for (int m = 0; m < meridians; m++) {
				final int f1 = offs + m;
				final int f2 = offs + ((m + 1) % meridians);
				final int f3 = offs + parallels + m;
				final int f4 = offs + parallels + ((m + 1) % meridians);
				if (f1 >= lv) throw new RuntimeException("p = " + p + " m = " + m +
					" f1 + " + f1);
				if (f2 >= lv) throw new RuntimeException("p = " + p + " m = " + m +
					" f2 + " + f2);
				if (f3 >= lv) throw new RuntimeException("p = " + p + " m = " + m +
					" f3 + " + f3);
				if (f4 >= lv) throw new RuntimeException("p = " + p + " m = " + m +
					" f4 + " + f4);

				faces[i++] = f1;
				faces[i++] = f3;
				faces[i++] = f4;
				faces[i++] = f1;
				faces[i++] = f4;
				faces[i++] = f2;
			}
		}

		// south pole:
		final int last = parallels * (meridians - 2) + 1;
		final int p = parallels - 2;
		offs = 1 + (p - 1) * meridians;
		for (int m = 0; m < meridians; m++) {
			faces[i++] = offs + m;
			faces[i++] = last;
			faces[i++] = offs + ((m + 1) % meridians);
		}

		return faces;
	}

	private static Point3f[] makeVertices(final Point3f center,
		final float radius, int meridians, int parallels)
	{
		if (meridians < 3) meridians = 3;
		if (parallels < 3) parallels = 3;
		/*
		 * -first loop makes horizontal circle using meridian points.
		 * -second loop scales it appropriately and makes parallels.
		 * Both loops are common for all balls and so should be done just once.
		 * Then this globe can be properly translocated and resized for each ball.
		 */

		// a circle of radius 1
		double da = 2 * Math.PI / meridians;
		final double[][] xy_points = new double[meridians + 1][2]; // plus 1 to
																																// repeat last
																																// point
		xy_points[0][0] = 1; // first point
		xy_points[0][1] = 0;
		for (int m = 1; m < meridians; m++) {
			final double angle = da * m;
			xy_points[m][0] = Math.cos(angle);
			xy_points[m][1] = Math.sin(angle);
		}
		xy_points[xy_points.length - 1][0] = 1; // last point
		xy_points[xy_points.length - 1][1] = 0;

		// Build parallels from circle
		da = Math.PI / parallels; // = 180 / parallels in radians
		final Point3f[] xyz = new Point3f[(parallels - 2) * meridians + 2];
		int i = 0;
		// south pole
		xyz[i++] = new Point3f(0, 0, 1);
		for (int p = 1; p < parallels - 1; p++) {
			final double r = Math.sin(da * p);
			final double Z = Math.cos(da * p);
			for (int mm = 0; mm < meridians; mm++) {
				xyz[i++] =
					new Point3f((float) (xy_points[mm][0] * r),
						(float) (xy_points[mm][1] * r), (float) Z);
			}
		}
		// north pole
		xyz[i++] = new Point3f(0, 0, -1);

		// Scale by radius 'r', and translate to center
		for (final Point3f p : xyz)
			p.scaleAdd(radius, p, center);

		return xyz;
	}
}
