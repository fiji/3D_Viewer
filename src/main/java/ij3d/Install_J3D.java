
package ij3d;

import ij.IJ;
import ij.gui.YesNoCancelDialog;
import ij.plugin.PlugIn;

import java.awt.Frame;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import javax.media.j3d.VirtualUniverse;

public class Install_J3D implements PlugIn {

	/** Base URL to the folder containing the Java3D files */
	private static final String JAVA3D_BASE_URL = "http://132.187.25.13/java3d/";

	/** Base URL to the folder containing the Jogl files */
	private static final String JOGL_BASE_URL =
		"http://download.java.net/media/jogl/builds/archive/jsr-231-1.1.1/";

	/** File name of the 32-bit windows version of Java3D */
	private static final String WIN_32 = "j3d-1_5_2-windows-i586.zip";

	/** File name of the 64-bit windows version of Java3D */
	private static final String WIN_64 = "j3d-1_5_2-windows-amd64.zip";

	/** File name of the 32-bit linux version of Java3D */
	private static final String LIN_32 = "j3d-1_5_2-linux-i586.zip";

	/** File name of the 64-bit linux version of Java3D */
	private static final String LIN_64 = "j3d-1_5_2-linux-amd64.zip";

	/** File name of the mac osx version of Java3D */
	private static final String MAC = "j3d-1_5_2-macosx.zip";

	/** File name of the mac osx version for power pc of Jogl */
	private static final String JOGL_PPC = "jogl-1.1.1-macosx-ppc.zip";

	/** File name of the mac osx version for intel macs of Jogl */
	private static final String JOGL_ITL = "jogl-1.1.1-macosx-universal.zip";

	/** Temporary directory */
	private static final String TMP = System.getProperty("java.io.tmpdir");

	/** Java home directory */
	private static final String JRE = System.getProperty("java.home");

	/** Overwrite without asking for confirmation? */
	protected static boolean overwrite = false;

	/**
	 * Run the installation stand-alone.
	 */
	public static void main(final String[] args) {
		overwrite = true;
		new Install_J3D();
		if (!Install_J3D.autoInstall()) System.exit(1);
	}

	/**
	 * Run the installation as a plugin.
	 */
	@Override
	public void run(final String arg) {
		autoInstall();
	}

	/**
	 * Returns the Java3D specification version, as returned by
	 * Universe.getProperty("j3d.specification.version"), or null if the universe
	 * class can not be loaded.
	 */
	public static String getJava3DVersion() {
		try {
			Class.forName("javax.media.j3d.VirtualUniverse");
		}
		catch (final ClassNotFoundException e) {
			return null;
		}
		final VirtualUniverse univ = new VirtualUniverse();
		return (String) VirtualUniverse.getProperties().get(
			"j3d.specification.version");
	}

	/**
	 * Detects the operating system and accordingly downloads the needed Java3D
	 * files and extracts them. In case of windows and linux, the j3djre.zip file,
	 * which is contained in the downloaded archive, is simply extracted in the
	 * java.home directory. In case of mac, the j3djre.zip is extracted into a
	 * temporary directory, and the jar files are then moved to the 1st folder as
	 * returned by System.getProperty("java.ext.dirs"). On Mac, Jogl has to be
	 * installed, in addition to Java3D. Depending on the architecture, the
	 * corresponding files are downloaded and extracted, as the files of
	 * j3djre.zip, to the 1st extension directory of java.
	 *
	 * @return true on success
	 */
	public static boolean autoInstall() {
		String filename = null;
		if (IJ.isLinux()) filename = IJ.is64Bit() ? LIN_64 : LIN_32;
		else if (IJ.isWindows()) filename = IJ.is64Bit() ? WIN_64 : WIN_32;
		else if (IJ.isMacOSX()) filename = MAC;

		if (filename == null) {
			println("could not detect operating system");
			return false;
		}

		try {
			installJava3D(filename);
			if (IJ.isMacOSX()) {
				filename = isPPC() ? JOGL_PPC : JOGL_ITL;
				installJogl(filename);
			}
		}
		catch (final Exception e) {
			IJ.error(e.getMessage());
			println(e.getMessage());
			e.printStackTrace();
			return false;
		}
		print("Installation successful!");
		return true;
	}

	/**
	 * Returns true if running on a Power PC.
	 */
	public static boolean isPPC() {
		final String arch = System.getProperty("os.arch");
		if (arch.startsWith("ppc")) return true;
		return false;
	}

	/**
	 * Downloads the given file from the JOGL homepage and extracts the contained
	 * jnilibs to the first directory as returned by
	 * System.getProperty("java.ext.dirs").
	 */
	public static void installJogl(final String zipname) throws Exception {
		println("Detecting Mac OS X operating system, installing Jogl");
		final File downloaded = download(JOGL_BASE_URL + zipname);
		final File tempdir = createFolder(TMP, "jogl", false);
		final List<File> files = unzip(downloaded, tempdir, null);

		final String extdir = getFirstExtDir();
		if (!new File(extdir).exists()) new File(extdir).mkdirs();
		if (!new File(extdir).canWrite()) throw new Exception(
			"No permissions to write to " + extdir);
		for (final File file : files) {
			if (!file.getName().endsWith(".jar") &&
				!file.getName().endsWith(".jnilib")) continue;

			final File dst = new File(extdir, file.getName());
			println("moving " + file.getAbsolutePath() + " to " +
				dst.getAbsolutePath());
			if (!file.renameTo(dst)) {
				println("could not move " + file.getAbsolutePath() + " to " +
					dst.getAbsolutePath());
			}
		}
		rm(tempdir);
	}

	public static String getFirstExtDir() throws Exception {
		final String extdirs = System.getProperty("java.ext.dirs");
		if (extdirs == null) throw new Exception(
			"Can't detect java extension directory");
		return extdirs.split(File.pathSeparator)[0];
	}

	/**
	 * Downloads the given file from the Java3D homepage and extracts the
	 * contained j3d-jre.zip to a temporary directory. In case of Linux or
	 * Windows, this file is afterwards extracted in the java.home folder. In case
	 * of MacOSX it is extracted to a temporary folder, and the jar files are then
	 * moved to the first folder returned by System.getProperty("java.ext.dirs").
	 */
	public static void installJava3D(final String zipname) throws Exception {
		final File downloaded = download(JAVA3D_BASE_URL + zipname);
		final File tempdir = createFolder(TMP, "java3d", false);

		List<File> files = unzip(downloaded, tempdir, "j3d-jre.zip");
		if (files.size() == 0) throw new Exception(
			"Could not find j3d-jre.zip in " + downloaded);
		final File j3djre = files.get(0);

		/*
		 * if not on a Mac, the j3djre zip file can just be extracted
		 * in java.home.
		 */
		if (!IJ.isMacOSX()) {
			files = unzip(j3djre, new File(JRE), null);
			return;
		}

		/*
		 * On a Mac, we extract the zip file to a temporary folder and
		 * move the contents afterwards in Mac's first java.ext folder.
		 */
		files = unzip(j3djre, tempdir, null);
		final String extdir = getFirstExtDir();
		if (!new File(extdir).exists()) new File(extdir).mkdirs();
		println("Found java extension folder: " + extdir);
		if (!new File(extdir).canWrite()) throw new Exception(
			"No permissions to write to " + extdir);
		for (final File file : files) {
			final File dst = new File(extdir, file.getName());
			println("moving " + file.getAbsolutePath() + " to " +
				dst.getAbsolutePath());
			if (!file.renameTo(dst)) {
				println("could not move " + file.getAbsolutePath() + " to " +
					dst.getAbsolutePath());
			}
		}
		rm(tempdir);
	}

	/**
	 * Create a folder with the given name in the given directory.
	 * 
	 * @param dir The directory in which the folder should be created.
	 * @param name The name of the folder to be created.
	 * @param failIfExists Flag to indicate whether to fail (throw an Exception)
	 *          if the folder exists already. If this is false, and the folder
	 *          exists, nothing will be done.
	 */
	public static File createFolder(final String dir, final String name,
		final boolean failIfExists) throws Exception
	{

		println("create folder " + dir + "/" + name);
		final File directory = new File(dir);
		if (!directory.exists()) directory.mkdirs();
		if (!directory.canWrite()) throw new Exception(
			"No permissions to write to folder " + dir);
		final File f = new File(dir, name);
		if (!f.exists()) {
			if (!f.mkdir()) {
				throw new Exception("Can't create directory " + f.getAbsolutePath());
			}
			return f;
		}
		if (failIfExists) {
			throw new Exception("Can't create directory " + f.getAbsolutePath() +
				": File exists already");
		}
		println(f.getAbsolutePath() + " not created; exists already");
		return f;
	}

	/**
	 * Download the file given by the specified url to a tmp folder. Returns a
	 * reference to the downloaded file.
	 */
	public static File download(final String u) throws Exception {
		print("downloading " + u);
		InputStream is = null;
		URL url = null;
		try {
			url = new URL(u);
			final URLConnection conn = url.openConnection();
			is = conn.getInputStream();
		}
		catch (final MalformedURLException e1) {
			throw new Exception(u + " is not a valid URL");
		}
		catch (final IOException e1) {
			throw new Exception("Can't open connection to " + u);
		}
		final byte[] content = readFully(is);
		final File out = new File(TMP, new File(url.getFile()).getName());
		println(" to " + out.getAbsolutePath());
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(out);
			fos.write(content);
			fos.close();
		}
		catch (final FileNotFoundException e1) {
			throw new Exception("Could not open " + out.getAbsolutePath() +
				" for writing. " + "Maybe not enough permissions?");
		}
		catch (final IOException e2) {
			throw new Exception("Error writing to " + out.getAbsolutePath());
		}
		return out;
	}

	/**
	 * Extracts fileToExtract from the zipfile in the specified directory. If a
	 * file which is about to be extracted exists already, the user is prompted
	 * asking whether to overwrite or skip. Returns a List with the extracted
	 * files.
	 */
	public static List<File> unzip(final File zipfile, final File dir,
		final String fileToExtract) throws Exception
	{

		println("Extracting " + (fileToExtract != null ? fileToExtract : "all") +
			" to " + dir.getAbsolutePath());
		if (!dir.canWrite()) throw new Exception(
			"No permissions to write to folder " + dir);
		ZipFile zfile = null;
		try {
			zfile = new ZipFile(zipfile);
		}
		catch (final ZipException e) {
			throw new Exception(zipfile.getAbsolutePath() +
				" is not a valid zip file.");
		}
		final Enumeration en = zfile.entries();
		final List<File> extracted = new ArrayList<File>();
		while (en.hasMoreElements()) {
			final ZipEntry ze = (ZipEntry) en.nextElement();
			if (ze.isDirectory()) {
				final File newDir =
					createFolder(dir.getAbsolutePath(), ze.getName(), false);
			}
			else if (fileToExtract == null || ze.getName().endsWith(fileToExtract)) {
				println("Extracting " + ze.getName() + " to " + dir);
				InputStream is = null;
				try {
					is = zfile.getInputStream(ze);
				}
				catch (final IOException e) {
					throw new Exception("Can't extract " + ze.getName());
				}
				final byte[] content = readFully(is);
				final File ext = new File(dir, ze.getName());
				if (!overwrite(ext)) continue;
				try {
					final FileOutputStream out = new FileOutputStream(ext);
					out.write(content);
					out.flush();
					out.close();
				}
				catch (final FileNotFoundException e) {
					throw new Exception("Could not open " + ext.getAbsolutePath() +
						" for writing. " + "Maybe not enough permissions?");
				}
				catch (final IOException e) {
					throw new Exception("Error writing to " + ext.getAbsolutePath());
				}
				extracted.add(ext);
			}
		}
		return extracted;
	}

	/**
	 * Reads all bytes from the given InputStream and returns it as a byte array.
	 */
	public static byte[] readFully(final InputStream is) throws Exception {
		final ByteArrayOutputStream buf = new ByteArrayOutputStream();
		int c = 0;
		try {
			while ((c = is.read()) != -1)
				buf.write(c);
			is.close();
		}
		catch (final IOException e) {
			throw new Exception("Error reading from " + is);
		}
		return buf.toByteArray();
	}

	static boolean overwrite(final File f) {
		if (overwrite || !f.exists()) return true;
		final YesNoCancelDialog d =
			new YesNoCancelDialog(IJ.getInstance() != null ? IJ.getInstance()
				: new Frame(), "Overwrite?", f.getAbsolutePath() + " exists already\n" +
				"Press OK to overwrite, or Cancel/No to skip");
		return d.yesPressed();
	}

	static void println(final String s) {
		IJ.log(s);
	}

	static void print(final String s) {
		IJ.log(s);
	}

	/**
	 * Recursively delete a file or directory.
	 */
	static void rm(final File f) {
		if (!f.exists()) return;
		if (f.isDirectory()) {
			final File[] ch = f.listFiles();
			for (final File child : ch)
				rm(child);
		}

		f.delete();
	}
}
