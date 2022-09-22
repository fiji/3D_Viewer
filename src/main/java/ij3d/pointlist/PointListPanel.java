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

package ij3d.pointlist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Label;
import java.awt.MenuItem;
import java.awt.Panel;
import java.awt.PopupMenu;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;

import ij.gui.GenericDialog;
import vib.BenesNamedPoint;
import vib.PointList;

/**
 * This class is a Panel showing a PointList as a list, which allows to modify
 * the point list: Adding, deleting points, moving them up and down, etc.
 * 
 * @author Benjamin Schmid
 */
public class PointListPanel extends Panel implements ActionListener,
	PointList.PointListListener
{

	/** A reference to the actual PointList */
	private PointList points;

	/** The constraints used by the layout */
	private GridBagConstraints c;

	/** The popup menu */
	private final PopupMenu popup = createPopup();

	/** The point under the cursor when opening the popup menu */
	private BenesNamedPoint current;

	/** The format for pretty printing points */
	private final DecimalFormat df = new DecimalFormat("00.000");

	/** The header of the list */
	private final String header;

	/**
	 * Constructs a new panel showing a list of the specified PointList points
	 * with the specified header.
	 * 
	 * @param header
	 * @param points
	 */
	public PointListPanel(final String header, final PointList points) {

		super();
		this.header = header;
		this.add(popup);
		this.points = points;
		points.addPointListListener(this);

		setLayout(new GridBagLayout());
		setBackground(Color.WHITE);
		c = new GridBagConstraints();

		recreatePointsPanel();
	}

	/**
	 * Set the PointList which should be displayed by this PointListPanel
	 */
	public void setPointList(final PointList pl) {
		this.points.removePointListListener(this);
		this.points = pl;
		this.points.addPointListListener(this);
		recreatePointsPanel();
	}

	/**
	 * Helper method to create the popup menu.
	 */
	private PopupMenu createPopup() {
		final PopupMenu popup = new PopupMenu();
		MenuItem mi = new MenuItem("Up");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Down");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Rename");
		mi.addActionListener(this);
		popup.add(mi);
		mi = new MenuItem("Remove");
		mi.addActionListener(this);
		popup.add(mi);
		return popup;
	}

	/**
	 * Method to re-create the panel, e.g. after the ordering in the underlying
	 * PointList has changed.
	 */
	private void recreatePointsPanel() {
		removeAll();
		c = new GridBagConstraints();
		addHeader();
		int i = 0;
		if (points.size() == 0) addEmptyRow();
		for (final BenesNamedPoint p : points)
			addRow(p, i++);
		layoutWindow();
	}

	/**
	 * Helper method to add the header of the PointList as a Label to the panel.
	 */
	private void addHeader() {
		final Label l = new Label(header);
		l.setFont(new Font("Verdana", Font.BOLD, 12));
		c.gridy = 0;
		c.gridwidth = 2;
		c.gridx = 0;
		add(l, c);
		c.gridwidth = 1;
	}

	/**
	 * Helper method to add an empty row in the list. Used if the underlying
	 * PointList is empty.
	 */
	private void addEmptyRow() {
		final Label l = new Label("     No points set       ");
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.anchor = GridBagConstraints.LINE_START;
		add(l, c);
	}

	private final Color grey = Color.LIGHT_GRAY;

	/**
	 * Helper method to add one row in the list with the specified point.
	 * 
	 * @param p
	 * @param row
	 */
	private void addRow(final BenesNamedPoint p, final int row) {
		if (!p.isSet()) return;
		c.gridx = 0;
		c.gridy = GridBagConstraints.RELATIVE;
		c.fill = GridBagConstraints.BOTH;
		c.anchor = GridBagConstraints.LINE_START;
		final Label label = new Label(p.getName() + "   ");
		label.setName(p.getName());
		label.setFont(new Font("Verdana", Font.BOLD, 12));
		label.setForeground(Color.BLUE);
		if (row % 2 == 0) label.setBackground(grey);
		label.addMouseListener(new MouseAdapter() {

			@Override
			public void mousePressed(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					current = p;
					popup.show(label, e.getX(), e.getY());
				}
				else {
					points.highlight(p);
				}
			}

			@Override
			public void mouseReleased(final MouseEvent e) {
				if (e.isPopupTrigger()) {
					current = p;
					popup.show(label, e.getX(), e.getY());
				}
			}
		});
		add(label, c);

		c.anchor = GridBagConstraints.LINE_START;
		c.gridx = 1;
		c.fill = GridBagConstraints.BOTH;
		final Label coordinateLabel =
			new Label(df.format(p.x) + "    " + df.format(p.y) + "    " +
				df.format(p.z));
		coordinateLabel.setFont(new Font("Verdana", Font.PLAIN, 12));
		if (row % 2 == 0) coordinateLabel.setBackground(grey);
		add(coordinateLabel, c);
	}

	/**
	 * Helper method to layout the window, after e.g. a point has been added.
	 */
	private void layoutWindow() {
		Container pa = getParent();
		while (pa != null && !(pa instanceof java.awt.Window)) {
			pa = pa.getParent();
		}
		if (pa != null) pa.validate();
	}

	/**
	 * Opens an input dialog to ask the user for a new name for the specified
	 * point
	 * 
	 * @param p
	 */
	public void renamePoint(final BenesNamedPoint p) {
		final GenericDialog gd = new GenericDialog("Rename point");
		gd.addStringField("New name ", p.getName());
		gd.showDialog();
		if (gd.wasCanceled()) return;
		points.rename(p, gd.getNextString());
	}

	/**
	 * Handling the events coming from the popup menu.
	 */
	@Override
	public void actionPerformed(final ActionEvent e) {
		final String command = e.getActionCommand();
		if (command.equals("Rename")) {
			renamePoint(current);
		}
		else if (command.equals("Remove")) {
			points.remove(current);
		}
		else if (command.equals("Up")) {
			points.up(current);
		}
		else if (command.equals("Down")) {
			points.down(current);
		}
	}

	/* ******************************************************************
	 * PointListListener interface
	 * ******************************************************************/

	/**
	 * @see PointList.PointListListener#added
	 */
	@Override
	public void added(final BenesNamedPoint p) {
		final int i = points.size();
		if (i == 1) {
			recreatePointsPanel();
		}
		else {
			addRow(p, points.size() - 1);
			layoutWindow();
		}
	}

	/**
	 * @see PointList.PointListListener#removed
	 */
	@Override
	public void removed(final BenesNamedPoint p) {
		recreatePointsPanel();
	}

	/**
	 * @see PointList.PointListListener#renamed
	 */
	@Override
	public void renamed(final BenesNamedPoint p) {
		recreatePointsPanel();
	}

	/**
	 * @see PointList.PointListListener#highlighted
	 */
	@Override
	public void highlighted(final BenesNamedPoint p) {}

	/**
	 * @see PointList.PointListListener#reordered
	 */
	@Override
	public void reordered() {
		recreatePointsPanel();
	}

	/**
	 * @see PointList.PointListListener#moved
	 */
	@Override
	public void moved(final BenesNamedPoint p) {
		final Component[] c = getComponents();
		boolean found = false;
		for (int i = 0; i < c.length; i++) {
			if (c[i].getName().equals(p.getName()) && i < c.length - 1) {
				final Label coord = (Label) c[i + 1];
				coord.setText(df.format(p.x) + "    " + df.format(p.y) + "    " +
					df.format(p.z));
				found = true;
			}
		}
		// if not successful, just update the whole panel
		if (!found) recreatePointsPanel();
	}
}
