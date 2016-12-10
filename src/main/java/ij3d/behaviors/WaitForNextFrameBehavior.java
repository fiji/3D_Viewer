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

package ij3d.behaviors;

import java.util.Enumeration;

import org.scijava.java3d.Behavior;
import org.scijava.java3d.WakeupOnBehaviorPost;
import org.scijava.java3d.WakeupOnElapsedFrames;

public class WaitForNextFrameBehavior extends Behavior {

	public static final int TRIGGER_ID = 1;

	private final WakeupOnBehaviorPost postCrit;
	private final WakeupOnElapsedFrames frameCrit;

	public WaitForNextFrameBehavior() {
		final boolean passive = false;
		postCrit = new WakeupOnBehaviorPost(null, TRIGGER_ID);
		frameCrit = new WakeupOnElapsedFrames(1, passive);
	}

	@Override
	public void initialize() {
		wakeupOn(postCrit);
	}

	@Override
	public void processStimulus(final Enumeration criteria) {
		while (criteria.hasMoreElements()) {
			final Object c = criteria.nextElement();
			if (c instanceof WakeupOnBehaviorPost) {
				wakeupOn(frameCrit);
			}
			else if (c instanceof WakeupOnElapsedFrames) {
				synchronized (this) {
					this.notifyAll();
				}
				wakeupOn(postCrit);
			}
		}
	}
}
