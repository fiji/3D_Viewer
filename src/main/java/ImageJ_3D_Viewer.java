import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import ij.IJ;
import ij.plugin.PlugIn;
import ij3d.ImageJ3DViewer;

public class ImageJ_3D_Viewer implements PlugIn {

	public static void main(String[] args) {
		if(checkJava3D())
			new ImageJ3DViewer().run("");
	}

	public void run(String args) {
		if(checkJava3D())
			new ImageJ3DViewer().run(args);
	}

	/**
	 * Returns true if the viewer can run.
	 */
	public static boolean checkJava3D() {
		final String version = getJava3DVersion();
		System.out.println("version = " + version);
		if(version != null && Float.parseFloat(version) >= 1.5)
			return true;

		if (version != null) {
			IJ.showMessage("3D Viewer", "Java 3D version " + version +
				" detected,\n" + "but version >= 1.5 is required.\n" +
				"Please uninstall the old Java 3D from your Java runtime.");
		}
		else {
			IJ.showMessage("3D Viewer", "Java 3D is required, but not available");
		}

		return false;
	}

	public static String getJava3DVersion() {
		try {
			final ClassLoader cl = Thread.currentThread().getContextClassLoader();
			final Class<?> c = cl.loadClass("org.scijava.java3d.VirtualUniverse");
			final Method getProperties = c.getMethod("getProperties");
			final Object props = getProperties.invoke(null);
			if (!(props instanceof Map)) return null;
			final Map<?, ?> map = (Map<?, ?>) props;
			final Object version = map.get("j3d.specification.version");
			return version == null ? null : version.toString();
		}
		catch (final ClassNotFoundException exc) {
			debug(exc);
		}
		catch (IllegalAccessException exc) {
			debug(exc);
		}
		catch (NoSuchMethodException exc) {
			debug(exc);
		}
		catch (SecurityException exc) {
			debug(exc);
		}
		catch (IllegalArgumentException exc) {
			debug(exc);
		}
		catch (InvocationTargetException exc) {
			debug(exc);
		}
		return null;
	}

	private static void debug(Throwable t) {
		t.printStackTrace();
	}

}

