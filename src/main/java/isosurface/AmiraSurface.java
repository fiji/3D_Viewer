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

package isosurface;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import amira.AmiraParameters;
import ij.IJ;
import ij.io.OpenDialog;
import ij.plugin.PlugIn;
import ij3d.Image3DUniverse;

public class AmiraSurface implements PlugIn {

	@Override
	public void run(String arg) {
		if (arg.equals("")) {
			final OpenDialog od = new OpenDialog("AmiraFile", null);
			if (od.getDirectory() == null) return; // canceled
			arg = od.getDirectory() + od.getFileName();
		}

		final Image3DUniverse universe = new Image3DUniverse();
		try {
			addMeshes(universe, arg);
		}
		catch (final IOException e) {
			IJ.error("Could not read '" + arg + "': " + e);
			return;
		}
		universe.show();
	}

	public static void addMeshes(final Image3DUniverse universe,
		final String fileName) throws IOException
	{
		final FileInputStream f = new FileInputStream(fileName);
		final DataInputStream input = new DataInputStream(f);

		if (!input.readLine().startsWith("# HyperSurface 0.1 BINARY")) throw new RuntimeException(
			"No Amira surface");

		String header = "";
		String line;
		while ((line = input.readLine()) != null && !line.startsWith("Vertices"))
			header += line + "\n";
		final AmiraParameters params = new AmiraParameters(header);

		final int vertexCount = Integer.parseInt(line.substring(9));
		final Point3f[] vertices = new Point3f[vertexCount];
		for (int i = 0; i < vertexCount; i++) {
			final float x = input.readFloat();
			final float y = input.readFloat();
			final float z = input.readFloat();
			vertices[i] = new Point3f(x, y, z);
		}

		while ((line = input.readLine()) != null &&
			!line.trim().startsWith("Patches"));

		final Map meshes = new HashMap();

		final int patchCount = Integer.parseInt(line.substring(8));
		for (int p = 0; p < patchCount; p++) {
			List mesh = null, opposite = null;
			while ((line = input.readLine()) != null &&
				!line.trim().startsWith("Triangles"))
				if (line.startsWith("InnerRegion")) mesh =
					getMesh(meshes, line.substring(12));
				else if (line.startsWith("OuterRegion")) opposite =
					getMesh(meshes, line.substring(12));

			if (mesh == null) {
				IJ.error("Funny mesh encountered");
				mesh = new ArrayList();
			}

			final int triangleCount = Integer.parseInt(line.trim().substring(10));
			for (int i = 0; i < triangleCount; i++) {
				final Point3f point1 = vertices[input.readInt() - 1];
				final Point3f point2 = vertices[input.readInt() - 1];
				final Point3f point3 = vertices[input.readInt() - 1];
				mesh.add(point1);
				mesh.add(point2);
				mesh.add(point3);
				if (opposite != null) {
					opposite.add(point3);
					opposite.add(point2);
					opposite.add(point1);
				}
			}
		}

		final Color3f lightGray = new Color3f(.5f, .5f, .5f);
		final Iterator iter = meshes.keySet().iterator();
		while (iter.hasNext()) {
			final String name = (String) iter.next();
			final int m = params.getMaterialID(name);
			final double[] c = params.getMaterialColor(m);
			final Color3f color =
				(c[0] == 0 && c[1] == 0 && c[2] == 0 ? lightGray : new Color3f(
					(float) c[0], (float) c[1], (float) c[2]));
			final List list = (List) meshes.get(name);
			if (list.size() == 0) continue;
			universe.addMesh(list, color, name, 0);
		}
	}

	private static List getMesh(final Map map, final String name) {
		if (name.equals("Exterior")) return null;
		if (!map.containsKey(name)) map.put(name, new ArrayList());
		return (List) map.get(name);
	}
}
