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

package view4d;

import ij.ImagePlus;
import ij.ImageStack;
import ij3d.Image3DUniverse;

/**
 * Implements the functionality for the 4D viewer, like loading and animation.
 *
 * @author Benjamin Schmid
 */
public class Timeline {

	private final Image3DUniverse univ;
	private Thread playing = null;
	private boolean bounceback = true;

	/**
	 * Initialize the timeline
	 * 
	 * @param univ
	 */
	public Timeline(final Image3DUniverse univ) {
		this.univ = univ;
	}

	public Image3DUniverse getUniverse() {
		return univ;
	}

	public void setBounceBack(final boolean bounce) {
		this.bounceback = bounce;
	}

	public boolean getBounceBack() {
		return bounceback;
	}

	/**
	 * Returns the number of time points.
	 * 
	 * @return
	 */
	public int size() {
		if (univ.getContents().size() == 0) return 0;
		return univ.getEndTime() - univ.getStartTime();
	}

	/**
	 * Speed up the animation.
	 */
	public void faster() {
		if (delay >= 50) delay -= 50;
	}

	/**
	 * Slows the animation down.
	 */
	public void slower() {
		delay += 50;
	}

	public ImagePlus record() {
		pause();
		final int s = univ.getStartTime();
		final int e = univ.getEndTime();

		univ.showTimepoint(s);
		try {
			Thread.sleep(100);
		}
		catch (final InterruptedException ex) {}
		final ImagePlus imp = univ.takeSnapshot();
		final ImageStack stack = new ImageStack(imp.getWidth(), imp.getHeight());
		stack.addSlice("", imp.getProcessor());

		for (int i = s + 1; i <= e; i++) {
			univ.showTimepoint(i);
			try {
				Thread.sleep(100);
			}
			catch (final InterruptedException ex) {}
			stack.addSlice("", univ.takeSnapshot().getProcessor());
		}
		return new ImagePlus("Movie", stack);
	}

	private boolean shouldPause = false;
	private int delay = 200;

	/**
	 * Start animation.
	 */
	public synchronized void play() {
		if (size() == 0) return;
		if (playing != null) return;
		playing = new Thread(new Runnable() {

			@Override
			public void run() {
				int inc = +1;
				shouldPause = false;
				while (!shouldPause) {
					int next = univ.getCurrentTimepoint() + inc;
					if (next > univ.getEndTime()) {
						if (bounceback) {
							inc = -inc;
							continue;
						}
						next = univ.getStartTime();
					}
					else if (next < univ.getStartTime()) {
						inc = -inc;
						continue;
					}
					univ.showTimepoint(next);
					try {
						Thread.sleep(delay);
					}
					catch (final Exception e) {
						shouldPause = true;
					}
				}
				shouldPause = false;
				playing = null;
			}
		});
		playing.start();
	}

	/**
	 * Stop/pause animation
	 */
	public synchronized void pause() {
		shouldPause = true;
	}

	/**
	 * Display next timepoint.
	 */
	public void next() {
		if (univ.getContents().size() == 0) return;
		final int curr = univ.getCurrentTimepoint();
		if (curr == univ.getEndTime()) return;
		univ.showTimepoint(curr + 1);
	}

	/**
	 * Display previous timepoint.
	 */
	public void previous() {
		if (univ.getContents().size() == 0) return;
		final int curr = univ.getCurrentTimepoint();
		if (curr == univ.getStartTime()) return;
		univ.showTimepoint(curr - 1);
	}

	/**
	 * Display first timepoint.
	 */
	public void first() {
		if (univ.getContents().size() == 0) return;
		final int first = univ.getStartTime();
		if (univ.getCurrentTimepoint() == first) return;
		univ.showTimepoint(first);
	}

	/**
	 * Display last timepoint.
	 */
	public void last() {
		if (univ.getContents().size() == 0) return;
		final int last = univ.getEndTime();
		if (univ.getCurrentTimepoint() == last) return;
		univ.showTimepoint(last);
	}
}
