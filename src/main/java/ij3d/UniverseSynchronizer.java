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

package ij3d;

import java.util.HashMap;

import org.scijava.java3d.View;
import org.scijava.java3d.utils.universe.MultiTransformGroup;

import ij3d.DefaultUniverse.GlobalTransform;

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
				//removeUniverse(u);
				// Do not call removeUniverse(u)
				// as this will remove this adapter from the list of universe listeners
				// (i.e. u.removeUniverseListener(l))
				// so modifying the current list of listeners on which universeClosed()
				// is currently being invoked by the closing universe.
				universes.remove(u);
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
