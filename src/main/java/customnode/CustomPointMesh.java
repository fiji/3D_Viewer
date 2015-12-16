
package customnode;

import java.util.Arrays;
import java.util.List;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.Material;
import org.scijava.java3d.PointArray;
import org.scijava.java3d.PointAttributes;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

public class CustomPointMesh extends CustomMesh {

	public static final float DEFAULT_POINT_SIZE = 1f;

	private float pointsize = DEFAULT_POINT_SIZE;

	public CustomPointMesh(final List<Point3f> mesh) {
		super(mesh);
	}

	public CustomPointMesh(final List<Point3f> mesh, final Color3f color,
		final float transparency)
	{
		super(mesh, color, transparency);
	}

	public float getPointSize() {
		return pointsize;
	}

	public void setPointSize(final float pointsize) {
		this.pointsize = pointsize;
		getAppearance().getPointAttributes().setPointSize(pointsize);
	}

	public void setAntiAliasing(final boolean b) {
		getAppearance().getPointAttributes().setPointAntialiasingEnable(b);
	}

	public void addPoints(final Point3f[] v) {
		addVertices(v);
	}

	private final Point3f[] onePoint = new Point3f[1];

	public void addPoint(final Point3f p) {
		onePoint[0] = p;
		addVertices(onePoint);
	}

	@Override
	public float getVolume() {
		return 0;
	}

	@Override
	protected Appearance createAppearance() {
		final Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);
		appearance.setCapability(Appearance.ALLOW_LINE_ATTRIBUTES_READ);

		final PointAttributes pointAttrib = new PointAttributes();
		pointAttrib.setCapability(PointAttributes.ALLOW_ANTIALIASING_WRITE);
		pointAttrib.setCapability(PointAttributes.ALLOW_SIZE_WRITE);
		pointAttrib.setPointSize(pointsize);
		appearance.setPointAttributes(pointAttrib);

		final PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		polyAttrib.setCullFace(PolygonAttributes.CULL_BACK);
		polyAttrib.setBackFaceNormalFlip(false);
		appearance.setPolygonAttributes(polyAttrib);

		final ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		colorAttrib.setColor(color);
		appearance.setColoringAttributes(colorAttrib);

		final TransparencyAttributes tr = new TransparencyAttributes();
		final int mode =
			transparency == 0f ? TransparencyAttributes.NONE
				: TransparencyAttributes.FASTEST;
		tr.setCapability(TransparencyAttributes.ALLOW_VALUE_WRITE);
		tr.setCapability(TransparencyAttributes.ALLOW_MODE_WRITE);
		tr.setTransparencyMode(mode);
		tr.setTransparency(transparency);
		appearance.setTransparencyAttributes(tr);

		final Material material = new Material();
		material.setCapability(Material.ALLOW_COMPONENT_WRITE);
		material.setAmbientColor(0.1f, 0.1f, 0.1f);
		material.setSpecularColor(0.1f, 0.1f, 0.1f);
		material.setDiffuseColor(0.1f, 0.1f, 0.1f);
		appearance.setMaterial(material);
		return appearance;
	}

	@Override
	protected GeometryArray createGeometry() {
		if (mesh == null || mesh.size() == 0) return null;
		final List<Point3f> tri = mesh;
		final int nValid = tri.size();
		final int nAll = 2 * nValid;

		final Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		final Color3f colors[] = new Color3f[nValid];
		Arrays.fill(colors, color);

		GeometryArray ta = null;
		ta =
			new PointArray(nAll, GeometryArray.COORDINATES | GeometryArray.COLOR_3);

		ta.setValidVertexCount(nValid);

		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		ta.setCapability(GeometryArray.ALLOW_COLOR_WRITE);
		ta.setCapability(GeometryArray.ALLOW_COORDINATE_WRITE);
		ta.setCapability(GeometryArray.ALLOW_COUNT_WRITE);
		ta.setCapability(GeometryArray.ALLOW_COUNT_READ);
		ta.setCapability(Geometry.ALLOW_INTERSECT);

		return ta;
	}
}
