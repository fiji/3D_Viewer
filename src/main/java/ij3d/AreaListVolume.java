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

package ij3d;

import java.awt.Rectangle;
import java.awt.geom.Area;
import java.util.List;

/**
 * Wraps a list of {@link Area}s to be triangulated.
 *
 * @author Johannes Schindelin
 */
public class AreaListVolume extends Volume {

	private final List<List<Area>> areas;

	public AreaListVolume(final List<List<Area>> areas, final double zSpacing,
		final double xOrigin, final double yOrigin, final double zOrigin)
	{
		super();

		this.areas = areas;

		pw = ph = 1;
		pd = zSpacing;

		minCoord.x = xOrigin;
		minCoord.y = yOrigin;
		minCoord.z = zOrigin;

		final Rectangle bounds = new Rectangle();
		for (final List<Area> list : areas) {
			if (list == null) continue;
			for (final Area area : list) {
				if (area != null) bounds.add(area.getBounds());
			}
		}
		xDim = bounds.width;
		yDim = bounds.height;
		zDim = areas.size();

		maxCoord.x = minCoord.x + xDim * pw;
		maxCoord.y = minCoord.y + yDim * ph;
		maxCoord.z = minCoord.z + zDim * pd;
	}

	public List<List<Area>> getAreas() {
		return areas;
	}

	@Override
	protected void initLoader() {
		// override super.initLoader();
	}

	@Override
	public boolean setAverage(final boolean a) {
		// override super.initLoader();
		return false;
	}

	@Override
	public void setNoCheck(final int x, final int y, final int z, final int v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(final int x, final int y, final int z, final int v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public int load(final int x, final int y, final int z) {
		if (z < 0 || z >= areas.size()) return 0;
		final List<Area> list = areas.get(z);
		if (list == null) return 0;
		for (final Area area : list) {
			if (area.contains(x, y)) return 0xff;
		};
		return 0;
	}

	@Override
	public int getDataType() {
		return BYTE_DATA;
	}

}
