
package customnode;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.GeometryStripArray;
import org.scijava.java3d.Material;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.java3d.utils.geometry.GeometryInfo;
import org.scijava.java3d.utils.geometry.NormalGenerator;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import ij.ImagePlus;
import ij.measure.Calibration;
import ij.plugin.Duplicator;
import ij.process.StackConverter;
import vib.InterpolatedImage;

public abstract class CustomMesh extends Shape3D {

	public static final Color3f DEFAULT_COLOR = new Color3f(0, 1, 0);

	protected Color3f color = DEFAULT_COLOR;
	protected List<Point3f> mesh = null;
	protected float transparency = 0;
	protected boolean shaded = true;

	protected String loadedFromName = null;
	protected String loadedFromFile = null;
	protected boolean changed = false;

	protected CustomMesh() {}

	protected CustomMesh(final List<Point3f> mesh) {
		this(mesh, DEFAULT_COLOR, 0);
	}

	protected CustomMesh(final List<Point3f> mesh, final Color3f color,
		final float transp)
	{
		this.mesh = mesh;
		if (color != null) this.color = color;
		this.transparency = transp;
		this.setCapability(ALLOW_GEOMETRY_READ);
		this.setCapability(ALLOW_GEOMETRY_WRITE);
		this.setCapability(ALLOW_APPEARANCE_READ);
		this.setCapability(ALLOW_APPEARANCE_WRITE);
		this.update();
	}

	public String getFile() {
		return loadedFromFile;
	}

	@Override
	public String getName() {
		return loadedFromName;
	}

	public boolean hasChanged() {
		return changed;
	}

	public void update() {
		this.setGeometry(createGeometry());
		this.setAppearance(createAppearance());
		changed = true;
	}

	public List getMesh() {
		return mesh;
	}

	public Color3f getColor() {
		return color;
	}

	public float getTransparency() {
		return transparency;
	}

	public boolean isShaded() {
		return shaded;
	}

	public void setShaded(final boolean b) {
		this.shaded = b;
		final PolygonAttributes pa = getAppearance().getPolygonAttributes();
		if (b) pa.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
	}

	public void calculateMinMaxCenterPoint(final Point3f min, final Point3f max,
		final Point3f center)
	{

		if (mesh == null || mesh.size() == 0) {
			min.set(0, 0, 0);
			max.set(0, 0, 0);
			center.set(0, 0, 0);
			return;
		}

		min.x = min.y = min.z = Float.MAX_VALUE;
		max.x = max.y = max.z = Float.MIN_VALUE;
		for (int i = 0; i < mesh.size(); i++) {
			final Point3f p = mesh.get(i);
			if (p.x < min.x) min.x = p.x;
			if (p.y < min.y) min.y = p.y;
			if (p.z < min.z) min.z = p.z;
			if (p.x > max.x) max.x = p.x;
			if (p.y > max.y) max.y = p.y;
			if (p.z > max.z) max.z = p.z;
		}
		center.x = (max.x + min.x) / 2;
		center.y = (max.y + min.y) / 2;
		center.z = (max.z + min.z) / 2;
	}

	public abstract float getVolume();

	private final int[] valid = new int[1];

	protected void addVerticesToGeometryStripArray(final Point3f[] v) {
		changed = true;
		mesh.addAll(Arrays.asList(v));

		// check maximum vertex count
		final GeometryStripArray ga = (GeometryStripArray) getGeometry();
		final int max = ga.getVertexCount();
		ga.getStripVertexCounts(valid);
		final int idx = valid[0];
		if (idx + v.length > max) {
			// enlarge arrays
			setGeometry(createGeometry());
			return;
		}

		valid[0] = idx + v.length;
		ga.setStripVertexCounts(valid);

		ga.setCoordinates(idx, v);

		// update colors
		final Color3f[] colors = new Color3f[v.length];
		Arrays.fill(colors, this.color);
		ga.setColors(idx, colors);

		recalculateNormals(ga);
	}

	protected void addVerticesToGeometryArray(final Point3f[] v) {
		changed = true;
		mesh.addAll(Arrays.asList(v));

		// check maximum vertex count
		final GeometryArray ga = (GeometryArray) getGeometry();
		final int max = ga.getVertexCount();
		final int idx = ga.getValidVertexCount();
		if (idx + v.length > max) {
			// enlarge arrays
			setGeometry(createGeometry());
			return;
		}

		ga.setValidVertexCount(idx + v.length);
		ga.setCoordinates(idx, v);

		// update colors
		final Color3f[] colors = new Color3f[v.length];
		Arrays.fill(colors, this.color);
		ga.setColors(idx, colors);

		recalculateNormals(ga);
	}

	public int[] vertexIndicesOfPoint(final Point3f p) {
		final int N = mesh.size();

		final int[] indices = new int[N];
		int i = 0;
		for (int v = 0; v < N; v++)
			if (mesh.get(v) != null && mesh.get(v).equals(p)) indices[i++] = v;

		final int[] ret = new int[i];
		System.arraycopy(indices, 0, ret, 0, i);
		return ret;
	}

	public void setCoordinate(final int i, final Point3f p) {
		changed = true;
		((GeometryArray) getGeometry()).setCoordinate(i, p);
		mesh.get(i).set(p);
	}

	public void setCoordinates(final int[] indices, final Point3f p) {
		changed = true;
		final GeometryArray ga = (GeometryArray) getGeometry();
		for (int i = 0; i < indices.length; i++) {
			ga.setCoordinate(indices[i], p);
			mesh.get(indices[i]).set(p);
		}
	}

	public void recalculateNormals(final GeometryArray ga) {
		if (ga == null) return;
		if ((ga.getVertexFormat() & GeometryArray.NORMALS) == 0) return;
		changed = true;
		final GeometryInfo gi = new GeometryInfo(ga);
		final NormalGenerator ng = new NormalGenerator();
		ng.generateNormals(gi);

		final GeometryArray tmp = gi.getGeometryArray();
		final int v = ga.getValidVertexCount();
		final float[] normals = new float[3 * v];
		tmp.getNormals(0, normals);
		ga.setNormals(0, normals);
	}

	protected void addVertices(final Point3f[] v) {
		if (mesh == null) return;
		changed = true;
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) {
			mesh.addAll(Arrays.asList(v));
			setGeometry(createGeometry());
			return;
		}

		if (ga instanceof GeometryStripArray) addVerticesToGeometryStripArray(v);
		else addVerticesToGeometryArray(v);
	}

	protected void removeVertices(final int[] indices) {
		if (mesh == null) return;

		changed = true;
		for (int i = indices.length - 1; i >= 0; i--) {
			if (indices[i] < 0 || indices[i] >= mesh.size()) continue;
			mesh.remove(indices[i]);
		}
		setGeometry(createGeometry());
	}

	public void setColor(final Color3f color) {
		this.color = color != null ? color : DEFAULT_COLOR;
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;
		final int N = ga.getVertexCount();
		final Color3f colors[] = new Color3f[N];
		for (int i = 0; i < N; i++) {
			colors[i] = this.color;
		}
		ga.setColors(0, colors);
		changed = true;
	}

	public void setColor(final List<Color3f> color) {
		this.color = null;
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;
		final int N = ga.getValidVertexCount();
		if (color.size() != N) throw new IllegalArgumentException("list of size " +
			N + " expected");
		final Color3f[] colors = new Color3f[N];
		color.toArray(colors);
		ga.setColors(0, colors);
		changed = true;
	}

	public void setColor(final int vtxIndex, final Color3f color) {
		this.color = null;
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;
		ga.setColor(vtxIndex, color);
		changed = true;
	}

	public void loadSurfaceColorsFromImage(ImagePlus imp) {
		final GeometryArray ga = (GeometryArray) getGeometry();
		if (ga == null) return;

		if (imp.getType() != ImagePlus.COLOR_RGB) {
			imp = new Duplicator().run(imp);
			new StackConverter(imp).convertToRGB();
		}
		final InterpolatedImage ii = new InterpolatedImage(imp);

		final int N = ga.getValidVertexCount();
		final Color3f[] colors = new Color3f[N];
		final Calibration cal = imp.getCalibration();
		final double pw = cal.pixelWidth;
		final double ph = cal.pixelHeight;
		final double pd = cal.pixelDepth;
		final Point3f coord = new Point3f();
		for (int i = 0; i < N; i++) {
			ga.getCoordinate(i, coord);
			final int v =
				(int) Math.round(ii.interpol.get(coord.x / pw, coord.y / ph, coord.z /
					pd));
			colors[i] =
				new Color3f(((v & 0xff0000) >> 16) / 255f, ((v & 0xff00) >> 8) / 255f,
					(v & 0xff) / 255f);
		}
		ga.setColors(0, colors);
		changed = true;
	}

	public void setTransparency(final float transparency) {
		final TransparencyAttributes ta =
			getAppearance().getTransparencyAttributes();
		if (transparency <= .01f) {
			this.transparency = 0.0f;
			ta.setTransparencyMode(TransparencyAttributes.NONE);
		}
		else {
			this.transparency = transparency;
			ta.setTransparencyMode(TransparencyAttributes.FASTEST);
		}
		ta.setTransparency(this.transparency);
	}

	protected Appearance createAppearance() {
		final Appearance appearance = new Appearance();
		appearance.setCapability(Appearance.ALLOW_TRANSPARENCY_ATTRIBUTES_READ);

		final PolygonAttributes polyAttrib = new PolygonAttributes();
		polyAttrib.setCapability(PolygonAttributes.ALLOW_MODE_WRITE);
		if (this.shaded) polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_FILL);
		else polyAttrib.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		polyAttrib.setCullFace(PolygonAttributes.CULL_NONE);
		polyAttrib.setBackFaceNormalFlip(true);
		appearance.setPolygonAttributes(polyAttrib);

		final ColoringAttributes colorAttrib = new ColoringAttributes();
		colorAttrib.setShadeModel(ColoringAttributes.SHADE_GOURAUD);
		if (null != color) // is null when colors are vertex-wise
		colorAttrib.setColor(color);
		appearance.setColoringAttributes(colorAttrib);

		final TransparencyAttributes tr = new TransparencyAttributes();
		final int mode = TransparencyAttributes.FASTEST;
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

	public void restoreDisplayedData(final String path, final String name) {
		HashMap<String, CustomMesh> contents = null;
		try {
			contents = WavefrontLoader.load(path);
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		if (contents.containsKey(name)) {
			this.mesh = contents.get(name).getMesh();
			update();
		}
	}

	public void swapDisplayedData(final String path, final String name) {
		final HashMap<String, CustomMesh> contents =
			new HashMap<String, CustomMesh>();
		contents.put(name, this);
		try {
			WavefrontExporter.save(contents, path + ".obj");
			this.mesh = null;
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
	}

	public void clearDisplayedData() {
		this.mesh = null;
	}

	protected abstract GeometryArray createGeometry();
}
