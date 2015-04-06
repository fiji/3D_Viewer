
package ij3d;

public class NoopAdapter implements UIAdapter {

	@Override
	public boolean isHandTool() {
		return true;
	}

	@Override
	public boolean isPointTool() {
		return false;
	}

	@Override
	public boolean isMagnifierTool() {
		return false;
	}

	@Override
	public boolean isRoiTool() {
		return false;
	}

	@Override
	public int getToolId() {
		return 0;
	}

	@Override
	public void setTool(final int id) {}

	@Override
	public void setHandTool() {}

	@Override
	public void setPointTool() {}

	@Override
	public void showStatus(final String status) {}

	@Override
	public void showProgress(final int a, final int b) {}
}
