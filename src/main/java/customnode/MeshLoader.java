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

import java.util.Map;

public class MeshLoader {

	public static Map<String, CustomMesh> load(final String file) {
		final String downCased = file.toLowerCase();
		if (downCased.endsWith(".obj")) return loadWavefront(file);
		if (downCased.endsWith(".dxf")) return loadDXF(file);
		if (downCased.endsWith(".stl")) return loadSTL(file);
		return null;
	}

	public static Map<String, CustomMesh> loadWavefront(final String file) {
		try {
			return WavefrontLoader.load(file);
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public static Map<String, CustomMesh> loadDXF(final String file) {
		throw new RuntimeException("Operation not yet implemented");
	}

	public static Map<String, CustomMesh> loadSTL(final String file) {
		try {
			return STLLoader.load(file);
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
		return null;
	}
}
