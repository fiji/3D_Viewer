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
/** Albert Cardona and Bene Schmid 20070614 at Janelia Farms*/

package isosurface;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import customnode.CustomMesh;
import customnode.CustomMeshNode;
import customnode.CustomMultiMesh;
import customnode.CustomQuadMesh;
import customnode.CustomTriangleMesh;
import customnode.WavefrontExporter;
import ij.IJ;
import ij3d.Content;
import ij3d.ContentNode;
import ij3d.Executer;

public class MeshExporter {

	public static final int ASCII = 0, BINARY = 1; // output filetype flag

	private MeshExporter() {}

	static private Collection<Content> filterMeshes(final Collection contents) {
		final ArrayList<Content> meshes = new ArrayList<Content>();
		for (final Iterator it = contents.iterator(); it.hasNext();) {
			final Content c = (Content) it.next();
			final ContentNode node = c.getContent();
			if (node instanceof voltex.VoltexGroup ||
				node instanceof orthoslice.OrthoGroup ||
				node instanceof surfaceplot.SurfacePlotGroup)
			{
				continue;
			}
			meshes.add(c);
		}
		return meshes;
	}

	@Deprecated
	static public void saveAsWaveFront(final Collection contents_) {
		final File obj_file =
			Executer.promptForFile("Save WaveFront", "untitled", ".obj");
		if (obj_file == null) return;
		saveAsWaveFront(contents_, obj_file);
	}

	/** Accepts a collection of MeshGroup objects. */
	static public void saveAsWaveFront(final Collection contents_,
		final File obj_file)
	{
		if (null == contents_ || 0 == contents_.size()) return;
		final Collection<Content> contents = filterMeshes(contents_);
		if (0 == contents.size()) {
			IJ.log("No meshes to export!");
			return;
		}

		final String obj_filename = obj_file.getName();
		final String mtl_filename =
			obj_filename.substring(0, obj_filename.lastIndexOf('.')) + ".mtl";

		final File mtl_file = new File(obj_file.getParentFile(), mtl_filename);

		OutputStreamWriter dos_obj = null, dos_mtl = null;
		try {
			dos_obj =
				new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
					obj_file)), "8859_1"); // encoding in Latin 1 (for macosx not to mess
																	// around
			dos_mtl =
				new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
					mtl_file)), "8859_1"); // encoding in Latin 1 (for macosx not to mess
																	// around
			writeAsWaveFront(contents, mtl_filename, dos_obj, dos_mtl);
			dos_obj.flush();
			dos_obj.flush();
		}
		catch (final IOException e) {
			IJ.log("Some error ocurred while saving to wavefront:\n" + e);
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

	@Deprecated
	static public void saveAsDXF(final Collection contents_) {
		final File dxf_file =
			Executer.promptForFile("Save as DXF", "untitled", ".dxf");
		if (dxf_file == null) return;
		saveAsDXF(contents_, dxf_file);
	}

	static public void saveAsDXF(Collection meshgroups, final File dxf_file) {
		if (null == meshgroups || 0 == meshgroups.size()) return;
		meshgroups = filterMeshes(meshgroups);
		if (0 == meshgroups.size()) {
			IJ.log("No meshes to export!");
			return;
		}

		OutputStreamWriter dos = null;
		try {
			dos =
				new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
					dxf_file)), "8859_1"); // encoding in Latin 1 (for macosx not to mess
																	// around
			writeDXF(meshgroups, dos);
			dos.flush();
		}
		catch (final IOException ioe) {
			ioe.printStackTrace();
		}
		finally {
			try {
				if (null != dos) dos.close();
			}
			catch (final Exception e) {}
		}
	}

	static public void writeDXF(final Collection contents, final Writer w)
		throws IOException
	{
		w.write("0\nSECTION\n2\nENTITIES\n"); // header of file
		for (final Iterator it = contents.iterator(); it.hasNext();) {
			final Content ob = (Content) it.next();

			CustomMesh cmesh = null;

			if (ob.getContent() instanceof CustomMeshNode) {
				final CustomMeshNode cmeshnode = (CustomMeshNode) ob.getContent();
				cmesh = cmeshnode.getMesh();
			}
			else if (ob.getContent() instanceof MeshGroup) {
				final MeshGroup mg = (MeshGroup) ob.getContent();
				cmesh = mg.getMesh();
			}
			else continue;

			final List triangles = cmesh.getMesh();

			final String title =
				ob.getName().replaceAll(" ", "_").replaceAll("#", "--");
			final Mtl mat = new Mtl(1 - ob.getTransparency(), cmesh.getColor());
			writeTrianglesDXF(w, triangles, title, "" + mat.getAsSingle());
		}
		w.append("0\nENDSEC\n0\nEOF\n"); // TRAILER of the file
	}

	@Deprecated
	static public void saveAsSTL(final Collection contents_, final int filetype) {
		final String title =
			"Save as STL (" + ((filetype == ASCII) ? "ASCII" : "binary") + ")";
		final File stl_file = Executer.promptForFile(title, "untitled", ".stl");
		if (stl_file == null) return;
		saveAsSTL(contents_, stl_file, filetype);
	}

	public static void saveAsSTL(Collection meshgroups, final File stl_file,
		final int filetype)
	{
		if (null == meshgroups || 0 == meshgroups.size()) return;
		meshgroups = filterMeshes(meshgroups);
		if (0 == meshgroups.size()) {
			IJ.log("No meshes to export!");
			return;
		}

		OutputStreamWriter dos = null;
		DataOutputStream out = null;
		try {
			if (filetype == ASCII) {
				dos =
					new OutputStreamWriter(new BufferedOutputStream(new FileOutputStream(
						stl_file)), "8859_1");
				writeAsciiSTL(meshgroups, dos, stl_file.getName());
				dos.flush();
			}
			else {
				out =
					new DataOutputStream(new BufferedOutputStream(new FileOutputStream(
						stl_file)));
				writeBinarySTL(meshgroups, out);
				out.flush();
			}
		}
		catch (final IOException ioe) {
			ioe.printStackTrace();
		}
		finally {
			try {
				if (null != dos) dos.close();
				if (null != out) out.close();
			}
			catch (final Exception e) {}
		}
	}

	private static void writeBinarySTL(final Collection meshgroups,
		final DataOutputStream out)
	{

		// get all the meshes and sort them into a hash
		final HashMap<String, CustomMesh> meshes =
			new HashMap<String, CustomMesh>();
		for (final Iterator<Content> it = meshgroups.iterator(); it.hasNext();) {
			final Content mob = it.next();

			final ContentNode node = mob.getContent();
			// First CustomMultiMesh, which is also a CustomMeshNode:
			if (node instanceof CustomMultiMesh) {
				final CustomMultiMesh multi = (CustomMultiMesh) node;
				for (int i = 0; i < multi.size(); i++) {
					meshes.put(mob.getName() + " [" + (i + 1) + "]", multi.getMesh(i));
				}
				// Then CustomMeshNode (all custom meshes):
			}
			else if (node instanceof CustomMeshNode) {
				meshes.put(mob.getName(), ((CustomMeshNode) node).getMesh());
				// An image volume rendered as isosurface:
			}
			else if (node instanceof MeshGroup) {
				meshes.put(mob.getName(), ((MeshGroup) node).getMesh());
			}
			else {
				IJ.log("Ignoring " + mob.getName() + " with node of class " +
					node.getClass());
				continue;
			}
		}
		// count all the triangles and add them to a list
		int triangles = 0;
		final ArrayList<List<Point3f>> surfaces = new ArrayList<List<Point3f>>();
		for (final String name : meshes.keySet()) {
			final CustomMesh cmesh = meshes.get(name);
			if (cmesh.getClass() == CustomQuadMesh.class) {
				IJ.log("Quad meshes are unsupported, can't save " + name + " as STL");
				continue;
			}
			else if (cmesh.getClass() != CustomTriangleMesh.class) {
				IJ.log("Unsupported content type, can't save " + name + " as STL");
				continue;
			}
			final List<Point3f> vertices = cmesh.getMesh();
			triangles += vertices.size() / 3;
			surfaces.add(vertices);
		}

		String header = "Binary STL created by ImageJ 3D Viewer.";
		for (int i = header.length(); i < 80; i++) {
			header = header + ".";
		}
		try {
			out.writeBytes(header);
			out.writeByte(triangles & 0xFF);
			out.writeByte((triangles >> 8) & 0xFF);
			out.writeByte((triangles >> 16) & 0xFF);
			out.writeByte((triangles >> 24) & 0xFF);
			for (final List<Point3f> vertices : surfaces) {
				for (int i = 0; i < vertices.size(); i += 3) {
					final Point3f p0 = vertices.get(i);
					final Point3f p1 = vertices.get(i + 1);
					final Point3f p2 = vertices.get(i + 2);
					final Point3f n = unitNormal(p0, p1, p2);
					final ByteBuffer bb = ByteBuffer.allocate(50);
					bb.order(ByteOrder.LITTLE_ENDIAN);
					bb.putFloat(n.x);
					bb.putFloat(n.y);
					bb.putFloat(n.z);
					bb.putFloat(p0.x);
					bb.putFloat(p0.y);
					bb.putFloat(p0.z);
					bb.putFloat(p1.x);
					bb.putFloat(p1.y);
					bb.putFloat(p1.z);
					bb.putFloat(p2.x);
					bb.putFloat(p2.y);
					bb.putFloat(p2.z);
					bb.putShort((short) 0);
					out.write(bb.array());
				}
			}
		}
		catch (final IOException e) {
			e.printStackTrace();
		}

	}

	private static Point3f unitNormal(final Point3f p0, final Point3f p1,
		final Point3f p2)
	{
		float nx = (p1.y - p0.y) * (p2.z - p0.z) - (p1.z - p0.z) * (p2.y - p0.y);
		float ny = (p1.z - p0.z) * (p2.x - p0.x) - (p1.x - p0.x) * (p2.z - p0.z);
		float nz = (p1.x - p0.x) * (p2.y - p0.y) - (p1.y - p0.y) * (p2.x - p0.x);

		final float length = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
		nx /= length;
		ny /= length;
		nz /= length;
		return new Point3f(nx, ny, nz);
	}

	private static void writeAsciiSTL(final Collection<Content> meshgroups,
		final OutputStreamWriter dos, final String stl_filename)
	{
		try {
			dos.write(" solid ");
			dos.write(stl_filename);

			// get all the meshes and sort them into a hash
			final HashMap<String, CustomMesh> meshes =
				new HashMap<String, CustomMesh>();
			for (final Iterator<Content> it = meshgroups.iterator(); it.hasNext();) {
				final Content mob = it.next();

				final ContentNode node = mob.getContent();
				// First CustomMultiMesh, which is also a CustomMeshNode:
				if (node instanceof CustomMultiMesh) {
					final CustomMultiMesh multi = (CustomMultiMesh) node;
					for (int i = 0; i < multi.size(); i++) {
						meshes.put(mob.getName() + " [" + (i + 1) + "]", multi.getMesh(i));
					}
					// Then CustomMeshNode (all custom meshes):
				}
				else if (node instanceof CustomMeshNode) {
					meshes.put(mob.getName(), ((CustomMeshNode) node).getMesh());
					// An image volume rendered as isosurface:
				}
				else if (node instanceof MeshGroup) {
					meshes.put(mob.getName(), ((MeshGroup) node).getMesh());
				}
				else {
					IJ.log("Ignoring " + mob.getName() + " with node of class " +
						node.getClass());
					continue;
				}
			}

			// go through all meshes and add them to STL file
			for (final String name : meshes.keySet()) {
				final CustomMesh cmesh = meshes.get(name);
				if (cmesh.getClass() == CustomQuadMesh.class) {
					IJ.log("Quad meshes are unsupported, can't save " + name + " as STL");
					continue;
				}
				else if (cmesh.getClass() != CustomTriangleMesh.class) {
					IJ.log("Unsupported content type, can't save " + name + " as STL");
					continue;
				}
				final List<Point3f> vertices = cmesh.getMesh();
				final int nPoints = vertices.size();
				for (int p = 0; p < nPoints; p += 3) {
					final Point3f p0 = vertices.get(p);
					final Point3f p1 = vertices.get(p + 1);
					final Point3f p2 = vertices.get(p + 2);
					final Point3f n = unitNormal(p0, p1, p2);

					final String e = "%E"; // Scientific format -3.141569E+03
					dos.write("\nfacet normal ");
					dos.write(String.format(e, n.x) + " ");
					dos.write(String.format(e, n.y) + " ");
					dos.write(String.format(e, n.z) + "\n");
					dos.write(" outer loop\n");
					dos.write("  vertex ");
					dos.write(String.format(e, p0.x) + " ");
					dos.write(String.format(e, p0.y) + " ");
					dos.write(String.format(e, p0.z) + "\n");
					dos.write("  vertex ");
					dos.write(String.format(e, p1.x) + " ");
					dos.write(String.format(e, p1.y) + " ");
					dos.write(String.format(e, p1.z) + "\n");
					dos.write("  vertex ");
					dos.write(String.format(e, p2.x) + " ");
					dos.write(String.format(e, p2.y) + " ");
					dos.write(String.format(e, p2.z) + "\n");
					dos.write(" endloop\n");
					dos.write("endfacet");
				}
			}
			dos.write("\n endsolid ");
			dos.write(stl_filename);
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
	}

	@Deprecated
	static public String createDXF(final Collection contents) {
		final StringWriter sw = new StringWriter();
		try {
			writeDXF(contents, sw);
		}
		catch (final IOException ioe) {}
		return sw.toString();
	}

	@Deprecated
	static public void writeTrianglesDXF(final StringBuffer sb,
		final List triangles, final String the_group, final String the_color)
	{
		try {
			final StringWriter sw = new StringWriter();
			writeTrianglesDXF(sw, triangles, the_group, the_color);
			sb.append(sw.getBuffer());
		}
		catch (final IOException ioe) {
			ioe.printStackTrace();
		}
	}

	static private void writeTrianglesDXF(final Writer w, final List triangles,
		final String the_group, final String the_color) throws IOException
	{

		final char L = '\n';
		final String s10 = "10\n";
		final String s11 = "11\n";
		final String s12 = "12\n";
		final String s13 = "13\n";
		final String s20 = "20\n";
		final String s21 = "21\n";
		final String s22 = "22\n";
		final String s23 = "23\n";
		final String s30 = "30\n";
		final String s31 = "31\n";
		final String s32 = "32\n";
		final String s33 = "33\n";
		final String triangle_header =
			"0\n3DFACE\n8\n" + the_group + "\n6\nCONTINUOUS\n62\n" + the_color + L;

		final int len = triangles.size();
		final Point3f[] vert = new Point3f[len];
		triangles.toArray(vert);

		final StringBuffer sb = new StringBuffer(150);

		for (int i = 0; i < len; i += 3) {

			w.write(triangle_header);

			sb.append(s10).append(vert[i].x).append(L).append(s20).append(vert[i].y)
				.append(L).append(s30).append(vert[i].z).append(L)

				.append(s11).append(vert[i + 1].x).append(L).append(s21).append(
					vert[i + 1].y).append(L).append(s31).append(vert[i + 1].z).append(L)

				.append(s12).append(vert[i + 2].x).append(L).append(s22).append(
					vert[i + 2].y).append(L).append(s32).append(vert[i + 2].z).append(L)

				.append(s13).append(vert[i + 2].x).append(L) // repeated point
				.append(s23).append(vert[i + 2].y).append(L).append(s33).append(
					vert[i + 2].z).append(L);

			w.write(sb.toString());
			sb.setLength(0);
		}
	}

	/**
	 * Expects a collection of MeshGroup objects, and the material file name to
	 * point to. Returns two String objects: - the contents of the .obj file with
	 * mesh data - the contents of the .mtl file with material data
	 */
	@Deprecated
	static public String[] createWaveFront(final Collection contents,
		final String mtl_filename)
	{
		final StringWriter sw_obj = new StringWriter();
		final StringWriter sw_mtl = new StringWriter();
		try {
			writeAsWaveFront(contents, mtl_filename, sw_obj, sw_mtl);
			return new String[] { sw_obj.toString(), sw_mtl.toString() };
		}
		catch (final IOException ioe) {
			ioe.printStackTrace();
		}
		return null;
	}

	static public void writeAsWaveFront(final Collection contents,
		final String mtl_filename, final Writer w_obj, final Writer w_mtl)
		throws IOException
	{
		final HashMap<String, CustomMesh> meshes =
			new HashMap<String, CustomMesh>();

		for (final Iterator it = contents.iterator(); it.hasNext();) {
			final Content mob = (Content) it.next();

			final ContentNode node = mob.getContent();

			// First CustomMultiMesh, which is also a CustomMeshNode:
			if (node instanceof CustomMultiMesh) {
				final CustomMultiMesh multi = (CustomMultiMesh) node;
				for (int i = 0; i < multi.size(); i++) {
					meshes.put(mob.getName() + " [" + (i + 1) + "]", multi.getMesh(i));
				}
				// Then CustomMeshNode (all custom meshes):
			}
			else if (node instanceof CustomMeshNode) {
				meshes.put(mob.getName(), ((CustomMeshNode) node).getMesh());
				// An image volume rendered as isosurface:
			}
			else if (node instanceof MeshGroup) {
				meshes.put(mob.getName(), ((MeshGroup) node).getMesh());
			}
			else {
				IJ.log("Ignoring " + mob.getName() + " with node of class " +
					node.getClass());
				continue;
			}
		}
		WavefrontExporter.save(meshes, mtl_filename, w_obj, w_mtl);
	}

	/** A Material, but avoiding name colisions. Not thread-safe. */
	static private int mat_index = 1;

	static private class Mtl {

		float alpha = 1;
		float R = 1;
		float G = 1;
		float B = 1;
		String name;

		Mtl(final float alpha, final Color3f c) {
			this.alpha = alpha;
			final float[] f = new float[3];
			c.get(f);
			this.R = f[0];
			this.G = f[1];
			this.B = f[2];
			name = "mat_" + mat_index;
			mat_index++;
		}

		Mtl(final float alpha, final float R, final float G, final float B) {
			this.alpha = alpha;
			this.R = R;
			this.G = G;
			this.B = B;
			name = "mat_" + mat_index;
			mat_index++;
		}

		@Override
		public boolean equals(final Object ob) {
			if (ob instanceof MeshExporter.Mtl) {
				final Mtl mat = (Mtl) ob;
				if (mat.alpha == alpha && mat.R == R && mat.G == G && mat.B == B) {
					return true;
				}
			}
			return false;
		}

		@Override
		public int hashCode() {
			long bits = 1L;
			bits = 31L * bits + Float.floatToIntBits(alpha);
			bits = 31L * bits + Float.floatToIntBits(R);
			bits = 31L * bits + Float.floatToIntBits(G);
			bits = 31L * bits + Float.floatToIntBits(B);
			return (int) (bits ^ (bits >> 32));
		}

		void fill(final StringBuffer sb) {
			sb.append("\nnewmtl ").append(name).append('\n').append("Ns 96.078431\n")
				.append("Ka 0.0 0.0 0.0\n").append("Kd ").append(R).append(' ').append(
					G).append(' ').append(B).append('\n') // this is INCORRECT but I'll
																								// figure out the conversion
																								// later
				.append("Ks 0.5 0.5 0.5\n").append("Ni 1.0\n").append("d ").append(
					alpha).append('\n').append("illum 2\n\n");
		}

		/** For DXF color */
		int getAsSingle() {
			return (int) ((R + G + B / 3) * 255);
		}
	}

	/** Utility method to encode text data in 8859_1. */
	static public boolean saveToFile(final File f, final String data) {
		if (null == f) return false;
		try {
			final OutputStreamWriter dos =
				new OutputStreamWriter(new BufferedOutputStream(
					new FileOutputStream(f), data.length()), "8859_1"); // encoding in
																															// Latin 1 (for
																															// macosx not to
																															// mess around
			dos.write(data, 0, data.length());
			dos.flush();
		}
		catch (final Exception e) {
			e.printStackTrace();
			IJ.showMessage("ERROR: Most likely did NOT save your file.");
			return false;
		}
		return true;
	}

}
