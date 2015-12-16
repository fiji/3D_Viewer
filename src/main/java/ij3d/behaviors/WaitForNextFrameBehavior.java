
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
