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

package customnode.u3d;

/// <summary>Constants is a class that holds constants that are needed by more than
/// one of the objects in the U3D namespace.</summary>
public class Constants {

	// context ranges
	// / <summary>
	// / the context for uncompressed U8
	// / </summary>
	public static final int Context8 = 0;
	// / <summary>
	// / contexts >= StaticFull are static contexts.
	// / </summary>
	public static final int StaticFull = 0x00000400;
	// /<summary>
	// /The largest allowable static context. values written to contexts >
	// MaxRange are
	// /written as uncompressed.
	// /</summary>
	public static final int MaxRange = StaticFull + 0x00003FFF;
	// / <summary>
	// / a defualt buffer size for U3D
	// / </summary>
	public static final int SizeBuff = 1024;
	// / <summary>
	// / the initial size allocated for buffers
	// / </summary>
	public static final int DataSizeInitial = 0x00000010;
	// Bit masks for reading and writing symbols.
	// / <summary>
	// / masks all but the most significan bit
	// / </summary>
	public static final int HalfMask = 0x00008000;
	// / <summary>
	// / masks the most significant bit
	// / </summary>
	public static final int NotHalfMask = 0x00007FFF;
	// / <summary>
	// / masks all but the 2nd most significan bit
	// / </summary>
	public static final int QuarterMask = 0x00004000;
	// / <summary>
	// / masks the 2 most significant bits
	// / </summary>
	public static final int NotThreeQuarterMask = 0x00003FFF;
	// / <summary>
	// / used to swap 8 bits in place
	// / </summary>
	public static final long[] Swap8 = { 0, 8, 4, 12, 2, 10, 6, 14, 1, 9, 5, 13,
		3, 11, 7, 15 };
}
