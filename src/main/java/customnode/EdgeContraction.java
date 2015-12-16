
package customnode;

import customnode.FullInfoMesh.Edge;
import customnode.FullInfoMesh.Vertex;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.scijava.vecmath.Point3f;
import org.scijava.vecmath.Vector3f;

public class EdgeContraction {

	private final boolean LENGTH_ONLY;
	private final ArrayList<FullInfoMesh> mesh;
	private final SortedSet<CEdge> queue;
	private final Map<CEdge, Float> edgeCosts = new HashMap<CEdge, Float>();

	public final void removeUntil(final float maxCost) {
		while (!queue.isEmpty()) {
			final CEdge e = queue.first();
			if (edgeCosts.get(e) > maxCost) break;
			queue.remove(e);
			fuse(e);
		}
	}

	public final int removeNext(final int n) {
		int curr = getRemainingVertexCount();
		final int goal = curr - n;

		while (curr > goal && !queue.isEmpty()) {
			final CEdge e = queue.first();
			queue.remove(e);
			fuse(e);
			curr = getRemainingVertexCount();
		}
		int v = 0;
		for (int i = 0; i < mesh.size(); i++)
			v += mesh.get(i).getVertexCount();

		return v;
	}

	public int getRemainingVertexCount() {
		int v = 0;
		for (int i = 0; i < mesh.size(); i++)
			v += mesh.get(i).getVertexCount();

		return v;
	}

	public final Edge nextToRemove() {
		return queue.first().edge;
	}

	public final int getVertexCount() {
		int v = 0;
		for (final FullInfoMesh m : mesh)
			v += m.getVertexCount();
		return v;
	}

	private static final ArrayList<FullInfoMesh> makeList(final FullInfoMesh m) {
		final ArrayList<FullInfoMesh> l = new ArrayList<FullInfoMesh>();
		l.add(m);
		return l;
	}

	public EdgeContraction(final FullInfoMesh mesh) {
		this(mesh, false);
	}

	public EdgeContraction(final FullInfoMesh mesh, final boolean edgeLengthOnly)
	{
		this(makeList(mesh), edgeLengthOnly);
	}

	public EdgeContraction(final ArrayList<FullInfoMesh> meshes,
		final boolean edgeLengthOnly)
	{

		this.LENGTH_ONLY = edgeLengthOnly;
		queue = new TreeSet<CEdge>(new EdgeComparator());
		mesh = meshes;

		int meshIdx = 0;
		for (final FullInfoMesh fim : mesh) {
			for (final Edge e : fim.edges.keySet()) {
				final CEdge ce = new CEdge(e, meshIdx);
				edgeCosts.put(ce, computeCost(ce));
				queue.add(ce);
			}
			meshIdx++;
		}
	}

	public ArrayList<FullInfoMesh> getMeshes() {
		return mesh;
	}

	protected float computeCost(final CEdge ce) {
		final float l = getLength(ce);
		if (LENGTH_ONLY) return l;

		final FullInfoMesh fim = mesh.get(ce.meshIdx);
		final Edge e = ce.edge;

		final HashSet<Integer> triangles = new HashSet<Integer>();
		triangles.addAll(fim.getVertex(e.p1).triangles);
		triangles.addAll(fim.getVertex(e.p2).triangles);
		for (final int i : e.triangles)
			triangles.remove(i);

		float angle = 0;
		final Vector3f oldN = new Vector3f();
		final Vector3f newN = new Vector3f();
		final Point3f midp = getMidpoint(ce);

		for (final int fIdx : triangles) {

			final int f1 = fim.faces.get(fIdx * 3);
			final int f2 = fim.faces.get(fIdx * 3 + 1);
			final int f3 = fim.faces.get(fIdx * 3 + 2);

			final Point3f v1 = fim.getVertex(f1);
			final Point3f v2 = fim.getVertex(f2);
			final Point3f v3 = fim.getVertex(f3);

			getNormal(v1, v2, v3, oldN);
			if (f1 == e.p1 || f1 == e.p2) getNormal(midp, v2, v3, newN);
			else if (f2 == e.p1 || f2 == e.p2) getNormal(v1, midp, v3, newN);
			else if (f3 == e.p1 || f3 == e.p2) getNormal(v1, v2, midp, newN);
			oldN.normalize();
			newN.normalize();
			final float dAngle = oldN.angle(newN);
			if (!Float.isNaN(dAngle)) angle += oldN.angle(newN);
		}
		return l * angle;
	}

	private final boolean shouldFuse(final CEdge ce) {
		// only allow to fuse if it's a well-behaved mesh region.
		// In particular, don't fuse if is kind of a fold-back,
		// which is recognized by checking the neighbor triangles:
		// if there are more than 4 vertices in common (the vertices
		// of the 2 triangles of e, which have 2 points in common).
		final FullInfoMesh fim = mesh.get(ce.meshIdx);
		final Edge e = ce.edge;
		final Set<Vertex> neighborVertices = new HashSet<Vertex>();
		final int n1 = fim.getVertex(e.p1).edges.size() + 1;
		if (n1 < 5) return false;
		for (final Edge e1 : fim.getVertex(e.p1).edges) {
			neighborVertices.add(fim.getVertex(e1.p1));
			neighborVertices.add(fim.getVertex(e1.p2));
		}
		final int n2 = fim.getVertex(e.p2).edges.size() + 1;
		if (n2 < 5) return false;
		for (final Edge e2 : fim.getVertex(e.p2).edges) {
			neighborVertices.add(fim.getVertex(e2.p1));
			neighborVertices.add(fim.getVertex(e2.p2));
		}
		return neighborVertices.size() == n1 + n2 - 4;
	}

	private final void fuse(final CEdge ce) {
		if (!shouldFuse(ce)) return;

		final FullInfoMesh fim = mesh.get(ce.meshIdx);
		final Edge e = ce.edge;

		// remove all edges of e.p1 and e.p2 from the queue
		for (final Edge ed : fim.getVertex(e.p1).edges)
			queue.remove(new CEdge(ed, ce.meshIdx));
		for (final Edge ed : fim.getVertex(e.p2).edges)
			queue.remove(new CEdge(ed, ce.meshIdx));

		final Point3f midp = getMidpoint(ce);

		final int mIdx = fim.contractEdge(e, midp);

		// re-add the affected edges to the priority queue
		final Set<Edge> newEdges = fim.getVertex(mIdx).edges;
		for (final Edge edge : newEdges) {
			final CEdge cEdge = new CEdge(edge, ce.meshIdx);
			edgeCosts.put(cEdge, computeCost(cEdge));
			queue.add(cEdge);
		}

		// get the neighbor points of midp
		final List<Integer> neighbors = new ArrayList<Integer>();
		for (final Edge edge : newEdges) {
			if (edge.p1 != mIdx) neighbors.add(edge.p1);
			if (edge.p2 != mIdx) neighbors.add(edge.p2);
		}

		// collect all the edges of the neighborpoints
		// these are all the edges whose cost must be updated.
		final Set<Edge> neighborEdges = new HashSet<Edge>();
		for (final int n : neighbors)
			neighborEdges.addAll(fim.getVertex(n).edges);
		neighborEdges.removeAll(newEdges);

		// update costs
		for (final Edge ed : neighborEdges)
			queue.remove(new CEdge(ed, ce.meshIdx));

		for (final Edge edge : neighborEdges) {
			final CEdge cEdge = new CEdge(edge, ce.meshIdx);
			edgeCosts.put(cEdge, computeCost(cEdge));
			queue.add(cEdge);
		}
	}

	private final Vector3f v1 = new Vector3f();
	private final Vector3f v2 = new Vector3f();

	void getNormal(final Point3f p1, final Point3f p2, final Point3f p3,
		final Vector3f ret)
	{
		v1.sub(p2, p1);
		v2.sub(p3, p1);
		ret.cross(v1, v2);
	}

	void getMidpoint(final CEdge e, final Point3f ret) {
		final Point3f p1 = mesh.get(e.meshIdx).getVertex(e.edge.p1);
		final Point3f p2 = mesh.get(e.meshIdx).getVertex(e.edge.p2);
		ret.add(p1, p2);
		ret.scale(0.5f);
	}

	Point3f getMidpoint(final CEdge e) {
		final Point3f ret = new Point3f();
		getMidpoint(e, ret);
		return ret;
	}

	float getLength(final CEdge e) {
		return mesh.get(e.meshIdx).getVertex(e.edge.p1).distance(
			mesh.get(e.meshIdx).getVertex(e.edge.p2));
	}

	private final class CEdge {

		final Edge edge;
		final int meshIdx;

		CEdge(final Edge edge, final int mIdx) {
			this.edge = edge;
			this.meshIdx = mIdx;
		}

		@Override
		public boolean equals(final Object o) {
			final CEdge e = (CEdge) o;
			return meshIdx == e.meshIdx && edge.equals(e.edge);
		}

		@Override
		public int hashCode() {
			long bits = 1L;
			bits = 31L * bits + edge.p1;
			bits = 31L * bits + edge.p2;
			bits = 31L * bits + meshIdx;
			return (int) (bits ^ (bits >> 32));
		}
	}

	private final class EdgeComparator implements Comparator<CEdge> {

		private final Point3f mp1 = new Point3f();
		private final Point3f mp2 = new Point3f();

		@Override
		public int compare(final CEdge e1, final CEdge e2) {
			if (e1.equals(e2)) return 0;
			final float l1 = edgeCosts.get(e1);
			final float l2 = edgeCosts.get(e2);
			if (l1 < l2) return -1;
			if (l2 < l1) return 1;

			if (e1.meshIdx < e2.meshIdx) return -1;
			if (e1.meshIdx > e2.meshIdx) return +1;

			getMidpoint(e1, mp1);
			getMidpoint(e2, mp2);

			if (mp1.z < mp2.z) return -1;
			if (mp1.z > mp2.z) return +1;
			if (mp1.y < mp2.y) return -1;
			if (mp1.y > mp2.y) return +1;
			if (mp1.x < mp2.x) return -1;
			if (mp1.x > mp2.x) return +1;

			return 0;
		}
	}
}
