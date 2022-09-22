/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2022 Fiji developers.
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;

import ij.plugin.PlugIn;

public class ImageJ_3D_Viewer implements PlugIn {

	public static void main(String[] args) {
		new ImageJ3DViewer().run("");
	}

	public void run(String args) {
		new ImageJ3DViewer().run(args);
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

