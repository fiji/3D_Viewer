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

package ij3d.shapes;

import java.awt.Font;
import java.text.DecimalFormat;

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.Font3D;
import org.scijava.java3d.FontExtrusion;
import org.scijava.java3d.Geometry;
import org.scijava.java3d.GeometryArray;
import org.scijava.java3d.LineArray;
import org.scijava.java3d.OrientedShape3D;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.Shape3D;
import org.scijava.java3d.Text3D;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

public class Scalebar extends BranchGroup {

	private final DecimalFormat df = new DecimalFormat("###0.00");

	private float length = 1f;
	private Color3f color = new Color3f(1.0f, 1.0f, 1.0f);
	private float x = 2, y = 2;
	private String unit = "";

	private final TransformGroup positionTG, textTG;
	private final Shape3D lineShape;
	private final OrientedShape3D textShape;

	public Scalebar() {
		this(1f);
	}

	public Scalebar(final float length) {
		final Transform3D position = new Transform3D();
		positionTG = new TransformGroup(position);
		positionTG.setCapability(TransformGroup.ALLOW_TRANSFORM_READ);
		positionTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		addChild(positionTG);

		lineShape = new Shape3D();
		lineShape.setGeometry(createLineGeometry());
		lineShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		positionTG.addChild(lineShape);

		final Transform3D texttranslate = new Transform3D();
		texttranslate.setTranslation(new Vector3f(length / 2, -length / 2, 0.0f));

		textTG = new TransformGroup(texttranslate);
		textTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);
		positionTG.addChild(textTG);

		textShape = new OrientedShape3D();
		textShape.setCapability(Shape3D.ALLOW_GEOMETRY_WRITE);
		textShape.setCapability(Shape3D.ALLOW_APPEARANCE_WRITE);
		textShape.setGeometry(createTextGeometry());
		textShape.setAppearance(createTextAppearance());
		textShape.setAlignmentAxis(0.0f, 1.0f, 0.0f);
		textTG.addChild(textShape);
	}

	public float getX() {
		return x;
	}

	public float getY() {
		return y;
	}

	public float getLength() {
		return length;
	}

	public String getUnit() {
		return unit;
	}

	public Color3f getColor() {
		return color;
	}

	public void setUnit(final String unit) {
		this.unit = unit;
		textShape.setGeometry(createTextGeometry());
	}

	public void setPosition(final float x, final float y) {
		this.x = x;
		this.y = y;
		final Transform3D p = new Transform3D();
		p.setTranslation(new Vector3f(x, y, 0.0f));
		positionTG.setTransform(p);
	}

	public void setLength(final float l) {
		this.length = l;
		lineShape.setGeometry(createLineGeometry());
		textShape.setGeometry(createTextGeometry());
		final Transform3D d = new Transform3D();
		d.setTranslation(new Vector3f(length / 2, -length / 2, 0.0f));
		textTG.setTransform(d);
	}

	public void setColor(final Color3f c) {
		this.color = c;
		lineShape.setGeometry(createLineGeometry());
		textShape.setAppearance(createTextAppearance());
	}

	public Appearance createTextAppearance() {
		final Appearance textAppear = new Appearance();
		final ColoringAttributes textColor = new ColoringAttributes();
		textColor.setColor(color);
		textAppear.setColoringAttributes(textColor);

		final PolygonAttributes pa = new PolygonAttributes();
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		pa.setBackFaceNormalFlip(true);
		textAppear.setPolygonAttributes(pa);

		return textAppear;
	}

	public Geometry createTextGeometry() {
		final String text = df.format(length) + " " + unit;
		final int fontSize = (int) length / 3;
		final Font3D font3D =
			new Font3D(
				new Font("Helvetica", Font.PLAIN, fontSize > 0 ? fontSize : 1),
				new FontExtrusion());
		try {
			final Text3D textGeom = new Text3D(font3D, text);
			textGeom.setAlignment(Text3D.ALIGN_CENTER);
			return textGeom;
		}
		catch (final Exception e) {}
		return null;
	}

	public Geometry createLineGeometry() {
		final Point3f origin = new Point3f();
		final Point3f onX = new Point3f(length < 1 ? 1f : length, 0, 0);

		final Point3f[] coords = { origin, onX };
		final int N = coords.length;

		final Color3f colors[] = new Color3f[N];
		for (int i = 0; i < N; i++) {
			colors[i] = color;
		}
		final LineArray ta =
			new LineArray(N, GeometryArray.COORDINATES | GeometryArray.COLOR_3);
		ta.setCoordinates(0, coords);
		ta.setColors(0, colors);

		return ta;
	}
}
