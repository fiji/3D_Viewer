
package ij3d.behaviors;

import ij3d.DefaultUniverse;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

/**
 * This class extends ViewPlatformTransformer, to transform mouse events into
 * real world transformations.
 *
 * @author Benjamin Schmid
 */
public class InteractiveViewPlatformTransformer extends ViewPlatformTransformer
{

	private static final double ONE_RAD = 2 * Math.PI / 360;
	private int xLast, yLast;

	/**
	 * Initializes a new InteractiveViewPlatformTransformer.
	 * 
	 * @param univ
	 * @param callback
	 */
	public InteractiveViewPlatformTransformer(final DefaultUniverse univ,
		final BehaviorCallback callback)
	{
		super(univ, callback);
	}

	/**
	 * This method should be called when a new transformation is started (i.e.
	 * when the mouse is pressed before dragging in order to rotate or translate).
	 * 
	 * @param e
	 */
	public void init(final MouseEvent e) {
		this.xLast = e.getX();
		this.yLast = e.getY();
	}

	/**
	 * This method should be called during the mouse is dragged, if the mouse
	 * event should result in a translation.
	 * 
	 * @param e
	 */
	public void translate(final MouseEvent e) {
		final int dx = xLast - e.getX();
		final int dy = yLast - e.getY();
		translateXY(-dx, dy);
		xLast = e.getX();
		yLast = e.getY();
	}

	/**
	 * This method should be called during the mouse is dragged, if the mouse
	 * event should result in a rotation.
	 * 
	 * @param e
	 */
	public void rotate(final MouseEvent e) {
		final int dx = xLast - e.getX();
		final int dy = yLast - e.getY();
		rotateXY(dy * ONE_RAD, dx * ONE_RAD);
		xLast = e.getX();
		yLast = e.getY();
	}

	/**
	 * This method should be called, if the specified MouseEvent should affect
	 * zooming based on wheel movement.
	 * 
	 * @param e
	 */
	public void wheel_zoom(final MouseEvent e) {
		final MouseWheelEvent we = (MouseWheelEvent) e;
		int units = 0;
		if (we.getScrollType() == MouseWheelEvent.WHEEL_UNIT_SCROLL) units =
			we.getUnitsToScroll();
		zoom(units);
	}

	/**
	 * This method should be called, if the specified MouseEvent should affect
	 * zooming based on vertical mouse dragging.
	 * 
	 * @param e
	 */
	public void zoom(final MouseEvent e) {
		final int y = e.getY();
		final int dy = y - yLast;
		zoom(dy);
		xLast = e.getX();
		yLast = y;
	}
}
