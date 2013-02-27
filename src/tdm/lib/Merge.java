// $Id: Merge.java,v 1.42 2006-02-06 11:57:59 ctl Exp $ D
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

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Vector;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/** 3-way merge of a base and two branch trees. */

public class Merge implements XMLNode.Merger {

  private TriMatching m = null;
  private PathTracker pt = new PathTracker();
  private ConflictLog clog = new ConflictLog(pt);
  private EditLog elog = new EditLog(pt);

  /** Construct a new merge object. The matched trees to merge are passed in
   *  the TriMatching argument.
   */

  public Merge(TriMatching am) {
    m = am;
  }

  public ConflictLog getConflictLog() {
    return clog;
  }

  public EditLog getEditLog() {
    return elog;
  }

  /** Run 3-way merge of trees */
  public void merge( ContentHandler ch ) throws SAXException {
    pt.resetContext();
    ch.startDocument();
    try {
      treeMerge(m.getLeftRoot(), m.getRightRoot(), new SAXExternalizer( ch ),this);
    } catch ( IOException x) {
      throw new SAXException("Externalizer threw IOException: "+x.getMessage());
    }
    ch.endDocument();
  }

 int debug = 0; // Debug variable
///
  /** Main merging function. Merges the child lists of a and b, and then
   *  recurses for each child in the merged list.
   */

  public void treeMerge( BranchNode a, BranchNode b, XMLNode.Externalizer ex,
                            XMLNode.Merger nodeMerger )
                            throws SAXException, IOException {
/*    if (debug >= 0)
      System.out.println("%%%% Recursion " + (a== null ? "-" : a.getContent()) + "," +
                         (b==null ? "-" : b.getContent()));
*/
//$CUT
    if( (a != null && ((a.getBaseMatchType() | BranchNode.MATCH_CHILDREN) == 0 )) ||
        (b != null && ((b.getBaseMatchType() | BranchNode.MATCH_CHILDREN) == 0 ) ) )
      throw new RuntimeException("mergeNode: match type should be match children, otherwise the node should be null!");
//$CUT
    MergeList mlistA = a != null ? makeMergeList( a ) : null;
    MergeList mlistB = b != null ? makeMergeList( b ) : null;
    MergePairList merged = null;
    pt.enterSubtree(); // Update path tracker to tell where we are...
//$CUT
//    System.out.println("A = " + a ==null ? "-" : a.getContent().toString());
//    System.out.println("B = " + b ==null ? "-" : b.getContent().toString());
//    System.out.println("--------------------------");
/*
    if( mlistA.getEntryCount() > 0 )
      debug=1;
    else debug=0;

    if(debug>=0 ) {
      System.out.println("#########################################Merge A list");
      if( mlistA != null )
        mlistA.print();
      else
        System.out.println("--none--");
      System.out.println("#####################################Merge B list");
      if( mlistB != null )
        mlistB.print();
      else
        System.out.println("--none--");
      debug =0;
    }*/
//$CUT
    // Generate merge pair List, store in the merged object
    if( mlistA != null && mlistB != null )
      merged = makeMergePairList( mlistA, mlistB ); // Merge lists
    else
      // Only one child list, construct mergepairllist directly from child mlist
      merged = mergeListToPairList( mlistA == null ? mlistB : mlistA, null );
//$CUT
/*
    if( debug>=0) {
      System.out.println("#########################################MERGED LIST");
      merged.print();
    }*/
//$CUT
    // Handle updates & Recurse
    for( int i=0;i<merged.getPairCount();i++) {
      MergePair mergePair = merged.getPair(i);
      XMLNode mergedNode = mergeNodeContent( mergePair, nodeMerger );
      if( mergedNode instanceof XMLTextNode ) {
        XMLTextNode text = (XMLTextNode) mergedNode;
        ex.startNode(text);
        ex.endNode(text);
        // NOTE: Theoretically, if we have matched text and element nodes we
        // need to recurse here. But, the current matching algo never matches
        // across types, so there's no need for recursion
      } else {
        // It's an element node
        XMLNode mergedElement = mergedNode;
        ex.startNode(mergedElement);
        // Figure out partners for recurse
        MergePair recursionPartners = getRecursionPartners( mergePair );
        // Recurse for subtrees
        treeMerge(recursionPartners.getFirstNode(),
                  recursionPartners.getSecondNode(),ex,nodeMerger);
        ex.endNode(mergedElement);
      }
      pt.nextChild();
    }
    pt.exitSubtree();
  }

  /** Return merged content of the nodes in a merge pair. */
  protected XMLNode mergeNodeContent( MergePair mp, XMLNode.Merger nodeMerger ) {
    // Merge contents of node and partner (but only if there's a partner)
    // -------------------
    // Table
    // n1    n2     Merge
    // any   null   n1
    // cont  cont   merge(n1,n2)
    // cont  str    n2
    // cont  full   merge(n1,n2)
    // str   str    FORCED content merge
    // str   full   n1 cont
    // full  full   merge(n1,n2)

    BranchNode n1 = mp.getFirstNode(), n2 = mp.getSecondNode();
    if( n1 == null || n2==null )
      return (n1==null ? n2 : n1).getContent();
    else if( n1.isMatch(BranchNode.MATCH_CONTENT) ) {
      if( !n2.isMatch(BranchNode.MATCH_CONTENT) ) {
        logUpdateOperation(n2);
        return n2.getContent();
      } else
        return cmerge( n1, n2, nodeMerger );
    } else {
       // n1 doesn't match content
      if( n2.isMatch(BranchNode.MATCH_CONTENT) ) {
        logUpdateOperation(n1);
        return n1.getContent();
      } else // Neither matches content => forced merge
        return cmerge( n1, n2, nodeMerger );
    }
  }

  /** Get partners for recursion in the merge dchild list */
  protected MergePair getRecursionPartners(MergePair mp) {
    BranchNode n1 = mp.getFirstNode(), n2 = mp.getSecondNode();
    if( n1 == null || n2 == null ) {
      // No pair, so just go on!
      return mp;
    } else {
      // We have a pair, do as the table in the thesis says:
      // n1    n2     Merge
      // ------------------
      // any   -      n1,-
      // -     any    -,n2
      // str   str    n1,n2
      // str   cont   -,n2
      // cont  str    n1,-
      // cont  cont   n1,n2 (FORCED)
      if( n1.isMatch(BranchNode.MATCH_CHILDREN) &&
          n2.isMatch(BranchNode.MATCH_CHILDREN) )
        return mp;
      else if( n1.isMatch(BranchNode.MATCH_CHILDREN) &&
               n2.isMatch(BranchNode.MATCH_CONTENT) )
        return new MergePair(n2,null);
      else if( n1.isMatch(BranchNode.MATCH_CONTENT) &&
               n2.isMatch(BranchNode.MATCH_CHILDREN) )
        return new MergePair(n1,null);
      else // Both content matches --> forced merge
        return mp;
    }
  }

  /** Merge content of two nodes. */
  private XMLNode cmerge( BranchNode a, BranchNode b, XMLNode.Merger nodeMerger ) {
    boolean aUpdated = !matches( a, a.getBaseMatch() ),
            bUpdated = !matches( b, b.getBaseMatch() );
    if( aUpdated && bUpdated ) {
///        System.out.println(a.isLeftTree() + ": " + a.getContent().toString() );
///        System.out.println(b.isLeftTree() + ": " + b.getContent().toString() );
      if( !a.isLeftTree() ) {
        // Ensure a is always left; its convenient for external node mergers
        BranchNode tmp=a; a=b; b=tmp;
      }
      if( matches( a, b ) ) {
        clog.addNodeWarning(ConflictLog.UPDATE,
          "Node updated in both branches, but updates are equal",
          a.getBaseMatch(),a,b);
        logUpdateOperation(a);
        return a.getContent();
      } else {
        // if XMLElementNode try merging attributes; if XMLTextnode give up
///          /*System.err.println("EMERGE");
///           System.err.println("base="+a.getBaseMatch().getContent().toString());
///           System.err.println("a="+a.getContent().toString());
///           System.err.println("b="+b.getContent().toString());*/
        XMLNode merged = nodeMerger.merge(a.getBaseMatch().getContent(),
                                          a.getContent(),
                                          b.getContent() );
///           //System.err.println("m=" + (merged==null ? "CONFLICT" : merged.toString()));
        if( merged != null )
          return merged;
        // XMLTextNodes, or failed to merge element nodes => conflict
        // CONFLICTCODE here
        clog.addNodeConflict(ConflictLog.UPDATE,
          "Node updated in both branches, using branch 1",a.getBaseMatch(),a,b);
/// /*
///         System.out.println("CONFLICT; Node updated in both branches, picking first one:");
///         System.out.println(a.getContent().toString());
///         System.out.println(b.getContent().toString());
/// */
        logUpdateOperation(a);
        return a.getContent();
      }
    } else if ( bUpdated ) {
      logUpdateOperation(b);
      return b.getContent();
    } else if ( aUpdated ) {
      logUpdateOperation(a);
      return a.getContent();
    } else
      return a.getContent(); // none modified, a or b is ok
  }

  /** Merge content of two nodes. */
  public XMLNode merge( XMLNode baseNode, XMLNode aNode, XMLNode bNode ) {
    if( !(baseNode instanceof XMLElementNode) ||
        !(aNode instanceof XMLElementNode) ||
        !(bNode instanceof XMLElementNode) )
      return null; // Can't merge
    XMLElementNode baseN = (XMLElementNode) baseNode;
    XMLElementNode aN = (XMLElementNode) aNode;
    XMLElementNode bN = (XMLElementNode) bNode;

///     //System.err.println("!!! in mergeElementNodes");
    String tagName ="";
    if( baseN.getQName().equals(bN.getQName()))
      tagName = aN.getQName();
    else if( baseN.getQName().equals(aN.getQName()))
      tagName = bN.getQName();
    else
      return null;//CONFLICT: Both changed (possibly same, but let's be careful)
    Attributes base = baseN.getAttributes(), a = aN.getAttributes(),
                      b=bN.getAttributes();
    // Check deleted attribs
    Set deletia = new HashSet();
    for( int i=0;i<base.getLength();i++) {
      int ixa = a.getIndex(base.getQName(i));
      int ixb = b.getIndex(base.getQName(i));
      if((ixa == -1 && ixb!= -1 && !base.getValue(i).equals(b.getValue(ixb))) ||
          (ixb == -1 && ixa!= -1 && !base.getValue(i).equals(a.getValue(ixa))) )
        return null; // CONFLICTCODE: attrib deleted & changed
      if( ixa == -1 || ixb == -1 )
        deletia.add(base.getQName(i));
    }
    AttributesImpl merged = new AttributesImpl();
    // Build combined list (inserts from A + updated common in A & B)
    for( int i=0;i<a.getLength();i++) {
      String qName = a.getQName(i);
      String value = a.getValue(i);
      if( deletia.contains(qName) )
        continue; // was deleted
      int ixb = b.getIndex(qName);
      if( ixb == -1 )
        merged.addAttribute("","",qName,a.getType(i),value); // Insert
      else {
        String valueB = b.getValue(ixb);
        String valueBase = base.getValue(qName);
        if( valueB.equals(valueBase) )
          // A possibly updated
          merged.addAttribute("","",qName,a.getType(i),value);
        else if( value.equals(valueBase) )
          // B possibly updated
          merged.addAttribute("","",qName,b.getType(ixb),valueB);
        else
          // CONFLICT: Both changed (possibly same, but let's be careful)
          return null;
      }
    }
    // Insertions from b
    for( int i=0;i<b.getLength();i++) {
      String qName = b.getQName(i);
      if( deletia.contains(qName) || a.getIndex(qName) != -1)
        continue; // was deleted or already processed
      merged.addAttribute("","",qName,b.getType(i),b.getValue(i)); // Insert
    }
    return new XMLElementNode( tagName, merged );
  }

  /** Make merge list from the children of a node. */
  protected MergeList makeMergeList( BranchNode parent ) {
    MergeList ml = new MergeList(parent);
    if( parent.getBaseMatch() == null ) {
      // The parent is unmatched, treat all nodes as inserts/n:th copies
      ml.add( START );
      for( int i = 0;i<parent.getChildCount();i++)
        ml.addHangOn( parent.getChild(i) );
      ml.lockNeighborhood(0,1);
      ml.add( END );
      return ml;
    }
    Map baseMatches = new HashMap();
    // Next is always prevChildPos + 1, so the first should be 0 =>
    // init to -1, -2 means not found in base
    int prevChildPos = -1;
    int childPos = -1;
    ml.add( START );
    for( int i = 0;i<parent.getChildCount();i++) {
      BranchNode current = parent.getChild(i);
      BaseNode match = current.getBaseMatch();
      if( match == null ) {
        // It's an insert node
        ml.addHangOn( current );
        ml.lockNeighborhood(0,1);
      } else if( match.getParent() != parent.getBaseMatch() ) {
        // Copied from elsewhere
        ml.addHangOn( current );
        ml.lockNeighborhood(0,1);
      } else if ( baseMatches.containsKey( match ) ) {
        // current is the n:th copy of a node (n>1)
        ml.addHangOn( current );
        Integer firstPos = (Integer) baseMatches.get(match);
        if( firstPos != null ) {
          // Lock the first occurenece as well
          ml.lockNeighborhood(firstPos.intValue(),1,1);
          // Put null into hashtable, so we won't lock more than once
          // (it wouldn't hurt, but just to be nice)
          baseMatches.put(match,null);
        }
        ml.lockNeighborhood(0,1);
//$CUT
     /* NOT USED, POSSIBLY FOR LOCK-OF-SRC conflict detect } else if ( (current.isLeftTree() && match.getLeft().getMatchCount() > 1 ) ||
                  (!current.isLeftTree() && match.getRight().getMatchCount() > 1 )  ) { // Lock all copy sources!
          System.err.println(">>>>>>> LOCKING " + current.getContent().toString() );
          ml.add( current,  !matches( match, current) );
          ml.lockNeighborhood(1,1);
          */
//$CUT
      } else {
        // Found in base, check for moves
        ml.add( current );
        baseMatches.put( match, new Integer( ml.tailPos ) );
        childPos = match.getChildPos();
        childPos = childPos == -1 ? -2 : childPos; // Remember; not found = -2
        if( (prevChildPos + 1) != childPos ) {
          // Out of sequence, lock previous and this
          // e.g. -1 0 1 3 4 5 => 1 & 3 locked
          boolean moved = false;
          // Possibly out of sequence.. check of nodes between prev
          if( prevChildPos != -2 && childPos != -2 && prevChildPos < childPos ){
            // Not moved if every node between prevChildPos+1 and childPos-1
            // (ends included) is deleted
            // This code uses a simple for loop, instead of the fancy
            // in-sequence table described in the thesis. So far, tast enough in
            // the real world (tm)
            for(int j=0;!moved && j<parent.getChildCount();j++) {
              BaseNode aBase = parent.getChild(j).getBaseMatch();
              int basePos = aBase == null ? -1 : aBase.getChildPos();
              if( basePos != -1 && basePos > prevChildPos && basePos < childPos)
                moved = true;
            }
          } else
            moved = true;
          if( moved ) {
            ml.lockNeighborhood(1,0);
            ml.setMoved(true);
          } else
            ml.setMoved(false);
        }
        prevChildPos = childPos;
      } // end if found in base
    }
    ml.add( END );
    if( (prevChildPos + 1 )!= parent.getBaseMatch().getChildCount() )
      // Possible end out-of-seq; e.g. -1 0 1 2 4=e,
      // and 4 children in parent i.e. ix 3 was deleted
      ml.lockNeighborhood(1,0);
    return ml;
  }

  // Holds merge pairs
  class MergePair {
    BranchNode first,second;
    MergePair( BranchNode aFirst, BranchNode aSecond ) {
      first = aFirst;
      second = aSecond;
    }

    public BranchNode getFirstNode() {
      return first;
    }

    public BranchNode getSecondNode() {
      return second;
    }

  }

  // Merge pair list
  class MergePairList {
    Vector list = new Vector();
    public void append( BranchNode a, BranchNode b ) {
      list.add(new MergePair(a,b));
    }

    public int getPairCount() {
      return list.size();
    }

    public MergePair getPair(int ix){
      return (MergePair) list.elementAt(ix);
    }
//$CUT
    public void print() {
      for(int i=0;i<list.size();i++) {
        MergePair mp = (MergePair) list.elementAt(i);
        System.out.println("<"+(mp.first != null ? mp.first.getContent().toString() : "." )+","+
        (mp.second != null ? mp.second.getContent().toString() : "." ) +">");
      }
    }
//$CUT
  }

  /** Convert merge list to merge pair list. Also used as a fallback if
   *  node reordering fails in
   *  {@link #makeMergePairList(MergeList, MergeList)  mergePairList}.
   *  In that case, the hangons from <code>mlistB</code> are also
   *  used (i.e. the order is <code>mlistA</code> but any inserts
   *  copies and far moves in <code>mlistB</code> are included.
   *  @param mlistA merge list to convert
   *  @param mlistB merge list to take extra hangons from, null if none */
  protected MergePairList mergeListToPairList( MergeList mlistA,
                                             MergeList mlistB ) {
    // NOTE: We want to log all children of a non-structurally matched node as
    // copy/updates, That's why logHangonStructOps() is used throughout the func
    MergePairList merged = new MergePairList();
    for( int i=0;i<mlistA.getEntryCount()-1;i++) { // -1 due to end symbol
      MergeEntry me = mlistA.getEntry(i);
      if( i > 0) { // Don't append __START__
        merged.append(me.getNode(),
          me.getNode().getFirstPartner(BranchNode.MATCH_FULL));
        logHangonStructOps(me.getNode(),merged.getPairCount()-1);
      }
      for( int ih=0;ih<me.getHangonCount();ih++) {
        BranchNode hangon=me.getHangon(ih).getNode();
        merged.append(hangon,hangon.getFirstPartner(BranchNode.MATCH_FULL));
        logHangonStructOps(hangon,merged.getPairCount()-1);
      }
      if( mlistB != null ) {
        MergeEntry pair = mlistB.getEntry(mlistB.findPartner(me));
        if( pair != null && !checkHangonCombine(me,pair,mlistA,mlistB) ) {
          for(int ih=0;ih<pair.getHangonCount();ih++) {
            BranchNode hangon = pair.getHangon(ih).getNode();
            merged.append(hangon,hangon.getFirstPartner(BranchNode.MATCH_FULL));
            logHangonStructOps(hangon,merged.getPairCount()-1);
          }
        }
      }
    }
    return merged;
  }


  /** Determine and log operation on hangon. */
  protected void logHangonStructOps( BranchNode n, int childPos ) {
    if( !n.hasBaseMatch() )
      elog.insert(n,childPos);
    else if( (n.isLeftTree() && n.getBaseMatch().getLeft().getMatchCount() > 1 ) ||
            (!n.isLeftTree() && n.getBaseMatch().getRight().getMatchCount() > 1 ))
      elog.copy(n,childPos); // hangon with base match = copy
    else
      elog.move(n,childPos);
  }

  /** Determine and log operation on entry in merge list. */
  protected void logEntryStructOps(MergeEntry m1, MergeEntry m2, int childPos) {
    if( m1.moved )
      elog.move(m1.getNode(),childPos);
    else if( m2.moved )
      elog.move(m2.getNode(),childPos);
  }

  /** Log update operation */
  protected void logUpdateOperation( BranchNode n ) {
    elog.update(n);
  }

  /** Combine two merge lists into a merge pair list. The order of the merged
   *  children is decided here.
   */
  protected MergePairList makeMergePairList( MergeList mlistA, MergeList mlistB ) {
    MergePairList merged = new MergePairList();
    removeDeletedOrMoved( mlistA, mlistB );
    elog.checkPoint();
//$CUT
/*
    System.out.println("A list (after delormove):");
    mlistA.print();
    System.out.println("B list (after delormove):");
    mlistB.print();
*/
//$CUT
    // Now we should have exactly the same entries in mlistA and mlistB
    // quick check
    if( mlistA.getEntryCount() != mlistB.getEntryCount() )
        throw new RuntimeException(
              "ASSERTION FAILED: MergeList.merge(): lists different lengths!");

    int posA = 0, posB = 0;
    MergeEntry ea = mlistA.getEntry(posA), eb= mlistB.getEntry(posB);
    while(true) {
      // Dump hangons from ea.. (we do this first as __START__ may have hangons)
      for(int i=0;i<ea.getHangonCount();i++) {
        BranchNode na = ea.getHangon(i).getNode();
        merged.append(na,na.getFirstPartner(BranchNode.MATCH_FULL));
        logHangonStructOps(na,merged.getPairCount()-1);
      }
      // And then from eb..
      if( eb.getHangonCount() > 0 ) {
        // Append b hangons (unless they were equal to the hangons of ea)
        if( !checkHangonCombine(ea,eb,mlistA,mlistB) ) {
          for(int i=0;i<eb.getHangonCount();i++) {
            BranchNode nb = eb.getHangon(i).getNode();
            merged.append(nb,nb.getFirstPartner(BranchNode.MATCH_FULL));
            logHangonStructOps(nb,merged.getPairCount()-1);
          }
        }
      }
      // end hangon dump
      int nextA=-1,nextB=-1;
      // figure out the next one
      nextA = ea.locked &&
                mlistA.getEntry(posA+1).locked ? posA + 1 : -1; // -1 means free
      nextB = eb.locked && mlistB.getEntry(posB+1).locked ? posB + 1 : -1;
      if( nextA == -1 && nextB == -1 ) { // No locking, just let both go forward
        nextA = posA + 1;
        nextB = posB + 1;
      }
      // Handle free positions
      if( nextB == -1 )
        nextB = mlistB.findPartner(mlistA.getEntry(nextA));
      else if (nextA == -1 )
        nextA = mlistA.findPartner(mlistB.getEntry(nextB));
      else if (nextB != mlistB.findPartner(mlistA.getEntry(nextA))) {
        // CONFLICTCODE
        clog.addListConflict( ConflictLog.MOVE,
        "Conflicting moves inside child list, using the sequencing of branch 1",
          ea.getNode() != START ? ea.getNode().getBaseMatch() : null,
          ea.getNode() != START ? ea.getNode() : null,
          eb.getNode() != START ? eb.getNode() : null );
        elog.rewind(); // Remove all edit ops made by this list merge attempt
        // Fallback on mergeListToPairList.
        return mergeListToPairList(mlistA.getEntryParent().isLeftTree() ? mlistA : mlistB,
          mlistA.getEntryParent().isLeftTree() ? mlistB : mlistA);
      }
      posA = nextA;
      posB = nextB;
      ea = mlistA.getEntry(posA);
      eb = mlistB.getEntry(posB);
      // See if we're done
      if( ea.node == END || eb.node == END ) {
        if( ea.node != eb.node )
          throw new RuntimeException(
          "ASSERTION FAILED: Merge.mergeLists(). Both cursors not at end");
        break;
      }
      // pos is set up so that ea and eb are merge-partners
      merged.append(ea.getNode(),eb.getNode());
      logEntryStructOps(ea,eb,merged.getPairCount()-1);
    }
    // Success, commit all edits generated to the log!
    elog.commit();
    return merged;
  }

  /** Check the situation of hangons that need to be combined, and generate
   *  conflict log entries.
   *  @return true if hangons are equal.
   */
  protected boolean checkHangonCombine(MergeEntry ea, MergeEntry eb,
                   MergeList mla, MergeList mlb ) {
    boolean hangonsAreEqual = false;
    if( ea.getHangonCount() > 0 ) {
      // Check if the hangons match _exactly_ (no inserts, and exactly same
      // sequence of copies). Then we may include the hangons just once. This
      // resembles the case when content of two nodes has been updated the same
      // way... not a conflict, but maybe suspicious.
      // NOTE! We need to match the entire subtrees rooted at the hangon, that's
      // why treeMatches is used. Otherwise <p>Hello</p> and <p>World</p> would
      // be considered equal (since <p>=<p>)
      if( eb.getHangonCount() == ea.getHangonCount() ) {
        hangonsAreEqual = true;
        for(int i=0;hangonsAreEqual && i<ea.getHangonCount();i++)
          hangonsAreEqual = treeMatches( eb.getHangon(i).getNode(),
                                          ea.getHangon(i).getNode() );
      }
      // Both have hangons, CONFLICTCODE
      // for now, chain A and B hangons
      if( hangonsAreEqual )
        // How should we encode the inserts, i.e. tell which nodes were inserted
        clog.addListWarning(ConflictLog.INSERT,
          "Equal insertions/copies in both branches after the context nodes.",
          ea.getNode().getBaseMatch() != null ? ea.getNode().getBaseMatch() :
          eb.getNode().getBaseMatch(), ea.getNode() != START ? ea.getNode() :
          null , eb.getNode() != START ? eb.getNode() : null );
///        //System.out.println(); // as updated(or A if no update)-Other");
      else
        clog.addListWarning(ConflictLog.INSERT, "Insertions/copies in both " +
          "branches after the context nodes. Sequencing the insertions.",
          ea.getNode().getBaseMatch() != null ? ea.getNode().getBaseMatch() :
          eb.getNode().getBaseMatch(),ea.getNode() != START ? ea.getNode() :
          null, eb.getNode() != START ? eb.getNode() : null );
/// //            System.out.println("CONFLICTW; both nodes have hangons; sequencing them"); // as updated(or A if no update)-Other");
/// /*        System.out.println("First list:");
///       mlistA.print();
///       System.out.println("Second list:");
///       mlistB.print();*/
    }
    return hangonsAreEqual;
  }

  /** NOP operation code. */
  protected static final int NOP = 1;
  /** MOVE_I operation code. Move inside childlist. */
  protected static final int MOVE_I = 2;
  /** MOVE_F operation code. Move outside childlist. */
  protected static final int MOVE_F = 3;
  /** DELETE operation code. */
  protected static final int DELETE = 4;

  /** Get operation on node in merge list.
   *  @return the operation on the node; one of NOP, MOVE_I, MOVE_F, DELETE */
  protected int getOperation( BaseNode bn, MergeList ml ) {
    int mlPos = ml.matchInList(bn);
    if( mlPos == -1 ) {
      // Movef or delete
      MatchedNodes copiesInThisTree = null;
      if( ml.getEntryParent().isLeftTree() )
        copiesInThisTree = bn.getLeft();
      else
        copiesInThisTree = bn.getRight();
      if( copiesInThisTree.getMatches().isEmpty() )
        return DELETE;
      else
        return MOVE_F;
    } else {
      if( ml.getEntry(mlPos).isMoved() )
        return MOVE_I;
      else
        return NOP;
    }
  }

  /** Remove deleted or far moved nodes from merge lists. */
  private void removeDeletedOrMoved( MergeList mlistA, MergeList mlistB ) {
    BaseNode baseParent = mlistA.getEntryParent().getBaseMatch();
    for( int i=0;i<baseParent.getChildCount();i++) {
      BaseNode bn = baseParent.getChild(i);
      int op1 = getOperation( bn, mlistA ),
          op2 = getOperation( bn, mlistB );
      // Swap ops, so that op1 is always the smaller (to simplify the if clauses)
      if( op1 > op2 ) {
        int t=op1; op1=op2; op2=t;
        MergeList tl = mlistA; mlistA = mlistB; mlistB = tl;
      }
///      System.out.println( op1 + " " + op2 + ": " + bn.getContent().toString() );
      /*************************************************************
      * Table to implement; mlistA is for op1 and mlistB for op2
      * Op1     Op2
      * NOP     NOP     OK
      * NOP     MOVE_I  OK, internal moves are handled by merging of the lists
      * NOP     MOVE_F  Delete the node from mlistA
      * NOP     DELETE  Delete the node from mlistA
      * MOVE_I  MOVE_I  OK, internal moves are handled by merging of the lists
      * MOVE_I  MOVE_F  Conflicting moves
      * MOVE_I  DELETE  Conflict - node is deleted and moved
      * MOVE_F  MOVE_F  Possibly conflict, see the code below
      * MOVE_F  DELETE  Conflict - node is deleted and moved
      * DELETE  DELETE  OK
      ***************************************************************/
      if( (op1==NOP && op2==NOP ) ||
          (op1==NOP && op2==MOVE_I ) ||
          (op1==MOVE_I && op2==MOVE_I ) ||
          (op1==DELETE && op2==DELETE ) )
        continue; // All OK cases
      if( op1 == NOP && ( op2 == MOVE_F || op2 == DELETE ) ) {
        // Delete the node from mlistA
        int ix = mlistA.matchInList(bn);
        if( op2==DELETE && isDeletiaModified(mlistA.getEntry(ix).getNode(),
            mlistA) )
          // CONFLICTCODE
          clog.addListWarning( ConflictLog.DELETE, "Modifications in "+
          "deleted subtree.",bn,mlistA.getEntry(ix).getNode(),null);
///          System.out.println("CONFLICTW: Modifications in deleted subtree.");
        if( mlistA.getEntry(ix).getHangonCount() > 0 ) {
          // we need to move the hangons to the predecessor
          for( int ih = 0; ih < mlistA.getEntry(ix).getHangonCount(); ih++)
            mlistA.getEntry(ix-1).addHangon(mlistA.getEntry(ix).getHangon(ih));
        }
        int matchIx = mlistA.matchInList(bn);
        if( op2==DELETE )
          elog.delete(mlistA.getEntry(matchIx).getNode().getBaseMatch(),mlistB.getEntryParent());
        if( op2==DELETE && mlistA.getEntry(matchIx).locked ) {
          clog.addListConflict(ConflictLog.DELETE, "Moved or copied node " +
          "deleted. Moving on by allowing the delete.", bn,
          mlistA.getEntry(ix).getNode(),null);
///          //System.err.println("!!!!LOCKED/DEL CONFLICT "+mlistA.getEntry(ix).getNode().getContent().toString());
        }
        mlistA.removeEntryAt(matchIx);
      } else if( op1 == MOVE_I && op2== MOVE_F ) {
        // CONFLICTCODE
        BranchNode op1node = mlistA.getEntry( mlistA.matchInList(bn)).getNode();
        clog.addListConflict( ConflictLog.MOVE, "Node moved to different "+
        "locations - trying to recover by ignoring move inside childlist "+
        "(copies and inserts immediately following the node may have been "+
        "deleted)",bn,op1node,op1node.getFirstPartner(BranchNode.MATCH_FULL));
///        System.out.println("CONFLICT: Node moved to different locations - moving on by ignoring MOVE_I + hangons!");
        mlistA.removeEntryAt(mlistA.matchInList(bn));
      } else if( op1 == MOVE_I && op2== DELETE ) {
        // CONFLICTCODE
        clog.addListConflict(ConflictLog.MOVE,"Node moved and deleted - trying"+
        " to recover by deleting the node (copies and inserts immediately "+
        "following the node may also have been deleted)",bn,
        mlistA.getEntry( mlistA.matchInList(bn) ).getNode(), null );
///        System.out.println("CONFLICT: Node moved and deleted - moving on by deleting the node + hangons!. ");
        int matchIx = mlistA.matchInList(bn);
        elog.delete(mlistA.getEntry(matchIx).getNode().getBaseMatch(),
        mlistB.getEntryParent());
        mlistA.removeEntryAt(matchIx);
      } else if( op1 == MOVE_F && op2 == MOVE_F ) {
        if( isMovefMovefConflict( bn ) ) {
          // CONFLICTCODE
          clog.addListConflict( ConflictLog.MOVE,"The node was moved to "+
          "different locations. It will appear at each location.", bn,
          bn.getLeft().getFullMatch(),bn.getRight().getFullMatch() );
///          System.out.println("CONFLICT: Node is far-moved to two different locations. " +
///                "Implicit copies will occur in the output.");
        }
      } else if (op1 == MOVE_F && op2 == DELETE ) {
          // CONFLICTCODE here
          clog.addListConflict( ConflictLog.MOVE,"The node was moved and "+
          "deleted. Ignoring the deletion.", bn,bn.getLeft().getFullMatch(),
          bn.getRight().getFullMatch() );
///          System.out.println("CONFLICT: Node is far-moved and deleted. The far-moved copy will persist.");
      }
    }
  }

  // Check if the deletia rooted at n is modified w.r.t. base

  private boolean isDeletiaModified(BranchNode n, MergeList ml) {
    BaseNode m = n.getBaseMatch();
    if( m == null )
      return true; // the node was inserted => modified
    if( getOperation(m,ml) != NOP )
      // Notice that we check for move instantly, but updates only when
      // we know the node was deleted. This is because moves are
      // visible on the previous tree level compared to the updates
      return true; // the node has been moved
    if( n.getBaseMatchType() != BranchNode.MATCH_FULL )
      return true;
    // either structural or content modification (otherwise match would be full)
    // NOTE: By definition of a natural matching, at least one match is full!
    boolean deletedInOther = n.getPartners().getMatches().isEmpty();
    if( deletedInOther ) {
      if( !matches( n, m ) )
        return true; // The node is updated
      // Check children
      MergeList mlistN = makeMergeList(n);
      for( int i=0;i<n.getChildCount();i++) {
        if( isDeletiaModified( n.getChild(i), mlistN ) )
          return true;
      }
      return false; // got trough the children (recursively), no modifications
    } else
      // No modification here, and no recurse needed (the node has a structmatch
      // in the other tree)
      return false;
  }

  // Check a base node for a movef-movef conflict
  private boolean isMovefMovefConflict( BaseNode n ) {
    return _isMovefMovefConflict( n, n.getRight().getMatches(),
           n.getLeft().getMatches() ) || _isMovefMovefConflict( n,
           n.getLeft().getMatches(), n.getRight().getMatches() );
  }

  // Check if base node n is moved under matching parents. If so, that is good,
  // because the list merge will handle possible conflicts. If they are not
  // moved under matching parents we have a conflict. (which unresolved results
  // in two copies). Specifically the condition is that the parent of each
  // BranchNode bn is moved to (structurally or content) must structurally match
  // another BranchNode that has bn as a child.

  // Although called rarely, this code is really horribly slow --- O(n^3)?!
  // Some easy optimizations hsould be possible. Also note that it is not
  // included in the complexity analysis of the algorithm

  // Final note: the author suspects this code might have a bug or few!
  private boolean _isMovefMovefConflict( BaseNode n, Set matchesA,
                                          Set matchesB ) {
    for( Iterator i = matchesB.iterator(); i.hasNext(); ) {
      BranchNode bnA = (BranchNode) i.next();
      BranchNode bnAparent = bnA.getParent();
      if( (bnAparent.getBaseMatchType() & BranchNode.MATCH_CHILDREN) == 0)
        // here's a copy with no structural match on the other side => conflict
        return true;
      for( Iterator ip = bnAparent.getPartners().getMatches().iterator();
                                                              ip.hasNext(); ) {
        BranchNode bnBparent = (BranchNode) ip.next();
        boolean hasNasChild = false;
        for( int ic = 0; ic < bnBparent.getChildCount() && !hasNasChild;ic ++ )
          hasNasChild = matchesA.contains( bnBparent.getChild(ic) );
        if( hasNasChild && ( bnBparent.getBaseMatchType() &
                             BranchNode.MATCH_CHILDREN ) == 0 )
          return true; // here's a copy with no structural match on the other side => conflict
      }
    }
    return false;
  }

  // Container for hangons
  private class HangonEntry {
    BranchNode node = null;
    HangonEntry( BranchNode an ) {
      node = an;
    }

    HangonEntry() {
    }

    public BranchNode getNode() {
      return node;
    }

    public String toString() {
      return node.getContent().toString();
    }
  }

  class MergeEntry extends HangonEntry {

    Vector inserts = new Vector();
    boolean locked = false;
    private BranchNode mergePartner = null;
    private boolean moved = false;

    MergeEntry( BranchNode n) {
      super(n );
    }

    MergeEntry() {
    }

    public boolean isMoved() {
      return moved;
    }

    public void setMoved(boolean amoved ) {
      moved=amoved;
    }

    public void setMergePartner(BranchNode n) {
      mergePartner = n;
    }

    public BranchNode getMergePartner() {
      return mergePartner;
    }
//$CUT

    // node | child0 | inserts
    void print(int pos) {
      System.out.print(pos+": ");
      System.out.print(isMoved() ? 'm' : '-');
      System.out.print(locked ? '*' : '-');
      System.out.print(mergePartner!=null ? 'p' : '-');
      System.out.print(' ' + node.getContent().toString() + " |");
      if( node.getChildCount() > 0 )
        System.out.print(' ' + node.getChild(0).getContent().toString() + " | ");
      else
        System.out.print(" - | "); // No children
      System.out.println( inserts.toString() );
    }

//$CUT

    int getHangonCount() {
      return inserts.size();
    }

    HangonEntry getHangon( int ix ) {
      return (HangonEntry) inserts.elementAt(ix);
    }

    void addHangon( BranchNode n  ) {
      addHangon( new HangonEntry( n ) );
    }

    void addHangon( HangonEntry e ) {
      inserts.add( e );
    }

  }

  // Merge list start and end markers. Cleaner if they were in MergeList, but
  // Java doesn't allow statics in nested classes
  static BranchNode START = new BranchNode(new XMLTextNode("__START__"));
  static BranchNode END = new BranchNode(new XMLTextNode("__END__"));

  // TODO: START end END markers should be completely hidden if possible
  class MergeList {
    private Vector list = new Vector();
    // lokkup table: look up Entry index based on base partner
    private Map index = new HashMap();
    private int tailPos = -1; // current tail pos
    private BranchNode entryParent = null; // Common parent of all entries
    private MergeEntry currentEntry = null;

    public MergeList( BranchNode anEntryParent ) {
      entryParent = anEntryParent;
    }

    public BranchNode getEntryParent() {
      return entryParent;
    }

    public void add( MergeEntry n ) {
      tailPos++;
      ensureCapacity( tailPos + 1, false);
      if( list.elementAt(tailPos) != null )
        n.locked = ((MergeEntry) list.elementAt(tailPos)).locked;
      list.setElementAt(n,tailPos);
      index.put( n.node.getBaseMatch(), new Integer(tailPos));
      currentEntry = n;
    }

    void add( BranchNode n ) {
      add( new MergeEntry(n ) );
    }

    void addHangOn( BranchNode n ) {
      getEntry(tailPos).addHangon(n );
      currentEntry = null;
    }

    public void setMoved( boolean moved ) {
      currentEntry.setMoved( moved );
    }

    public int getEntryCount() {
      return tailPos + 1;
    }

    public MergeEntry getEntry( int ix ) {
      return (MergeEntry) list.elementAt(ix);
    }

    // OPTIMIZATION FIX: The method needs to rebuild the index-- this should be
    // be optimized away with e.g. lazy deletions and an aggregated index
    // rebuild. Or maybe we could make the index use pointers.
    public void removeEntryAt( int ix ) {
      list.removeElementAt(ix);
      tailPos--;
      index.clear();
      for( int i=0;i<getEntryCount();i++)
        index.put( getEntry(i).node.getBaseMatch(), new Integer(i));
    }

    public void lockNeighborhood(  int left, int right ) {
      lockNeighborhood( tailPos, left, right );
    }

    public void lockNeighborhood( int acurrentPos, int left, int right ) {
      ensureCapacity( acurrentPos + right + 1, true);
      for( int i = acurrentPos - left; i<=acurrentPos + right; i++)
        ((MergeEntry) list.elementAt(i)).locked = true;
    }

    public int findPartner( MergeEntry b ) {
      if( b.node == START )
        return 0;
      else if( b.node == END )
        return getEntryCount() - 1; // Assuming the other list is equally long
      return ((Integer) index.get( b.node.getBaseMatch() )).intValue();
    }

    public int matchInList( BaseNode n ) {
      Integer i = (Integer) index.get( n );
      if( i == null )
        return -1;
      else
        return i.intValue();
    }

    private void ensureCapacity( int size, boolean fill ) {
      for( int i = list.size(); i < size; i ++ )
        list.add( fill ? new MergeEntry() : null);
    }
//$CUT
    void print() {
      int pos = 0;
      MergeEntry me = null;
      do {
        me = (MergeEntry) list.elementAt(pos);
        me.print(pos);
        pos++;
      } while( me.node != END );
    }
//$CUT
  }

  //
  //
  // Utility functions
  //
  protected boolean matches( Node a, Node b ) {
    if( a== null || b==null)
      return false;
/// /*    if( a.getContent() == null )
///      throw new RuntimeException("NULL content?");*/
    return a.getContent().contentEquals(b.getContent());
  }

  /** Check if entire subtrees match exactly. */
  protected boolean treeMatches( Node a, Node b ) {
    if( !matches(a,b) )
      return false;
    if( a.getChildCount() != b.getChildCount() )
      return false;
    boolean matches = true;
    for( int i=0;i<a.getChildCount() && matches;i++)
      matches &= treeMatches(a.getChildAsNode(i),b.getChildAsNode(i));
    return matches;
  }

  private class SAXExternalizer implements XMLNode.Externalizer {

    private ContentHandler ch;

    public SAXExternalizer(ContentHandler ch) {
      this.ch=ch;
    }

    public void startNode(XMLNode n) throws SAXException {
      if( n instanceof XMLTextNode )
        ch.characters( ( (XMLTextNode) n).getText(), 0,
                      ( (XMLTextNode) n).getText().length);
      else {
        XMLElementNode e = (XMLElementNode) n;
        ch.startElement(e.getNamespaceURI(),
                        e.getLocalName(),e.getQName(),
                        e.getAttributes());

      }
    }

    public void endNode(XMLNode n) throws SAXException {
      if( n instanceof XMLTextNode )
        return;
      XMLElementNode e = (XMLElementNode) n;
      ch.endElement(e.getNamespaceURI(),
                    e.getLocalName(),
                    e.getQName());

    }
  }

/*
  public interface Node {

    public Node getParentAsNode();
    public int getChildCount();

    public Node getChildAsNode(int ix);

    public boolean hasLeftSibling();

    public boolean hasRightSibling();
    public Node getLeftSibling();

    public Node getRightSibling();

    public XMLNode getContent( );
    public int getChildPos();

  }

  public interface BaseNode extends Node {

  }

  public interface BranchNode extends Node {
    // Match types
    // NOTE: If you change these, remember to change MATCHING_ATT_TYPES also!
    public static final int MATCH_FULL = 3;
    public static final int MATCH_CONTENT = 1;
    public static final int MATCH_CHILDREN = 2;

    public BranchNode getChild( int ix );

    public BranchNode getParent();

    public void setPartners(MatchedNodes p);

    public MatchedNodes getPartners();

    public void setBaseMatch(BaseNode p, int amatchType);

    public void setMatchType( int amatchType );

    public void delBaseMatch();

    public int getBaseMatchType();

    public boolean hasBaseMatch();

    public BaseNode getBaseMatch();

    public boolean isLeftTree();

    public boolean isMatch( int type);

    public BranchNode getFirstPartner( int typeFlags );

  }*/
}

