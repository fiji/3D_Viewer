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

package ij3d.behaviors;

import java.awt.event.MouseEvent;

import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.vecmath.AxisAngle4d;
import org.scijava.vecmath.Point3d;
import org.scijava.vecmath.Vector3d;

import ij3d.Content;
import ij3d.DefaultUniverse;
import ij3d.ImageCanvas3D;

/**
 * This class is a helper class which transforms MouseEvents to an appropriate
 * transformation of the selected Content.
 *
 * @author Benjamin Schmid
 */
public class ContentTransformer {

	private final Initializer initializer;

	private final DefaultUniverse univ;
	private final ImageCanvas3D canvas;
	private final BehaviorCallback callback;
	private Content content;

	private final Vector3d axisPerDx = new Vector3d();
	private final Vector3d axisPerDy = new Vector3d();
	private double anglePerPix;

	private final AxisAngle4d aaX = new AxisAngle4d();
	private final AxisAngle4d aaY = new AxisAngle4d();
	private final Transform3D transX = new Transform3D();
	private final Transform3D transY = new Transform3D();

	private final Transform3D transl = new Transform3D();
	private final Transform3D transl_inv = new Transform3D();

	private final Vector3d translationPerDx = new Vector3d();
	private final Vector3d translationPerDy = new Vector3d();

	private TransformGroup translateTG, rotateTG;

	private int xLast, yLast;

	/**
	 * Constructs a new ContentTransformer.
	 * 
	 * @param univ
	 * @param callback
	 */
	public ContentTransformer(final DefaultUniverse univ,
		final BehaviorCallback callback)
	{
		this.univ = univ;
		this.canvas = (ImageCanvas3D) univ.getCanvas();
		this.callback = callback;
		this.initializer = new Initializer();
	}

	/**
	 * This method should be called to initiate a new transformation, e.g. when
	 * the mouse is pressed before rotation or translation.
	 * 
	 * @param c
	 * @param x
	 * @param y
	 */
	public void init(final Content c, final int x, final int y) {
		initializer.init(c, x, y);
	}

	/**
	 * Translate the selected Content suitably to the specified MouseEvent.
	 * 
	 * @param e
	 */
	public void translate(final MouseEvent e) {
		translate(e.getX(), e.getY());
	}

	/**
	 * Rotate the selected Content suitably to the specified MouseEvent.
	 * 
	 * @param e
	 */
	public void rotate(final MouseEvent e) {
		rotate(e.getX(), e.getY());
	}

	private final Transform3D translateNew = new Transform3D();
	private final Transform3D translateOld = new Transform3D();
	private final Vector3d translation = new Vector3d();
	private final Point3d v1 = new Point3d();
	private final Point3d v2 = new Point3d();

	void translate(final int xNew, final int yNew) {
		if (content == null || content.isLocked()) return;
		final int dx = xNew - xLast;
		final int dy = yNew - yLast;
		translateTG.getTransform(translateOld);
		v1.scale(dx, translationPerDx);
		v2.scale(-dy, translationPerDy);
		translation.add(v1, v2);
		translateNew.set(translation);
		translateNew.mul(translateOld);

		translateTG.setTransform(translateNew);
		transformChanged(BehaviorCallback.TRANSLATE, translateNew);

		xLast = xNew;
		yLast = yNew;
	}

	private final Transform3D rotateNew = new Transform3D();
	private final Transform3D rotateOld = new Transform3D();

	void rotate(final int xNew, final int yNew) {
		if (content == null || content.isLocked()) return;

		final int dx = xNew - xLast;
		final int dy = yNew - yLast;

		aaX.set(axisPerDx, dx * anglePerPix);
		aaY.set(axisPerDy, dy * anglePerPix);

		transX.set(aaX);
		transY.set(aaY);

		rotateTG.getTransform(rotateOld);

		rotateNew.set(transl_inv);
		rotateNew.mul(transY);
		rotateNew.mul(transX);
		rotateNew.mul(transl);
		rotateNew.mul(rotateOld);

		rotateTG.setTransform(rotateNew);
		xLast = xNew;
		yLast = yNew;

		transformChanged(BehaviorCallback.ROTATE, rotateNew);
	}

	private void transformChanged(final int type, final Transform3D t) {
		if (callback != null) callback.transformChanged(type, t);
	}

	private class Initializer {

		private final Point3d centerInVWorld = new Point3d();
		private final Point3d centerInIp = new Point3d();

		private final Transform3D ipToVWorld = new Transform3D();
		private final Transform3D ipToVWorldInverse = new Transform3D();
		private final Transform3D localToVWorld = new Transform3D();
		private final Transform3D localToVWorldInverse = new Transform3D();

		private final Point3d eyePtInVWorld = new Point3d();
		private final Point3d pickPtInVWorld = new Point3d();

		private final Point3d p1 = new Point3d();
		private final Point3d p2 = new Point3d();
		private final Point3d p3 = new Point3d();

		private final Vector3d vec = new Vector3d();

		private void init(final Content c, final int x, final int y) {
			xLast = x;
			yLast = y;

			content = c;

			// some transforms
			c.getLocalToVworld(localToVWorld);
			localToVWorldInverse.invert(localToVWorld);
			canvas.getImagePlateToVworld(ipToVWorld);
			ipToVWorldInverse.invert(ipToVWorld);

			// calculate the canvas position in world coords
			c.getContent().getCenter(centerInVWorld);
			localToVWorld.transform(centerInVWorld);
			ipToVWorldInverse.transform(centerInVWorld, centerInIp);

			// get the eye point in world coordinates
			canvas.getCenterEyeInImagePlate(eyePtInVWorld);
			ipToVWorld.transform(eyePtInVWorld);

			// use picking to infer the radius of the virtual sphere which is rotated
			final Point3d p = univ.getPicker().getPickPointGeometry(c, x, y);
			float r = 0, dD = 0;
			if (p != null) {
				pickPtInVWorld.set(p);
				localToVWorld.transform(pickPtInVWorld);
				r = (float) pickPtInVWorld.distance(centerInVWorld);
			}
			else {
				c.getContent().getMin(p1);
				localToVWorld.transform(p1);
				r = (float) p1.distance(centerInVWorld);
				vec.sub(centerInVWorld, eyePtInVWorld);
				vec.normalize();
				vec.scale(-r);
				pickPtInVWorld.add(centerInVWorld, vec);
			}
			dD = (float) pickPtInVWorld.distance(eyePtInVWorld);

			// calculate distance between eye and canvas point
			canvas.getPixelLocationInImagePlate(x, y, p1);
			ipToVWorld.transform(p1);
			final float dd = (float) p1.distance(eyePtInVWorld);

			// calculate the virtual distance between two neighboring pixels
			canvas.getPixelLocationInImagePlate(x + 1, y, p2);
			ipToVWorld.transform(p2);
			final float dx = (float) p1.distance(p2);

			// calculate the virtual distance between two neighboring pixels
			canvas.getPixelLocationInImagePlate(x, y + 1, p3);
			ipToVWorld.transform(p3);
			final float dy = (float) p1.distance(p3);

			final float dX = dD / dd * dx;
			final float dY = dD / dd * dy;

			anglePerPix = Math.atan2(dX, r);

			univ.getViewPlatformTransformer().getYDir(axisPerDx, ipToVWorld);
			univ.getViewPlatformTransformer().getXDir(axisPerDy, ipToVWorld);

			translationPerDx.set(axisPerDy);
			translationPerDx.scale(dX);

			translationPerDy.set(axisPerDx);
			translationPerDy.scale(dY);

			rotateTG = c.getLocalRotate();
			translateTG = c.getLocalTranslate();
			c.getContent().getCenter(vec);
			transl_inv.set(vec);
			vec.set(-vec.x, -vec.y, -vec.z);
			transl.set(vec);
		}
	}
}
