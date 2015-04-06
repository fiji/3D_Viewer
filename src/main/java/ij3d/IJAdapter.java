
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
