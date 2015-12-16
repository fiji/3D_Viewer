
package ij3d;

import org.scijava.java3d.utils.universe.MultiTransformGroup;

import ij3d.DefaultUniverse.GlobalTransform;

import java.util.HashMap;

import org.scijava.java3d.View;

public class UniverseSynchronizer {

	private final HashMap<Image3DUniverse, UniverseListener> universes =
		new HashMap<Image3DUniverse, UniverseListener>();

	void addUniverse(final Image3DUniverse u) {
		final UniverseListener l = new UniverseAdapter() {

			@Override
			public void transformationUpdated(final View view) {
				final GlobalTransform xform = new GlobalTransform();
				u.getGlobalTransform(xform);
				for (final Image3DUniverse o : universes.keySet())
					if (!o.equals(u)) setGlobalTransform(o, xform);
			}

			@Override
			public void universeClosed() {
				removeUniverse(u);
			}
		};
		u.addUniverseListener(l);
		universes.put(u, l);
	}

	void removeUniverse(final Image3DUniverse u) {
		if (!universes.containsKey(u)) return;
		final UniverseListener l = universes.get(u);
		u.removeUniverseListener(l);
		universes.remove(u);
	}

	/* Need to implement this here again in order to prevent
	 * firing transformationUpdated() */
	private static final GlobalTransform old = new GlobalTransform();

	private static final void setGlobalTransform(final Image3DUniverse u,
		final GlobalTransform transform)
	{
		u.getGlobalTransform(old);
		if (equals(old, transform)) return;
		final MultiTransformGroup group =
			u.getViewingPlatform().getMultiTransformGroup();
		final int num = group.getNumTransforms();
		for (int i = 0; i < num; i++)
			group.getTransformGroup(i).setTransform(transform.transforms[i]);
		u.fireTransformationUpdated();
	}

	private static final boolean equals(final GlobalTransform t1,
		final GlobalTransform t2)
	{
		final int num = t1.transforms.length;
		for (int i = 0; i < num; i++)
			if (!t1.transforms[i].equals(t2.transforms[i])) return false;
		return true;
	}

	private class UniverseAdapter implements UniverseListener {

		@Override
		public void transformationStarted(final View view) {}

		@Override
		public void transformationUpdated(final View view) {}

		@Override
		public void transformationFinished(final View view) {}

		@Override
		public void contentAdded(final Content c) {}

		@Override
		public void contentRemoved(final Content c) {}

		@Override
		public void contentChanged(final Content c) {}

		@Override
		public void contentSelected(final Content c) {}

		@Override
		public void canvasResized() {}

		@Override
		public void universeClosed() {}
	}
}
