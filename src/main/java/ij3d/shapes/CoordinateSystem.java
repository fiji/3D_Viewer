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

package ij3d.shapes;

import java.awt.Font;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.LineArray;
import org.scijava.java3d.OrientedShape3D;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.utils.geometry.Text2D;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

public class CoordinateSystem extends BranchGroup {

	private final float length;
	private final Color3f color;

	public CoordinateSystem(final float length, final Color3f color) {
		this.length = length;
		this.color = color;
		setCapability(BranchGroup.ALLOW_DETACH);

		final Shape3D lines = new Shape3D();
		lines.setGeometry(createGeometry());
		addChild(lines);

		// the appearance for all texts
		final Appearance textAppear = new Appearance();
		final ColoringAttributes textColor = new ColoringAttributes();
		textColor.setColor(color);
		textAppear.setColoringAttributes(textColor);

		final PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setBackFaceNormalFlip(true);
		textAppear.setPolygonAttributes(pa);

		try {
			final Transform3D translate = new Transform3D();

			translate.setTranslation(new Vector3f(length, -length / 8, 0.0f));
			addText("x", translate, textAppear);
			translate.setTranslation(new Vector3f(-length / 8, length, 0.0f));
			addText("y", translate, textAppear);
			translate.setTranslation(new Vector3f(-length / 8, -length / 8, length));
			addText("z", translate, textAppear);
		}
		catch (final Exception e) {
// 			e.printStackTrace();
		}
	}

	public void addText(final String s, final Transform3D translate,
		final Appearance textAppear)
	{

		// translation
		final TransformGroup tg = new TransformGroup(translate);
		addChild(tg);
		// text
		final OrientedShape3D textShape = new OrientedShape3D();
		textShape.setAlignmentMode(OrientedShape3D.ROTATE_ABOUT_POINT);
		textShape.setAlignmentAxis(0.0f, 1.0f, 0.0f);
		textShape.setRotationPoint(new Point3f(0, 0, 0));
		textShape.setConstantScaleEnable(true);
		final Text2D t2d = new Text2D(s, color, "Helvetica", 24, Font.PLAIN);
		t2d.setRectangleScaleFactor(0.03f);
		textShape.setGeometry(t2d.getGeometry());
		textShape.setAppearance(t2d.getAppearance());

		tg.addChild(textShape);
	}

	public Geometry createGeometry() {
		final Point3f origin = new Point3f();
		final Point3f onX = new Point3f(length, 0, 0);
		final Point3f onY = new Point3f(0, length, 0);
		final Point3f onZ = new Point3f(0, 0, length);

		final Point3f[] coords = { origin, onX, origin, onY, origin, onZ };
		final int N = coords.length;

		final Color3f colors[] = new Color3f[N];
		for (int i = 0; i < N; i++) {
			colors[i] = color;
		}

		final LineArray ta =
			new LineArray(N, GeometryArray.COORDINATES | GeometryArray.COLOR_3);
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);
		// initialize the geometry info here

		return ta;
	}
}
