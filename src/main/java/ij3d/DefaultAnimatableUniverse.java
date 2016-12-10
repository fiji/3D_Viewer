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

package ij3d;

import org.scijava.java3d.Alpha;
import org.scijava.java3d.BranchGroup;
import org.scijava.java3d.RotationInterpolator;
import org.scijava.java3d.Transform3D;
import org.scijava.java3d.TransformGroup;
import org.scijava.java3d.View;
import org.scijava.vecmath.AxisAngle4d;
import org.scijava.vecmath.Vector3d;
import org.scijava.vecmath.Vector3f;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ImageProcessor;

public abstract class DefaultAnimatableUniverse extends DefaultUniverse {

	/** The axis of rotation */
	private final Vector3d rotationAxis = new Vector3d(0, 1, 0);

	/* Temporary Transform3D objects which are re-used in the methods below */
	private final Transform3D centerXform = new Transform3D();
	private final Transform3D animationXform = new Transform3D();
	private final Transform3D rotationXform = new Transform3D();
	private final Transform3D rotate = new Transform3D();
	private final Transform3D centerXformInv = new Transform3D();

	private float rotationInterval = 2f; // degree

	/**
	 * A reference to the RotationInterpolator used for animation.
	 */
	private final RotationInterpolator rotpol;

	/**
	 * The Alpha object used for interpolation.
	 */
	private final Alpha animation;

	/**
	 * A reference to the TransformGroup of the universe's viewing platform which
	 * is responsible for animation.
	 */
	private final TransformGroup animationTG;

	/**
	 * A reference to the TransformGroup of the universe's viewing platform which
	 * is responsible for rotation.
	 */
	private final TransformGroup rotationTG;

	/**
	 * ImageStack holding the image series after recording an animation.
	 */
	private ImageStack freehandStack;

	/**
	 * Constructor
	 * 
	 * @param width of the universe
	 * @param height of the universe
	 */
	public DefaultAnimatableUniverse(final int width, final int height) {
		super(width, height);

		animationTG = getAnimationTG();
		rotationTG = getRotationTG();

		// Set up the default rotation axis and origin
		updateRotationAxisAndCenter();

		// Animation
		animation = new Alpha(-1, 4000);
		animation.pause();
		animation.setStartTime(System.currentTimeMillis());
		final BranchGroup bg = new BranchGroup();
		rotpol = new RotationInterpolator(animation, animationTG) {

			@Override
			public void processStimulus(final java.util.Enumeration e) {
				super.processStimulus(e);
				if (!animation.isPaused()) {
					fireTransformationUpdated();
				}
				else {
					// this is the point where we actually know that
					// the animation has stopped
					animationPaused();
				}
			}
		};
		rotpol.setTransformAxis(centerXform);
		rotpol.setSchedulingBounds(bounds);
		// set disabled; it's enabled at startAnimation
		rotpol.setEnable(false);
		bg.addChild(rotpol);
		animationTG.addChild(bg);

		addUniverseListener(new UniverseListener() {

			@Override
			public void transformationStarted(final View view) {}

			@Override
			public void transformationFinished(final View view) {}

			@Override
			public void contentAdded(final Content c) {}

			@Override
			public void contentRemoved(final Content c) {}

			@Override
			public void canvasResized() {}

			@Override
			public void universeClosed() {}

			@Override
			public void contentSelected(final Content c) {}

			@Override
			public void transformationUpdated(final View view) {
				addFreehandRecordingFrame();
			}

			@Override
			public void contentChanged(final Content c) {
				addFreehandRecordingFrame();
			}
		});
	}

	/**
	 * Set the rotation interval (in degree)
	 */
	public void setRotationInterval(final float f) {
		this.rotationInterval = f;
	}

	/**
	 * Returns the rotation interval (in degree)
	 */
	public float getRotationInterval() {
		return rotationInterval;
	}

	/**
	 * Add a new frame to the freehand recording stack.
	 */
	private void addFreehandRecordingFrame() {
		if (freehandStack == null) return;

		win.updateImagePlus();
		final ImageProcessor ip = win.getImagePlus().getProcessor();
		freehandStack.addSlice("", ip);
	}

	/**
	 * Start freehand recording.
	 */
	public void startFreehandRecording() {
		// check if is's already running.
		if (freehandStack != null) return;

		// create a new stack
		win.updateImagePlus();
		final ImageProcessor ip = win.getImagePlus().getProcessor();
		freehandStack = new ImageStack(ip.getWidth(), ip.getHeight());
		freehandStack.addSlice("", ip);
	}

	/**
	 * Stop freehand recording. Returns an ImagePlus whose stack contains the
	 * frames of the movie.
	 */
	public ImagePlus stopFreehandRecording() {
		if (freehandStack == null || freehandStack.getSize() == 1) return null;

		final ImagePlus imp = new ImagePlus("Movie", freehandStack);
		freehandStack = null;
		return imp;
	}

	/**
	 * Records a full 360 degree rotation and returns an ImagePlus containing the
	 * frames of the animation.
	 */
	public ImagePlus record360() {
		// check if freehand recording is running
		if (freehandStack != null) {
			IJ.error("Freehand recording is active. Stop first.");
			return null;
		}
		// stop the animation
		if (!animation.isPaused()) pauseAnimation();
		// create a new stack
		ImageProcessor ip = win.getImagePlus().getProcessor();
		ImageStack stack = new ImageStack(ip.getWidth(), ip.getHeight());
		// prepare everything
		updateRotationAxisAndCenter();
		try {
			Thread.sleep(1000);
		}
		catch (final Exception e) {
			e.printStackTrace();
		}
		centerXformInv.invert(centerXform);
		final double deg2 = rotationInterval * Math.PI / 180;
		final int steps = (int) Math.round(2 * Math.PI / deg2);

		getCanvas().getView().stopView();

		double alpha = 0;

		// update transformation and record
		for (int i = 0; i < steps; i++) {
			alpha = i * deg2;
			rotationXform.rotY(alpha);
			rotate.mul(centerXform, rotationXform);
			rotate.mul(rotate, centerXformInv);
			animationTG.setTransform(rotate);
			fireTransformationUpdated();
			getCanvas().getView().renderOnce();
			win.updateImagePlusAndWait();
			ip = win.getImagePlus().getProcessor();
			final int w = ip.getWidth(), h = ip.getHeight();
			if (stack == null) stack = new ImageStack(w, h);
			stack.addSlice("", ip);
		}

		// restart the view and animation
		getCanvas().getView().startView();

		// cleanup
		incorporateAnimationInRotation();

		if (stack.getSize() == 0) return null;
		final ImagePlus imp = new ImagePlus("Movie", stack);
		return imp;
	}

	/**
	 * Copy the current rotation axis into the given Vector3f.
	 */
	public void getRotationAxis(final Vector3f ret) {
		ret.set(rotationAxis);
	}

	/**
	 * Set the rotation axis to the specified Vector3f.
	 */
	public void setRotationAxis(final Vector3f a) {
		rotationAxis.set(a);
	}

	/**
	 * Convenience method which rotates the universe around the x-axis (regarding
	 * the view, not the vworld) the specified amount of degrees (in rad).
	 */
	public void rotateX(final double rad) {
		viewTransformer.rotateX(rad);
	}

	/**
	 * Convenience method which rotates the universe around the y-axis (regarding
	 * the view, not the vworld) the specified amount of degrees (in rad).
	 */
	public void rotateY(final double rad) {
		viewTransformer.rotateY(rad);
		fireTransformationUpdated();
	}

	/**
	 * Convenience method which rotates the universe around the z-axis (regarding
	 * the view, not the vworld) the specified amount of degrees (in rad).
	 */
	public void rotateZ(final double rad) {
		viewTransformer.rotateZ(rad);
	}

	/**
	 * Starts animating the universe.
	 */
	public void startAnimation() {
		animationTG.getTransform(animationXform);
		updateRotationAxisAndCenter();
		rotpol.setTransformAxis(centerXform);
		rotpol.setEnable(true);
		animation.resume();
		fireTransformationStarted();
	}

	/**
	 * Pauses the animation.
	 */
	public void pauseAnimation() {
		animation.pause();
	}

	/**
	 * Called from the RotationInterpolator, indicating that the animation was
	 * paused.
	 */
	private void animationPaused() {
		rotpol.setEnable(false);
		incorporateAnimationInRotation();
		animation.setStartTime(System.currentTimeMillis());
		fireTransformationUpdated();
		fireTransformationFinished();
	}

	/**
	 * After animation was stopped, the transformation of the animation
	 * TransformGroup is incorporated in the rotation TransformGroup and the
	 * animation TransformGroup is set to identity. This is necessary, because
	 * otherwise a following rotation by the mouse would not take place around the
	 * expected axis.
	 */
	private void incorporateAnimationInRotation() {
		rotationTG.getTransform(rotationXform);
		animationTG.getTransform(animationXform);
		rotationXform.mul(rotationXform, animationXform);

		animationXform.setIdentity();
		animationTG.setTransform(animationXform);
		rotationTG.setTransform(rotationXform);
	}

	private final Vector3d v1 = new Vector3d();
	private final Vector3d v2 = new Vector3d();
	private final AxisAngle4d aa = new AxisAngle4d();

	private void updateRotationAxisAndCenter() {
		v1.set(0, 1, 0);
		v2.cross(v1, rotationAxis);
		final double angle = Math.acos(v1.dot(rotationAxis));
		aa.set(v2, angle);
		centerXform.set(aa);
	}
}
