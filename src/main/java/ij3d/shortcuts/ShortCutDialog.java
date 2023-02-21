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

package ij3d.shortcuts;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

@SuppressWarnings("serial")
public class ShortCutDialog extends JDialog {

	public ShortCutDialog(final ShortCuts shortcuts) {
		final ShortCutTable table = new ShortCutTable(shortcuts);
		getContentPane().add(new JScrollPane(table));

		final JPanel buttons = new JPanel(new FlowLayout());
		final JButton cancel = new JButton("Cancel");
		cancel.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				shortcuts.reload();
				dispose();
			}
		});
		buttons.add(cancel);
		final JButton ok = new JButton("Ok");
		ok.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(final ActionEvent e) {
				shortcuts.save();
				dispose();
			}
		});
		buttons.add(ok);
		getContentPane().add(buttons, BorderLayout.SOUTH);

		pack();
		setVisible(true);
	}
}
