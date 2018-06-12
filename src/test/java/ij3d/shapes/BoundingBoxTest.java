package ij3d.shapes;

import org.junit.Test;
import org.scijava.vecmath.Point3d;

public class BoundingBoxTest
{
	@Test
	public void canCreateWithNoRange()
	{
		// This used to cause a negative array size exception.
		// It is relevant when adding content that is a single
		// coordinate, e.g. a CustomPointMesh with 1 point.
		double d = Double.parseDouble("-25.632442551701516");
		Point3d min = new Point3d(d, d, d);
		new BoundingBox(min, min);
	}
}