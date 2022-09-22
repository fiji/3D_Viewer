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

package customnode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

public class FullInfoMesh {

	ArrayList<Integer> faces;
	ArrayList<Vertex> vertices;
	HashMap<Point3f, Integer> vertexToIndex;
	HashMap<Edge, Edge> edges;
	HashSet<Triangle> triangles;

	public FullInfoMesh() {
		faces = new ArrayList<Integer>();
		vertices = new ArrayList<Vertex>();
		vertexToIndex = new HashMap<Point3f, Integer>();
		triangles = new HashSet<Triangle>();
		edges = new HashMap<Edge, Edge>();
	}

	public FullInfoMesh(final List<Point3f> mesh) {
		this();
		for (int i = 0; i < mesh.size(); i += 3) {
			final int f1 = addVertex(mesh.get(i));
			final int f2 = addVertex(mesh.get(i + 1));
			final int f3 = addVertex(mesh.get(i + 2));

			addFace(f1, f2, f3);
		}
	}

	public List<Point3f> getMesh() {
		final List<Point3f> ret = new ArrayList<Point3f>();
		for (int i = 0; i < faces.size(); i++) {
			final int f = getFace(i);
			if (f != -1) ret.add(new Point3f(getVertex(f)));
		}
		return ret;
	}

	public Set<Point3f> getVertices() {
		return vertexToIndex.keySet();
	}

	public void moveVertex(final int vIdx, final Vector3f displacement) {
		final Point3f p = vertices.get(vIdx);
		vertexToIndex.remove(p);
		p.add(displacement);
		vertexToIndex.put(p, vIdx);

	}

	public int getIndex(final Point3f v) {
		if (vertexToIndex.containsKey(v)) return vertexToIndex.get(v);
		return -1;
	}

	public int getVertexCount() {
		return vertexToIndex.size();
	}

	public Vertex getVertex(final int i) {
		return vertices.get(i);
	}

	public int getFaceCount() {
		return faces.size();
	}

	public int getFace(final int i) {
		return faces.get(i);
	}

	public int addVertex(final Point3f p) {
		if (vertexToIndex.containsKey(p)) return vertexToIndex.get(p);

		final Vertex v = new Vertex(p);
		vertices.add(v);
		final int idx = vertices.size() - 1;
		vertexToIndex.put(v, idx);
		return idx;
	}

	public void removeVertex(final Point3f p) {
		removeVertex(vertexToIndex.get(p));
	}

	public void removeVertex(final int vIdx) {
		Vertex v = getVertex(vIdx);
		final ArrayList<Integer> toRemove = new ArrayList<Integer>(v.triangles);
		for (final int f : toRemove)
			removeFace(f);

		v = vertices.get(vIdx);
	}

	public void removeFace(final int fIdx) {
		final int f1 = getFace(3 * fIdx);
		final int f2 = getFace(3 * fIdx + 1);
		final int f3 = getFace(3 * fIdx + 2);

		if (f1 == -1 && f2 == -1 && f3 == -1) return;

		final boolean b = triangles.remove(new Triangle(f1, f2, f3));
		assert b;

		faces.set(3 * fIdx, -1);
		faces.set(3 * fIdx + 1, -1);
		faces.set(3 * fIdx + 2, -1);

		final Vertex v1 = getVertex(f1);
		final Vertex v2 = getVertex(f2);
		final Vertex v3 = getVertex(f3);

		Edge etmp = new Edge(f1, f2);
		etmp = edges.get(etmp);
		etmp.removeTriangle(fIdx);
		if (etmp.nTriangles() == 0) {
			v1.removeEdge(etmp);
			v2.removeEdge(etmp);
			edges.remove(etmp);
		}

		etmp = new Edge(f2, f3);
		etmp = edges.get(etmp);
		etmp.removeTriangle(fIdx);
		if (etmp.nTriangles() == 0) {
			v2.removeEdge(etmp);
			v3.removeEdge(etmp);
			edges.remove(etmp);
		}

		etmp = new Edge(f3, f1);
		etmp = edges.get(etmp);
		etmp.removeTriangle(fIdx);
		if (etmp.nTriangles() == 0) {
			v3.removeEdge(etmp);
			v1.removeEdge(etmp);
			edges.remove(etmp);
		}

		v1.removeTriangle(fIdx);
		v2.removeTriangle(fIdx);
		v3.removeTriangle(fIdx);

		if (v1.triangles.size() == 0) {
			vertexToIndex.remove(v1);
			vertices.set(f1, null);
		}
		if (v2.triangles.size() == 0) {
			vertexToIndex.remove(v2);
			vertices.set(f2, null);
		}
		if (v3.triangles.size() == 0) {
			vertexToIndex.remove(v3);
			vertices.set(f3, null);
		}
	}

	public void addFace(final int f1, final int f2, final int f3) {
		final Triangle tri = new Triangle(f1, f2, f3);
		if (triangles.contains(tri)) return;

		triangles.add(tri);

		final Vertex v1 = getVertex(f1);
		final Vertex v2 = getVertex(f2);
		final Vertex v3 = getVertex(f3);

		Edge e1 = new Edge(f2, f3);
		Edge e2 = new Edge(f3, f1);
		Edge e3 = new Edge(f1, f2);

		Edge etmp = edges.get(e1);
		if (etmp != null) e1 = etmp;
		else edges.put(e1, e1);

		etmp = edges.get(e2);
		if (etmp != null) e2 = etmp;
		else edges.put(e2, e2);

		etmp = edges.get(e3);
		if (etmp != null) e3 = etmp;
		else edges.put(e3, e3);

		v1.addEdges(e2, e3);
		v2.addEdges(e1, e3);
		v3.addEdges(e1, e2);

		final int fIdx = faces.size() / 3;

		faces.add(f1);
		faces.add(f2);
		faces.add(f3);

		e1.addTriangle(fIdx);
		e2.addTriangle(fIdx);
		e3.addTriangle(fIdx);

		v1.addTriangle(fIdx);
		v2.addTriangle(fIdx);
		v3.addTriangle(fIdx);
	}

	private final Vector3f tmpv1 = new Vector3f();
	private final Vector3f tmpv2 = new Vector3f();

	public void getFaceNormal(final int fIdx, final Vector3f ret) {
		final int f1 = getFace(3 * fIdx);
		final int f2 = getFace(3 * fIdx + 1);
		final int f3 = getFace(3 * fIdx + 2);

		final Vertex v1 = getVertex(f1);
		final Vertex v2 = getVertex(f2);
		final Vertex v3 = getVertex(f3);

		tmpv1.sub(v2, v1);
		tmpv2.sub(v3, v1);

		ret.cross(tmpv1, tmpv2);
	}

	public void getVertexNormal(final Vertex v, final Vector3f ret) {
		ret.set(0, 0, 0);
		final Vector3f tn = new Vector3f();
		for (final int fIdx : v.triangles) {
			getFaceNormal(fIdx, tn);
			ret.add(tn);
		}
		ret.normalize();
	}

	public void getVertexNormal(final int vIdx, final Vector3f ret) {
		final Vertex v = getVertex(vIdx);
		getVertexNormal(v, ret);
	}

	public int contractEdge(final Edge e, final Point3f p) {
		if (!edges.containsKey(e)) throw new IllegalArgumentException("no edge " +
			e);

		final Vertex v1 = getVertex(e.p1);

		HashSet<Integer> remainingTri = new HashSet<Integer>();
		remainingTri.addAll(v1.triangles);
		remainingTri.removeAll(e.triangles);

		// need to store this because it's destroyed
		// in removeVertex()
		ArrayList<Integer> remainingFaces = new ArrayList<Integer>();
		for (final int fIdx : remainingTri) {
			remainingFaces.add(getFace(3 * fIdx));
			remainingFaces.add(getFace(3 * fIdx + 1));
			remainingFaces.add(getFace(3 * fIdx + 2));
		}

		removeVertex(e.p1);

		// create the new vertex
		int vIdx = -1;
		if (vertexToIndex.containsKey(p)) {
			vIdx = vertexToIndex.get(p);
		}
		else {
			final Vertex v = new Vertex(p);
			vIdx = e.p1;
			vertices.set(vIdx, v);
			vertexToIndex.put(v, vIdx);
		}

		// add the remaining triangles, where the edge points
		// are replaced by the midpoint
		for (int i = 0; i < remainingFaces.size(); i += 3) {
			final int f1 = remainingFaces.get(i);
			final int f2 = remainingFaces.get(i + 1);
			final int f3 = remainingFaces.get(i + 2);
			if (f1 == e.p1) addFace(vIdx, f2, f3);
			if (f2 == e.p1) addFace(f1, vIdx, f3);
			if (f3 == e.p1) addFace(f1, f2, vIdx);
		}

		final Vertex v2 = getVertex(e.p2);
		remainingTri = new HashSet<Integer>();
		remainingTri.addAll(v2.triangles);
		remainingTri.removeAll(e.triangles);

		// need to store this because it's destroyed
		// in removeVertex()
		remainingFaces = new ArrayList<Integer>();
		for (final int fIdx : remainingTri) {
			remainingFaces.add(getFace(3 * fIdx));
			remainingFaces.add(getFace(3 * fIdx + 1));
			remainingFaces.add(getFace(3 * fIdx + 2));
		}

		removeVertex(e.p2);

		// add the remaining triangles, where the edge points
		// are replaced by the midpoint
		for (int i = 0; i < remainingFaces.size(); i += 3) {
			final int f1 = remainingFaces.get(i);
			final int f2 = remainingFaces.get(i + 1);
			final int f3 = remainingFaces.get(i + 2);
			if (f1 == e.p2) addFace(vIdx, f2, f3);
			if (f2 == e.p2) addFace(f1, vIdx, f3);
			if (f3 == e.p2) addFace(f1, f2, vIdx);
		}
		return vIdx;
	}

	public ArrayList<ArrayList<Point3f>> getSubmeshes() {
		final HashSet<Vertex> open = new HashSet<Vertex>();
		open.addAll(vertices);
		open.remove(null);
		final ArrayList<ArrayList<Point3f>> ret =
			new ArrayList<ArrayList<Point3f>>();
		while (!open.isEmpty()) {
			final HashSet<Integer> meshSet = new HashSet<Integer>();
			final LinkedList<Integer> queue = new LinkedList<Integer>();

			final Vertex start = open.iterator().next();
			open.remove(start);
			queue.add(vertexToIndex.get(start));

			while (!queue.isEmpty()) {
				final Integer vIdx = queue.poll();
				meshSet.add(vIdx);

				final Vertex v = getVertex(vIdx);
				for (final Edge e : v.edges) {
					final int nIdx = e.p1 == vIdx ? e.p2 : e.p1;
					final Vertex n = getVertex(nIdx);
					if (open.contains(n)) {
						open.remove(n);
						queue.offer(nIdx);
					}
				}
			}

			final ArrayList<Point3f> tris = new ArrayList<Point3f>();
			for (final int f : faces) {
				if (f != -1 && meshSet.contains(f)) tris.add(getVertex(f));
			}
			ret.add(tris);
		}
		return ret;
	}

	@SuppressWarnings("serial")
	protected static final class Vertex extends Point3f {

		final HashSet<Edge> edges;
		final HashSet<Integer> triangles;

		public Set<Edge> getEdges() {
			return edges;
		}

		public Set<Integer> getTriangles() {
			return triangles;
		}

		private Vertex(final Point3f p) {
			super(p);
			edges = new HashSet<Edge>();
			triangles = new HashSet<Integer>();
		}

		private void addEdge(final Edge e) {
			edges.add(e);
		}

		private void addEdges(final Edge e1, final Edge e2) {
			addEdge(e1);
			addEdge(e2);
		}

		private void removeEdge(final Edge e) {
			edges.remove(e);
		}

		private void addTriangle(final int i) {
			triangles.add(i);
		}

		private void removeTriangle(final int i) {
			triangles.remove(i);
		}
	}

	protected static final class Triangle {

		public final int f1, f2, f3;

		private Triangle(final int f1, final int f2, final int f3) {
			this.f1 = f1;
			this.f2 = f2;
			this.f3 = f3;
		}

		@Override
		public int hashCode() {
			return f1 * f2 * f3;
		}

		@Override
		public boolean equals(final Object o) {
			final Triangle r = (Triangle) o;
			int tmp;
			int tf1 = f1, tf2 = f2, tf3 = f3;
			if (tf2 < tf1) {
				tmp = tf1;
				tf1 = tf2;
				tf2 = tmp;
			}
			if (tf3 < tf2) {
				tmp = tf2;
				tf2 = tf3;
				tf3 = tmp;
			}
			if (tf2 < tf1) {
				tmp = tf1;
				tf1 = tf2;
				tf2 = tmp;
			}

			int rf1 = r.f1, rf2 = r.f2, rf3 = r.f3;
			if (rf2 < rf1) {
				tmp = rf1;
				rf1 = rf2;
				rf2 = tmp;
			}
			if (rf3 < rf2) {
				tmp = rf2;
				rf2 = rf3;
				rf3 = tmp;
			}
			if (rf2 < rf1) {
				tmp = rf1;
				rf1 = rf2;
				rf2 = tmp;
			}

			return tf1 == rf1 && tf2 == rf2 && tf3 == rf3;
		}
	}

	protected static final class Edge {

		public final int p1, p2;
		final HashSet<Integer> triangles;

		Edge(final int p1, final int p2) {
			this.p1 = p1;
			this.p2 = p2;
			triangles = new HashSet<Integer>();
		}

		@Override
		public boolean equals(final Object o) {
			final Edge e = (Edge) o;
			return (p1 == e.p1 && p2 == e.p2) || (p1 == e.p2 && p2 == e.p1);
		}

		@Override
		public int hashCode() {
			return p1 * p2;
		}

		private void addTriangle(final int i) {
			triangles.add(i);
		}

		private void removeTriangle(final int i) {
			triangles.remove(i);
		}

		public int nTriangles() {
			return triangles.size();
		}
	}
}
