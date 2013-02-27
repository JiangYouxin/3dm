// $Id: PathTracker.java,v 1.6 2003-01-30 09:23:52 ctl Exp $ D
//
// Copyright (c) 2001, Tancred Lindholm <ctl@cs.hut.fi>
//
// This file is part of 3DM.
//
// 3DM is free software; you can redistribute it and/or modify
// it under the terms of the GNU Lesser General Public License as published by
// the Free Software Foundation; either version 2.1 of the License, or
// (at your option) any later version.
//
// 3DM is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public License
// along with 3DM; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
package tdm.lib;

import java.util.LinkedList;
import java.util.Iterator;
import java.util.Collections;

/** Tracks current position in a tree. */

public class PathTracker {

  static final char PATHSEP='/';

  private LinkedList path = null;
  private int childPos = -1;

  /** Create new tracker. The location is initialized to the root, or "/" */
  public PathTracker() {
    resetContext();
  }

  /** The location is initialized to the root, or "/" */
  public void resetContext() {
    path = new LinkedList();
    childPos = 0;
  }

  /** Move to next child. Must be in a subtree, created by enterSubtree() */
  public void nextChild() {
    childPos++;
  }

  /** Start new subtree */
  public void enterSubtree() {
    path.addLast(new Integer(childPos));
    childPos = 0;
  }

  /** End subtree. */
  public void exitSubtree() {
    Integer oldpos = (Integer) path.removeLast();
    childPos = oldpos.intValue();
  }

  /** Get string describing current location, excluding current child position */
  public String getPathString() {
    return getPathString(path,-1,false);
  }

  /** Get string describing current location, including current child position */
  public String getFullPathString() {
    return getPathString(path,childPos,true);
  }

  /** Get string describing current location, including current child position */
  public String getPathString(int achildPos) {
    return getPathString(path,achildPos,true);
  }

  /** Get string describing current location of a node in a tree. Exclude the
   *  child position of the node (path ends in "/"). */
  public static String getPathString(Node n ) {
    return getPathString(makePath(n),-1,false);
  }

  /** Get string describing current location of a node in a tree. Include the
   *  child position of the node (path ends in a number). */
  public static String getPathString(Node n,int achildPos) {
    return getPathString(makePath(n),achildPos,true);
  }

  // Create path string from linked list of nodes.
  private static String getPathString( LinkedList path, int childPos,
    boolean useChildPos ) {
    StringBuffer p = new StringBuffer();
    Iterator i=path.iterator(); // Skip artificial root node
    i.next();
    for(;i.hasNext();) {
      p.append(PATHSEP);
      p.append(((Integer) i.next()).toString());
    }
    if( useChildPos ) {
      p.append(PATHSEP);
      p.append(childPos);
    }
    return p.toString();
  }

  // Create linked list of nodes to the root from a node in a tree.
  private static LinkedList makePath( Node n ) {
    LinkedList path = new LinkedList();
    do {
      path.addLast(new Integer(n.getChildPos()));
    } while( (n = n.getParentAsNode()) != null);
///    path.removeLast(); // We don't want the artificial root node in the path
    Collections.reverse(path);
    return path;
  }

  public static Node followPath(Node root, String path) {
    int pos = 1;
    if (path.length() < 1)
      return root;
    while (pos < path.length()) {
      int childno = 0;
      while (pos < path.length() && Character.isDigit(path.charAt(pos))) {
        childno = childno * 10 + (path.charAt(pos) - '0');
        pos++;
      }
      pos++; // skip '/'
      root = root.getChildAsNode(childno);
    }
    return root;
  }

}