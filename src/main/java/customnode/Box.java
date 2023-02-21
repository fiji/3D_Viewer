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

import org.scijava.vecmath.Point3f;

public class Box extends Primitive {

	public Box(final Point3f min, final Point3f max) {
		super(makeVertices(min, max), makeFaces());
	}

	private static Point3f[] makeVertices(final Point3f min, final Point3f max) {
		final Point3f[] p = new Point3f[8];
		p[0] = new Point3f(min.x, min.y, max.z);
		p[1] = new Point3f(max.x, min.y, max.z);
		p[2] = new Point3f(max.x, max.y, max.z);
		p[3] = new Point3f(min.x, max.y, max.z);
		p[4] = new Point3f(min.x, min.y, min.z);
		p[5] = new Point3f(max.x, min.y, min.z);
		p[6] = new Point3f(max.x, max.y, min.z);
		p[7] = new Point3f(min.x, max.y, min.z);
		return p;
	}

	private static int[] makeFaces() {
		return new int[] { 0, 1, 2, 0, 2, 3, // back
			4, 7, 6, 4, 6, 5, // front
			1, 5, 6, 1, 6, 2, // right
			0, 3, 7, 0, 7, 4, // left
			0, 4, 5, 0, 5, 1, // top
			7, 3, 2, 7, 2, 6 // bottom
		};
	}
}
