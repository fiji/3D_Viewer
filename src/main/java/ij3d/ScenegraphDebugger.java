
package ij3d;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;

import javax.media.j3d.Group;
import javax.media.j3d.Node;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.TreeNode;

public class ScenegraphDebugger {

	public static void displayTree(final Node root) {
		displayTree(root, "");
	}

	private static void displayTree(final Node node, final String indent) {
		System.out.println(indent + node);
		if (node instanceof Group) {
			final Enumeration ch = ((Group) node).getAllChildren();
			while (ch.hasMoreElements())
				displayTree((Node) ch.nextElement(), indent + "   ");
		}
	}

	public static void showTree(final Node root) {
		final JTree tree = new JTree(new J3DNode(root, null));
		final JFrame parent = null;
		final JDialog dialog = new JDialog(parent, "Scenegraph");
		final JScrollPane scroll = new JScrollPane(tree);
		dialog.getContentPane().add(scroll);
		dialog.pack();
		dialog.setVisible(true);
	}

	private static class J3DNode implements TreeNode {

		private final Node node;
		private final J3DNode parent;
		private J3DNode[] children = null;

		public J3DNode(final Node n, final J3DNode parent) {
			this.node = n;
			this.parent = parent;
			if (node instanceof Group) {
				final Group g = (Group) node;
				children = new J3DNode[g.numChildren()];
				for (int i = 0; i < children.length; i++)
					children[i] = new J3DNode(g.getChild(i), this);
			}
		}

		@Override
		public Enumeration children() {
			if (children != null) {
				return Collections.enumeration(Arrays.asList(children));
			}
			return null;
		}

		@Override
		public boolean getAllowsChildren() {
			return node instanceof Group;
		}

		@Override
		public TreeNode getChildAt(final int arg0) {
			if (!(node instanceof Group)) return null;
			return children[arg0];
		}

		@Override
		public int getChildCount() {
			return children.length;
		}

		@Override
		public int getIndex(final TreeNode arg0) {
			for (int i = 0; i < children.length; i++) {
				if (children[i].equals(arg0)) return i;
			}
			return -1;
		}

		@Override
		public TreeNode getParent() {
			return parent;
		}

		@Override
		public boolean isLeaf() {
			return !(node instanceof Group);
		}

		@Override
		public String toString() {
			return node.toString();
		}
	}
}
