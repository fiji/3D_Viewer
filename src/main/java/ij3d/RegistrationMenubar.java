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

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Panel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import org.scijava.java3d.Transform3D;
import org.scijava.java3d.View;

import ij.IJ;
import ij.gui.GenericDialog;
import vib.BenesNamedPoint;
import vib.FastMatrix;
import vib.PointList;

public class RegistrationMenubar extends JMenuBar implements ActionListener,
	UniverseListener
{

	private final Image3DUniverse univ;

	private final JMenu register;
	private final JMenuItem exit;
	private final JMenuItem adjustSlices;

	private final List openDialogs = new ArrayList();

	private Content templ, model;

	public RegistrationMenubar(final Image3DUniverse univ) {
		super();
		this.univ = univ;

		univ.addUniverseListener(this);

		register = new JMenu("Register");

		exit = new JMenuItem("Exit registration");
		exit.addActionListener(this);
		register.add(exit);

		adjustSlices = new JMenuItem("Adjust slices");
		adjustSlices.addActionListener(this);
		register.add(adjustSlices);

		this.add(register);

	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		if (e.getSource() == exit) {
			exitRegistration();
		}
		else if (e.getSource() == adjustSlices) {
			final Content c = univ.getSelected();
			if (c != null) univ.getExecuter().changeSlices(c);
		}
		else if (e.getActionCommand().equals("LS_TEMPLATE")) {
			// select landmarks of the template
			selectLandmarkSet(templ, "LS_MODEL");
		}
		else if (e.getActionCommand().equals("LS_MODEL")) {
			// select the landmarks of the model
			selectLandmarkSet(model, "REGISTER");
		}
		else if (e.getActionCommand().equals("REGISTER")) {
			// do registration
			doRegistration(templ, model);
		}
	}

	// usually called from the main menu bar.
	public void register() {
		new Thread(new Runnable() {

			@Override
			public void run() {
				initRegistration();
			}
		}).start();
	}

	public void exitRegistration() {
		templ.showPointList(false);
		model.showPointList(false);
		final JMenuBar mb = univ.getMenuBar();
		univ.setMenubar(mb);
		univ.clearSelection();
		univ.setStatus("");
		univ.getPointListDialog().removeExtraPanel();
		univ.ui.setHandTool();
	}

	private void hideAll() {
		for (final Iterator it = univ.contents(); it.hasNext();)
			((Content) it.next()).setVisible(false);
	}

	private void selectLandmarkSet(final Content content,
		final String actionCommand)
	{
		hideAll();
		content.setVisible(true);
		content.displayAs(ContentConstants.ORTHO);
		content.showPointList(true);
		univ.ui.setPointTool();
		univ.select(content);

		univ
			.setStatus("Select landmarks in " + content.getName() + " and click OK");

		final Panel p = new Panel(new FlowLayout());
		Button b = new Button("OK");
		b.setActionCommand(actionCommand);
		b.addActionListener(this);
		p.add(b);

		if (actionCommand.equals("REGISTER")) {
			b = new Button("Back to template");
			b.setActionCommand("LS_TEMPLATE");
			b.addActionListener(this);
			p.add(b);
		}

		univ.getPointListDialog().addPanel(p);
	}

	public void initRegistration() {
		// Select the contents used for registration
		final Collection contents = univ.getContents();
		if (contents.size() < 2) {
			IJ.error("At least two bodies are required for " + " registration");
			return;
		}
		final String[] conts = new String[contents.size()];
		int i = 0;
		for (final Iterator it = contents.iterator(); it.hasNext();)
			conts[i++] = ((Content) it.next()).getName();
		final GenericDialog gd = new GenericDialog("Registration");
		gd.addChoice("template", conts, conts[0]);
		gd.addChoice("model", conts, conts[1]);
		gd.addCheckbox("allow scaling", true);
		openDialogs.add(gd);
		gd.showDialog();
		openDialogs.remove(gd);
		if (gd.wasCanceled()) return;
		templ = univ.getContent(gd.getNextChoice());
		model = univ.getContent(gd.getNextChoice());
		final boolean scaling = gd.getNextBoolean();

		// Select the landmarks of the template
		selectLandmarkSet(templ, "LS_MODEL");
	}

	public void doRegistration(final Content templ, final Content model) {

		univ.setStatus("");
		// select the landmarks common to template and model
		final PointList tpoints = templ.getPointList();
		final PointList mpoints = model.getPointList();
		if (tpoints.size() < 2 || mpoints.size() < 2) {
			IJ.error("At least two points are required in each "
				+ "of the point lists");
		}
		final List sett = new ArrayList();
		final List setm = new ArrayList();
		for (int i = 0; i < tpoints.size(); i++) {
			final BenesNamedPoint pt = tpoints.get(i);
			final BenesNamedPoint pm = mpoints.get(pt.getName());
			if (pm != null) {
				sett.add(pt);
				setm.add(pm);
			}
		}
		if (sett.size() < 2) {
			IJ.error("At least two points with the same name "
				+ "must exist in both bodies");
			univ.setStatus("");
			return;
		}

		// Display common landmarks
		final DecimalFormat df = new DecimalFormat("00.000");
		String message = "Points used for registration\n \n";
		for (int i = 0; i < sett.size(); i++) {
			final BenesNamedPoint bnp = (BenesNamedPoint) sett.get(i);
			message +=
				(bnp.getName() + "    " + df.format(bnp.x) + "    " + df.format(bnp.y) +
					"    " + df.format(bnp.z) + "\n");
		}
		final boolean cont =
			IJ.showMessageWithCancel("Points used for registration", message);
		if (!cont) return;

		// calculate best rigid
		final BenesNamedPoint[] sm = new BenesNamedPoint[setm.size()];
		final BenesNamedPoint[] st = new BenesNamedPoint[sett.size()];
		final FastMatrix fm =
			FastMatrix.bestRigid((BenesNamedPoint[]) setm.toArray(sm),
				(BenesNamedPoint[]) sett.toArray(st));

		// reset the transformation of the template
		// and set the transformation of the model.
		final Transform3D t3d = new Transform3D(fm.rowwise16());
		templ.setTransform(new Transform3D());
		model.setTransform(t3d);

		templ.setVisible(true);
		templ.setLocked(true);
		model.setVisible(true);
		model.setLocked(true);

		univ.clearSelection();
		univ.ui.setHandTool();

		IJ.showMessage("Contents are locked to prevent\n"
			+ "accidental transformations");
		exitRegistration();
	}

	public void closeAllDialogs() {
		while (openDialogs.size() > 0) {
			final GenericDialog gd = (GenericDialog) openDialogs.get(0);
			gd.dispose();
			openDialogs.remove(gd);
		}
	}

	// Universe Listener interface
	@Override
	public void transformationStarted(final View view) {}

	@Override
	public void transformationFinished(final View view) {}

	@Override
	public void canvasResized() {}

	@Override
	public void transformationUpdated(final View view) {}

	@Override
	public void contentChanged(final Content c) {}

	@Override
	public void universeClosed() {}

	@Override
	public void contentAdded(final Content c) {}

	@Override
	public void contentRemoved(final Content c) {}

	@Override
	public void contentSelected(final Content c) {}
}
