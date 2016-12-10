/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2016 Fiji developers.
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

package customnode;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Color4f;
import org.scijava.vecmath.Point3f;

public class WavefrontLoader {

	/**
	 * Load the specified obj file and returns the result as a hash map, mapping
	 * the object names to the corresponding <code>CustomMesh</code> objects.
	 */
	public static HashMap<String, CustomMesh> load(final String objfile)
		throws IOException
	{
		final WavefrontLoader wl = new WavefrontLoader();
		try {
			wl.parse(objfile);
		}
		catch (final RuntimeException e) {
			System.out.println("error reading " + wl.name);
			throw e;
		}
		return wl.meshes;
	}

	private HashMap<String, CustomMesh> meshes;

	private WavefrontLoader() {}

	private BufferedReader in;
	private String line;

	// attributes of the currently read mesh
	private final ArrayList<Point3f> vertices = new ArrayList<Point3f>();
	private ArrayList<Point3f> indices = new ArrayList<Point3f>();
	private String name = null;
	private Color4f material = null;
	private int type = -1;
	private String objfile = null;

	private void parse(final String objfile) throws IOException {
		this.objfile = objfile;
		final File f = new File(objfile);

		in = new BufferedReader(new FileReader(objfile));
		HashMap<String, Color4f> materials = null;

		meshes = new HashMap<String, CustomMesh>();

		while ((line = in.readLine()) != null) {
			if (line.startsWith("mtllib")) {
				final String mtlName = line.split("\\s+")[1].trim();
				materials = readMaterials(f, mtlName);
			}
			else if (line.startsWith("g ")) {
				if (name != null) {
					final CustomMesh cm = createCustomMesh();
					if (cm != null) meshes.put(name, cm);
					indices = new ArrayList<Point3f>();
					material = null;
				}
				name = line.split("\\s+")[1].trim();
			}
			else if (line.startsWith("usemtl ")) {
				if (materials != null) material = materials.get(line.split("\\s+")[1]);
			}
			else if (line.startsWith("v ")) {
				readVertex();
			}
			else if (line.startsWith("f ")) {
				readFace();
			}
			else if (line.startsWith("l ")) {
				readFace();
			}
			else if (line.startsWith("p ")) {
				readFace();
			}
		}
		if (name != null && indices.size() > 0) {
			final CustomMesh cm = createCustomMesh();
			if (cm != null) meshes.put(name, cm);
			indices = new ArrayList<Point3f>();
			material = null;
		}
	}

	private CustomMesh createCustomMesh() {
		if (indices.size() == 0) return null;
		CustomMesh cm = null;
		switch (type) {
			case 1:
				cm = new CustomPointMesh(indices);
				break;
			case 2:
				cm = new CustomLineMesh(indices, CustomLineMesh.PAIRWISE);
				break;
			case 3:
				cm = new CustomTriangleMesh(indices);
				break;
			case 4:
				cm = new CustomQuadMesh(indices);
				break;
			default:
				throw new RuntimeException("Unexpected number of vertices for faces");
		}
		cm.loadedFromFile = objfile;
		cm.loadedFromName = name;
		cm.changed = false;
		if (material == null) return cm;
		cm.setColor(new Color3f(material.x, material.y, material.z));
		cm.setTransparency(material.w);
		cm.changed = false;
		return cm;
	}

	private void readFace() {
		final String[] sp = line.split("\\s+");
		type = sp.length - 1;
		for (int i = 1; i < sp.length; i++) {
			int idx = -1;
			try {
				idx = Integer.parseInt(sp[i]) - 1;
			}
			catch (final NumberFormatException e) {
				final int l = sp[i].indexOf('/');
				if (l != -1) {
					sp[i] = sp[i].substring(0, l);
					idx = Integer.parseInt(sp[i]) - 1;
				}
			}
			if (idx == -1) throw new RuntimeException("Error parsing faces: " + name);
			indices.add(vertices.get(idx));
		}
	}

	private void readVertex() {
		final String[] sp = line.split("\\s+");
		vertices.add(new Point3f(Float.parseFloat(sp[1]), Float.parseFloat(sp[2]),
			Float.parseFloat(sp[3])));
	}

	private HashMap<String, Color4f> readMaterials(final File objfile,
		final String mtlFileName) throws IOException
	{
		File mtlFile = new File(objfile.getParentFile(), mtlFileName);
		if (mtlFile.exists()) return readMaterials(mtlFile);

		mtlFile = new File(mtlFileName);
		if (mtlFile.exists()) return readMaterials(mtlFile);
		return null;
	}

	private static HashMap<String, Color4f> readMaterials(final File f)
		throws IOException
	{
		return readMaterials(f.getAbsolutePath());
	}

	private static HashMap<String, Color4f> readMaterials(final String file)
		throws IOException
	{
		String name = null;
		Color4f color = null;

		final HashMap<String, Color4f> materials = new HashMap<String, Color4f>();

		final BufferedReader in = new BufferedReader(new FileReader(file));
		String line;
		while ((line = in.readLine()) != null) {
			// newmtl: if we've read one before
			// add it to the hash map
			if (line.startsWith("newmtl")) {
				if (name != null && color != null) materials.put(name, color);
				final String[] sp = line.split("\\s+");
				name = sp[1].trim();
				color = null;
			}

			if (line.startsWith("Kd")) {
				final String[] sp = line.split("\\s+");
				color =
					new Color4f(Float.parseFloat(sp[1]), Float.parseFloat(sp[2]), Float
						.parseFloat(sp[3]), 1);
			}

			if (line.startsWith("d ")) {
				if (color == null) color = new Color4f(1, 1, 1, 1);
				final String[] sp = line.split("\\s+");
				color.w = 1 - Float.parseFloat(sp[1]);
			}
		}

		if (name != null && color != null) materials.put(name, color);

		return materials;
	}
}
