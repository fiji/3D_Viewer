
package customnode;

import com.sun.j3d.utils.geometry.GeometryInfo;
import com.sun.j3d.utils.geometry.NormalGenerator;

import java.util.Arrays;
import java.util.List;

import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.QuadArray;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

public class CustomQuadMesh extends CustomTriangleMesh {

	public CustomQuadMesh(final List<Point3f> mesh) {
		super(mesh);
	}

	public CustomQuadMesh(final List<Point3f> mesh, final Color3f color,
		final float trans)
	{
		super(mesh, color, trans);
	}

	public void addQuads(final Point3f[] v) {
		if (v.length % 4 != 0) throw new IllegalArgumentException(
			"Number must be a multiple of 4");
		addVertices(v);
	}

	private final Point3f[] fourPoints = new Point3f[4];

	public void addQuad(final Point3f p1, final Point3f p2, final Point3f p3,
		final Point3f p4)
	{
		fourPoints[0] = p1;
		fourPoints[1] = p2;
		fourPoints[2] = p3;
		fourPoints[3] = p4;
		addVertices(fourPoints);
	}

	@Override
	protected GeometryArray createGeometry() {
		if (mesh == null || mesh.size() < 4) return null;
		final List<Point3f> tri = mesh;
		final int nValid = tri.size();
		final int nAll = 2 * nValid;

		final Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		final Color3f colors[] = new Color3f[nValid];
		Arrays.fill(colors, color);

		final GeometryArray ta =
			new QuadArray(nAll, GeometryArray.COORDINATES | GeometryArray.COLOR_3 |
				GeometryArray.NORMALS);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		final GeometryInfo gi = new GeometryInfo(ta);
		// generate normals
		final NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		final GeometryArray result = gi.getGeometryArray();
		result.setValidVertexCount(nValid);

		result.setCapability(GeometryArray.ALLOW_NORMAL_WRITE);
		result.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		result.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		result.setCapability(GeometryArray.ALLOW_COUNT_READ);
		result.setCapability(GeometryArray.ALLOW_FORMAT_READ);
		result.setCapability(Geometry.ALLOW_INTERSECT);

		return result;
	}
}
