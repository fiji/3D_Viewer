
package customnode;

import org.scijava.vecmath.Point3f;

public abstract class Primitive extends CustomIndexedTriangleMesh {

	public Primitive(final Point3f[] vertices, final int[] faces) {
		super(vertices, faces);
	}
}
