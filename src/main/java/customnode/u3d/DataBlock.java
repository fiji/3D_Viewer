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

package customnode.u3d;

public class DataBlock {

	private long[] data = null, metaData = null;
	private long dataSize = 0, metaDataSize = 0, priority = 0, blockType = 0;

	public DataBlock() {

	}

	public long getDataSize() {
		return this.dataSize;
	}

	public void setDataSize(final long value) {
		this.dataSize = value;
		// allocate data buffer for block.
		// the data is generally aligned to byte values
		// but array is 4 bytes values . . .
		if ((this.dataSize & 0x3) == 0) this.data = new long[(int) value >> 2];
		else this.data = new long[((int) value >> 2) + 1];
	}

	public long[] getData() {
		return this.data;
	}

	public void setData(final long[] value) {
		this.data = value;
	}

	public long getMetaDataSize() {
		return this.metaDataSize;
	}

	public void setMetaDataSize(final long value) {
		this.metaDataSize = value;
		// allocate data buffer for block.
		// the data is generally aligned to byte values
		// but array is 4 bytes values . . .
		if ((this.metaDataSize & 0x3) == 0) this.metaData =
			new long[(int) value >> 2];
		else this.metaData = new long[((int) value >> 2) + 1];
	}

	public long[] getMetaData() {
		return this.metaData;
	}

	public void setMetaData(final long[] value) {
		if (value.length == this.metaData.length) {
			System.arraycopy(value, 0, this.metaData, 0, value.length);
		}
	}

	public long getBlockType() {
		return this.blockType;
	}

	public void setBlockType(final long value) {
		this.blockType = value;
	}

	public long getPriority() {
		return this.priority;
	}

	public void setPriority(final long value) {
		this.priority = value;
	}
}
