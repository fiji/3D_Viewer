
package marchingcubes;

import java.util.ArrayList;
import java.util.List;

import javax.swing.text.View;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.scijava.Context;
import org.scijava.vecmath.Point3f;

import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.measure.Calibration;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.ImageProcessor;
import ij3d.AreaListVolume;
import ij3d.Volume;
import isosurface.Triangulator;
import net.imagej.ops.OpService;
import net.imagej.ops.geom.geom3d.DefaultMarchingCubes;
import net.imagej.ops.geom.geom3d.mesh.DefaultMesh;
import vib.NaiveResampler;
import net.imagej.ops.geom.geom3d.mesh.Facet;
import net.imagej.ops.geom.geom3d.mesh.Mesh;
import net.imagej.ops.geom.geom3d.mesh.TriangularFacet;
import net.imglib2.AbstractInterval;
import net.imglib2.AbstractRealInterval;
import net.imglib2.FinalInterval;
import net.imglib2.Interval;
import net.imglib2.RandomAccessible;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.RealInterval;
import net.imglib2.RealRandomAccessible;
import net.imglib2.img.Img;
import net.imglib2.img.imageplus.ImagePlusImg;
import net.imglib2.interpolation.randomaccess.LanczosInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NLinearInterpolatorFactory;
import net.imglib2.interpolation.randomaccess.NearestNeighborInterpolatorFactory;
import net.imglib2.type.logic.BitType;
import net.imglib2.type.numeric.NumericType;
import net.imglib2.type.numeric.integer.ByteType;
import net.imglib2.type.numeric.integer.IntType;
import net.imglib2.type.numeric.integer.UnsignedByteType;
import net.imglib2.view.Views;
import net.imglib2.realtransform.AbstractScale;
import net.imglib2.realtransform.AffineTransform;
import net.imglib2.realtransform.RealViews;
import net.imglib2.realtransform.Scale3D;

public class MCTriangulator implements Triangulator {

	@Override
	public List getTriangles(ImagePlus image, final int threshold,
		final boolean[] channels, final int resamplingF)
	{

		if (resamplingF != 1) image = NaiveResampler.resample(image, resamplingF);
		// There is no need to zero pad any more. MCCube automatically
		// scans one pixel more in each direction, assuming a value
		// of zero outside the image.
		// zeroPad(image);
		// create Volume
		final Volume volume = new Volume(image, channels);
		volume.setAverage(true);

		// get triangles
		final List l = MCCube.getTriangles(volume, threshold);		
		
		System.out.println( "3DViewer: Number of vertices in triangulator: " + l.size() );
		
		return l;
	}
		
	static public Point3f convertVector3DtoPoint3f( Vector3D v ) {
		return new Point3f( (float) v.getX(), (float) v.getY(), (float) v.getZ() );
	}
	
	public List getTrianglesOps(ImagePlus imp, final int threshold,
		final boolean[] channels, final int resamplingF)
	{

		if (resamplingF != 1) imp = NaiveResampler.resample(imp, resamplingF);
		// There is no need to zero pad any more. MCCube automatically
		// scans one pixel more in each direction, assuming a value
		// of zero outside the image.
		// zeroPad(image);
		// create Volume
		//final Volume volume = new Volume(image, channels);
		//volume.setAverage(true);

		// get triangles
		//final List l = MCCube.getTriangles(volume, threshold);
		DefaultMarchingCubes dmc = new net.imagej.ops.geom.geom3d.DefaultMarchingCubes();
		
		Context context = (Context) IJ.runPlugIn("org.scijava.Context", "");
		OpService ops = context.service( OpService.class );
		
		Img<UnsignedByteType> img = net.imglib2.img.ImagePlusAdapter.wrap(imp);
		
		Calibration cal = imp.getCalibration();
		AbstractScale tform = new Scale3D( cal.pixelWidth, cal.pixelHeight, cal.pixelDepth );
		
		//final RealRandomAccessible< UnsignedByteType > interpolated = Views.interpolate( img, new NearestNeighborInterpolatorFactory< UnsignedByteType >() );
		final RealRandomAccessible< UnsignedByteType > interpolated = Views.interpolate( img, new NLinearInterpolatorFactory< UnsignedByteType >() );
		//final RealRandomAccessible< UnsignedByteType > interpolated = Views.interpolate( Views.extendZero( img ), new LanczosInterpolatorFactory< UnsignedByteType >() );
		final RealRandomAccessible< UnsignedByteType > transformed = RealViews.affine( interpolated, tform ); 			
		
		/* Threshold then interpolate == bad
		Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply( img, new UnsignedByteType( threshold ) );
						
		Calibration cal = imp.getCalibration();
		
		AbstractScale tform = new Scale3D( cal.pixelWidth, cal.pixelHeight, cal.pixelDepth );
		
		final RealRandomAccessible< BitType > interpolated = Views.interpolate( bitImg, new NearestNeighborInterpolatorFactory< BitType >() );
		final RealRandomAccessible< BitType > transformed = RealViews.affine( interpolated, tform ); */
		
		long[] minPos = new long[3];
		long[] maxPos = new long[3];
		img.min( minPos );
		img.max( maxPos );
		double[] dMinPos = new double[3]; dMinPos[0] = minPos[0]; dMinPos[1] = minPos[1]; dMinPos[2] = minPos[2]; 
		double[] dMaxPos = new double[3]; dMaxPos[0] = maxPos[0]; dMaxPos[1] = maxPos[1]; dMaxPos[2] = maxPos[2];
		double[] tformMinPos = new double[3];
		double[] tformMaxPos = new double[3];		
		tform.apply( dMinPos, tformMinPos );
		tform.apply( dMaxPos, tformMaxPos );
		//RealInterval tinterval = new AbstractRealInterval( tformMinPos, tformMaxPos ); 
		
		long[] lTformMinPos = new long[3]; for( int k = 0; k < tformMinPos.length; k++ ) lTformMinPos[k] = (long) tformMinPos[k];
		long[] lTformMaxPos = new long[3]; for( int k = 0; k < tformMaxPos.length; k++ ) lTformMaxPos[k] = (long) tformMaxPos[k];
		Interval tinterval = new FinalInterval( lTformMinPos, lTformMaxPos ); 
		
		//Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply( Views.interval( Views.raster( transformed ), tinterval ), new UnsignedByteType( threshold ) );
		Img<BitType> bitImg = (Img<BitType>) ops.threshold().apply( Views.interval( Views.raster( transformed ), tinterval ), new UnsignedByteType( threshold ) );		
		
		
		//Mesh mesh = ops.geom().marchingcubes( Views.interval( Views.raster( transformed ), (Interval) tinterval ) );
		Mesh mesh = ops.geom().marchingcubes( bitImg );
		
		
		
		List<Point3f> l = new ArrayList<Point3f>(); 
		
		for( Facet facet : mesh.getFacets() ) {
			TriangularFacet tfacet = (TriangularFacet) facet;
			l.add( convertVector3DtoPoint3f( tfacet.getP0() ) );
			l.add( convertVector3DtoPoint3f( tfacet.getP1() ) );
			l.add( convertVector3DtoPoint3f( tfacet.getP2() ) );
		}
		
		System.out.println( "Ops: Number of vertices in triangulator: " + l.size() );
		
		//System.out.println( "Number of vertices in triangulator: " + l.size() + " Number of faces from ops-mc: " + mesh.getFacets().size() + " Sum intensity(img): " + ops.stats().sum(img) + " Sum intensity(bitImg): " + ops.stats().sum(bitImg) );
		
		return l;
	}

	/**
	 * Triangulates the specified volume.
	 *
	 * @param volume the volume to triangulate
	 * @return the triangles
	 */
	public List<Point3f> getTriangles(final AreaListVolume volume)
		throws Exception
	{
		return MCCube.getTriangles(volume, 128);
	}

	static public void zeroPad(final ImagePlus imp) {
		final ImageStack stack = imp.getStack();
		final int w = stack.getWidth();
		final int h = stack.getHeight();
		final int d = stack.getSize();
		final int type = imp.getType();
		// create new stack
		final ImageStack st = new ImageStack(w + 2, h + 2);

		// retrieve 1st processor
		ImageProcessor old = stack.getProcessor(1);

		// enlarge it and add it as a first slide.
		ImageProcessor ne = createProcessor(w + 2, h + 2, type);
		st.addSlice("", ne);

		// now do the same for all slices in the old stack
		for (int z = 0; z < d; z++) {
			old = stack.getProcessor(z + 1);
			ne = createProcessor(w + 2, h + 2, type);
			ne.insert(old, 1, 1);
			st.addSlice(Integer.toString(z + 1), ne);
		}

		// now add an empty new slice
		ne = createProcessor(w + 2, h + 2, type);
		st.addSlice(Integer.toString(d + 1), ne);

		imp.setStack(null, st);

		// update the origin
		final Calibration cal = imp.getCalibration();
		cal.xOrigin -= cal.pixelWidth;
		cal.yOrigin -= cal.pixelHeight;
		cal.zOrigin -= cal.pixelDepth;
		imp.setCalibration(cal);
	}

	private static final ImageProcessor createProcessor(final int w, final int h,
		final int type)
	{
		if (type == ImagePlus.COLOR_RGB) return new ColorProcessor(w, h);
		return new ByteProcessor(w, h);
	}
}
