// $Id: EditLog.java,v 1.8 2003-01-09 14:15:26 ctl Exp $ D
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
import java.util.Stack;

import org.xml.sax.SAXException;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;

/** Logs edit operations perfomed by the merge algorithm. */

public class EditLog {

  // Inyternal edit op codes
  private static final int INSERT = 0;
  private static final int UPDATE = 1;
  private static final int COPY = 2;
  private static final int MOVE = 3;
  private static final int DELETE = 4;

  // Edit tag names
  private static final String[] OPTAGS = {"insert","update","copy","move",
                                          "delete"};

  private static class Path {

    String path =null;
    Node n = null;

    public Path(String aPath) {
      path = aPath;
    }

    public Path(Node aNode) {
      n = aNode;
    }

    public String toString() {
      if( path != null )
        return path;
      else
        return PathTracker.getPathString(n);
    }
  }

  // Class for storing an edit operation in memory.
  private class EditEntry {
    int type = -1;
    BaseNode baseSrc=null;
    BranchNode branchSrc = null;
    Path dstPath = null;

    EditEntry( int aType, BaseNode aBaseNode, BranchNode aBranchNode,
                String aDstPath ) {
      type = aType;
      baseSrc = aBaseNode;
      branchSrc = aBranchNode;
      dstPath = new Path(aDstPath);
    }

    EditEntry( int aType, BaseNode aBaseNode, BranchNode aBranchNode,
                Node aDstNode ) {
      type = aType;
      baseSrc = aBaseNode;
      branchSrc = aBranchNode;
      dstPath = new Path(aDstNode);
    }

  }


  private Stack checkPoints = new Stack();
  // Edits in the log. A list of EditEntries.
  private Vector edits = new Vector();
  private PathTracker pt = null;

  public EditLog() {
  }

  /** Construct edit log. The PathTracker given as argument is queried for the
   *  current position in the merge tree each time an edit operation is
   *  added.
   *  @param apt PathTracker that tracks the current position in the merged tree
   */
  public EditLog(PathTracker apt) {
    pt = apt;
  }

  /** Add insert operation.
   *  @param n Node that is inserted
   *  @param childPos position in the current child list of the merge tree */
  public void insert( BranchNode n, int childPos ) {
    edits.add( new EditEntry(INSERT,null,n,pt.getPathString(childPos)));
  }

  public void insert( BranchNode n ) {
    edits.add( new EditEntry(INSERT,null,n,n));
  }

  /** Add move operation.
   *  @param n Node that is moved
   *  @param childPos position in the current child list of the merge tree */
  public void move( BranchNode n, int childPos ) {
    edits.add( new EditEntry(MOVE,n.getBaseMatch(),n,
                                  pt.getPathString(childPos)));
  }

  public void move( BranchNode n ) {
    edits.add( new EditEntry(MOVE,n.getBaseMatch(),n,n));
  }

  /** Add copy operation.
   *  @param n Node that is copied
   *  @param childPos position in the current child list of the merge tree */
  public void copy( BranchNode n, int childPos ) {
    edits.add( new EditEntry(COPY,n.getBaseMatch(),n,
                                  pt.getPathString(childPos)));
  }

  public void copy( BranchNode n ) {
    edits.add( new EditEntry(COPY,n.getBaseMatch(),n,n));
  }

  /** Add move operation.
   *  @param n Node that is upated.
   *  @param childPos position in the current child list of the merge tree */
  public void update( BranchNode n ) {
    if( pt == null )
      edits.add( new EditEntry(UPDATE,n.getBaseMatch(),n,n));
    else
      edits.add( new EditEntry(UPDATE,n.getBaseMatch(),n,
                             pt.getFullPathString()));
  }

  /** Add delete operation.
   *  @param n Node that is deleted
   *  @param originatingList Node, whose child list originated the delete.
   */
  public void delete( BaseNode n, BranchNode originatingList ) {
    edits.add( new EditEntry(DELETE,n,originatingList,""));
  }

  /** Write out the edit log. */
  public void writeEdits( ContentHandler ch ) throws SAXException {
    ch.startDocument();
    AttributesImpl atts = new AttributesImpl();
    ch.startElement("","","edits",atts);
    for(int i=0;i<edits.size();i++)
      outputEdit((EditEntry) edits.elementAt(i),ch);
    ch.endElement("","","edits");
    ch.endDocument();
  }

  protected void outputEdit( EditEntry ee, ContentHandler ch )
                              throws SAXException {
    AttributesImpl atts = new AttributesImpl();
    if( ee.type != DELETE )
      atts.addAttribute("","","path","CDATA",ee.dstPath.toString());
    if( ee.type != INSERT )
      atts.addAttribute("","","src","CDATA",PathTracker.getPathString(ee.baseSrc));
    atts.addAttribute("","","originTree","CDATA",ee.branchSrc.isLeftTree() ?
                      "branch1" : "branch2");
    atts.addAttribute("","",(ee.type != DELETE ) ? "originNode" : "originList",
      "CDATA",PathTracker.getPathString(ee.branchSrc));
    ch.startElement("","",OPTAGS[ee.type],atts);
    ch.endElement("","",OPTAGS[ee.type]);
  }

  /** Mark a checkpoint in the edit log. */
  public void checkPoint() {
    checkPoints.push(new  Integer( edits.size() ) );
  }

  /** Remove all edits added after the last checkpoint. */
  public void rewind() {
    int firstFree = ((Integer) checkPoints.pop()).intValue();
    edits.setSize(firstFree);
  }

  /** Commit edits made after the last checkpoint. */
  public void commit() {
    checkPoints.pop();
  }
}