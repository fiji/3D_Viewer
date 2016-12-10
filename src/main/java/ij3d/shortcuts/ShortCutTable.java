/*-
 * #%L
 * Fiji distribution of ImageJ for the life sciences.
 * %%
 * Copyright (C) 2010 - 2016 Fiji developers.
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

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.DefaultCellEditor;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

@SuppressWarnings("serial")
public class ShortCutTable extends JTable {

	public ShortCutTable(final ShortCuts shortcuts) {
		super(new ShortcutTableModel(shortcuts));
		setAutoResizeMode(AUTO_RESIZE_ALL_COLUMNS);
		setDefaultEditor(String.class, new ShortcutTableEditor());
	}

	private static final class ShortcutTableEditor extends DefaultCellEditor {

		ShortcutTableEditor() {
			super(getEditingTextField());
		}

		public static JTextField getEditingTextField() {
			final JTextField tf = new JTextField();
			tf.addKeyListener(new KeyAdapter() {

				@Override
				public void keyPressed(final KeyEvent e) {
					if (e.getKeyCode() != KeyEvent.VK_ENTER) {
						final String stroke = KeyStroke.getKeyStrokeForEvent(e).toString();
						((JTextField) e.getComponent()).setText(stroke);
						e.consume();
					}
				}

				@Override
				public void keyTyped(final KeyEvent e) {
					e.consume();
				}

				@Override
				public void keyReleased(final KeyEvent e) {
					e.consume();
				}
			});
			return tf;
		}
	}

	private static final class ShortcutTableModel implements TableModel {

		final ShortCuts shortcuts;

		public ShortcutTableModel(final ShortCuts shortcuts) {
			this.shortcuts = shortcuts;
		}

		@Override
		public void addTableModelListener(final TableModelListener l) {}

		@Override
		public void removeTableModelListener(final TableModelListener l) {}

		@Override
		public Class<?> getColumnClass(final int columnIndex) {
			return String.class;
		}

		@Override
		public int getColumnCount() {
			return 2;
		}

		@Override
		public String getColumnName(final int col) {
			return col == 0 ? "Command" : "Shortcut";
		}

		@Override
		public int getRowCount() {
			return shortcuts.getNumberOfCommands();
		}

		@Override
		public Object getValueAt(final int row, final int col) {
			final String command = shortcuts.getCommand(row);
			return col == 0 ? command : shortcuts.getShortCut(command);
		}

		@Override
		public boolean isCellEditable(final int rowIndex, final int columnIndex) {
			return columnIndex == 1;
		}

		@Override
		public void setValueAt(final Object aValue, final int row, final int col) {
			if (col != 1) return;
			final String command = shortcuts.getCommand(row);
			shortcuts.setShortCut(command, (String) aValue);
		}
	}
}
