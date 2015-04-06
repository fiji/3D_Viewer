
package customnode;

import java.util.Arrays;
import java.util.List;

import javax.media.j3d.Appearance;
import javax.media.j3d.ColoringAttributes;
import javax.media.j3d.Geometry;
import javax.media.j3d.GeometryArray;
import javax.media.j3d.LineArray;
import javax.media.j3d.LineAttributes;
import javax.media.j3d.LineStripArray;
import javax.media.j3d.Material;
import javax.media.j3d.PolygonAttributes;
import javax.media.j3d.TransparencyAttributes;
import javax.vecmath.Color3f;
import javax.vecmath.Point3f;

public class CustomLineMesh extends CustomMesh {

	public static final int PAIRWISE = 0;
	public static final int CONTINUOUS = 1;

	public static final int SOLID = LineAttributes.PATTERN_SOLID;
	public static final int DOT = LineAttributes.PATTERN_DOT;
	public static final int DASH = LineAttributes.PATTERN_DASH;
	public static final int DASH_DOT = LineAttributes.PATTERN_DASH_DOT;

	public static final int DEFAULT_MODE = CONTINUOUS;
	public static final int DEFAULT_PATTERN = SOLID;
	public static final float DEFAULT_LINEWIDTH = 1.0f;

	private int mode = DEFAULT_MODE;
	private int pattern = DEFAULT_PATTERN;
	private float linewidth = DEFAULT_LINEWIDTH;

	public CustomLineMesh(final List<Point3f> mesh) {
		this(mesh, DEFAULT_MODE);
	}

	public CustomLineMesh(final List<Point3f> mesh, final int mode) {
		this(mesh, mode, DEFAULT_COLOR, 0);
	}

	public CustomLineMesh(final List<Point3f> mesh, final int mode,
		final Color3f color, final float transparency)
	{
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		if (color != null) this.color = color;
		this.mesh = mesh;
		this.mode = mode;
		this.transparency = transparency;
		this.update();
	}

	public int getMode() {
		return mode;
	}

	public void setPattern(final int pattern) {
		this.pattern = pattern;
		getAppearance().getLineAttributes().setLinePattern(pattern);
	}

	public void setAntiAliasing(final boolean b) {
		getAppearance().getLineAttributes().setLineAntialiasingEnable(b);
	}

	public void setLineWidth(final float w) {
		this.linewidth = w;
		getAppearance().getLineAttributes().setLineWidth(w);
	}

	public float getLineWidth() {
		return linewidth;
	}

	public void addLines(final Point3f[] v) {
		if (mode == PAIRWISE && (v.length % 2) != 0) throw new IllegalArgumentException(
			"Even number expected");
		addVertices(v);
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

		final LineAttributes lineAttrib = new LineAttributes();
		lineAttrib.setCapability(LineAttributes.ALLOW_ANTIALIASING_WRITE);
		lineAttrib.setCapability(LineAttributes.ALLOW_PATTERN_WRITE);
		lineAttrib.setCapability(LineAttributes.ALLOW_WIDTH_WRITE);
		lineAttrib.setLineWidth(linewidth);
		lineAttrib.setLinePattern(pattern);
		appearance.setLineAttributes(lineAttrib);

		final PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
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
		if (mesh == null || mesh.size() < 2) return null;
		final List<Point3f> tri = mesh;
		final int nValid = tri.size();
		final int nAll = 2 * nValid;

		final Point3f[] coords = new Point3f[nValid];
		tri.toArray(coords);

		final Color3f colors[] = new Color3f[nValid];
		Arrays.fill(colors, color);

		GeometryArray ta = null;
		if (mode == PAIRWISE) {
			ta =
				new LineArray(nAll, GeometryArray.COORDINATES | GeometryArray.COLOR_3);
			ta.setValidVertexCount(nValid);
		}
		else if (mode == CONTINUOUS) {
			ta =
				new LineStripArray(nAll, GeometryArray.COORDINATES |
					GeometryArray.COLOR_3, new int[] { nValid });
		}

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
