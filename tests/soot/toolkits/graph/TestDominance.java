package soot.toolkits.graph;

/* Tim Henderson (tadh@case.edu)
 *
 * Copyright (c) 2014, Tim Henderson, Case Western Reserve University
 *   Cleveland, Ohio 44106
 *   All Rights Reserved.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc.,
 *   51 Franklin Street, Fifth Floor,
 *   Boston, MA  02110-1301
 *   USA
 * or retrieve version 2.1 at their website:
 *   http://www.gnu.org/licenses/lgpl-2.1.html
 */

import static org.junit.Assert.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import org.junit.Test;
import org.junit.Ignore;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.*;

import soot.toolkits.graph.*;
import soot.toolkits.graph.pdg.MHGDominatorTree;

public class TestDominance {

    public Set<Integer> kid_ids(DominatorNode dn) {
        Set<Integer> kids = new HashSet<Integer>();
        for (Object o : dn.getChildren()) {
            DominatorNode dkid = (DominatorNode)o;
            Node kid = (Node)dkid.getGode();
            kids.add(kid.id);
        }
        return kids;
    }

    public Map<Integer,DominatorNode> kid_map(DominatorNode dn) {
        Map<Integer,DominatorNode> kids = new HashMap<Integer,DominatorNode>();
        for (Object o : dn.getChildren()) {
            DominatorNode dkid = (DominatorNode)o;
            Node kid = (Node)dkid.getGode();
            kids.put(kid.id, dkid);
        }
        return kids;
    }

    @Test
    public void TestSimpleDiamond() {
        Node x = new Node(4);
        Node n = new Node(1).addkid((new Node(2)).addkid(x)).addkid((new Node(3)).addkid(x));
        Graph g = new Graph(n);
        MHGDominatorsFinder finder = new MHGDominatorsFinder(g);
        DominatorTree tree = new DominatorTree(finder);
        assertThat(tree.getHeads().size(), is(1));

        DominatorNode head = tree.getHeads().get(0);
        assertThat(((Node)head.getGode()).id, is(1));

        Set<Integer> kids = kid_ids(head);
        assertThat(kids.size(), is(3));
        assertThat(kids, contains(2, 3, 4));
    }

    @Test
    public void TestAcyclicCFG() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        Node n5 = new Node(5);
        Node n6 = new Node(6);
        Node n7 = new Node(7);
        Node n8 = new Node(8);
        Node n9 = new Node(9);
        Node n10 = new Node(10);
        Node n11 = new Node(11);
        n1.addkid(n2).addkid(n3);
        n2.addkid(n9);
        n3.addkid(n4).addkid(n5);
        n4.addkid(n9);
        n5.addkid(n6).addkid(n10);
        n6.addkid(n7).addkid(n8);
        n7.addkid(n10);
        n8.addkid(n10);
        n9.addkid(n11);
        n10.addkid(n11);
        Graph g = new Graph(n1);

        MHGDominatorsFinder finder = new MHGDominatorsFinder(g);
        DominatorTree tree = new DominatorTree(finder);
        assertThat(tree.getHeads().size(), is(1));

        DominatorNode n = tree.getHeads().get(0);
        assertThat(((Node)n.getGode()).id, is(1));
        Set<Integer> kids = kid_ids(n);
        assertThat(kids.size(), is(4));
        assertThat(kids, contains(2, 3, 9, 11));

        Map<Integer, DominatorNode> KM = kid_map(n);
        DominatorNode m = KM.get(2);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        m = KM.get(9);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        m = KM.get(11);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        n = KM.get(3);
        kids = kid_ids(n);
        assertThat(kids.size(), is(2));
        assertThat(kids, contains(4, 5));

        KM = kid_map(n);
        m = KM.get(4);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        n = KM.get(5);
        kids = kid_ids(n);
        assertThat(kids.size(), is(2));
        assertThat(kids, contains(6, 10));

        KM = kid_map(n);
        m = KM.get(10);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        n = KM.get(6);
        kids = kid_ids(n);
        assertThat(kids.size(), is(2));
        assertThat(kids, contains(7, 8));

        KM = kid_map(n);
        m = KM.get(7);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        m = KM.get(8);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));
    }

    @Test
    public void TestMultiTailedPostDom() {
        Node n1 = new Node(1);
        Node n2 = new Node(2);
        Node n3 = new Node(3);
        Node n4 = new Node(4);
        Node n5 = new Node(5);
        Node n6 = new Node(6);
        n1.addkid(n2).addkid(n3);
        n3.addkid(n4).addkid(n5);
        n4.addkid(n6);
        n5.addkid(n6);
        Graph g = new Graph(n1);

        MHGDominatorsFinder finder = new MHGDominatorsFinder(g);
        MHGDominatorTree tree = new MHGDominatorTree(finder);
        assertThat(tree.getHeads().size(), is(1));

        DominatorNode n = tree.getHeads().get(0);
        assertThat(((Node)n.getGode()).id, is(1));
        Set<Integer> kids = kid_ids(n);
        assertThat(kids.size(), is(2));
        assertThat(kids, contains(2, 3));

        Map<Integer, DominatorNode> KM = kid_map(n);
        DominatorNode m = KM.get(2);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        n = KM.get(3);
        kids = kid_ids(n);
        assertThat(kids.size(), is(3));
        assertThat(kids, contains(4, 5, 6));

        KM = kid_map(n);
        m = KM.get(4);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        m = KM.get(5);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        m = KM.get(6);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        // ---------- now post-dom --------------

        MHGPostDominatorsFinder pfinder = new MHGPostDominatorsFinder(g);
        tree = new MHGDominatorTree(pfinder);

        Map<Integer,DominatorNode> heads = new HashMap<Integer,DominatorNode>();
        for (Object o : tree.getHeads()) {
            DominatorNode dhead = (DominatorNode)o;
            Node head = (Node)dhead.getGode();
            heads.put(head.id, dhead);
        }

        Set<Integer> head_ids = heads.keySet();
        assertThat(head_ids.size(), is(3));
        assertThat(head_ids, contains(1, 2, 6));

        m = heads.get(1);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        m = heads.get(2);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        n = heads.get(6);
        kids = kid_ids(n);
        assertThat(kids.size(), is(3));
        assertThat(kids, contains(3, 4, 5));

        KM = kid_map(n);
        m = KM.get(3);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        m = KM.get(4);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));

        m = KM.get(5);
        kids = kid_ids(m);
        assertThat(kids.size(), is(0));
    }
}

class Graph implements DirectedGraph<Node> {

    Node root;
    List<Node> nodes;
    List<Node> tails = new ArrayList<Node>();

    public Graph(Node root) {
        this.root = root;
        for (Node n : this) {
            if (n.succs.size() == 0) {
                tails.add(n);
            }
        }
    }

    /** 
     *  Returns a list of entry points for this graph.
     */
    public List<Node> getHeads() {
        return Collections.singletonList(this.root);
    }

    /** Returns a list of exit points for this graph. */
    public List<Node> getTails() {
        return tails;
    }

    /** 
     *  Returns a list of predecessors for the given node in the graph.
     */
    public List<Node> getPredsOf(Node s){
        return ((Node)s).preds;
    }

    /**
     *  Returns a list of successors for the given node in the graph.
     */
    public List<Node> getSuccsOf(Node s) {
        return ((Node)s).succs;
    }

    /**
     *  Returns the node count for this graph.
     */
    public int size() {
        if (this.nodes == null) {
             this.nodes = this.dfs(this.root);
        }
        return this.nodes.size();
    }

    /**
     *  Returns an iterator for the nodes in this graph. No specific ordering
     *  of the nodes is guaranteed.
     */
    public Iterator<Node> iterator() {
        if (this.nodes == null) {
             this.nodes = this.dfs(this.root);
        }
        Iterator<Node> i = this.nodes.iterator();
        return i;
    }

    public List<Node> dfs(Node root) {
        List<Node> list = new ArrayList<Node>();
        Set<Node> seen = new HashSet<Node>();
        dfs_visit(root, seen, list);
        return list;
    }

    void dfs_visit(Node node, Set<Node> seen, List<Node> list) {
        seen.add(node);
        list.add(node);
        for (Node kid : node.succs) {
            if (!seen.contains(kid)) {
                dfs_visit(kid, seen, list);
            }
        }
        for (Node parent : node.preds) {
            if (!seen.contains(parent)) {
                dfs_visit(parent, seen, list);
            }
        }
    }
}

class Node {

    int id;
    List<Node> preds = new ArrayList<Node>();
    List<Node> succs = new ArrayList<Node>();

    public Node(int id) {
        this.id = id;
    }

    public Node addkid(Node kid) {
        kid.preds.add(this);
        this.succs.add(kid);
        return this;
    }

    public int hashCode() {
        return this.id;
    }

    public boolean Equals(Object o) {
        if (o == null) {
            return false;
        } else if (o instanceof Node) {
            return this.Equals((Node)o);
        }
        return false;
    }

    public boolean Equals(Node o) {
        if (o == null) {
            return false;
        }
        return this.id == o.id;
    }

}
