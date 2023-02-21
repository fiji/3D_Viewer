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

package ij3d.gui;

import java.awt.Button;
import java.awt.Label;
import java.awt.TextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import customnode.CustomTriangleMesh;
import customnode.EdgeContraction;
import customnode.FullInfoMesh;
import ij.IJ;
import ij.gui.GenericDialog;

public class InteractiveMeshDecimation {

	public void run(final CustomTriangleMesh ctm) {
		@SuppressWarnings("unchecked")
		final FullInfoMesh fim = new FullInfoMesh(ctm.getMesh());
		final EdgeContraction ec = new EdgeContraction(fim, false);
		@SuppressWarnings("serial")
		final GenericDialog gd = new GenericDialog("Mesh simplification") {

			@Override
			public void keyPressed(final KeyEvent e) {
				if (e.getKeyCode() != KeyEvent.VK_ENTER) super.keyPressed(e);
			}
		};
		gd.addNumericField("Contract next n edges", 100, 0);
		final TextField tf = (TextField) gd.getNumericFields().get(0);
		gd.addMessage(ec.getVertexCount() + " remaining vertices");
		final Label label = (Label) gd.getMessage();
		// gd.enableYesNoCancel("Simplify", "Save");
		gd.setModal(false);
		gd.showDialog();
		final Button[] buttons = gd.getButtons();
		// yes button
		buttons[0].setLabel("Simplify");
		buttons[0].removeActionListener(gd);
		buttons[0].addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				final int n = Integer.parseInt(tf.getText());
				gd.setEnabled(false);
				new Thread() {

					@Override
					public void run() {
						final int v = simplify(ec, n);
						gd.setEnabled(true);
						ctm.setMesh(fim.getMesh());
						label.setText(v + " remaining vertices");
					}
				}.start();
			}
		});
		// no button
		buttons[1].setLabel("Ok");
		buttons[1].removeActionListener(gd);
		buttons[1].addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				gd.dispose();
			}
		});
	}

	private int simplify(final EdgeContraction ec, final int n) {
		final int part = n / 10;
		final int last = n % 10;
		int ret = 0;
		for (int i = 0; i < 10; i++) {
			IJ.showProgress(i + 1, 10);
			ret = ec.removeNext(part);
		}
		if (last != 0) ret = ec.removeNext(last);
		IJ.showProgress(1);

		return ret;
	}
}
