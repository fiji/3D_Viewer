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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.scijava.vecmath.Point3f;

import ij.IJ;

public class STLLoader {

	/**
	 * Load the specified stl file and returns the result as a hash map, mapping
	 * the object names to the corresponding <code>CustomMesh</code> objects.
	 */
	public static Map<String, CustomMesh> load(final String stlfile)
		throws IOException
	{
		final STLLoader sl = new STLLoader();
		try {
			sl.parse(stlfile);
		}
		catch (final RuntimeException e) {
			System.out.println("error reading " + sl.name);
			throw e;
		}
		return sl.meshes;
	}

	private HashMap<String, CustomMesh> meshes;

	private STLLoader() {}

	String line;
	BufferedReader in;

	// attributes of the currently read mesh
	private ArrayList<Point3f> vertices = new ArrayList<Point3f>();
	private String name = null;
	private String stlfile = null;
	private final Point3f normal = new Point3f(0.0f, 0.0f, 0.0f); // to be used
																																// for file
																																// checking
	private FileInputStream fis;
	private int triangles;
	private final DecimalFormat decimalFormat = new DecimalFormat("0.0E0");

	private void parse(final String stlfile) throws IOException {
		this.stlfile = stlfile;
		final File f = new File(stlfile);
		name = f.getName();

		// determine if this is a binary or ASCII STL
		// and send to the appropriate parsing method

		// Hypothesis 1: this is an ASCII STL
		final BufferedReader br = new BufferedReader(new FileReader(stlfile));
		final String line = br.readLine();
		final String[] words = line.trim().split("\\s+");
		if (line.indexOf('\0') < 0 && words[0].equalsIgnoreCase("solid")) {
			IJ.log("Looks like an ASCII STL");
			parseAscii(f);
			return;
		}

		// Hypothesis 2: this is a binary STL
		final FileInputStream fs = new FileInputStream(f);

		// bytes 80, 81, 82 and 83 form a little-endian int
		// that contains the number of triangles
		final byte[] buffer = new byte[84];
		fs.read(buffer, 0, 84);
		triangles =
			((buffer[83] & 0xff) << 24) | ((buffer[82] & 0xff) << 16) |
				((buffer[81] & 0xff) << 8) | (buffer[80] & 0xff);
		if (((f.length() - 84) / 50) == triangles) {
			IJ.log("Looks like a binary STL");
			parseBinary(f);
			return;
		}
		IJ.error("STL Import", "File is not a valid STL");
	}

	private void parseAscii(final File f) {
		try {
			in = new BufferedReader(new FileReader(stlfile));
		}
		catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		meshes = new HashMap<String, CustomMesh>();
		vertices = new ArrayList<Point3f>();
		try {
			while ((line = in.readLine()) != null) {
				final String[] numbers = line.trim().split("\\s+");
				if (numbers[0].equals("vertex")) {
					final float x = parseFloat(numbers[1]);
					final float y = parseFloat(numbers[2]);
					final float z = parseFloat(numbers[3]);
					final Point3f vertex = new Point3f(x, y, z);
					vertices.add(vertex);
				}
				else if (numbers[0].equals("facet") && numbers[1].equals("normal")) {
					normal.x = parseFloat(numbers[2]);
					normal.y = parseFloat(numbers[3]);
					normal.z = parseFloat(numbers[4]);
				}
			}
			in.close();
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		catch (final ParseException e) {
			e.printStackTrace();
		}
		final CustomMesh cm = createCustomMesh();
		meshes.put(name, cm);
	}

	private void parseBinary(final File f) {
		meshes = new HashMap<String, CustomMesh>();
		vertices = new ArrayList<Point3f>();
		try {
			fis = new FileInputStream(f);
			for (int h = 0; h < 84; h++)
				fis.read();// skip the header bytes
			for (int t = 0; t < triangles; t++) {
				final byte[] tri = new byte[50];
				for (int tb = 0; tb < 50; tb++) {
					tri[tb] = (byte) fis.read();
				}
				normal.x = leBytesToFloat(tri[0], tri[1], tri[2], tri[3]);
				normal.y = leBytesToFloat(tri[4], tri[5], tri[6], tri[7]);
				normal.z = leBytesToFloat(tri[8], tri[9], tri[10], tri[11]);
				for (int i = 0; i < 3; i++) {
					final int j = i * 12 + 12;
					final float px =
						leBytesToFloat(tri[j], tri[j + 1], tri[j + 2], tri[j + 3]);
					final float py =
						leBytesToFloat(tri[j + 4], tri[j + 5], tri[j + 6], tri[j + 7]);
					final float pz =
						leBytesToFloat(tri[j + 8], tri[j + 9], tri[j + 10], tri[j + 11]);
					final Point3f p = new Point3f(px, py, pz);
					vertices.add(p);
				}
			}
			fis.close();
		}
		catch (final FileNotFoundException e) {
			e.printStackTrace();
		}
		catch (final IOException e) {
			e.printStackTrace();
		}
		final CustomMesh cm = createCustomMesh();
		meshes.put(name, cm);
	}

	private float parseFloat(String string) throws ParseException {
		// E+05 -> E05, e+05 -> E05
		string = string.replaceFirst("[eE]\\+", "E");
		// E-05 -> E-05, e-05 -> E-05
		string = string.replaceFirst("e\\-", "E-");
		return decimalFormat.parse(string).floatValue();
	}

	private float leBytesToFloat(final byte b0, final byte b1, final byte b2,
		final byte b3)
	{
		return Float.intBitsToFloat((((b3 & 0xff) << 24) | ((b2 & 0xff) << 16) |
			((b1 & 0xff) << 8) | (b0 & 0xff)));
	}

	private CustomMesh createCustomMesh() {
		if (vertices.size() == 0) return null;
		CustomMesh cm = null;
		cm = new CustomTriangleMesh(vertices);
		cm.loadedFromName = name;
		cm.changed = false;
		return cm;
	}

}
