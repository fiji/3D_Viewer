/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2016 Fiji developers.
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
/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/* This is a cut-down version of the TrakEM2 VectorString3D class. */

/**

 TrakEM2 plugin for ImageJ(C).
 Copyright (C) 2005, 2006 Albert Cardona and Rodney Douglas.

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU General Public License
 as published by the Free Software Foundation (http://www.gnu.org/licenses/gpl.txt )

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program; if not, write to the Free Software
 Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.

 In addition, as a special exception, the copyright holders give
 you permission to combine this program with free software programs or
 libraries that are released under the Apache Public License.

 You may contact Albert Cardona at acardona at ini.phys.ethz.ch
 Institute of Neuroinformatics, University of Zurich / ETH, Switzerland.
 **/

package ij3d;

// A mini version of Vector3 taken from ini.trakem2.utils.Vector3
class Vector3 {

	double x, y, z;

	public Vector3() {
		this(0, 0, 0);
	}

	public double length() {
		return Math.sqrt(x * x + y * y + z * z);
	}

	public Vector3(final double x, final double y, final double z) {
		this.x = x;
		this.y = y;
		this.z = z;
	}

	public Vector3 normalize(Vector3 r) {
		if (r == null) r = new Vector3();
		final double vlen = length();
		if (vlen != 0.0) {
			return r.set(x / vlen, y / vlen, z / vlen);
		}
		return null;
	}

	public Vector3 scale(final double s, Vector3 r) {
		if (r == null) r = new Vector3();
		return r.set(s * x, s * y, s * z);
	}

	public Vector3 set(final double x, final double y, final double z) {
		this.x = x;
		this.y = y;
		this.z = z;
		return this;
	}

	public void setFrom(final Vector3 other) {
		this.x = other.x;
		this.y = other.y;
		this.z = other.z;
	}

	public Vector3 crossWith(final Vector3 other, Vector3 result) {
		if (result == null) result = new Vector3();
		final double x2 = other.x;
		final double y2 = other.y;
		final double z2 = other.z;
		result.set(y * z2 - z * y2, z * x2 - x * z2, x * y2 - y * x2);
		return result;
	}

	public double dotWith(final Vector3 other) {
		return x * other.x + y * other.y + z * other.z;
	}

	public boolean isZero(final double epsilon) {
		if (Math.abs(x) > epsilon) return false;
		else if (Math.abs(y) > epsilon) return false;
		else if (Math.abs(z) > epsilon) return false;
		else return true;
	}

	@Override
	public String toString() {
		return "(" + x + "," + y + "," + z + ")";
	}
}
