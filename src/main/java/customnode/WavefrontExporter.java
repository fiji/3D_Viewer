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

package customnode;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Color4f;
import org.scijava.vecmath.Point3f;

public class WavefrontExporter {

	public static void save(final Map<String, CustomMesh> meshes,
		final String objFile) throws IOException
	{
		final File objF = new File(objFile);

		final String objname = objF.getName();
		String mtlname = objname;
		if (mtlname.endsWith(".obj")) mtlname =
			mtlname.substring(0, mtlname.length() - 4);
		mtlname += ".mtl";

		OutputStreamWriter dos_obj = null, dos_mtl = null;
		try {
			dos_obj =
				new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
					objF)), "8859_1");
			dos_mtl =
				new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
					new File(objF.getParent(), mtlname))), "8859_1");
			save(meshes, mtlname, dos_obj, dos_mtl);
			dos_obj.flush();
			dos_obj.flush();
			for (final String n : meshes.keySet()) {
				final CustomMesh m = meshes.get(n);
				m.loadedFromFile = objFile;
				m.loadedFromName = n.replaceAll(" ", "_").replaceAll("#", "--");
				m.changed = false;
			}
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (null != dos_obj) dos_obj.close();
			}
			catch (final Exception e) {}
			try {
				if (null != dos_mtl) dos_mtl.close();
			}
			catch (final Exception e) {}
		}
	}

	/**
	 * Write the given collection of <code>CustomMesh</code>es;
	 * 
	 * @param meshes maps a name to a <code>CustomMesh</code>. The name is used to
	 *          set the group name ('g') in the obj file.
	 * @param mtlFileName name of the material file, which is used to store in the
	 *          obj-file.
	 * @param objWriter <code>Writer</code> for the obj file
	 * @param mtlWriter <code>Writer</code> for the material file.
	 */
	public static void save(final Map<String, CustomMesh> meshes,
		final String mtlFileName, final Writer objWriter, final Writer mtlWriter)
		throws IOException
	{

		objWriter.write("# OBJ File\n");
		objWriter.write("mtllib ");
		objWriter.write(mtlFileName);
		objWriter.write('\n');

		final HashMap<Mtl, Mtl> ht_mat = new HashMap<Mtl, Mtl>();

		// Vert indices in .obj files are global, not reset for every
		// object. Starting at '1' because vert indices start at one.
		int j = 1;

		final StringBuffer tmp = new StringBuffer(100);

		for (final String name : meshes.keySet()) {
			final CustomMesh cmesh = meshes.get(name);

			final List<Point3f> vertices = cmesh.getMesh();
			// make material, and see whether it exists already
			Color3f color = cmesh.getColor();
			if (null == color) {
				// happens when independent colors
				// have been set for each vertex.
				color = CustomMesh.DEFAULT_COLOR;
			}
			Mtl mat = new Mtl(1 - cmesh.getTransparency(), color);
			if (ht_mat.containsKey(mat)) mat = ht_mat.get(mat);
			else ht_mat.put(mat, mat);

			// make list of vertices
			final String title = name.replaceAll(" ", "_").replaceAll("#", "--");
			final HashMap<Point3f, Integer> ht_points =
				new HashMap<Point3f, Integer>();
			objWriter.write("g ");
			objWriter.write(title);
			objWriter.write('\n');
			final int len = vertices.size();
			final int[] index = new int[len];

			// index over index array, to make faces later
			int k = 0;
			for (final Point3f p : vertices) {
				// check if point already exists
				if (ht_points.containsKey(p)) {
					index[k] = ht_points.get(p);
				}
				else {
					// new point
					index[k] = j;
					// record
					ht_points.put(p, j);
					// append vertex
					tmp.append('v').append(' ').append(p.x).append(' ').append(p.y)
						.append(' ').append(p.z).append('\n');
					objWriter.write(tmp.toString());
					tmp.setLength(0);
					j++;
				}
				k++;
			}
			objWriter.write("usemtl ");
			objWriter.write(mat.name);
			objWriter.write('\n');
			// print faces
			if (cmesh.getClass() == CustomTriangleMesh.class) writeTriangleFaces(
				index, objWriter, name);
			else if (cmesh.getClass() == CustomQuadMesh.class) writeQuadFaces(index,
				objWriter, name);
			else if (cmesh.getClass() == CustomPointMesh.class) writePointFaces(
				index, objWriter, name);
			else if (cmesh.getClass() == CustomLineMesh.class) {
				final CustomLineMesh clm = (CustomLineMesh) cmesh;
				switch (clm.getMode()) {
					case CustomLineMesh.PAIRWISE:
						writePairwiseLineFaces(index, objWriter, name);
						break;
					case CustomLineMesh.CONTINUOUS:
						writeContinuousLineFaces(index, objWriter, name);
						break;
					default:
						throw new IllegalArgumentException("Unknown line mesh mode");
				}
			}
			else {
				throw new IllegalArgumentException("Unknown custom mesh class: " +
					cmesh.getClass());
			}
		}
		// make mtl file
		mtlWriter.write("# MTL File\n");
		for (final Mtl mat : ht_mat.keySet()) {
			final StringBuffer sb = new StringBuffer(150);
			mat.fill(sb);
			mtlWriter.write(sb.toString());
		}
	}

	/**
	 * Write faces for triangle meshes.
	 */
	static void writeTriangleFaces(final int[] indices, final Writer objWriter,
		final String name) throws IOException
	{
		if (indices.length % 3 != 0) throw new IllegalArgumentException(
			"list of triangles not multiple of 3: " + name);
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length; i += 3) {
			buf.append('f').append(' ').append(indices[i]).append(' ').append(
				indices[i + 1]).append(' ').append(indices[i + 2]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/**
	 * Write faces for point meshes.
	 */
	static void writePointFaces(final int[] indices, final Writer objWriter,
		final String name) throws IOException
	{
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length; i++) {
			buf.append('f').append(' ').append(indices[i]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/**
	 * Write faces for quad meshes.
	 */
	static void writeQuadFaces(final int[] indices, final Writer objWriter,
		final String name) throws IOException
	{
		if (indices.length % 4 != 0) throw new IllegalArgumentException(
			"list of quads not multiple of 4: " + name);
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length; i += 4) {
			buf.append('f').append(' ').append(indices[i]).append(' ').append(
				indices[i + 1]).append(' ').append(indices[i + 2]).append(' ').append(
				indices[i + 3]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/**
	 * Write faces for pairwise line meshes, ie the points addressed by the
	 * indices array are arranged in pairs each specifying one line segment.
	 */
	static void writePairwiseLineFaces(final int[] indices,
		final Writer objWriter, final String name) throws IOException
	{
		if (indices.length % 2 != 0) throw new IllegalArgumentException(
			"list of lines not multiple of 2: " + name);
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length; i += 2) {
			buf.append('f').append(' ').append(indices[i]).append(' ').append(
				indices[i + 1]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/**
	 * Write faces for continuous line meshes, ie the points addressed by the
	 * indices array represent a continuous line.
	 */
	static void writeContinuousLineFaces(final int[] indices,
		final Writer objWriter, final String name) throws IOException
	{
		final StringBuffer buf = new StringBuffer(100);
		objWriter.write("s 1\n");
		for (int i = 0; i < indices.length - 1; i++) {
			buf.append('f').append(' ').append(indices[i]).append(' ').append(
				indices[i + 1]).append('\n');
			objWriter.write(buf.toString());
			buf.setLength(0);
		}
		objWriter.write('\n');
	}

	/** A Material, but avoiding name colisions. Not thread-safe. */
	static private int mat_index = 1;

	static private class Mtl {

		private final Color4f col;
		String name;

		Mtl(final float alpha, final Color3f c) {
			this.col = new Color4f(c.x, c.y, c.z, alpha);
			name = "mat_" + mat_index;
			mat_index++;
		}

		Mtl(final float alpha, final float R, final float G, final float B) {
			this.col = new Color4f(R, G, B, alpha);
			name = "mat_" + mat_index;
			mat_index++;
		}

		@Override
		public boolean equals(final Object ob) {
			if (ob instanceof Mtl) {
				return this.col == ((Mtl) ob).col;
			}
			return false;
		}

		@Override
		public int hashCode() {
			return col.hashCode();
		}

		void fill(final StringBuffer sb) {
			sb.append("\nnewmtl ").append(name).append('\n').append("Ns 96.078431\n")
				.append("Ka 0.0 0.0 0.0\n").append("Kd ").append(col.x)
				.append(' ')
				// this is INCORRECT but I'll figure out the
				// conversion later
				.append(col.y).append(' ').append(col.z).append('\n').append(
					"Ks 0.5 0.5 0.5\n").append("Ni 1.0\n").append("d ").append(col.w)
				.append('\n').append("illum 2\n\n");
		}
	}

	/** Utility method to encode text data in 8859_1. */
	static public boolean saveToFile(final File f, final String data)
		throws IOException
	{
		if (null == f) return false;
		final OutputStreamWriter dos =
			new OutputStreamWriter(new BufferedOutputStream(
			// encoding in Latin 1 (for macosx not to mess around
				new FileOutputStream(f), data.length()), "8859_1");
		dos.write(data, 0, data.length());
		dos.flush();
		return true;
	}
}
