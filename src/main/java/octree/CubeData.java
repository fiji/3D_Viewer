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

package octree;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.scijava.java3d.TexCoordGeneration;
import org.scijava.vecmath.Vector4f;

import ij3d.AxisConstants;

public class CubeData implements AxisConstants {

	private static final int SIZE = VolumeOctree.SIZE;
	private static final int B_IMG_TYPE = BufferedImage.TYPE_BYTE_GRAY;

	final float[] cal = new float[3];
	final float[] min = new float[3];
	final float[] max = new float[3];

	BufferedImage[] images;

	private final TexCoordGeneration tgx, tgy, tgz;

	int axis;

	TexCoordGeneration tg;
	ShapeGroup[] shapes;
	Cube cube;

	public CubeData(final Cube c) {
		this.cube = c;
		readCalibration(c.dir + c.name + ".info", cal);

		min[0] = c.x * c.octree.pw;
		min[1] = c.y * c.octree.ph;
		min[2] = c.z * c.octree.pd;

		max[0] = min[0] + SIZE * cal[0];
		max[1] = min[1] + SIZE * cal[1];
		max[2] = min[2] + SIZE * cal[2];

		final float xTexGenScale = (float) (1.0 / (cal[0] * SIZE));
		final float yTexGenScale = (float) (1.0 / (cal[1] * SIZE));
		final float zTexGenScale = (float) (1.0 / (cal[2] * SIZE));

		tgz = new TexCoordGeneration();
		tgz.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(xTexGenScale * min[0])));
		tgz.setPlaneT(new Vector4f(0f, yTexGenScale, 0f, -(yTexGenScale * min[1])));

		tgx = new TexCoordGeneration();
		tgx.setPlaneS(new Vector4f(0f, yTexGenScale, 0f, -(yTexGenScale * min[1])));
		tgx.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(zTexGenScale * min[2])));

		tgy = new TexCoordGeneration();
		tgy.setPlaneS(new Vector4f(xTexGenScale, 0f, 0f, -(xTexGenScale * min[0])));
		tgy.setPlaneT(new Vector4f(0f, 0f, zTexGenScale, -(zTexGenScale * min[2])));

		shapes = new ShapeGroup[SIZE];
		for (int i = 0; i < SIZE; i++)
			shapes[i] = new ShapeGroup();

		images = new BufferedImage[SIZE];
	}

	public void prepareForAxis(final int axis) {
		this.axis = axis;
		for (int i = 0; i < SIZE; i++)
			shapes[i].prepareForAxis(min[axis] + cal[axis] * i);
		switch (axis) {
			case X_AXIS:
				tg = tgx;
				break;
			case Y_AXIS:
				tg = tgy;
				break;
			case Z_AXIS:
				tg = tgz;
				break;
		}
	}

	public void show() {
		try {
			createData();
		}
		catch (final IOException e) {
			throw new RuntimeException(e);
		}
		for (int i = 0; i < SIZE; i++)
			shapes[i].show(this, i);
	}

	public void hide() {
		for (int i = 0; i < SIZE; i++)
			shapes[i].hide();
		releaseData();
	}

	private void createData() throws IOException {
		switch (axis) {
			case X_AXIS:
				createImages(cube.dir + "/x/" + cube.name);
				break;
			case Y_AXIS:
				createImages(cube.dir + "/y/" + cube.name);
				break;
			case Z_AXIS:
				createImages(cube.dir + "/z/" + cube.name);
				break;
		}
	}

	private void releaseData() {
		for (int i = 0; i < SIZE; i++)
			images[i] = null;
		tg = null;
	}

	public static final float[] readCalibration(final String path, float[] ret) {
		if (ret == null) ret = new float[3];
		final File f = new File(path);
		DataInputStream in = null;
		try {
			in = new DataInputStream(new FileInputStream(f));
			if (in == null) return null;
			ret[0] = in.readFloat();
			ret[1] = in.readFloat();
			ret[2] = in.readFloat();
			in.close();
		}
		catch (final Exception ex) {
			ex.printStackTrace();
			return null;
		}
		return ret;
	}

	private void createImages(final String path) throws IOException {
		final DataInputStream is = new DataInputStream(new FileInputStream(path));
		for (int i = 0; i < SIZE; i++) {
			images[i] = new BufferedImage(SIZE, SIZE, B_IMG_TYPE);
			final byte[] pixels =
				((DataBufferByte) images[i].getRaster().getDataBuffer()).getData();
			is.readFully(pixels);
		}
		is.close();
	}
}
