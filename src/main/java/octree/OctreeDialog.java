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

package octree;

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import ij.IJ;
import ij.gui.GenericDialog;
import ij.io.DirectoryChooser;
import ij.io.OpenDialog;

public class OctreeDialog {

	private String imagePath;
	private String imageDir;
	private String name;

	private boolean dirEmpty = false;

	private boolean canceled = false;

	public void showDialog() {
		final GenericDialog gd = new GenericDialog("Add large volume");

		gd.addStringField("Name:", "", 10);

		gd.addMessage("Please specify the path to the image which you\n"
			+ "want to display.\n"
			+ "If you specify a directory with precomputed data\n"
			+ "below, you can leave this field empty");
		Panel p = new Panel(new FlowLayout());
		final TextField imagePathTF = new TextField(30);
		p.add(imagePathTF);
		Button b = new Button("...");
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final OpenDialog od = new OpenDialog("Image path", null);
				final String dir = od.getDirectory();
				final String file = od.getFileName();
				if (dir != null && file != null) {
					final File f = new File(od.getDirectory(), od.getFileName());
					imagePathTF.setText(f.getAbsolutePath());
				}
			}
		});
		p.add(b);
		gd.addPanel(p);

		gd.addMessage("For displaying large volumes, much data has to be\n"
			+ "precomputed. Please specify an empty directory where\n"
			+ "this data can be stored. If data for this image was\n"
			+ "precomputed before, please specify the directory\n"
			+ "containing it here.");
		p = new Panel(new FlowLayout());
		final TextField imageDirTF = new TextField(30);
		p.add(imageDirTF);
		b = new Button("...");
		b.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final DirectoryChooser dc = new DirectoryChooser("Data directory");
				final String dir = dc.getDirectory();
				if (dir != null) imageDirTF.setText(dir);
			}
		});
		p.add(b);
		gd.addPanel(p);
		gd.showDialog();

		if (gd.wasCanceled()) {
			canceled = true;
			return;
		}

		this.name = gd.getNextString();
		this.imagePath = imagePathTF.getText();
		this.imageDir = imageDirTF.getText();
	}

	public boolean checkUserInput() {
		// TODO check name
		if (canceled) return false;

		File f = new File(imageDir);
		if (!f.exists() && !f.mkdir()) {
			IJ.error(imageDir + " does not exist and can't be created.");
			return false;
		}
		if (!f.isDirectory()) {
			IJ.error(imageDir + " is not a directory.");
			return false;
		}
		dirEmpty = f.list().length == 0;

		f = new File(imagePath);
		if (!f.exists() && dirEmpty) {
			IJ.error("Found an empty directory and " + imagePath + " does not exist");
			return false;
		}
		if (!f.isFile() && dirEmpty) {
			IJ.error("Found an empty directory and " + imagePath +
				" is not a valid file.");
			return false;
		}
		return true;
	}

	public boolean shouldCreateData() {
		return dirEmpty;
	}

	public String getImagePath() {
		return imagePath;
	}

	public String getImageDir() {
		return imageDir;
	}

	public String getName() {
		return name;
	}
}
