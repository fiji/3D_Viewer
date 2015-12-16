
package ij3d;

import java.awt.Button;
import java.awt.Checkbox;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Label;
import java.awt.Panel;
import java.awt.Scrollbar;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.TextEvent;
import java.awt.event.TextListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.JFileChooser;

import org.scijava.java3d.Background;
import org.scijava.java3d.PointLight;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.VirtualUniverse;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Matrix4d;
import org.scijava.vecmath.Matrix4f;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

import customnode.CustomMesh;
import customnode.CustomMeshNode;
import customnode.CustomMultiMesh;
import customnode.CustomTriangleMesh;
import customnode.u3d.U3DExporter;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.YesNoCancelDialog;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;
import ij.io.SaveDialog;
import ij.plugin.frame.Recorder;
import ij.text.TextWindow;
import ij3d.gui.ContentCreatorDialog;
import ij3d.gui.InteractiveMeshDecimation;
import ij3d.gui.InteractiveTransformDialog;
import ij3d.gui.LUTDialog;
import ij3d.gui.PrimitiveDialogs;
import ij3d.shapes.Scalebar;
import ij3d.shortcuts.ShortCutDialog;
import isosurface.MeshEditor;
import isosurface.MeshExporter;
import isosurface.MeshGroup;
import isosurface.SmoothControl;
import math3d.TransformIO;
import orthoslice.MultiOrthoGroup;
import orthoslice.OrthoGroup;
import vib.FastMatrix;
import voltex.VoltexGroup;

public class Executer {

	// These strings are the names of the static methods in
	// ImageJ3DViewer.
	public static final String START_ANIMATE = "startAnimate";
	public static final String STOP_ANIMATE = "stopAnimate";
	public static final String START_FREEHAND_RECORDING =
		"startFreehandRecording";
	public static final String STOP_FREEHAND_RECORDING = "stopFreehandRecording";
	public static final String RECORD_360 = "record360";
	public static final String RESET_VIEW = "resetView";
	public static final String SCALEBAR = "scalebar";
	public static final String CLOSE = "close";
	public static final String WINDOW_SIZE = "windowSize";

	public static final String SET_COLOR = "setColor";
	public static final String SET_TRANSPARENCY = "setTransparency";
	public static final String SET_CHANNELS = "setChannels";
	public static final String FILL_SELECTION = "fillSelection";
	public static final String SET_SLICES = "setSlices";
	public static final String LOCK = "lock";
	public static final String UNLOCK = "unlock";
	public static final String SET_THRESHOLD = "setThreshold";
	public static final String SET_CS = "setCoordinateSystem";
	public static final String SET_TRANSFORM = "setTransform";
	public static final String APPLY_TRANSFORM = "applyTransform";
	public static final String EXPORT_TRANSFORMED = "exportTransformed";
	public static final String SAVE_TRANSFORM = "saveTransform";
	public static final String RESET_TRANSFORM = "resetTransform";
	public static final String IMPORT = "importContent";
	public static final String EXPORT = "exportContent";
	public static final String SNAPSHOT = "snapshot";
	
	public static final String SHOW_CONTENT = "showContent";
	public static final String ADD_SPHERE = "addSphere";
	public static final String ADD_BOX = "addBox";
	public static final String ADD_CONE = "addCone";
	public static final String ADD_TUBE_POINT = "addTubePoint";
	public static final String FINISH_TUBE = "finishTube";

	// TODO
	public static final String ADD = "add";
	public static final String DELETE = "delete";

	public static final String SMOOTH = "smooth";

	private final Image3DUniverse univ;

	public Executer(final Image3DUniverse univ) {
		this.univ = univ;
	}

	/* **********************************************************
	 * File menu
	 * *********************************************************/

	public void addContentFromFile() {
		final JFileChooser chooser =
			new JFileChooser(OpenDialog.getLastDirectory());
		chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		chooser.setMultiSelectionEnabled(false);
		final int returnVal = chooser.showOpenDialog(null);
		if (returnVal != JFileChooser.APPROVE_OPTION) return;
		final File f = chooser.getSelectedFile();
		OpenDialog.setLastDirectory(f.getParentFile().getAbsolutePath());
		addContent(null, f);
	}

	public void addContentFromImage(final ImagePlus image) {
		addContent(image, null);
	}

	public void addTimelapseFromFile() {
		addContentFromFile();
	}

	public void addTimelapseFromFolder() {
		final DirectoryChooser dc = new DirectoryChooser("Open from folder");
		final String dir = dc.getDirectory();
		if (dir == null) return;
		final File d = new File(dir);
		if (d.exists()) addContent(null, d);
		else IJ.error("Cannot load " + d.getAbsolutePath());
	}

	public void addTimelapseFromHyperstack(final ImagePlus image) {
		addContentFromImage(image);
	}

	public void addContent(final ImagePlus image, final File file) {
		new Thread() {

			{
				setPriority(Thread.NORM_PRIORITY);
			}

			@Override
			public void run() {
				addC(image, file);
			}
		}.start();
	}

	private Content addC(final ImagePlus image, final File file) {
		final ContentCreatorDialog gui = new ContentCreatorDialog();
		final Content c = gui.showDialog(univ, image, file);
		if (c == null) return null;

		univ.addContent(c);

		// record
		final String title =
			gui.getFile() != null ? gui.getFile().getAbsolutePath() : gui.getImage()
				.getTitle();
		final boolean[] channels = gui.getChannels();
		final String[] arg =
			new String[] { title, ColorTable.getColorName(gui.getColor()),
				gui.getName(), Integer.toString(gui.getThreshold()),
				Boolean.toString(channels[0]), Boolean.toString(channels[1]),
				Boolean.toString(channels[2]),
				Integer.toString(gui.getResamplingFactor()),
				Integer.toString(gui.getType()) };
		record(ADD, arg);

		return c;
	}

	public void delete(final Content c) {
		if (!checkSel(c)) return;
		univ.removeContent(c.getName());
		record(DELETE);
	}

// 	public void loadOctree() {
// 		OctreeDialog od = new OctreeDialog();
// 		od.showDialog();
// 		if(!od.checkUserInput())
// 			return;
// 		String dir = od.getImageDir();
// 		String name = od.getName();
// 		String path = od.getImagePath();
// 		if(od.shouldCreateData()) {
// 			try {
// 				new FilePreparer(path, VolumeOctree.SIZE, dir).createFiles();
// 			} catch(Exception e) {
// 				IJ.error(e.getMessage());
// 				e.printStackTrace();
// 				return;
// 			}
// 		}
// 		univ.addOctree(dir, name);
// 	}
//
// 	public void removeOctree() {
// 		univ.removeOctree();
// 	}

	protected void importFile(final String dialogTitle, final String extension,
		final String formatDescription)
	{
		final OpenDialog od =
			new OpenDialog(dialogTitle, OpenDialog.getDefaultDirectory(), null);
		final String filename = od.getFileName();
		if (null == filename) return;
		if (!filename.toLowerCase().endsWith(extension)) {
			IJ.showMessage("Must select a " + formatDescription + " file!");
			return;
		}
		final String path =
			new StringBuilder(od.getDirectory()).append(filename).toString();
		IJ.log("path: " + path);
		Object ob;
		try {
			ob = univ.addContentLater(path);
			record(IMPORT, path);
		}
		catch (final Exception e) {
			e.printStackTrace();
			ob = null;
		}
		if (null == ob) IJ.showMessage("Could not load the file:\n" + path);
	}

	public void importWaveFront() {
		importFile("Select .obj file", ".obj", "wavefront .ob");
	}

	public void importSTL() {
		importFile("Select .stl file", ".stl", "STL");
	}

	public void saveAsDXF() {
		final File dxf_file = promptForFile("Save as DXF", "untitled", ".dxf");
		if (dxf_file == null) return;
		MeshExporter.saveAsDXF(univ.getContents(), dxf_file);
		record(EXPORT, "DXF", dxf_file.getAbsolutePath());
	}

	public void saveAsWaveFront() {
		final File obj_file = promptForFile("Save WaveFront", "untitled", ".obj");
		if (obj_file == null) return;
		MeshExporter.saveAsWaveFront(univ.getContents(), obj_file);
		record(EXPORT, "WaveFront", obj_file.getAbsolutePath());
	}

	public void saveAsAsciiSTL() {
		final File stl_file =
			promptForFile("Save as STL (ASCII)", "untitled", ".stl");
		if (stl_file == null) return;
		MeshExporter.saveAsSTL(univ.getContents(), stl_file, MeshExporter.ASCII);
		record(EXPORT, "STL ASCII", stl_file.getAbsolutePath());
	}

	public void saveAsBinarySTL() {
		final File stl_file =
			promptForFile("Save as STL (binary)", "untitled", ".stl");
		if (stl_file == null) return;
		MeshExporter.saveAsSTL(univ.getContents(), stl_file, MeshExporter.BINARY);
		record(EXPORT, "STL Binary", stl_file.getAbsolutePath());
	}

	public static File promptForFile(final String title, final String suggestion,
		final String ending)
	{
		final SaveDialog sd = new SaveDialog(title, suggestion, ending);
		final String dir = sd.getDirectory();
		if (null == dir) return null;
		String filename = sd.getFileName();
		if (!filename.toLowerCase().endsWith(ending)) filename += ending;

		final File file = new File(dir, filename);
		// check if file exists
		if (!IJ.isMacOSX()) {
			if (file.exists()) {
				final YesNoCancelDialog yn =
					new YesNoCancelDialog(IJ.getInstance(), "Overwrite?", "File  " +
						filename + " exists!\nOverwrite?");
				if (!yn.yesPressed()) return null;
			}
		}

		return file;
	}

	public void saveAsU3D() {
		final SaveDialog sd = new SaveDialog("Save meshes as u3d...", "", ".u3d");
		final String dir = sd.getDirectory();
		final String name = sd.getFileName();
		if (dir == null || name == null) return;
		try {
			U3DExporter.export(univ, dir + name);
			final String tex = U3DExporter.getTexStub(univ, dir + name);
			IJ.log("% Here are a few latex example lines");
			IJ.log("% You can compile them for example via");
			IJ.log("% pdflatex yourfilename.tex");
			IJ.log("");
			IJ.log(tex);
			record(EXPORT, "U3D", dir + name);
		}
		catch (final Exception e) {
			IJ.error(e.getMessage());
		}
	}

	public void loadView() {
		final OpenDialog sd = new OpenDialog("Open view...", "", ".view");
		final String dir = sd.getDirectory();
		final String name = sd.getFileName();
		if (dir == null || name == null) return;
		try {
			univ.loadView(dir + name);
		}
		catch (final Exception e) {
			IJ.error(e.getMessage());
		}
	}

	public void saveView() {
		final SaveDialog sd = new SaveDialog("Save view...", "", ".view");
		final String dir = sd.getDirectory();
		final String name = sd.getFileName();
		if (dir == null || name == null) return;
		try {
			univ.saveView(dir + name);
		}
		catch (final Exception e) {
			IJ.error(e.getMessage());
		}
	}

	public void loadSession() {
		final OpenDialog sd =
			new OpenDialog("Open session...", "session", ".scene");
		final String dir = sd.getDirectory();
		final String name = sd.getFileName();
		if (dir == null || name == null) return;
		new Thread() {

			{
				setPriority(Thread.NORM_PRIORITY);
			}

			@Override
			public void run() {
				try {
					univ.loadSession(dir + name);
				}
				catch (final Exception e) {
					IJ.error(e.getMessage());
				}
			}
		}.start();
	}

	public void saveSession() {
		final SaveDialog sd =
			new SaveDialog("Save session...", "session", ".scene");
		final String dir = sd.getDirectory();
		final String name = sd.getFileName();
		if (dir == null || name == null) return;
		try {
			univ.saveSession(dir + name);
		}
		catch (final Exception e) {
			IJ.error(e.getMessage());
		}
	}

	public void close() {
		univ.close();
		record(CLOSE);
	}

	/* **********************************************************
	 * Edit menu
	 * *********************************************************/
	public void updateVolume(final Content c) {
		if (!checkSel(c)) return;
		if (c.getType() != ContentConstants.VOLUME &&
			c.getType() != ContentConstants.ORTHO) return;
		if (c.getResamplingFactor() != 1) {
			IJ.error("Object must be loaded " + "with resamplingfactor 1");
			return;
		}
		((VoltexGroup) c.getContent()).update();
	}

	public void changeSlices(final Content c) {
		if (!checkSel(c)) return;
		switch (c.getType()) {
			case ContentConstants.ORTHO:
				changeOrthslices(c);
				break;
			case ContentConstants.MULTIORTHO:
				changeMultiOrthslices(c);
				break;
		}
	}

	private void changeMultiOrthslices(final Content c) {
		if (!checkSel(c)) return;
		final GenericDialog gd =
			new GenericDialog("Adjust slices...", univ.getWindow());
		final MultiOrthoGroup os = (MultiOrthoGroup) c.getContent();

		final boolean opaque = os.getTexturesOpaque();

		gd.addMessage("Number of slices {x: " + os.getSliceCount(0) + ", y: " +
			os.getSliceCount(1) + ", z: " + os.getSliceCount(2) + "}");
		gd.addStringField("x_slices (e.g. 1, 2-5, 20)", "", 10);
		gd.addStringField("y_slices (e.g. 1, 2-5, 20)", "", 10);
		gd.addStringField("z_slices (e.g. 1, 2-5, 20)", "", 10);

		gd.addCheckbox("Opaque textures", opaque);

		gd.showDialog();
		if (gd.wasCanceled()) return;

		final int X = AxisConstants.X_AXIS;
		final int Y = AxisConstants.Y_AXIS;
		final int Z = AxisConstants.Z_AXIS;

		final boolean[] xAxis = new boolean[os.getSliceCount(X)];
		final boolean[] yAxis = new boolean[os.getSliceCount(Y)];
		final boolean[] zAxis = new boolean[os.getSliceCount(Z)];

		parseRange(gd.getNextString(), xAxis);
		parseRange(gd.getNextString(), yAxis);
		parseRange(gd.getNextString(), zAxis);

		os.setVisible(X, xAxis);
		os.setVisible(Y, yAxis);
		os.setVisible(Z, zAxis);

		os.setTexturesOpaque(gd.getNextBoolean());
	}

	private static void parseRange(final String rangeString, final boolean[] b) {
		Arrays.fill(b, false);
		if (rangeString.trim().length() == 0) return;
		try {
			final String[] tokens1 = rangeString.split(",");
			for (final String tok1 : tokens1) {
				final String[] tokens2 = tok1.split("-");
				if (tokens2.length == 1) {
					b[Integer.parseInt(tokens2[0].trim())] = true;
				}
				else {
					final int start = Integer.parseInt(tokens2[0].trim());
					final int end = Integer.parseInt(tokens2[1].trim());
					for (int i = start; i <= end; i++) {
						if (i >= 0 && i < b.length) b[i] = true;
					}
				}
			}
		}
		catch (final Exception e) {
			IJ.error("Cannot parse " + rangeString);
			return;
		}
	}

	private void changeOrthslices(final Content c) {
		if (!checkSel(c)) return;
		final GenericDialog gd =
			new GenericDialog("Adjust slices...", univ.getWindow());
		final OrthoGroup os = (OrthoGroup) c.getContent();
		final int ind1 = os.getSlice(AxisConstants.X_AXIS);
		final int ind2 = os.getSlice(AxisConstants.Y_AXIS);
		final int ind3 = os.getSlice(AxisConstants.Z_AXIS);
		final boolean vis1 = os.isVisible(AxisConstants.X_AXIS);
		final boolean vis2 = os.isVisible(AxisConstants.Y_AXIS);
		final boolean vis3 = os.isVisible(AxisConstants.Z_AXIS);
		final ImagePlus imp = c.getImage();
		final int w = imp.getWidth() / c.getResamplingFactor();
		final int h = imp.getHeight() / c.getResamplingFactor();
		final int d = imp.getStackSize() / c.getResamplingFactor();

		gd.addCheckbox("Show_yz plane", vis1);
		gd.addSlider("x coordinate", 0, w - 1, ind1);
		gd.addCheckbox("Show_xz plane", vis2);
		gd.addSlider("y coordinate", 0, h - 1, ind2);
		gd.addCheckbox("Show_xy plane", vis3);
		gd.addSlider("z coordinate", 0, d - 1, ind3);

		gd.addMessage("You can use the x, y and z key plus\n"
			+ "the arrow keys to adjust slices in\n"
			+ "x, y and z direction respectively.\n \n"
			+ "x, y, z + SPACE switches planes on\n" + "and off");

		final int[] dirs =
			new int[] { AxisConstants.X_AXIS, AxisConstants.Y_AXIS,
				AxisConstants.Z_AXIS };
		final Scrollbar[] sl = new Scrollbar[3];
		final Checkbox[] cb = new Checkbox[3];

		for (int k = 0; k < 3; k++) {
			final int i = k;
			sl[i] = (Scrollbar) gd.getSliders().get(i);
			sl[i].addAdjustmentListener(new AdjustmentListener() {

				@Override
				public void adjustmentValueChanged(final AdjustmentEvent e) {
					os.setSlice(dirs[i], sl[i].getValue());
					univ.fireContentChanged(c);
				}
			});

			cb[i] = (Checkbox) gd.getCheckboxes().get(i);
			cb[i].addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(final ItemEvent e) {
					os.setVisible(dirs[i], cb[i].getState());
				}
			});
		}

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(final WindowEvent e) {
				if (gd.wasCanceled()) {
					os.setSlice(AxisConstants.X_AXIS, ind1);
					os.setSlice(AxisConstants.Y_AXIS, ind2);
					os.setSlice(AxisConstants.Z_AXIS, ind3);
					os.setVisible(AxisConstants.X_AXIS, vis1);
					os.setVisible(AxisConstants.Y_AXIS, vis2);
					os.setVisible(AxisConstants.Z_AXIS, vis3);
					univ.fireContentChanged(c);
					return;
				}
				record(SET_SLICES, Integer.toString(sl[0].getValue()), Integer
					.toString(sl[1].getValue()), Integer.toString(sl[2].getValue()));
			}
		});
		gd.showDialog();
	}

	public void fill(final Content c) {
		if (!checkSel(c)) return;
		final int type = c.getType();
		if (type != ContentConstants.VOLUME && type != ContentConstants.ORTHO) return;
		new Thread() {

			{
				setPriority(Thread.NORM_PRIORITY);
			}

			@Override
			public void run() {
				final ImageCanvas3D canvas = (ImageCanvas3D) univ.getCanvas();
				((VoltexGroup) c.getContent()).fillRoi(canvas, canvas.getRoi(),
					(byte) 0);
				univ.fireContentChanged(c);
				record(FILL_SELECTION);
			}
		}.start();
	}

	public void smoothMesh(final Content c) {
		if (!checkSel(c)) return;
		final ContentNode cn = c.getContent();
		// Check multi first; it extends CustomMeshNode
		if (cn instanceof CustomMultiMesh) {
			final CustomMultiMesh multi = (CustomMultiMesh) cn;
			for (int i = 0; i < multi.size(); i++) {
				final CustomMesh m = multi.getMesh(i);
				if (m instanceof CustomTriangleMesh) MeshEditor.smooth2(
					(CustomTriangleMesh) m, 1);
			}
		}
		else if (cn instanceof CustomMeshNode) {
			final CustomMesh mesh = ((CustomMeshNode) cn).getMesh();
			if (mesh instanceof CustomTriangleMesh) MeshEditor.smooth2(
				(CustomTriangleMesh) mesh, 1); // 0.25f);
		}
	}

	public void smoothAllMeshes() {
		// process each Mesh in a separate thread
		final Collection all = univ.getContents();
		final Content[] c = new Content[all.size()];
		all.toArray(c);
		final AtomicInteger ai = new AtomicInteger(0);
		final Thread[] thread =
			new Thread[Runtime.getRuntime().availableProcessors()];
		for (int i = 0; i < thread.length; i++) {
			thread[i] = new Thread() {

				{
					setPriority(Thread.NORM_PRIORITY);
				}

				@Override
				public void run() {
					try {
						for (int k = ai.getAndIncrement(); k < c.length; k =
							ai.getAndIncrement())
						{
							smoothMesh(c[k]);
						}
					}
					catch (final Exception e) {
						e.printStackTrace();
					}
				}
			};
			thread[i].start();
		}
	}

	/** Interactively smooth meshes, with undo. */
	public void smoothControl() {
		new SmoothControl(univ);
	}

	public void decimateMesh() {
		final Content c = univ.getSelected();
		if (c == null) return;
		CustomTriangleMesh ctm;
		final ContentNode n = c.getContent();
		if (n instanceof CustomMeshNode) {
			if (((CustomMeshNode) n).getMesh() instanceof CustomTriangleMesh) ctm =
				(CustomTriangleMesh) ((CustomMeshNode) n).getMesh();
			else return;
		}
		else if (n instanceof MeshGroup) {
			ctm = ((MeshGroup) n).getMesh();
		}
		else {
			return;
		}
		new InteractiveMeshDecimation().run(ctm);
	}

	/* ----------------------------------------------------------
	 * Display As submenu
	 * --------------------------------------------------------*/
	public void displayAs(final Content c, final int type) {
		if (!checkSel(c)) return;
		c.displayAs(type);
	}

	private interface ColorListener {

		public void colorChanged(Color3f color);

		public void ok(GenericDialog gd);
	}

	protected void showColorDialog(final String title, final Color3f oldC,
		final ColorListener colorListener, final boolean showDefaultCheckbox,
		final boolean showTimepointsCheckbox)
	{
		final GenericDialog gd = new GenericDialog(title, univ.getWindow());

		if (showDefaultCheckbox) gd.addCheckbox("Use default color", oldC == null);
		gd.addSlider("Red", 0, 255, oldC == null ? 255 : oldC.x * 255);
		gd.addSlider("Green", 0, 255, oldC == null ? 0 : oldC.y * 255);
		gd.addSlider("Blue", 0, 255, oldC == null ? 0 : oldC.z * 255);

		if (showTimepointsCheckbox) gd.addCheckbox("Apply to all timepoints", true);

		final Scrollbar rSlider = (Scrollbar) gd.getSliders().get(0);
		final Scrollbar gSlider = (Scrollbar) gd.getSliders().get(1);
		final Scrollbar bSlider = (Scrollbar) gd.getSliders().get(2);

		rSlider.setEnabled(oldC != null);
		gSlider.setEnabled(oldC != null);
		bSlider.setEnabled(oldC != null);

		if (showDefaultCheckbox) {
			final Checkbox cBox = (Checkbox) gd.getCheckboxes().get(0);
			cBox.addItemListener(new ItemListener() {

				@Override
				public void itemStateChanged(final ItemEvent e) {
					gd.setCursor(new Cursor(Cursor.WAIT_CURSOR));
					rSlider.setEnabled(!cBox.getState());
					gSlider.setEnabled(!cBox.getState());
					bSlider.setEnabled(!cBox.getState());
					colorListener.colorChanged(new Color3f(rSlider.getValue() / 255f,
						gSlider.getValue() / 255f, bSlider.getValue() / 255f));
					gd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			});
		}

		final AdjustmentListener listener = new AdjustmentListener() {

			@Override
			public void adjustmentValueChanged(final AdjustmentEvent e) {
				colorListener.colorChanged(new Color3f(rSlider.getValue() / 255f,
					gSlider.getValue() / 255f, bSlider.getValue() / 255f));
			}
		};
		rSlider.addAdjustmentListener(listener);
		gSlider.addAdjustmentListener(listener);
		bSlider.addAdjustmentListener(listener);

		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(final WindowEvent e) {
				if (gd.wasCanceled()) colorListener.colorChanged(oldC);
				else {
					gd.setCursor(new Cursor(Cursor.WAIT_CURSOR));
					colorListener.ok(gd);
					gd.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			}
		});
		gd.showDialog();
	}

	/* ----------------------------------------------------------
	 * Attributes submenu
	 * --------------------------------------------------------*/
	public void changeColor(final Content c) {
		if (!checkSel(c)) return;
		final ContentInstant ci = c.getCurrent();
		final Color3f oldC = ci.getColor();
		final ColorListener colorListener = new ColorListener() {

			@Override
			public void colorChanged(final Color3f color) {
				ci.setColor(color);
				univ.fireContentChanged(c);
			}

			@Override
			public void ok(final GenericDialog gd) {
				if (gd.getNextBoolean()) record(SET_COLOR, "null", "null", "null");
				else record(SET_COLOR, "" + (int) gd.getNextNumber(), "" +
					(int) gd.getNextNumber(), "" + (int) gd.getNextNumber());

				// gd.wasOKed: apply to all time points
				if (gd.getNextBoolean()) c.setColor(ci.getColor());
				univ.fireContentChanged(c);
			}
		};
		showColorDialog("Change color...", oldC, colorListener, true, true);
	}

	/** Adjust the background color in place. */
	public void changeBackgroundColor() {
		final Background background = ((ImageCanvas3D) univ.getCanvas()).getBG();
		final Label status = univ.getWindow().getStatusLabel();
		final Color3f oldC = new Color3f();
		background.getColor(oldC);

		final ColorListener colorListener = new ColorListener() {

			@Override
			public void colorChanged(final Color3f color) {
				background.setColor(color);
				status.setBackground(color.get());
				((ImageCanvas3D) univ.getCanvas()).render();
			}

			@Override
			public void ok(final GenericDialog gd) {
				// TODO macro record
			}
		};
		showColorDialog("Adjust background color ...", oldC, colorListener, false,
			false);
	}

	public void changePointColor(final Content c) {
		if (!checkSel(c)) return;
		final ContentInstant ci = c.getCurrent();
		final Color3f oldC = ci.getLandmarkColor();
		final ColorListener colorListener = new ColorListener() {

			@Override
			public void colorChanged(final Color3f color) {
				ci.setLandmarkColor(color);
				univ.fireContentChanged(c);
			}

			@Override
			public void ok(final GenericDialog gd) {
				// TODO: record
				// gd.wasOKed: apply to all time points
				if (gd.getNextBoolean()) c.setLandmarkColor(ci.getLandmarkColor());
				univ.fireContentChanged(c);
			}
		};
		showColorDialog("Change point color...", oldC, colorListener, false, true);
	}

	public void adjustLUTs(final Content c) {
		if (!checkSel(c)) return;
		final int[] r = new int[256];
		c.getRedLUT(r);
		final int[] g = new int[256];
		c.getGreenLUT(g);
		final int[] b = new int[256];
		c.getBlueLUT(b);
		final int[] a = new int[256];
		c.getAlphaLUT(a);

		final LUTDialog ld = new LUTDialog(r, g, b, a);
		ld.addCtrlHint();

		ld.addListener(new LUTDialog.Listener() {

			@Override
			public void applied() {
				c.setLUT(r, g, b, a);
				univ.fireContentChanged(c);
			}
		});
		ld.showDialog();

		// TODO record
	}

	public void changeChannels(final Content c) {
		if (!checkSel(c)) return;
		final ContentInstant ci = c.getCurrent();
		final GenericDialog gd =
			new GenericDialog("Adjust channels ...", univ.getWindow());
		gd.addMessage("Channels");
		gd.addCheckboxGroup(1, 3, new String[] { "red", "green", "blue" }, ci
			.getChannels());
		gd.addCheckbox("Apply to all timepoints", true);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		final boolean[] channels =
			new boolean[] { gd.getNextBoolean(), gd.getNextBoolean(),
				gd.getNextBoolean() };
		if (gd.getNextBoolean()) c.setChannels(channels);
		else ci.setChannels(channels);
		univ.fireContentChanged(c);
		record(SET_CHANNELS, Boolean.toString(channels[0]), Boolean
			.toString(channels[1]), Boolean.toString(channels[2]));
	}

	public void changeTransparency(final Content c) {
		if (!checkSel(c)) return;
		final ContentInstant ci = c.getCurrent();
		final SliderAdjuster transp_adjuster = new SliderAdjuster() {

			@Override
			public synchronized final void setValue(final ContentInstant ci,
				final int v)
			{
				ci.setTransparency(v / 100f);
				univ.fireContentChanged(c);
			}
		};
		final GenericDialog gd =
			new GenericDialog("Adjust transparency ...", univ.getWindow());
		final int oldTr = (int) (ci.getTransparency() * 100);
		gd.addSlider("Transparency", 0, 100, oldTr);
		gd.addCheckbox("Apply to all timepoints", true);

		((Scrollbar) gd.getSliders().get(0))
			.addAdjustmentListener(new AdjustmentListener() {

				@Override
				public void adjustmentValueChanged(final AdjustmentEvent e) {
					if (!transp_adjuster.go) transp_adjuster.start();
					transp_adjuster.exec(e.getValue(), ci, univ);
				}
			});
		((TextField) gd.getNumericFields().get(0))
			.addTextListener(new TextListener() {

				@Override
				public void textValueChanged(final TextEvent e) {
					if (!transp_adjuster.go) transp_adjuster.start();
					final TextField input = (TextField) e.getSource();
					final String text = input.getText();
					try {
						final int value = Integer.parseInt(text);
						transp_adjuster.exec(value, ci, univ);
					}
					catch (final Exception exception) {
						// ignore intermediately invalid number
					}
				}
			});
		final Checkbox aBox = (Checkbox) (gd.getCheckboxes().get(0));
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(final WindowEvent e) {
				if (null != transp_adjuster) transp_adjuster.quit();
				if (gd.wasCanceled()) {
					final float newTr = oldTr / 100f;
					ci.setTransparency(newTr);
					univ.fireContentChanged(c);
					return;
				}
				// apply to all instants of the content
				if (aBox.getState()) c.setTransparency(ci.getTransparency());

				record(SET_TRANSPARENCY, Float.toString(((Scrollbar) gd.getSliders()
					.get(0)).getValue() / 100f));
			}
		});
		gd.showDialog();
	}

	public void changeThreshold(final Content c) {
		if (!checkSel(c)) return;
		if (c.getImage() == null) {
			IJ.error("The selected object contains no image data,\n"
				+ "therefore the threshold can't be changed");
			return;
		}
		final ContentInstant ci = c.getCurrent();
		final SliderAdjuster thresh_adjuster = new SliderAdjuster() {

			@Override
			public synchronized final void setValue(final ContentInstant ci,
				final int v)
			{
				ci.setThreshold(v);
				univ.fireContentChanged(c);
			}
		};
		final int oldTr = (ci.getThreshold());
		if (c.getType() == ContentConstants.SURFACE) {
			final GenericDialog gd =
				new GenericDialog("Adjust threshold ...", univ.getWindow());
			final int old = ci.getThreshold();
			gd.addNumericField("Threshold", old, 0);
			gd.addCheckbox("Apply to all timepoints", true);
			gd.showDialog();
			if (gd.wasCanceled()) return;
			int th = (int) gd.getNextNumber();
			th = Math.max(0, th);
			th = Math.min(th, 255);
			if (gd.getNextBoolean()) c.setThreshold(th);
			else ci.setThreshold(th);
			univ.fireContentChanged(c);
			record(SET_THRESHOLD, Integer.toString(th));
			return;
		}
		// in case we've not a mesh, change it interactively
		final GenericDialog gd = new GenericDialog("Adjust threshold...");
		gd.addSlider("Threshold", 0, 255, oldTr);
		((Scrollbar) gd.getSliders().get(0))
			.addAdjustmentListener(new AdjustmentListener() {

				@Override
				public void adjustmentValueChanged(final AdjustmentEvent e) {
					// start adjuster and request an action
					if (!thresh_adjuster.go) thresh_adjuster.start();
					thresh_adjuster.exec(e.getValue(), ci, univ);
				}
			});
		gd.addCheckbox("Apply to all timepoints", true);
		final Checkbox aBox = (Checkbox) gd.getCheckboxes().get(0);
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(final WindowEvent e) {
				try {
					if (gd.wasCanceled()) {
						ci.setThreshold(oldTr);
						univ.fireContentChanged(c);
						return;
					}
					// apply to other time points
					if (aBox.getState()) c.setThreshold(ci.getThreshold());

					record(SET_THRESHOLD, Integer.toString(c.getThreshold()));
				}
				finally {
					// [ This code block executes even when
					// calling return above ]
					//
					// clean up
					if (null != thresh_adjuster) thresh_adjuster.quit();
				}
			}
		});
		gd.showDialog();
	}

	public void setSaturatedVolumeRendering(final Content c, final boolean b) {
		if (!checkSel(c)) return;
		final int t = c.getType();
		if (t != ContentConstants.VOLUME) return;

		if (c.getNumberOfInstants() == 1) {
			c.setSaturatedVolumeRendering(b);
			return;
		}

		final ContentInstant ci = c.getCurrent();
		final GenericDialog gd = new GenericDialog("Saturated volume rendering");
		gd.addCheckbox("Apply to all timepoints", true);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		if (gd.getNextBoolean()) c.setSaturatedVolumeRendering(b);
		else ci.setSaturatedVolumeRendering(b);
	}

	public void setShaded(final Content c, final boolean b) {
		if (!checkSel(c)) return;
		final int t = c.getType();
		if (t != ContentConstants.SURFACE && t != ContentConstants.SURFACE_PLOT2D &&
			t != ContentConstants.CUSTOM) return;

		if (c.getNumberOfInstants() == 1) {
			c.setShaded(b);
			return;
		}

		final ContentInstant ci = c.getCurrent();
		final GenericDialog gd = new GenericDialog("Set shaded");
		gd.addCheckbox("Apply to all timepoints", true);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		if (gd.getNextBoolean()) c.setShaded(b);
		else ci.setShaded(b);
	}

	public void applySurfaceColors(final Content c) {
		if (!checkSel(c)) return;
		final int t = c.getType();
		if (t != ContentConstants.SURFACE && t != ContentConstants.CUSTOM) return;

		final GenericDialog gd = new GenericDialog("Apply color from image");
		final int[] ids = WindowManager.getIDList();
		final String[] titles = new String[ids.length];
		for (int i = 0; i < ids.length; i++)
			titles[i] = WindowManager.getImage(ids[i]).getTitle();

		gd.addChoice("Color image", titles, titles[0]);
		gd.addCheckbox("Apply to all timepoints", true);
		gd.showDialog();
		if (gd.wasCanceled()) return;

		final ImagePlus colorImage = WindowManager.getImage(gd.getNextChoice());
		if (gd.getNextBoolean()) c.applySurfaceColors(colorImage);
		else if (c.getCurrent() != null) c.getCurrent().applySurfaceColors(
			colorImage);
	}

	/* ----------------------------------------------------------
	 * Hide/Show submenu
	 * --------------------------------------------------------*/
	public void showCoordinateSystem(final Content c, final boolean b) {
		if (!checkSel(c)) return;
		c.showCoordinateSystem(b);
		record(SET_CS, Boolean.toString(b));
	}

	public void showBoundingBox(final Content c, final boolean b) {
		if (!checkSel(c)) return;
		c.showBoundingBox(b);
	}

	public void showContent(final Content c, final boolean b) {
		if (!checkSel(c)) return;
		c.setVisible(b);
		record( SHOW_CONTENT, c.getName(), ""+b );
		if (!b) univ.clearSelection();
	}

	public void showAllCoordinateSystems(final boolean b) {
		for (final Iterator it = univ.contents(); it.hasNext();)
			((Content) it.next()).showCoordinateSystem(b);
	}

	/* ----------------------------------------------------------
	 * Point list submenu
	 * --------------------------------------------------------*/
	public void loadPointList(final Content c) {
		if (!checkSel(c)) return;
		c.loadPointList();
	}

	public void savePointList(final Content c) {
		if (!checkSel(c)) return;
		c.savePointList();
	}

	public void changePointSize(final Content c) {
		if (!checkSel(c)) return;
		final GenericDialog gd = new GenericDialog("Point size", univ.getWindow());
		final float oldS = (c.getLandmarkPointSize());
		final float minS = oldS / 10f;
		final float maxS = oldS * 10f;
		gd.addSlider("Size", minS, maxS, oldS);
		final TextField textField = (TextField) gd.getNumericFields().get(0);
		textField.addTextListener(new TextListener() {

			@Override
			public void textValueChanged(final TextEvent e2) {
				try {
					c.setLandmarkPointSize(Float.parseFloat(textField.getText()));
				}
				catch (final NumberFormatException e) {
					// ignore
				}
			}
		});
		((Scrollbar) gd.getSliders().get(0))
			.addAdjustmentListener(new AdjustmentListener() {

				@Override
				public void adjustmentValueChanged(final AdjustmentEvent e) {
					final float newS = Float.parseFloat(textField.getText());
					c.setLandmarkPointSize(newS);
				}
			});
		gd.setModal(false);
		gd.addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosed(final WindowEvent e) {
				if (gd.wasCanceled()) {
					c.setLandmarkPointSize(oldS);
					return;
				}
			}
		});
		gd.showDialog();
	}

	public void showPointList(final Content c, final boolean b) {
		if (!checkSel(c)) return;
		c.showPointList(b);
	}

	public void register() {
		// Select the contents used for registration
		final Collection contents = univ.getContents();
		if (contents.size() < 2) {
			IJ.error("At least two bodies are " + "required for registration");
			return;
		}
		final RegistrationMenubar rm = univ.getRegistrationMenuBar();
		univ.setMenubar(rm);
		rm.register();
	}

	public void contentProperties(final Content c) {
		if (!checkSel(c)) return;
		final Point3d min = new Point3d();
		final Point3d max = new Point3d();
		final Point3d center = new Point3d();

		c.getContent().getMin(min);
		c.getContent().getMax(max);
		c.getContent().getCenter(center);

		final TextWindow tw =
			new TextWindow(c.getName(), " \tx\ty\tz", "min\t" + (float) min.x + "\t" +
				(float) min.y + "\t" + (float) min.z + "\n" + "max\t" + (float) max.x +
				"\t" + (float) max.y + "\t" + (float) max.z + "\n" + "cog\t" +
				(float) center.x + "\t" + (float) center.y + "\t" + (float) center.z +
				"\n\n" + "volume\t" + c.getContent().getVolume(), 512, 512);
	}

	/* **********************************************************
	 * Select menu
	 * *********************************************************/
	public void select(final String name) {
		if (name == null) {
			univ.select(null);
			return;
		}
		final Content c = univ.getContent(name);
		univ.select(c);
	}

	/* **********************************************************
	 * Transformation menu
	 * *********************************************************/
	public void setLocked(final Content c, final boolean b) {
		if (!checkSel(c)) return;
		c.setLocked(b);
		if (b) record(LOCK);
		else record(UNLOCK);
	}

	public void resetTransform(final Content c) {
		if (!checkSel(c)) return;
		if (c.isLocked()) {
			IJ.error(c.getName() + " is locked");
			return;
		}
		univ.fireTransformationStarted();
		c.setTransform(new Transform3D());
		univ.fireTransformationFinished();
		record(RESET_TRANSFORM);
	}

	@SuppressWarnings("serial")
	public void setTransform(final Content c) {
		if (!checkSel(c)) return;
		if (c.isLocked()) {
			IJ.error(c.getName() + " is locked");
			return;
		}
		final boolean useToFront = univ.getUseToFront();
		univ.setUseToFront(false);

		final Transform3D org = new Transform3D();
		c.getLocalTranslate().getTransform(org);
		final Transform3D t2 = new Transform3D();
		c.getLocalRotate().getTransform(t2);
		org.mul(t2);
		final Matrix4f m = new Matrix4f();
		org.get(m);

		final Point3d contentCenter = new Point3d();
		c.getContent().getCenter(contentCenter);
		final Point3f center = new Point3f(contentCenter);

		new InteractiveTransformDialog("Set transformation", center, m) {

			@Override
			public void transformationUpdated(final Matrix4f mat) {
				univ.fireTransformationStarted();
				c.setTransform(new Transform3D(mat));
				univ.fireTransformationFinished();
			}

			@Override
			public void oked(final Matrix4f mat) {
				final Transform3D t = new Transform3D(mat);
				final float[] v = new float[16];
				t.get(v);
				univ.setUseToFront(useToFront);
				record(SET_TRANSFORM, affine2string(v));
			}

			@Override
			public void canceled() {
				c.setTransform(org);
				univ.setUseToFront(useToFront);
			}
		};
	}

	@SuppressWarnings("serial")
	public void applyTransform(final Content c) {
		if (!checkSel(c)) return;
		if (c.isLocked()) {
			IJ.error(c.getName() + " is locked");
			return;
		}
		final boolean useToFront = univ.getUseToFront();
		univ.setUseToFront(false);

		final Transform3D org = new Transform3D();
		c.getLocalTranslate().getTransform(org);
		final Transform3D t2 = new Transform3D();
		c.getLocalRotate().getTransform(t2);
		org.mul(t2);
		final Matrix4f m = new Matrix4f();
		org.get(m);

		final Matrix4f conc = new Matrix4f();

		final Point3d contentCenter = new Point3d();
		c.getContent().getCenter(contentCenter);
		final Point3f center = new Point3f(contentCenter);

		final Matrix4f init = new Matrix4f();
		init.setIdentity();

		new InteractiveTransformDialog("Set transformation", center, init) {

			@Override
			public void transformationUpdated(final Matrix4f mat) {
				univ.fireTransformationStarted();
				conc.mul(mat, m);
				c.setTransform(new Transform3D(conc));
				univ.fireTransformationFinished();
			}

			@Override
			public void oked(final Matrix4f mat) {
				final Transform3D t = new Transform3D(mat);
				final float[] v = new float[16];
				t.get(v);
				univ.setUseToFront(useToFront);
				record(APPLY_TRANSFORM, affine2string(v));
			}

			@Override
			public void canceled() {
				c.setTransform(org);
				univ.setUseToFront(useToFront);
			}
		};
	}

	public void saveTransform(final Content c) {
		if (!checkSel(c)) return;
		final Transform3D t1 = new Transform3D();
		c.getLocalTranslate().getTransform(t1);
		final Transform3D t2 = new Transform3D();
		c.getLocalRotate().getTransform(t2);
		t1.mul(t2);
		final float[] matrix = new float[16];
		t1.get(matrix);
		if (new TransformIO().saveAffineTransform(matrix)) record(SAVE_TRANSFORM,
			affine2string(matrix));
	}

	public void exportTransformed(final Content c) {
		if (!checkSel(c)) return;
		new Thread() {

			{
				setPriority(Thread.NORM_PRIORITY);
			}

			@Override
			public void run() {
				exportTr(c);
			}
		}.start();
	}

	private void exportTr(final Content c) {
		try {
			c.exportTransformed().show();
			record(EXPORT_TRANSFORMED);
		}
		catch (final Exception e) {
			e.printStackTrace();
			IJ.error(e.getMessage());
		}
	}

	/* **********************************************************
	 * Add menu
	 * *********************************************************/
	public void addTube() {
		PrimitiveDialogs.addTube(univ);
	}

	public void addSphere() {
		PrimitiveDialogs.addSphere(univ);
	}

	public void addCone() {
		PrimitiveDialogs.addCone(univ);
	}

	public void addBox() {
		PrimitiveDialogs.addBox(univ);
	}

	/* **********************************************************
	 * View menu
	 * *********************************************************/
	public void resetView() {
		univ.resetView();
		record(RESET_VIEW);
	}

	public void centerSelected(final Content c) {
		if (!checkSel(c)) return;

		univ.centerSelected(c);
	}

	public void centerUniverse() {
		final Point3d c = new Point3d();
		univ.getGlobalCenterPoint(c);
		univ.centerAt(c);
	}

	public void centerOrigin() {
		univ.centerAt(new Point3d());
	}

	public void fitViewToUniverse() {
		univ.adjustView();
	}

	public void fitViewToContent(final Content c) {
		if (!checkSel(c)) return;
		univ.adjustView(c);
	}

	public void record360() {
		new Thread() {

			@Override
			public void run() {
				final ImagePlus movie = univ.record360();
				if (movie != null) movie.show();
				record(RECORD_360);
			}
		}.start();
	}

	public void startFreehandRecording() {
		univ.startFreehandRecording();
		record(START_FREEHAND_RECORDING);
	}

	public void stopFreehandRecording() {
		final ImagePlus movie = univ.stopFreehandRecording();
		if (movie != null) movie.show();
		record(STOP_FREEHAND_RECORDING);
	}

	public void startAnimation() {
		univ.startAnimation();
		record(START_ANIMATE);
	}

	public void stopAnimation() {
		univ.pauseAnimation();
		record(STOP_ANIMATE);
	}

	public void changeAnimationOptions() {
		final GenericDialog gd = new GenericDialog("Change animation axis");
		final String[] choices = new String[] { "x axis", "y axis", "z axis" };

		final Vector3f axis = new Vector3f();
		univ.getRotationAxis(axis);
		axis.normalize();
		float interval = univ.getRotationInterval();

		int idx = 0;
		if (axis.x == 0 && axis.y == 1 && axis.z == 0) idx = 1;
		if (axis.x == 0 && axis.y == 0 && axis.z == 1) idx = 2;
		gd.addChoice("Rotate around", choices, choices[idx]);
		gd.addNumericField("Rotation interval", interval, 2, 6, "degree");

		gd.showDialog();
		if (gd.wasCanceled()) return;

		idx = gd.getNextChoiceIndex();
		switch (idx) {
			case 0:
				axis.x = 1;
				axis.y = 0;
				axis.z = 0;
				break;
			case 1:
				axis.x = 0;
				axis.y = 1;
				axis.z = 0;
				break;
			case 2:
				axis.x = 0;
				axis.y = 0;
				axis.z = 1;
				break;
		}
		interval = (float) gd.getNextNumber();
		univ.setRotationAxis(axis);
		univ.setRotationInterval(interval);
	}

	public void snapshot() {
		int w = univ.getCanvas().getWidth();
		int h = univ.getCanvas().getHeight();

		final GenericDialog gd = new GenericDialog("Snapshot", univ.getWindow());
		gd.addNumericField("Target_width", w, 0);
		gd.addNumericField("Target_height", h, 0);
		gd.showDialog();
		if (gd.wasCanceled()) return;
		w = (int) gd.getNextNumber();
		h = (int) gd.getNextNumber();

		final Map props = univ.getCanvas().queryProperties();
		final int maxW = (Integer) props.get("textureWidthMax");
		final int maxH = (Integer) props.get("textureHeightMax");

		if (w < 0 || w >= maxW || h < 0 || h >= maxH) {
			IJ.error("Width must be between 0 and " + maxW +
				",\nheight between 0 and " + maxH);
			return;
		}
		univ.takeSnapshot(w, h).show();
		record(SNAPSHOT, Integer.toString(w), Integer.toString(h));
	}

	public void viewPreferences() {
		UniverseSettings.initFromDialog(univ);
	}

	public void editShortcuts() {
		new ShortCutDialog(univ.getShortcuts());
	}

	public void adjustLight() {
		final PointLight l = univ.getLight();
		final Point3f pos = new Point3f();
		final Color3f col = new Color3f();
		l.getPosition(pos);
		l.getColor(col);

		final ColorListener colorListener = new ColorListener() {

			@Override
			public void colorChanged(final Color3f color) {
				l.setColor(color);
			}

			@Override
			public void ok(final GenericDialog gd) {
				// TODO macro record
			}
		};
		showColorDialog("Adjust light", col, colorListener, false, false);
	}

	public void sync(final boolean b) {
		univ.sync(b);
	}

	public void setFullScreen(final boolean b) {
		univ.setFullScreen(b);
	}

	public void editScalebar() {
		final Scalebar sc = univ.getScalebar();
		final GenericDialog gd =
			new GenericDialog("Edit scalebar...", univ.getWindow());
		gd.addNumericField("x position", sc.getX(), 2);
		gd.addNumericField("y position", sc.getY(), 2);
		gd.addNumericField("length", sc.getLength(), 2);
		gd.addStringField("Units", sc.getUnit(), 5);
		gd.addChoice("Color", ColorTable.colorNames, ColorTable.getColorName(sc
			.getColor()));
		gd.addCheckbox("show", univ
			.isAttributeVisible(DefaultUniverse.ATTRIBUTE_SCALEBAR));
		gd.showDialog();
		if (gd.wasCanceled()) return;
		sc.setPosition((float) gd.getNextNumber(), (float) gd.getNextNumber());
		sc.setLength((float) gd.getNextNumber());
		sc.setUnit(gd.getNextString());
		sc.setColor(ColorTable.getColor(gd.getNextChoice()));
		final boolean vis = gd.getNextBoolean();
		univ.showAttribute(DefaultUniverse.ATTRIBUTE_SCALEBAR, vis);
	}

	/* **********************************************************
	 * Help menu
	 * *********************************************************/
	public void j3dproperties() {
		final TextWindow tw =
			new TextWindow("Java 3D Properties", "Key\tValue", "", 512, 512);
		Map props = VirtualUniverse.getProperties();
		tw.append("Java 3D properties\n \n");
		for (final Iterator it = props.entrySet().iterator(); it.hasNext();) {
			final Map.Entry me = (Map.Entry) it.next();
			tw.append(me.getKey() + "\t" + me.getValue());
		}
		props = univ.getCanvas().queryProperties();
		tw.append(" \nRendering properties\n \n");
		for (final Iterator it = props.entrySet().iterator(); it.hasNext();) {
			final Map.Entry me = (Map.Entry) it.next();
			tw.append(me.getKey() + "\t" + me.getValue());
		}
	}

	private float[] readTransform(final Content selected) {
		final GenericDialog gd =
			new GenericDialog("Read transformation", univ.getWindow());
		final Transform3D t1 = new Transform3D();
		selected.getLocalTranslate().getTransform(t1);
		final Transform3D t2 = new Transform3D();
		selected.getLocalRotate().getTransform(t2);
		t1.mul(t2);
		final float[] matrix = new float[16];
		t1.get(matrix);
		String transform = affine2string(matrix);
		gd.addStringField("Transformation", transform, 25);
		final Panel p = new Panel(new FlowLayout());
		final Button b = new Button("Open from file");
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final float[] m = new TransformIO().openAffineTransform();
				if (m != null) {
					final TextField tf = (TextField) gd.getStringFields().get(0);
					tf.setText(affine2string(m));
					tf.repaint();
				}
			}
		});
		p.add(b);
		gd.addPanel(p);
		gd.showDialog();
		if (gd.wasCanceled()) return null;

		transform = gd.getNextString();
		final float[] m = string2affine(transform);
		return m;
	}

	static FastMatrix toFastMatrix(final Transform3D t3d) {
		final Matrix4d m = new Matrix4d();
		t3d.get(m);
		return new FastMatrix(new double[][] { { m.m00, m.m01, m.m02, m.m03 },
			{ m.m10, m.m11, m.m12, m.m13 }, { m.m20, m.m21, m.m22, m.m23 } });
	}

	private String affine2string(final float[] matrix) {
		String transform = "";
		for (int i = 0; i < matrix.length; i++) {
			transform += matrix[i] + " ";
		}
		return transform;
	}

	private float[] string2affine(final String transform) {
		final String[] s = ij.util.Tools.split(transform);
		final float[] m = new float[s.length];
		for (int i = 0; i < s.length; i++) {
			m[i] = Float.parseFloat(s[i]);
		}
		return m;
	}

	private static final int getAutoThreshold(final ImagePlus imp) {
		final int[] histo = new int[256];
		final int d = imp.getStackSize();
		for (int z = 0; z < d; z++) {
			final byte[] p = (byte[]) imp.getStack().getPixels(z + 1);
			for (int i = 0; i < p.length; i++) {
				histo[(p[i] & 0xff)]++;
			}
		}
		return imp.getProcessor().getAutoThreshold(histo);
	}

	private final boolean checkSel(final Content c) {
		if (c == null) {
			IJ.error("Selection required");
			return false;
		}
		return true;
	}

	/* **********************************************************
	 * Recording methods
	 * *********************************************************/
	public static void record(String command, final String... args) {
		command = "call(\"ij3d.ImageJ3DViewer." + command;
		for (int i = 0; i < args.length; i++)
			command += "\", \"" + args[i];
		command += "\");\n";
		if (Recorder.record) Recorder.recordString(command);
	}

	/* **********************************************************
	 * Thread which handles the updates of sliders
	 * *********************************************************/
	private abstract class SliderAdjuster extends Thread {

		boolean go = false;
		int newV;
		ContentInstant content;
		Image3DUniverse univ;
		final Object lock = new Object();

		SliderAdjuster() {
			super("VIB-SliderAdjuster");
			setPriority(Thread.NORM_PRIORITY);
			setDaemon(true);
		}

		/*
		 * Set a new event, overwritting previous if any.
		 */
		void exec(final int newV, final ContentInstant content,
			final Image3DUniverse univ)
		{
			synchronized (lock) {
				this.newV = newV;
				this.content = content;
				this.univ = univ;
			}
			synchronized (this) {
				notify();
			}
		}

		public void quit() {
			this.go = false;
			synchronized (this) {
				notify();
			}
		}

		/*
		 * This class has to be implemented by subclasses, to define
		 * the specific updating function.
		 */
		protected abstract void setValue(final ContentInstant c, final int v);

		@Override
		public void run() {
			go = true;
			while (go) {
				try {
					if (null == content) {
						synchronized (this) {
							wait();
						}
					}
					if (!go) return;
					// 1 - cache vars, to free the lock very quickly
					ContentInstant c;
					int transp = 0;
					Image3DUniverse u;
					synchronized (lock) {
						c = this.content;
						transp = this.newV;
						u = this.univ;
					}
					// 2 - exec cached vars
					if (null != c) {
						setValue(c, transp);
					}
					// 3 - done: reset only if no new request was put
					synchronized (lock) {
						if (c == this.content) {
							this.content = null;
							this.univ = null;
						}
					}
				}
				catch (final Throwable t) {
					t.printStackTrace();
				}
			}
		}
	}

	ExecutorService exec = Executors.newSingleThreadExecutor();

	/** Destroy the ExecutorService that runs Runnable tasks. */
	public void flush() {
		if (null != exec) {
			synchronized (exec) {
				exec.shutdownNow();
				exec = null;
			}
		}
	}

	/** Submit a task for execution, to the single-threaded executor. */
	public void execute(final Runnable task) {
		if (null == exec) {
			IJ.log("The executer service has been shut down!");
			return;
		}
		exec.submit(task);
	}
}
