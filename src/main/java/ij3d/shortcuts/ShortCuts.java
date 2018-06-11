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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

import ij3d.UniverseSettings;

public class ShortCuts
{

	private final List<String> commands;
	private final HashMap<String, JMenuItem> items;
	private final HashMap<String, String> shortcuts;

	public ShortCuts(final JMenuBar menubar)
	{
		commands = new ArrayList<String>();
		items = new HashMap<String, JMenuItem>();
		shortcuts = UniverseSettings.shortcuts;

		for (int i = 0; i < menubar.getMenuCount(); i++)
			scan(menubar.getMenu(i), "");

		for (final String command : commands)
		{
			final String shortcut = shortcuts.get(command);
			if (shortcut != null)
				setShortCut(command, shortcut);
		}
	}

	public void save()
	{
		UniverseSettings.save();
	}

	public void reload()
	{
		UniverseSettings.load();
	}

	public Iterable<String> getCommands()
	{
		return commands;
	}

	public String getShortCut(final String command)
	{
		return shortcuts.get(command);
	}

	public void setShortCut(final String command, final String shortcut)
	{
		if (shortcut.trim().length() == 0)
		{
			clearShortCut(command);
			return;
		}
		shortcuts.put(command, shortcut);
		final KeyStroke stroke = KeyStroke.getKeyStroke(shortcut);
		JMenuItem o = items.get(command);
		if (o != null)
			o.setAccelerator(stroke);
	}

	public void clearShortCut(final String command)
	{
		JMenuItem o = items.get(command);
		if (o != null)
			o.setAccelerator(null);
		shortcuts.remove(command);
	}

	public int getNumberOfCommands()
	{
		return commands.size();
	}

	public String getCommand(final int i)
	{
		return commands.get(i);
	}

	private void scan(final JMenu menu, String prefix)
	{
		prefix += menu.getText() + " > ";
		for (int i = 0; i < menu.getItemCount(); i++)
		{
			final JMenuItem mi = menu.getItem(i);
			if (mi == null)
				continue;
			if (mi instanceof JMenu)
			{
				scan((JMenu) mi, prefix);
			}
			else
			{
				final String c = prefix + mi.getText();
				commands.add(c);
				items.put(c, mi);
			}
		}
	}
}
