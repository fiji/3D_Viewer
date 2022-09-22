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

package ij3d.behaviors;

import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;

import ij3d.DefaultUniverse;

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
