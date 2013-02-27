// $Id: Node.java,v 1.15 2006-02-06 11:57:59 ctl Exp $ D
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

import java.util.Vector;

/** Node in the parse tree. Each node in the parse trees has 0-n children,
 * content and a tag to identify nodes in the same matching subtrees (the
 * <code>matchArea</code> field). In addition, all nodes except the root
 * have a parent.
 */

public abstract class Node {

  protected Vector children = new Vector();
  protected XMLNode content = null;
  protected Node parent = null;
  protected int childPos=-1; // zero-based, i.e. first child = 0
  protected MatchArea area = null;

  protected Node() {
    parent = null;
    childPos = -1;
  }

  public void addChild( Node n) {
    n.parent=this;
    n.childPos=children.size();
    children.add(n);
  }


  public void setContent( XMLNode aContent ) {
    content = aContent;
  }

  public void replaceChild(int ix, Node n ) {
    children.setElementAt(n,ix);
  }

  public void addChild( int ix, Node n) {
    //System.err.println("add @ ix "+ix+" count ="+children.size());
    n.parent=this;
    children.add(ix,n);
    n.childPos=ix;
    // Update childPoses
    for( int i=ix+1;i<getChildCount();i++)
      getChildAsNode(i).childPos=i;
/*    System.err.print("Chilposes after add is:");
    for(int i=0;i<getChildCount();i++)
      System.err.print(" "+getChildAsNode(i).getChildPos());
    System.err.println();*/
  }

  public void removeChildren( ) {
    children.clear();
  }

  public void removeChild( int ix) {
    children.remove(ix);
    // Update childPoses
    for( int i=ix;i<getChildCount();i++)
      getChildAsNode(i).childPos=i;
      /*
    System.err.print("Chilposes after del is:");
    for(int i=0;i<getChildCount();i++)
      System.err.print(" "+getChildAsNode(i).getChildPos());
    System.err.println();*/

  }

  public Node getParentAsNode() {
    return parent;
  }

  public int getChildCount() {
    return children.size();
  }

  public Node getChildAsNode(int ix) {
    return (Node) children.elementAt(ix);
  }

  public boolean hasLeftSibling() {
    return childPos > 0;
  }

  public boolean hasRightSibling() {
    return parent != null && childPos < parent.children.size()-1;
  }

  public Node getLeftSibling() {
    if( parent == null || childPos == 0 )
      return null;
    else
      return parent.getChildAsNode(childPos-1);
  }

  public Node getRightSibling() {
    if( parent == null || childPos == parent.children.size() -1 )
      return null;
    else
      return parent.getChildAsNode(childPos+1);
  }

  public XMLNode getContent( ) {
    return content;
  }

  public int getChildPos() {
    return childPos;
  }

  public MatchArea getMatchArea() {
    return area;
  }

  public void setMatchArea(MatchArea anArea) {
    area=anArea;
  }
//$CUT

  public void debug( java.io.PrintWriter pw, int indent ) {
    String ind = "                                                   ".substring(0,indent+1);
    pw.println( ind + content );
    //pw.println("MatchArea:"+getMatchArea());
  }

  public void debugTree( java.io.PrintWriter pw, int indent ) {
    debug(pw,indent );
    for( int i=0;i<getChildCount();i++)
      ((Node) children.elementAt(i)).debugTree(pw,indent+1);
  }
//$CUT
}
