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

package ij3d;

import java.awt.AWTException;
import java.awt.GraphicsConfigTemplate;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Label;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.lang.reflect.Method;

import javax.swing.JFrame;

import org.scijava.java3d.Canvas3D;
import org.scijava.java3d.GraphicsConfigTemplate3D;
import org.scijava.java3d.ImageComponent;
import org.scijava.java3d.ImageComponent2D;
import org.scijava.java3d.RenderingError;
import org.scijava.java3d.RenderingErrorListener;
import org.scijava.java3d.Screen3D;
import org.scijava.java3d.View;
import org.scijava.java3d.utils.universe.SimpleUniverse;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.process.ColorProcessor;

public class ImageWindow3D extends JFrame implements UniverseListener {

	private DefaultUniverse universe;
	private final ImageCanvas3D canvas3D;
	private final Label status = new Label("");
	private boolean noOffScreen = true;
	private final ErrorListener error_listener;
	private ImagePlus imp;

	public ImageWindow3D(final String title, final DefaultUniverse universe) {
		super(title);
		final String j3dNoOffScreen = System.getProperty("j3d.noOffScreen");
		if (j3dNoOffScreen != null && j3dNoOffScreen.equals("true")) noOffScreen =
			true;
		imp = new ImagePlus();
		imp.setTitle("ImageJ 3D Viewer");
		this.universe = universe;
		this.canvas3D = (ImageCanvas3D) universe.getCanvas();

		error_listener = new ErrorListener();
		error_listener.addTo(universe);

		add(canvas3D, -1);

		addWindowListener(new WindowAdapter() {

			@Override
			public void windowClosing(final WindowEvent e) {
				close();
			}
		});

		universe.addUniverseListener(this);
		updateImagePlus();
		universe.ui.setHandTool();
	}

	public DefaultUniverse getUniverse() {
		return universe;
	}

	public ImageCanvas getCanvas() {
		return new ImageCanvas(getImagePlus());
	}

	/* off-screen rendering stuff */
	private Canvas3D offScreenCanvas3D;

	private Canvas3D getOffScreenCanvas() {
		if (offScreenCanvas3D != null) return offScreenCanvas3D;

		final GraphicsConfigTemplate3D templ = new GraphicsConfigTemplate3D();
		templ.setDoubleBuffer(GraphicsConfigTemplate.UNNECESSARY);
		final GraphicsConfiguration gc =
			GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice().getBestConfiguration(templ);

		offScreenCanvas3D = new Canvas3D(gc, true);
		final Screen3D sOn = canvas3D.getScreen3D();
		final Screen3D sOff = offScreenCanvas3D.getScreen3D();
		sOff.setSize(sOn.getSize());
		sOff.setPhysicalScreenWidth(sOn.getPhysicalScreenWidth());
		sOff.setPhysicalScreenHeight(sOn.getPhysicalScreenHeight());

		universe.getViewer().getView().addCanvas3D(offScreenCanvas3D);

		return offScreenCanvas3D;
	}

	private static ImagePlus makeDummyImagePlus() {
		final ColorProcessor cp = new ColorProcessor(1, 1);
		return new ImagePlus("3D", cp);
	}

	public void updateImagePlus() {
		// this.imp = getNewImagePlus();
		imp_updater.update();
	}

	public void updateImagePlusAndWait() {
		imp_updater.updateAndWait();
	}

	void quitImageUpdater() {
		imp_updater.quit();
	}

	final ImagePlusUpdater imp_updater = new ImagePlusUpdater();

	private class ImagePlusUpdater extends Thread {

		boolean go = true;
		int update = 0;

		ImagePlusUpdater() {
			super("3D-V-IMP-updater");
			try {
				setDaemon(true);
			}
			catch (final Exception e) {
				e.printStackTrace();
			}
			setPriority(Thread.NORM_PRIORITY);
			start();
		}

		void update() {
			synchronized (this) {
				update++;
				notify();
			}
		}

		void updateAndWait() {
			update();
			synchronized (this) {
				while (update > 0) {
					try {
						wait();
					}
					catch (final InterruptedException ie) {
						ie.printStackTrace();
					}
				}
			}
		}

		@Override
		public void run() {
			while (go) {
				final int u;
				synchronized (this) {
					if (0 == update) {
						try {
							wait();
						}
						catch (final InterruptedException ie) {
							ie.printStackTrace();
						}
					}
					u = update;
				}
				ImageWindow3D.this.imp = getNewImagePlus();
				synchronized (this) {
					if (u != update) continue; // try again, there was a new request
					// Else, done:
					update = 0;
					notify(); // for updateAndWait
				}
			}
		}

		void quit() {
			go = false;
			synchronized (this) {
				update = -Integer.MAX_VALUE;
				notify();
			}
		}
	}

	public ImagePlus getImagePlus() {
		if (imp == null) imp_updater.updateAndWait(); // updateImagePlus();
		return imp;
	}

	private final int top = 0, bottom = 0, left = 0, right = 0;

	private ImagePlus getNewImagePlus() {
		if (getWidth() <= 0 || getHeight() <= 0) return makeDummyImagePlus();
		if (noOffScreen) {
			if (universe != null && universe.getUseToFront()) toFront();
			final Point p = canvas3D.getLocationOnScreen();
			final int w = canvas3D.getWidth();
			final int h = canvas3D.getHeight();
			Robot robot;
			try {
				robot = new Robot(getGraphicsConfiguration().getDevice());
			}
			catch (final AWTException e) {
				return makeDummyImagePlus();
			}
			final Rectangle r =
				new Rectangle(p.x + left, p.y + top, w - left - right, h - top - bottom);
			final BufferedImage bImage = robot.createScreenCapture(r);
			final ColorProcessor cp = new ColorProcessor(bImage);
			final ImagePlus result = new ImagePlus("3d", cp);
			result.setRoi(canvas3D.getRoi());
			return result;
		}
		BufferedImage bImage =
			new BufferedImage(canvas3D.getWidth(), canvas3D.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		final ImageComponent2D buffer =
			new ImageComponent2D(ImageComponent.FORMAT_RGBA, bImage);

		try {
			getOffScreenCanvas();
			offScreenCanvas3D.setOffScreenBuffer(buffer);
			offScreenCanvas3D.renderOffScreenBuffer();
			offScreenCanvas3D.waitForOffScreenRendering();
			bImage = offScreenCanvas3D.getOffScreenBuffer().getImage();
			// To release the reference of buffer inside Java 3D.
			offScreenCanvas3D.setOffScreenBuffer(null);
		}
		catch (final Exception e) {
			noOffScreen = true;
			universe.getViewer().getView().removeCanvas3D(offScreenCanvas3D);
			offScreenCanvas3D = null;
			System.err.println("Java3D error: "
				+ "Off-screen rendering not supported by this\n"
				+ "setup. Falling back to screen capturing");
			return getNewImagePlus();
		}

		final ColorProcessor cp = new ColorProcessor(bImage);
		final ImagePlus result = new ImagePlus("3d", cp);
		result.setRoi(canvas3D.getRoi());
		return result;
	}

	public Label getStatusLabel() {
		return status;
	}

	public void close() {
		if (null == universe) return;
		universe.removeUniverseListener(this);

		// Must remove the listener so this instance can be garbage
		// collected and removed from the Canvas3D, overcomming the limit
		// of 32 total Canvas3D instances.
		try {
			final Method m =
				SimpleUniverse.class.getMethod("removeRenderingErrorListener",
					new Class[] { RenderingErrorListener.class });
			if (null != m) m.invoke(universe, new Object[] { error_listener });
		}
		catch (final Exception ex) {
			System.out.println("Could NOT remove the RenderingErrorListener!");
			ex.printStackTrace();
		}

		if (null != universe.getWindow()) universe.cleanup();

		imp_updater.quit();
		canvas3D.flush();
		universe = null;
		dispose();
	}

	/*
	 * The UniverseListener interface
	 */
	@Override
	public void universeClosed() {}

	@Override
	public void transformationStarted(final View view) {}

	@Override
	public void transformationUpdated(final View view) {}

	@Override
	public void contentSelected(final Content c) {}

	@Override
	public void transformationFinished(final View view) {
		updateImagePlus();
	}

	@Override
	public void contentAdded(final Content c) {
		updateImagePlus();
	}

	@Override
	public void contentRemoved(final Content c) {
		updateImagePlus();
	}

	@Override
	public void contentChanged(final Content c) {
		updateImagePlus();
	}

	@Override
	public void canvasResized() {
		updateImagePlus();
	}

	private int lastToolID;

	private class ErrorListener implements RenderingErrorListener {

		@Override
		public void errorOccurred(final RenderingError error) {
			throw new RuntimeException(error.getDetailMessage());
		}

		/*
		 * This is a slightly ugly workaround for DefaultUniverse not
		 * having addRenderingErrorListener() in Java3D 1.5.
		 * The problem, of course, is that Java3D 1.5 just exit(1)s
		 * on error by default, _unless_ you add a listener!
		 */
		public void addTo(final DefaultUniverse universe) {
			try {
				final Class[] params = { RenderingErrorListener.class };
				final Class c = universe.getClass();
				final String name = "addRenderingErrorListener";
				final Method m = c.getMethod(name, params);
				final Object[] list = { this };
				m.invoke(universe, list);
			}
			catch (final Exception e) {
				/* method not found -> Java3D 1.4 */
				System.err.println("Java3D < 1.5 detected");
				// e.printStackTrace();
			}
		}
	}
}
