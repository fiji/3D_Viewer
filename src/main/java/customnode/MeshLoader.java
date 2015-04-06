
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
