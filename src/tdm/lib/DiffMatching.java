// $Id: DiffMatching.java,v 1.7 2003-01-09 14:15:26 ctl Exp $ D
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
import java.util.Iterator;

/** Tree matching suitable for producing diffs. Compared to the standard
 *  matching, DiffMatching does not match nodes with similar content (there's
 *  no point to do that), and tries to find matches that form uninterrupted runs
 *  of src attributes => more efficient encoding of the diff.
 */

public class DiffMatching extends HeuristicMatching {

  public DiffMatching() {}
  /** Construct a matching between a base and branch tree. */
  public DiffMatching(BaseNode abase, BranchNode abranch ) {
    buildMatching( abase, abranch );
  }

  public void buildMatching( BaseNode base, BranchNode branch ) {
    baseRoot = base;
    branchRoot = branch;
    matchSubtrees( base, branch );
  }

  // We never match fuzzy when diffing
  protected boolean dfsTryFuzzyMatch( Node a, Node b) {
    return false;
  }

  // Only find exact candidates
  protected Vector findCandidates( BaseNode tree, BranchNode key ) {
    Vector candidates = new Vector();
    findExactMatches( tree, key, candidates );
    return candidates;
  }

  protected CandidateEntry getBestCandidate(  BranchNode branch,
                                        Vector bestCandidates ) {
    // Try to return a node who is next to the previously matched node
    // (sequencing of src nodes!)
    if( bestCandidates.size() > 1 ) {
      for( Iterator i = bestCandidates.iterator();i.hasNext();) {
        CandidateEntry ce = (CandidateEntry) i.next();
        BranchNode left = (BranchNode) branch.getLeftSibling();
        BaseNode cand = ce.candidate;
        if( left != null && left.hasBaseMatch() && left.getBaseMatch() ==
            cand.getLeftSibling() )
          return ce;
      }
    }
    if( bestCandidates.isEmpty() )
      return null;
    else
      return (CandidateEntry) bestCandidates.elementAt(0);
  }

  // DiffMatching has public getAreaStopNodes
  public void getAreaStopNodes( Vector stopNodes, BranchNode n ) {
    super.getAreaStopNodes(stopNodes,n);
  }

  public BranchNode getBranchRoot() {
    return branchRoot;
  }
}
