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

package ij3d;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.scijava.java3d.Transform3D;
import org.scijava.vecmath.Color3f;
import org.scijava.vecmath.Point3f;

import customnode.Box;
import customnode.Cone;
import customnode.Sphere;
import customnode.Tube;
import customnode.u3d.U3DExporter;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.GUI;
import ij.plugin.PlugIn;
import ij3d.gui.PrimitiveDialogs;
import isosurface.MeshExporter;
import orthoslice.OrthoGroup;
import voltex.VoltexGroup;

public class ImageJ3DViewer implements PlugIn {

	public static void main(final String[] args) {
		if (IJ.getInstance() == null) new ij.ImageJ();
		IJ.runPlugIn("ij3d.ImageJ3DViewer", "");
	}

	@Override
	public void run(final String arg) {
		final ImagePlus image = WindowManager.getCurrentImage();
		try {
			final Image3DUniverse univ = new Image3DUniverse();
			univ.show();
			GUI.center(univ.getWindow());
			if (arg != null && !arg.equals("")) importContent(arg);
			// only when there is an image and we are not called
			// from a macro
			else if (image != null && !IJ.isMacro()) univ.getExecuter().addContent(
				image, null);

		}
		catch (final Exception e) {
			final StringBuffer buf = new StringBuffer();
			final StackTraceElement[] st = e.getStackTrace();
			buf.append("An unexpected exception occurred. \n"
				+ "Please mail me the following lines if you \n" + "need help.\n"
				+ "bene.schmid@gmail.com\n   \n");
			buf.append(e.getClass().getName() + ":" + e.getMessage() + "\n");
			for (int i = 0; i < st.length; i++) {
				buf.append("    at " + st[i].getClassName() + "." +
					st[i].getMethodName() + "(" + st[i].getFileName() + ":" +
					st[i].getLineNumber() + ")\n");
			}
			new ij.text.TextWindow("Error", buf.toString(), 500, 400);
		}
	}

	private static Image3DUniverse getUniv() {
		if (Image3DUniverse.universes.size() > 0) return Image3DUniverse.universes
			.get(0);
		return null;
	}

	// View menu
	public static void resetView() {
		final Image3DUniverse univ = getUniv();
		if (univ != null) univ.resetView();
	}

	public static void startAnimate() {
		final Image3DUniverse univ = getUniv();
		if (univ != null) univ.startAnimation();
	}

	public static void stopAnimate() {
		final Image3DUniverse univ = getUniv();
		if (univ != null) univ.pauseAnimation();
	}

	public static void record360() {
		final Image3DUniverse univ = getUniv();
		if (univ == null) return;
		final ImagePlus movie = univ.record360();
		if (movie != null) movie.show();
	}

	public static void startFreehandRecording() {
		final Image3DUniverse univ = getUniv();
		if (univ != null) univ.startFreehandRecording();
	}

	public static void stopFreehandRecording() {
		final Image3DUniverse univ = getUniv();
		if (univ == null) return;
		final ImagePlus movie = univ.stopFreehandRecording();
		if (movie != null) movie.show();
	}

	public static void close() {
		final Image3DUniverse univ = getUniv();
		if (univ != null) {
			univ.close();
		}
	}

	public static void select(final String name) {
		final Image3DUniverse univ = getUniv();
		if (univ != null) univ.select(univ.getContent(name));
	}
	
	public static void showContent( final String name, final String b ) {
		final Image3DUniverse univ = getUniv();		
		if ( univ != null ){
			boolean bShow = b.equals( "true" );
			univ.getContent( name ).setVisible( bShow );
			if ( !bShow ) 
				univ.clearSelection();
		}
	}
		
	// Contents menu
	public static void add(final String image, final String c, final String name,
		final String th, final String r, final String g, final String b,
		final String resamplingF, final String type)
	{

		final Image3DUniverse univ = getUniv();
		final ImagePlus grey = WindowManager.getImage(image);
		final Color3f color = ColorTable.getColor(c);

		final int factor = getInt(resamplingF);
		final int thresh = getInt(th);
		final boolean[] channels =
			new boolean[] { getBoolean(r), getBoolean(g), getBoolean(b) };
		final int ty = getInt(type);
		univ.addContent(grey, color, name, thresh, channels, factor, ty);
	}

	public static void addVolume(final String image, final String c,
		final String name, final String r, final String g, final String b,
		final String resamplingF)
	{

		final Image3DUniverse univ = getUniv();
		final ImagePlus grey = WindowManager.getImage(image);
		final Color3f color = ColorTable.getColor(c);

		final int factor = getInt(resamplingF);
		final boolean[] channels =
			new boolean[] { getBoolean(r), getBoolean(g), getBoolean(b) };
		univ.addVoltex(grey, color, name, 0, channels, factor);
	}

	public static void addOrthoslice(final String image, final String c,
		final String name, final String r, final String g, final String b,
		final String resamplingF)
	{

		final Image3DUniverse univ = getUniv();
		final ImagePlus grey = WindowManager.getImage(image);
		final Color3f color = ColorTable.getColor(c);

		final int factor = getInt(resamplingF);
		final boolean[] channels =
			new boolean[] { getBoolean(r), getBoolean(g), getBoolean(b) };
		univ.addOrthoslice(grey, color, name, 0, channels, factor);
	}
	
	/**
	 * Add a sphere into the current 3D universe
	 * @param name content name for the sphere
	 * @param center string containing the center coordinates (Ex: "0,2.3,8.4")
	 * @param radius string containing the radius of the sphere
	 */
	public static void addSphere( 
			final String name, 
			final String center,
			final String radius ) {
		
		final Image3DUniverse univ = getUniv();		
		if ( univ != null ){
			final Point3f c = PrimitiveDialogs.parsePoint( center );
			final float r = Float.parseFloat( radius );
			final Sphere s = new Sphere( c, r );			
			univ.addCustomMesh( s, name );
		}
	}
	
	/**
	 * Add a box into the current 3D universe
	 * @param name content name for the box
	 * @param lowerCorner string containing the lower corner coordinates (Ex: "0,2.3,8.4")
	 * @param upperCorner string containing the upper corner coordinates (Ex: "10,12.3,18.4")
	 */
	public static void addBox( 
			final String name, 
			final String lowerCorner,
			final String upperCorner ) {
		
		final Image3DUniverse univ = getUniv();		
		if ( univ != null ){
			final Point3f lc = PrimitiveDialogs.parsePoint( lowerCorner );
			final Point3f uc = PrimitiveDialogs.parsePoint( upperCorner );					
			univ.addCustomMesh( new Box( lc, uc ) , name );
		}
	}

	/**
	 * Add a cone into the current 3D universe
	 * @param name content name for the cone
	 * @param from string containing the initial coordinates (Ex: "0,2.3,8.4")
	 * @param to string containing the end coordinates (Ex: "10,12.3,18.4")
	 * @param radius string containing the radius of the cone
	 */
	public static void addCone( 
			final String name, 
			final String from,
			final String to,
			final String radius ) {
		
		final Image3DUniverse univ = getUniv();		
		if ( univ != null ){
			final Point3f p1 = PrimitiveDialogs.parsePoint( from );
			final Point3f p2 = PrimitiveDialogs.parsePoint( to );		
			final float r = Float.parseFloat( radius );
			univ.addCustomMesh( new Cone( p1, p2, r ), name );
		}
	}

	/** auxiliary list of points to add to the current tube */
	private static final List<Point3f> pts = new ArrayList<>();
	
	/**
	 * Add a point to the list that will conform a tube into the current 
	 * 3D universe
	 * @param point string containing tube point coordinates
	 */
	public static void addTubePoint( final String point ) {
		
		final Image3DUniverse univ = getUniv();		
		if ( univ != null ){													
			pts.add( PrimitiveDialogs.parsePoint( point ) );						
		}
	}
	
	/**
	 * Add a tube into the current 3D universe
	 * @param name content name for the tube
	 * @param radius string containing the radius of the tube
	 * @param point string containing the coordinates of the last point to add to the tube
	 */
	public static void finishTube( 
			final String name,
			final String radius,
			final String point ) {
		
		final Image3DUniverse univ = getUniv();		
		if ( univ != null ){				
			final float r = Float.parseFloat( radius );
			pts.add( PrimitiveDialogs.parsePoint( point ) );			
			univ.addCustomMesh( new Tube( pts, r ), name );
			
			// reset list of points
			pts.clear();
		}
	}
	
	public static void delete() {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			univ.removeContent(univ.getSelected().getName());
		}
	}

	public static void snapshot(final String w, final String h) {
		final Image3DUniverse univ = getUniv();
		if (univ == null) return;

		final int iw = Integer.parseInt(w);
		final int ih = Integer.parseInt(h);
		univ.takeSnapshot(iw, ih).show();
	}

	// Individual content's menu
	public static void setSlices(final String x, final String y, final String z) {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null &&
			univ.getSelected().getType() == ContentConstants.ORTHO)
		{

			final OrthoGroup vg = (OrthoGroup) univ.getSelected().getContent();
			vg.setSlice(AxisConstants.X_AXIS, getInt(x));
			vg.setSlice(AxisConstants.Y_AXIS, getInt(y));
			vg.setSlice(AxisConstants.Z_AXIS, getInt(z));
		}
	}

	public static void fillSelection() {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null &&
			univ.getSelected().getType() == ContentConstants.VOLUME)
		{

			final VoltexGroup vg = (VoltexGroup) univ.getSelected().getContent();
			final ImageCanvas3D canvas = (ImageCanvas3D) univ.getCanvas();
			vg.fillRoi(canvas, canvas.getRoi(), (byte) 0);
		}
	}

	public static void lock() {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			univ.getSelected().setLocked(true);
		}
	}

	public static void unlock() {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			univ.getSelected().setLocked(false);
		}
	}

	public static void setChannels(final String red, final String green,
		final String blue)
	{
		final Image3DUniverse univ = getUniv();
		final boolean r = Boolean.valueOf(red).booleanValue();
		final boolean g = Boolean.valueOf(green).booleanValue();
		final boolean b = Boolean.valueOf(blue).booleanValue();
		if (univ != null && univ.getSelected() != null) {
			univ.getSelected().setChannels(new boolean[] { r, g, b });
		}
	}

	public static void setColor(final String red, final String green,
		final String blue)
	{
		final Image3DUniverse univ = getUniv();
		if (univ == null || univ.getSelected() == null) return;
		final Content sel = univ.getSelected();
		try {
			final float r = getInt(red) / 256f;
			final float g = getInt(green) / 256f;
			final float b = getInt(blue) / 256f;
			if (univ != null && univ.getSelected() != null) {
				sel.setColor(new Color3f(r, g, b));
			}
		}
		catch (final NumberFormatException e) {
			sel.setColor(null);
		}
	}

	public static void setTransparency(final String t) {
		final Image3DUniverse univ = getUniv();
		final float tr = Float.parseFloat(t);
		if (univ != null && univ.getSelected() != null) {
			univ.getSelected().setTransparency(tr);
		}
	}

	public static void setCoordinateSystem(final String s) {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			univ.getSelected().showCoordinateSystem(getBoolean(s));
		}
	}

	public static void setThreshold(final String s) {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			univ.getSelected().setThreshold(getInt(s));
		}
	}

	public static void applyTransform(final String transform) {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			final String[] s = ij.util.Tools.split(transform);
			final float[] m = new float[s.length];
			for (int i = 0; i < s.length; i++) {
				m[i] = Float.parseFloat(s[i]);
			}
			univ.getSelected().applyTransform(new Transform3D(m));
			univ.fireTransformationUpdated();
		}
	}

	public static void resetTransform() {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			univ.getSelected().setTransform(new Transform3D());
			univ.fireTransformationUpdated();
		}
	}

	public static void saveTransform(final String transform, final String path) {
		final String[] s = ij.util.Tools.split(transform);
		final float[] m = new float[s.length];
		for (int i = 0; i < s.length; i++) {
			m[i] = Float.parseFloat(s[i]);
		}
		new math3d.TransformIO().saveAffineTransform(m);
	}

	public static void setTransform(final String transform) {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			final String[] s = ij.util.Tools.split(transform);
			final float[] m = new float[s.length];
			for (int i = 0; i < s.length; i++) {
				m[i] = Float.parseFloat(s[i]);
			}
			univ.getSelected().setTransform(new Transform3D(m));
			univ.fireTransformationUpdated();
		}
	}

	public static void importContent(final String path) {
		final Image3DUniverse univ = getUniv();
		if (univ != null) {
			univ.addContentLater(path);
		}
	}

	public static void exportTransformed() {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) univ.getSelected()
			.exportTransformed().show();
	}

	public static void exportContent(String format, final String path) {
		final Image3DUniverse univ = getUniv();
		if (univ != null && univ.getSelected() != null) {
			format = format.toLowerCase();
			if (format.equals("dxf")) MeshExporter.saveAsDXF(univ.getContents(),
				new File(path));
			else if (format.equals("wavefront")) MeshExporter.saveAsWaveFront(univ
				.getContents(), new File(path));
			else if (format.startsWith("stl")) {
				if (format.indexOf("ascii") > 0) MeshExporter.saveAsSTL(univ
					.getContents(), new File(path), MeshExporter.ASCII);
				else MeshExporter.saveAsSTL(univ.getContents(), new File(path),
					MeshExporter.BINARY);
			}
			else if (format.equals("u3d")) try {
				U3DExporter.export(univ, path);
			}
			catch (final IOException e) {
				IJ.handleException(e);
			}
		}
	}

	private static int getInt(final String s) {
		return Integer.parseInt(s);
	}

	private static boolean getBoolean(final String s) {
		return new Boolean(s).booleanValue();
	}
}
