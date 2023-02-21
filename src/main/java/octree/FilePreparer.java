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

package octree;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.Properties;

import vib.NaiveResampler;

public class FilePreparer {

	private static final NaiveResampler.Averager accu =
		new NaiveResampler.Averager();

	private static final class Volume {

		private final int w, h, d;
		private final double pw, ph, pd;
		private final long wh;
		private final RandomAccessFile ra;

		Volume(final File file, final int w, final int h, final int d,
			final double pw, final double ph, final double pd) throws IOException
		{

			ra = new RandomAccessFile(file, "rw");
			this.w = w;
			this.h = h;
			this.d = d;
			this.pw = pw;
			this.ph = ph;
			this.pd = pd;
			this.wh = w * h;
		}

		final void close() throws IOException {
			ra.close();
		}

		final int get(final int x, final int y, final int z) throws IOException {
			if (x < 0 || x >= w || y < 0 || y >= h || z < 0 || z >= d) return 0;
			final long i = z * wh + y * w + x;
			ra.seek(i);
			return 0xff & ra.readByte();
		}

		final void createBlock(final int x, final int y, final int z,
			final String dir, final String file, final int size) throws IOException
		{
			final byte[] blob = new byte[size * size * size];
			int i = 0;
			for (int iz = 0; iz < size && z + iz < d; iz++) {
				for (int iy = 0; iy < size && y + iy < h; iy++, i += size) {
					final int n = Math.min(size, w - x);
					final long pos = (z + iz) * wh + (y + iy) * w + x;
					ra.seek(pos);
					ra.readFully(blob, i, n);
				}
			}

			final DataOutputStream fos =
				new DataOutputStream(new FileOutputStream(dir + "/" + file + ".info"));
			fos.writeFloat((float) pw);
			fos.writeFloat((float) ph);
			fos.writeFloat((float) pd);
			fos.close();

			writeBlob(blob, dir + "/z/" + file);

			byte[] b = createYBlobFromZ(blob, size);
			writeBlob(b, dir + "/y/" + file);

			b = createXBlobFromZ(blob, size);
			writeBlob(b, dir + "/x/" + file);
		}

		final void writeBlob(final byte[] blob, final String file)
			throws IOException
		{
			final DataOutputStream fos =
				new DataOutputStream(new FileOutputStream(file));
			fos.write(blob, 0, blob.length);
			fos.close();
		}

		static final byte[] createYBlobFromZ(final byte[] blob, final int size) {
			final byte[] ret = new byte[blob.length];
			final int s2 = size * size;
			for (int y = 0; y < size; y++) {
				for (int z = 0; z < size; z++) {
					System.arraycopy(blob, z * s2 + y * size, ret, y * s2 + z * size,
						size);
				}
			}
			return ret;
		}

		static final byte[] createXBlobFromZ(final byte[] blob, final int size) {
			final byte[] ret = new byte[blob.length];
			final int s2 = size * size;
			for (int z = 0; z < size; z++) {
				for (int y = 0; y < size; y++) {
					for (int x = 0; x < size; x++) {
						ret[x * s2 + z * size + y] = blob[z * s2 + y * size + x];
					}
				}
			}
			return ret;
		}

		final void downsample(final String file, final int fx, final int fy,
			final int fz) throws IOException
		{
			final FileOutputStream out = new FileOutputStream(file);

			final int ws = nextPow2(w), hs = nextPow2(h), ds = nextPow2(d);
			final int wn = ws / fx, hn = hs / fy, dn = ds / fz;
			final byte[] bytes = new byte[wn];
			final byte[][] cache = new byte[fz * fy][ws];
			for (int tmp = 0; tmp < fz * fy; tmp++)
				Arrays.fill(cache[tmp], (byte) 0);
			int z = 0, y = 0;
			for (z = 0; z < ds; z += fz) {
				// update cache
				System.arraycopy(cache, fy, cache, 0, (fz - 1) * fy);
				for (int ytmp = 0; ytmp < fy; ytmp++) {
					Arrays.fill(cache[(fz - 1) * fy], (byte) 0);
					if (y + ytmp < h) {
						final long pos = z * wh + (y + ytmp) * w;
						ra.seek(pos);
						ra.readFully(cache[(fz - 1) * fy]);
					}
				}

				for (y = 0; y < hs; y += fy) {
					// update cache
					for (int ztmp = 0; ztmp < fz; ztmp++) {
						System.arraycopy(cache, ztmp * fy + 1, cache, ztmp * fy, fy - 1);
						Arrays.fill(cache[ztmp + fy - 1], (byte) 0);
						if (z + ztmp < d) {
							final long pos = (z + ztmp) * wh + (y + fy - 1) * w;
							ra.seek(pos);
							ra.readFully(cache[ztmp + fy - 1]);
						}
					}

					for (int x = 0; x < ws; x += fx) {
						accu.reset();
						for (int k = 0; k < fz; k++) {
							final int iz = z + k;
							for (int j = 0; j < fy; j++) {
								final int iy = y + j;
								for (int i = 0; i < fx; i++) {
									final int ix = x + i;
									accu.add(0xff & cache[k * fy + j][ix]);
								}
							}
						}
						bytes[x / fx] = (byte) accu.get();
					}
					out.write(bytes, 0, wn);
				}
			}
		}
	}

	public static final void createFiles(String path, final int size,
		final String dir, int w, int h, int d, double pw, double ph, double pd)
		throws IOException
	{

		final int wOrg = w, hOrg = h, dOrg = d;
		final double pwOrg = pw, phOrg = ph, pdOrg = pd;
		int level = 1;

		new File(dir, "x").mkdir();
		new File(dir, "y").mkdir();
		new File(dir, "z").mkdir();

		while (true) {
			final File file = new File(path);
			final Volume v = new Volume(file, w, h, d, pw, ph, pd);
			for (int z = 0; z < d; z += size) {
				for (int y = 0; y < h; y += size) {
					for (int x = 0; x < w; x += size) {
						final String n =
							(x * level) + "_" + (y * level) + "_" + (z * level) + "_" + level;
						v.createBlock(x, y, z, dir, n, size);
					}
				}
			}
			final int fx = w > size ? 2 : 1;
			final int fy = h > size ? 2 : 1;
			final int fz = d > size ? 2 : 1;

			if (fx == 1 && fy == 1 && fz == 1) break;

			final File downs = new File(dir, file.getName() + ".l" + level);
			v.downsample(downs.getPath(), fx, fy, fz);
			v.close();
			pw *= fx;
			ph *= fy;
			pd *= fz;
			w = nextPow2(w) / fx;
			h = nextPow2(h) / fy;
			d = nextPow2(d) / fz;

			if (level > 1) file.delete();
			path = downs.getPath();
			level <<= 1;
		}
		writeProperties(wOrg, hOrg, dOrg, pwOrg, phOrg, pdOrg, level, dir +
			"/props.txt");
	}

	private static final void writeProperties(final int w, final int h,
		final int d, final double pw, final double ph, final double pd,
		final int l, final String path) throws IOException
	{

		final Properties props = new Properties();
		props.setProperty("width", Integer.toString(w));
		props.setProperty("height", Integer.toString(h));
		props.setProperty("depth", Integer.toString(d));
		props.setProperty("level", Integer.toString(l));
		props.setProperty("pixelWidth", Float.toString((float) pw));
		props.setProperty("pixelHeight", Float.toString((float) ph));
		props.setProperty("pixelDepth", Float.toString((float) pd));

		final FileOutputStream fw = new FileOutputStream(new File(path));
		props.store(fw, "octree");
	}

	private static final int nextPow2(final int n) {
		int retval = 2;
		while (retval < n) {
			retval = retval << 1;
		}
		return retval;
	}
}
