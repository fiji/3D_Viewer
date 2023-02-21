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

import org.scijava.java3d.Appearance;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.ColoringAttributes;
import org.scijava.java3d.PolygonAttributes;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.TransparencyAttributes;
import org.scijava.java3d.utils.geometry.Sphere;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

public class BoundingSphere extends BranchGroup {

	private final Point3f center;
	private float radius;
	private final Color3f color;
	private final float transparency;

	private final TransformGroup scaleTG;
	private final Transform3D scale = new Transform3D();

	private final TransformGroup translateTG;
	private final Transform3D translate = new Transform3D();

	private final TransparencyAttributes ta;
	private final PolygonAttributes pa;
	private final ColoringAttributes ca;
	private final Appearance appearance;

	public BoundingSphere(final Point3f center, final float radius) {
		this(center, radius, new Color3f(1, 0, 0));
	}

	public BoundingSphere(final Point3f center, final float radius,
		final Color3f color)
	{
		this(center, radius, color, 0f);
	}

	public BoundingSphere(final Point3f center, final float radius,
		final Color3f color, final float transparency)
	{
		setCapability(BranchGroup.ALLOW_DETACH);
//		setCapability(BranchGroup.ENABLE_PICK_REPORTING);
		this.center = new Point3f(center);
		this.radius = radius;
		this.color = color;
		this.transparency = transparency;

		appearance = new Appearance();
		pa = new PolygonAttributes();
		pa.setPolygonMode(PolygonAttributes.POLYGON_LINE);
		pa.setCullFace(PolygonAttributes.CULL_NONE);
		appearance.setPolygonAttributes(pa);
		ta = new TransparencyAttributes();
		ta.setTransparency(transparency);
		ta.setTransparencyMode(TransparencyAttributes.FASTEST);
		appearance.setTransparencyAttributes(ta);
		ca = new ColoringAttributes();
		ca.setColor(color);
		appearance.setColoringAttributes(ca);

		final Sphere sphere =
			new Sphere(1, /*Primitive.ENABLE_GEOMETRY_PICKING,*/appearance);
		sphere.setName("BS");

		final Vector3f translateV = new Vector3f(center);
		translate.set(translateV);
		translateTG = new TransformGroup(translate);
		translateTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		scale.set(radius);
		scaleTG = new TransformGroup(scale);
		scaleTG.setCapability(TransformGroup.ALLOW_TRANSFORM_WRITE);

		scaleTG.addChild(sphere);
		translateTG.addChild(scaleTG);
		addChild(translateTG);
	}

	public void setRadius(final float radius) {
		this.radius = radius;
		scale.set(radius);
		scaleTG.setTransform(scale);
	}

	public void setCenter(final Point3f center) {
		this.center.set(center);
		final Vector3f translateV = new Vector3f(center);
		translate.set(translateV);
		translateTG.setTransform(translate);
	}

	public void getTransform(final Transform3D transform) {
		transform.set(new Vector3f(center));
		transform.mul(scale);
	}

	public void getTransformInverse(final Transform3D transform) {
		getTransform(transform);
		transform.invert();
	}

	@Override
	public String toString() {
		return "[BoundingSphere center: " + center + " radius: " + radius + "] " +
			hashCode();
	}
}
