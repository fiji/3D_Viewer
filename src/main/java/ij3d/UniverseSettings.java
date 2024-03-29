/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2023 Fiji developers.
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

package ij3d;

import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.HashMap;
import java.util.Properties;

import javax.swing.KeyStroke;

import org.scijava.java3d.View;
import org.scijava.vecmath.Color3f;

import ij.gui.GenericDialog;

public class UniverseSettings {

	protected static String getPrefsDir() {
		final String env = System.getenv("IJ_PREFS_DIR");
		if (env != null && !env.equals("")) return env;
		return System.getProperty("user.home");
	}

	public static final File propsfile = new File(getPrefsDir(),
		".ImageJ_3D_Viewer.props");

	public static final int PERSPECTIVE = View.PERSPECTIVE_PROJECTION;
	public static final int PARALLEL = View.PARALLEL_PROJECTION;

	public static int startupWidth = 512;
	public static int startupHeight = 512;
	public static int projection = PERSPECTIVE;
	public static boolean showGlobalCoordinateSystem = false;
	public static boolean showLocalCoordinateSystemsByDefault = false;
	public static boolean showScalebar = false;
	public static boolean showSelectionBox = true;
	public static Color3f defaultBackground = new Color3f();
	public static final HashMap<String, String> shortcuts =
		new HashMap<String, String>();

	public static void save() {
		final Properties properties = new Properties();
		properties.put("Startup_Width", str(startupWidth));
		properties.put("Startup_Height", str(startupHeight));
		properties.put("Projection", str(projection));
		properties.put("Show_Global_Coordinate_System",
			str(showGlobalCoordinateSystem));
		properties.put("Show_Local_Coordinate_System_When_Adding_Content",
			str(showLocalCoordinateSystemsByDefault));
		properties.put("Show_Scalebar", str(showScalebar));
		properties.put("Background", str(defaultBackground));
		for (final String key : shortcuts.keySet())
			properties.put("shortcut." + key, shortcuts.get(key));
		try {
			properties.store(new FileOutputStream(propsfile),
				"ImageJ 3D Viewer properties");
		}
		catch (final Exception e) {
			System.err.println(e.getMessage());
		}
	}

	private static void setDefaultShortcuts() {
		shortcuts.clear();
		shortcuts.put("File > Open...", getKeyStroke(KeyEvent.VK_O));
		shortcuts.put("Edit > Delete", "pressed DELETE");
		shortcuts.put("File > Quit", getKeyStroke(KeyEvent.VK_W));
		shortcuts.put("Edit > Change transparency", getKeyStroke(KeyEvent.VK_T));
		shortcuts.put("Edit > Change color", getKeyStroke(KeyEvent.VK_C));
		shortcuts.put("View > Fullscreen", getKeyStroke(KeyEvent.VK_F));
		shortcuts.put("View > Reset view", "ctrl pressed H");
		shortcuts.put("Help > Java 3D Properties", "pressed F1");
	}

	private static String getKeyStroke(final int kc) {
		return KeyStroke.getKeyStroke(kc,
			Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()).toString();
	}

	public static void load() {
		try {
			final Properties properties = new Properties();
			properties.load(new FileInputStream(propsfile));
			startupWidth =
				integer(properties.getProperty("Startup_Width", str(startupWidth)));
			startupHeight =
				integer(properties.getProperty("Startup_Height", str(startupHeight)));
			projection =
				integer(properties.getProperty("Projection", str(projection)));
			showGlobalCoordinateSystem =
				bool(properties.getProperty("Show_Global_Coordinate_System",
					str(showGlobalCoordinateSystem)));
			showLocalCoordinateSystemsByDefault =
				bool(properties.getProperty(
					"Show_Local_Coordinate_System_When_Adding_Content",
					str(showLocalCoordinateSystemsByDefault)));
			showScalebar =
				bool(properties.getProperty("Show_Scalebar", str(showScalebar)));
			defaultBackground =
				col(properties.getProperty("Background", str(defaultBackground)));
			shortcuts.clear();
			for (final Object o : properties.keySet()) {
				String key = (String) o;
				if (key.startsWith("shortcut.")) {
					key = key.substring(".shortcut".length());
					final String v = properties.getProperty((String) o);
					shortcuts.put(key, v);
				}
			}
			if (shortcuts.isEmpty()) setDefaultShortcuts();
		}
		catch (final Exception e) {
			System.err.println(e.getMessage());
		}
	}

	public static void initFromDialog(final Image3DUniverse univ) {
		final GenericDialog gd =
			new GenericDialog("View Preferences", univ.getWindow());
		gd.addMessage("The following options are startup options\n"
			+ "They are not applied, unless you activate\n"
			+ "'Apply changes now' below.");

		gd.addNumericField("Width", startupWidth, 0);
		gd.addNumericField("Height", startupHeight, 0);

		final String[] choice = new String[] { "PARALLEL", "PERSPECTIVE" };
		final int v1[] = new int[] { PARALLEL, PERSPECTIVE };
		final String def = projection == v1[0] ? choice[0] : choice[1];
		gd.addChoice("Projection", choice, def);

		gd.addCheckbox("Show global coordinate system", showGlobalCoordinateSystem);
		gd.addCheckbox("Use current color as default backround", false);
		gd.addCheckbox("Show scalebar", showScalebar);
		gd.addCheckbox("Apply changes now", true);

		gd.addMessage("The following options are applied immediately:");

		gd.addCheckbox("Show local coordinate system by default",
			showLocalCoordinateSystemsByDefault);

		gd.showDialog();
		if (gd.wasCanceled()) return;

		startupWidth = (int) gd.getNextNumber();
		startupHeight = (int) gd.getNextNumber();
		projection = v1[gd.getNextChoiceIndex()];
		showGlobalCoordinateSystem = gd.getNextBoolean();
		if (gd.getNextBoolean() && univ != null) ((ImageCanvas3D) univ.getCanvas())
			.getBG().getColor(defaultBackground);
		showScalebar = gd.getNextBoolean();
		final boolean apply = gd.getNextBoolean();

		showLocalCoordinateSystemsByDefault = gd.getNextBoolean();

		save();
		if (apply) apply(univ);
	}

	public static void apply(final Image3DUniverse univ) {
		if (univ == null) return;

		univ.setSize(startupWidth, startupHeight);
		univ.getViewer().getView().setProjectionPolicy(projection);
		univ.showAttribute(DefaultUniverse.ATTRIBUTE_COORD_SYSTEM,
			showGlobalCoordinateSystem);
		univ.showAttribute(DefaultUniverse.ATTRIBUTE_SCALEBAR, showScalebar);
	}

	private static final String str(final int i) {
		return Integer.toString(i);
	}

	private static final String str(final boolean b) {
		return Boolean.toString(b);
	}

	private static final String str(final Color3f c) {
		return "[" + c.x + "," + c.y + "," + c.z + "]";
	}

	private static final Color3f col(String s) {
		s = s.substring(1, s.length() - 1);
		final String[] tmp = s.split(",");
		return new Color3f(Float.parseFloat(tmp[0]), Float.parseFloat(tmp[1]),
			Float.parseFloat(tmp[2]));
	}

	private static final boolean bool(final String s) {
		return Boolean.parseBoolean(s);
	}

	private static final int integer(final String s) {
		return Integer.parseInt(s);
	}
}
