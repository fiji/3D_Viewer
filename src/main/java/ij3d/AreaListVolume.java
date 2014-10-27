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
			for (final Area area: list) {
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
	public boolean setAverage(boolean a) {
		// override super.initLoader();
		return false;
	}

	@Override
	public void setNoCheck(int x, int y, int z, int v) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void set(int x, int y, int z, int v) {
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
