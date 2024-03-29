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
/* -*- mode: java; c-basic-offset: 8; indent-tabs-mode: t; tab-width: 8 -*- */

/**
 * Example plugin on how to add spheres and tubes to the 3D Viewer.
 * Albert Cardona 2008-12-09
 * Released under the General Public License, latest version.
 */

package customnode;

import java.awt.Color;

import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;

import ij.plugin.PlugIn;
import ij3d.Content;
import ij3d.Image3DUniverse;

public class Mesh_Maker extends MeshMaker implements PlugIn {

	@Override
	public void run(final String arg) {
		final Image3DUniverse univ = new Image3DUniverse(512, 512);
		univ.show();

		// define two spheres: an X,Y,Z point with a radius
		final double x1 = 10, y1 = 100, z1 = 150, r1 = 20;
		final Color3f color1 = new Color3f(Color.pink);

		final double x2 = 50, y2 = 200, z2 = 40, r2 = 35;
		final Color3f color2 = new Color3f(Color.white);

		// define a tube as a polyline in space
		final double[] px = new double[] { 100, 230, 320, 400 };
		final double[] py = new double[] { 100, 120, 230, 400 };
		final double[] pz = new double[] { 100, 200, 300, 400 };
		final double[] pr = new double[] { 10, 15, 40, 110 };
		final Color3f colort = new Color3f(Color.yellow);

		// define a second tube as a curving spiral in space
		final double[] px2 = new double[200];
		final double[] py2 = new double[200];
		final double[] pz2 = new double[200];
		final double[] pr2 = new double[200];
		final Color3f color_t2 = new Color3f(Color.magenta);
		for (int i = 0; i < px2.length; i++) {
			final double angle = Math.toRadians(10 * i);
			final double radius = 50 + i * 5;
			px2[i] = Math.cos(angle) * radius;
			py2[i] = Math.sin(angle) * radius;
			pz2[i] = i * 5;
			pr2[i] = 10;
		}

		// Add both spheres and the tubes
		// Last parameter is the resampling (1 means no resampling)

		final Content sph1 =
			univ.addMesh(createSphere(x1, y1, z1, r1, 12, 12), color1, "Sphere 1", 1);
		final Content sph2 =
			univ.addMesh(createSphere(x2, y2, z2, r2, 12, 12), color2, "Sphere 2", 1);
		final Content tube1 =
			univ.addMesh(createTube(px, py, pz, pr, 12, false), colort, "Tube", 1);
		final Content tube2 =
			univ.addMesh(createTube(px2, py2, pz2, pr2, 12, false), color_t2,
				"Tube spiral", 1);
		final Content disc1 =
			univ.addMesh(createDisc(100, 100, 50, 3, 3, 3, 50, 12), new Color3f(
				Color.blue), "Disc 1", 1);

		// Extra:
		// Now modify some attributes:

		// Lock the transformation of the spiral:
		// so it can't be rotated relative to its center when selected
		// Try it: when selecting the spiral and rotating it with the mouse,
		// the whole view will rotate, not the spiral relative to itself.
		tube2.toggleLock();

		// Change the transparency of the first tube
		tube1.setTransparency(0.5f); // from 0 (opaque) to 1 (fully transparent)

		disc1.setTransparency(0.3f);

		// Move the small sphere in x,y,z to a new location
		sph1.applyTransform(new Transform3D(new double[] { 1, 0, 0, 50, 0, 1, 0,
			100, 0, 0, 1, -50, 0, 0, 0, 1 }));
	}
}
