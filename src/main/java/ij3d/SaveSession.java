
package ij3d;

import customnode.CustomMesh;
import customnode.CustomMeshNode;
import customnode.CustomMultiMesh;
import customnode.MeshLoader;
import customnode.WavefrontExporter;
import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.FileInfo;
import ij.io.SaveDialog;

import java.awt.Button;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.media.j3d.Transform3D;
import javax.vecmath.Color3f;

import orthoslice.OrthoGroup;
import surfaceplot.SurfacePlotGroup;

public class SaveSession {

	public static void saveView(final Image3DUniverse univ, final String path)
		throws IOException
	{
		final SaveSession sase = new SaveSession();
		final PrintWriter out = new PrintWriter(new FileWriter(path));
		sase.saveView(out, univ);
		out.close();
	}

	public static void loadView(final Image3DUniverse univ, final String path)
		throws IOException
	{
		final SaveSession sase = new SaveSession();
		final BufferedReader in = new BufferedReader(new FileReader(path));
		final HashMap<String, String> view = sase.readView(in, univ);
		in.close();
		sase.apply(view, univ);
	}

	public static void saveScene(final Image3DUniverse univ, final String path)
		throws IOException
	{
		final SaveSession sase = new SaveSession();
		if (!sase.ensureAllSaved(univ.getContents())) {
			IJ.error("Could not save session");
			return;
		}
		final PrintWriter out = new PrintWriter(new FileWriter(path));
		sase.saveView(out, univ);
		for (final Object c : univ.getContents())
			sase.saveContent(out, (Content) c);
		out.close();
	}

	public static void loadScene(final Image3DUniverse univ, final String path)
		throws IOException
	{
		final BufferedReader in = new BufferedReader(new FileReader(path));
		final SaveSession sase = new SaveSession();
		univ.removeAllContents();
		final HashMap<String, String> view = sase.readView(in, univ);
		final boolean b = univ.getAutoAdjustView();
		Content c = null;
		while ((c = sase.readContent(in)) != null) {
			// TODO
			c.setPointListDialog(univ.getPointListDialog());
			univ.addContent(c);
		}
		in.close();
		sase.apply(view, univ);
	}

	private class CMesh {

		private final CustomMesh mesh;
		private final String name;

		CMesh(final CustomMesh mesh, final String name) {
			this.mesh = mesh;
			this.name = name;
		}
	}

	boolean ensureAllSaved(final Collection<Content> contents) throws IOException
	{
		// go through all contents, make sure that those with
		// images have saved images, and collect the custom
		// meshes file-wise.
		final HashMap<String, ArrayList<CMesh>> custommeshes =
			new HashMap<String, ArrayList<CMesh>>();

		final ArrayList<String> unsavedImages = new ArrayList<String>();
		for (final Content content : contents) {
			for (final ContentInstant c : content.getInstants().values()) {
				final int t = c.getType();
				if (t != ContentConstants.CUSTOM) {
					final FileInfo fi = c.getImage().getOriginalFileInfo();
					if (fi == null || c.image.changes) unsavedImages.add(c.image
						.getTitle());
					continue;
				}
				final CustomMeshNode cn = (CustomMeshNode) c.getContent();
				final ArrayList<CustomMesh> meshes = getMeshes(cn);
				int i = -1;
				for (final CustomMesh cm : meshes) {
					i++;
					final String file = cm.getFile();
					final boolean changed = file == null || cm.hasChanged();
					if (!changed) continue;
					if (!custommeshes.containsKey(file)) custommeshes.put(file,
						new ArrayList<CMesh>());
					String name = cm.getName();
					if (name == null) name = c.getName();
					if (meshes.size() > 1) // it's a multimesh; make sure the name is
																	// unique
					name += "-multimesh" + i;
					custommeshes.get(file).add(new CMesh(cm, name));
				}
			}
		}

		// show the user a dialog with all changed images and ask them
		// to save them all
		if (!unsavedImages.isEmpty()) {
			final StringBuilder sb = new StringBuilder();
			sb.append("Error.\n").append("The following images have unsaved ")
				.append("changes:\n  \n");
			for (final String s : unsavedImages)
				sb.append("  ").append(s).append("\n");
			sb.append("  \nPlease save them separately ").append(
				"before calling 'Save session' again");

			IJ.error(sb.toString());
			return false;
		}

		// ask user to save all meshes with no file in single new file
		if (custommeshes.containsKey(null)) {
			final ArrayList<CMesh> meshes = custommeshes.get(null);
			if (meshes != null && !saveObj(meshes)) {
				IJ.error("Error saving session");
				return false;
			}
			custommeshes.remove(null);
		}

		if (custommeshes.isEmpty()) return true;

		// and the other files to where they came from
		final StringBuilder sb = new StringBuilder();
		sb.append("The following mesh(es) were loaded from file \n");
		sb.append("but changed. Save them to their original\n");
		sb.append("file(s)?\n  \n");
		for (final String file : custommeshes.keySet())
			for (final CMesh m : custommeshes.get(file))
				sb.append("  " + m.name + "\n");
		sb.append("  \n");
		final GenericDialog gd = new GenericDialog("");
		gd.addMessage(sb.toString());
		gd.showDialog();
		if (gd.wasCanceled()) return false;

		for (final String file : custommeshes.keySet()) {
			final ArrayList<CMesh> meshes = custommeshes.get(file);
			if (meshes == null) continue;
			if (!updateObj(meshes, file)) return false;
		}
		return true;
	}

	static boolean updateObj(final ArrayList<CMesh> meshes, final String path)
		throws IOException
	{
		final Map<String, CustomMesh> prev = MeshLoader.load(path);
		for (final CMesh m : meshes)
			prev.put(m.name, m.mesh);

		try {
			WavefrontExporter.save(prev, path);
			return true;
		}
		catch (final IOException e) {
			e.printStackTrace();
			IJ.error(e.getMessage());
			return false;
		}
	}

	static boolean saveObj(final ArrayList<CMesh> meshes) {
		final StringBuilder sb = new StringBuilder();
		sb.append("The following surfaces are not saved:\n");
		for (final CMesh m : meshes)
			sb.append("  " + m.name + "\n");
		sb.append("Select a path below to save them");

		final String path = showPathDialog("Save meshes", sb.toString());
		if (path == null) return false;

		final HashMap<String, CustomMesh> m2w = new HashMap<String, CustomMesh>();
		for (final CMesh m : meshes)
			m2w.put(m.name, m.mesh);

		try {
			WavefrontExporter.save(m2w, path);
			return true;
		}
		catch (final IOException e) {
			e.printStackTrace();
			IJ.error(e.getMessage());
			return false;
		}
	}

	static String showPathDialog(final String title, final String msg) {
		final GenericDialog gd = new GenericDialog(title);
		gd.addMessage(msg);
		final Panel p = new Panel(new FlowLayout());
		final TextField tf = new TextField(30);
		p.add(tf);
		final Button b = new Button("...");
		p.add(b);
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final SaveDialog sd = new SaveDialog("Select path", "untitled", ".obj");
				final String dir = sd.getDirectory();
				final String file = sd.getFileName();
				final File f = new File(dir, file);
				tf.setText(f.getAbsolutePath());
			}
		});
		gd.addPanel(p);
		gd.showDialog();
		if (gd.wasCanceled()) return null;
		return new File(tf.getText()).getAbsolutePath();
	}

	void saveView(final PrintWriter out, final Image3DUniverse univ)
		throws IOException
	{
		out.println("BeginView");
		final Transform3D t3d = new Transform3D();
		univ.getCenterTG().getTransform(t3d);
		out.println("center = " + toString(t3d));
		univ.getTranslateTG().getTransform(t3d);
		out.println("translate = " + toString(t3d));
		univ.getRotationTG().getTransform(t3d);
		out.println("rotate = " + toString(t3d));
		univ.getZoomTG().getTransform(t3d);
		out.println("zoom = " + toString(t3d));
		univ.getAnimationTG().getTransform(t3d);
		out.println("animate = " + toString(t3d));
		out.println("EndView");
	}

	HashMap<String, String> readView(final BufferedReader in,
		final Image3DUniverse univ) throws IOException
	{
		String line;
		boolean foundNext = false;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("BeginView")) {
				foundNext = true;
				break;
			}
		}
		if (!foundNext) return null;

		final HashMap<String, String> props = new HashMap<String, String>();
		while ((line = in.readLine()) != null) {
			if (line.startsWith("EndView")) break;
			if (line.startsWith("#")) continue;
			final String[] keyval = line.split("=");
			props.put(keyval[0].trim(), keyval[1].trim());
		}
		return props;
	}

	public void apply(final HashMap<String, String> props,
		final Image3DUniverse univ)
	{
		String tmp;
		// Set up new Content
		if ((tmp = props.get("center")) != null) univ.getCenterTG().setTransform(
			t(tmp));
		if ((tmp = props.get("translate")) != null) univ.getTranslateTG()
			.setTransform(t(tmp));
		if ((tmp = props.get("rotate")) != null) univ.getRotationTG().setTransform(
			t(tmp));
		if ((tmp = props.get("zoom")) != null) univ.getZoomTG()
			.setTransform(t(tmp));
		if ((tmp = props.get("animate")) != null) univ.getAnimationTG()
			.setTransform(t(tmp));

		univ.getViewPlatformTransformer().updateFrontBackClip();
	}

	void saveContent(final PrintWriter out, final Content c) {
		out.println("BeginContent");
		out.println("name = " + c.getName());
		for (final ContentInstant ci : c.getInstants().values())
			saveContentInstant(out, ci);
		out.println("EndContent");
	}

	void saveContentInstant(final PrintWriter out, final ContentInstant c) {
		// color string
		final String col =
			c.color == null ? null : Integer.toString(c.color.get().getRGB());
		// channel string
		final String chan =
			c.channels[0] + "%%%" + c.channels[1] + "%%%" + c.channels[2];
		// transformations
		final Transform3D t = new Transform3D();
		c.getLocalRotate(t);
		final String rot = toString(t);
		c.getLocalTranslate(t);
		final String trans = toString(t);

		out.println("BeginContentInstant");
		out.println("name = " + c.getName());
		if (col != null) out.println("color = " + col);
		out.println("timepoint = " + c.timepoint);
		out.println("channels = " + chan);
		out.println("transparency = " + c.transparency);
		out.println("threshold = " + c.threshold);
		out.println("resampling = " + c.resamplingF);
		out.println("type = " + c.type);
		out.println("locked = " + c.isLocked());
		out.println("shaded = " + c.shaded);
		out.println("visible = " + c.isVisible());
		out.println("coordVisible = " + c.hasCoord());
		out.println("plVisible = " + c.isPLVisible());
		out.println("rotation = " + rot);
		out.println("translation = " + trans);
		if (c.image != null) out.println("imgfile = " + getImageFile(c));

		final int type = c.getType();
		final ContentNode cn = c.getContent();
		if (type == ContentConstants.SURFACE_PLOT2D) {
			out.println("surfplt = " + ((SurfacePlotGroup) cn).getSlice());
		}
		else if (type == ContentConstants.ORTHO) {
			out.println("ortho = " + getOrthoString(cn));
		}
		else if (type == ContentConstants.CUSTOM) {
			out.println("surffiles = " + getMeshString(c));
		}
		out.println("EndContentInstant");
	}

	public Content readContent(final BufferedReader in) throws IOException {
		String name = null;
		String line;
		boolean foundNext = false;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("BeginContent")) {
				foundNext = true;
				break;
			}
		}
		if (!foundNext) return null;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("name")) {
				name = line.split("=")[1].trim();
				break;
			}
		}
		final TreeMap<Integer, ContentInstant> cis =
			new TreeMap<Integer, ContentInstant>();
		ContentInstant ci = null;
		while ((ci = readContentInstant(in)) != null)
			cis.put(ci.timepoint, ci);
		if (name == null) throw new RuntimeException("no name for content");
		return new Content(name, cis);
	}

	public ContentInstant readContentInstant(final BufferedReader in)
		throws IOException
	{
		String line;
		boolean foundNext = false;
		while ((line = in.readLine()) != null) {
			if (line.startsWith("EndContent")) break;
			if (line.startsWith("BeginContentInstant")) {
				foundNext = true;
				break;
			}
		}
		if (!foundNext) return null;

		final HashMap<String, String> props = new HashMap<String, String>();
		while ((line = in.readLine()) != null) {
			if (line.startsWith("EndContentInstant")) break;
			if (line.startsWith("#")) continue;
			final String[] keyval = line.split("=");
			props.put(keyval[0].trim(), keyval[1].trim());
		}
		String tmp;
		String[] sp;

		// Set up new Content
		final ContentInstant c = new ContentInstant(props.get("name"));
		if ((tmp = props.get("channels")) != null) {
			sp = tmp.split("%%%");
			c.channels = new boolean[] { b(sp[0]), b(sp[1]), b(sp[2]) };
		}
		if ((tmp = props.get("timepoint")) != null) c.timepoint = i(tmp);
		if ((tmp = props.get("resampling")) != null) c.resamplingF = i(tmp);
		if ((tmp = props.get("rotation")) != null) c.getLocalRotate().setTransform(
			t(tmp));
		if ((tmp = props.get("translation")) != null) c.getLocalTranslate()
			.setTransform(t(tmp));
		final int type = i(props.get("type"));
		if (type != ContentConstants.CUSTOM) {
			c.image = IJ.openImage(props.get("imgfile"));
			if (c.image == null) throw new RuntimeException("Cannot load image: " +
				props.get("imgfile"));
			c.displayAs(type);
			if (type == ContentConstants.SURFACE_PLOT2D &&
				(tmp = props.get("surfplt")) != null)
			{
				((SurfacePlotGroup) c.getContent()).setSlice(i(tmp));
			}
			else if (type == ContentConstants.ORTHO &&
				(tmp = props.get("ortho")) != null)
			{
				final OrthoGroup og = (OrthoGroup) c.getContent();
				sp = tmp.split("%%%");

				int slice = i(sp[0]);
				if (slice == -1) og.setVisible(0, false);
				else og.setSlice(0, slice);

				slice = i(sp[1]);
				if (slice == -1) og.setVisible(1, false);
				else og.setSlice(1, slice);

				slice = i(sp[2]);
				if (slice == -1) og.setVisible(2, false);
				else og.setSlice(2, slice);

			}
		}
		else {
			tmp = props.get("surffiles");
			c.display(createCustomNode(tmp));
		}

		if ((tmp = props.get("color")) != null) c.setColor(new Color3f(new Color(
			i(tmp))));
		if ((tmp = props.get("transparency")) != null) c.setTransparency(f(tmp));
		if ((tmp = props.get("threshold")) != null) c.setThreshold(i(tmp));
		if ((tmp = props.get("shaded")) != null) c.setShaded(b(tmp));
		if ((tmp = props.get("visible")) != null) c.setVisible(b(tmp));
		if ((tmp = props.get("coordVisible")) != null) c
			.showCoordinateSystem(b(tmp));
		if ((tmp = props.get("plVisible")) != null) c.showPointList(b(tmp));
		if ((tmp = props.get("locked")) != null) c.setLocked(b(tmp));

		return c;
	}

	private CustomMeshNode createCustomNode(final String s) {
		final String[] sp = s.split("%%%");
		if (sp.length == 2) {
			final Map<String, CustomMesh> meshes = MeshLoader.load(sp[0]);
			if (meshes == null) {
				IJ.error("Could not load " + sp[0]);
				return null;
			}
			return new CustomMeshNode(meshes.get(sp[1]));
		}

		final HashMap<String, ArrayList<String>> file2name =
			new HashMap<String, ArrayList<String>>();
		for (int i = 0; i < sp.length; i += 2) {
			if (!file2name.containsKey(sp[i])) file2name.put(sp[i],
				new ArrayList<String>());
			file2name.get(sp[i]).add(sp[i + 1]);
		}

		final ArrayList<CustomMesh> cms = new ArrayList<CustomMesh>();
		for (final String file : file2name.keySet()) {
			final ArrayList<String> names = file2name.get(file);
			final Map<String, CustomMesh> meshes = MeshLoader.load(file);
			if (meshes == null) {
				IJ.error("Could not load " + file);
				continue;
			}
			for (final String name : names)
				cms.add(meshes.get(name));
		}
		return new CustomMultiMesh(cms);
	}

	private static final int i(final String s) {
		return Integer.parseInt(s);
	}

	private static final boolean b(final String s) {
		return Boolean.parseBoolean(s);
	}

	private static final float f(final String s) {
		return Float.parseFloat(s);
	}

	private static final Transform3D t(final String s) {
		final String[] sp = s.split(" ");
		final float[] f = new float[16];
		for (int i = 0; i < sp.length; i++)
			f[i] = f(sp[i]);
		return new Transform3D(f);
	}

	private static final String toString(final Transform3D t3d) {
		final float[] xf = new float[16];
		t3d.get(xf);
		String ret = "";
		for (int i = 0; i < 16; i++)
			ret += " " + xf[i];
		return ret;
	}

	private static ArrayList<CustomMesh> getMeshes(final CustomMeshNode cn) {
		final ArrayList<CustomMesh> meshes = new ArrayList<CustomMesh>();
		if (cn instanceof CustomMultiMesh) {
			final CustomMultiMesh cmm = (CustomMultiMesh) cn;
			for (int i = 0; i < cmm.size(); i++)
				meshes.add(cmm.getMesh(i));
		}
		else {
			meshes.add(cn.getMesh());
		}
		return meshes;
	}

	private static final String getMeshString(final ContentInstant c) {
		final ArrayList<CustomMesh> meshes =
			getMeshes((CustomMeshNode) c.getContent());
		String ret = "";
		for (final CustomMesh cm : meshes) {
			String name = cm.getName();
			if (name == null) name = c.getName();
			name.replaceAll(" ", "_").replaceAll("#", "--");
			ret += "%%%" + cm.getFile() + "%%%" + name;
		}
		return ret.substring(3, ret.length());
	}

	private static final String getOrthoString(final ContentNode c) {
		final OrthoGroup og = (OrthoGroup) c;
		final int xSlide =
			og.isVisible(AxisConstants.X_AXIS) ? og.getSlice(AxisConstants.X_AXIS)
				: -1;
		final int ySlide =
			og.isVisible(AxisConstants.Y_AXIS) ? og.getSlice(AxisConstants.Y_AXIS)
				: -1;
		final int zSlide =
			og.isVisible(AxisConstants.Z_AXIS) ? og.getSlice(AxisConstants.Z_AXIS)
				: -1;
		return xSlide + "%%%" + ySlide + "%%%" + zSlide;
	}

	private static final String getImageFile(final ContentInstant c) {
		if (c.image == null) return null;
		final FileInfo fi = c.image.getOriginalFileInfo();
		if (fi == null || c.image.changes) throw new RuntimeException(
			"Image not saved");
		return fi.directory + fi.fileName;
	}
}
