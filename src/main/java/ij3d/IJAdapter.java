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

package ij3d;

import ij.IJ;
import ij.gui.Toolbar;

public class IJAdapter implements UIAdapter {

	@Override
	public boolean isHandTool() {
		return Toolbar.getToolId() == Toolbar.HAND;
	}

	@Override
	public boolean isPointTool() {
		return Toolbar.getToolId() == Toolbar.POINT;
	}

	@Override
	public boolean isMagnifierTool() {
		return Toolbar.getToolId() == Toolbar.MAGNIFIER;
	}

	@Override
	public boolean isRoiTool() {
		final int tool = Toolbar.getToolId();
		return tool == Toolbar.RECTANGLE || tool == Toolbar.OVAL ||
			tool == Toolbar.POLYGON || tool == Toolbar.FREEROI ||
			tool == Toolbar.LINE || tool == Toolbar.POLYLINE ||
			tool == Toolbar.FREELINE || tool == Toolbar.POINT || tool == Toolbar.WAND;
	}

	@Override
	public int getToolId() {
		return Toolbar.getToolId();
	}

	@Override
	public void setTool(final int id) {
		Toolbar.getInstance().setTool(id);
	}

	@Override
	public void setHandTool() {
		setTool(Toolbar.HAND);
	}

	@Override
	public void setPointTool() {
		setTool(Toolbar.POINT);
	}

	@Override
	public void showStatus(final String status) {
		IJ.showStatus(status);
	}

	@Override
	public void showProgress(final int a, final int b) {
		IJ.showProgress(a, b);
	}
}
