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

package ij3d.contextmenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

import ij3d.Content;
import ij3d.ContentConstants;
import ij3d.Executer;
import ij3d.Image3DUniverse;

public class ContextMenu implements ActionListener, ItemListener,
	ContentConstants
{

	private final JPopupMenu popup = new JPopupMenu();

	private final Image3DUniverse univ;
	private final Executer executer;

	private Content content;

	private final JMenuItem slices, updateVol, fill, smoothMesh, smoothAllMeshes,
			smoothDialog, colorSurface, decimateMesh;
	private final JCheckBoxMenuItem shaded, saturated;

	public ContextMenu(final Image3DUniverse univ) {

		this.univ = univ;
		this.executer = univ.getExecuter();

		slices = new JMenuItem("Adjust slices");
		slices.addActionListener(this);
		popup.add(slices);

		updateVol = new JMenuItem("Update Volume");
		updateVol.addActionListener(this);
		popup.add(updateVol);

		fill = new JMenuItem("Fill selection");
		fill.addActionListener(this);
		popup.add(fill);

		final JMenu smooth = new JMenu("Smooth");
		popup.add(smooth);

		smoothMesh = new JMenuItem("Smooth mesh");
		smoothMesh.addActionListener(this);
		smooth.add(smoothMesh);

		smoothAllMeshes = new JMenuItem("Smooth all meshes");
		smoothAllMeshes.addActionListener(this);
		smooth.add(smoothAllMeshes);

		decimateMesh = new JMenuItem("Decimate mesh");
		decimateMesh.addActionListener(this);
		popup.add(decimateMesh);

		smoothDialog = new JMenuItem("Smooth control");
		smoothDialog.addActionListener(this);
		smooth.add(smoothDialog);

		shaded = new JCheckBoxMenuItem("Shade surface");
		shaded.setState(true);
		shaded.addItemListener(this);
		popup.add(shaded);

		saturated = new JCheckBoxMenuItem("Saturated volume rendering");
		saturated.setState(false);
		saturated.addItemListener(this);
		popup.add(saturated);

		colorSurface = new JMenuItem("Color surface from image");
		colorSurface.addActionListener(this);
		popup.add(colorSurface);

	}

	public void showPopup(final MouseEvent e) {
		content = univ.getPicker().getPickedContent(e.getX(), e.getY());
		if (content == null) return;
		univ.select(content);
		shaded.setState(content.isShaded());
		saturated.setState(content.isSaturatedVolumeRendering());
		if (popup.isPopupTrigger(e)) popup.show(e.getComponent(), e.getX(), e
			.getY());
	}

	@Override
	public void itemStateChanged(final ItemEvent e) {
		final Object src = e.getSource();
		if (src == shaded) executer.setShaded(content, shaded.getState());
		else if (src == saturated) executer.setSaturatedVolumeRendering(content,
			saturated.getState());
	}

	@Override
	public void actionPerformed(final ActionEvent e) {
		final Object src = e.getSource();
		if (src == updateVol) executer.updateVolume(content);
		else if (src == slices) executer.changeSlices(content);
		else if (src == fill) executer.fill(content);
		else if (src == smoothMesh) executer.smoothMesh(content);
		else if (src == smoothAllMeshes) executer.smoothAllMeshes();
		else if (src == smoothDialog) executer.smoothControl();
		else if (src == decimateMesh) executer.decimateMesh();
		else if (src == colorSurface) executer.applySurfaceColors(content);
	}
}
