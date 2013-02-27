// $Id: MatchedNodes.java,v 1.11 2006-02-06 11:35:51 ctl Exp $ D
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

import java.util.Set;
import java.util.HashSet;
import java.util.Iterator;

/** Container for a set of nodes, matched to the node owning the container. */

public class MatchedNodes {

  private BaseNode owner=null;
  private Set matches=new HashSet();

  /** Create a new container of matched nodes. All nodes in the container are
   *  matched to the owner node.
   *  @param aowner Owner of the container.
   */
  public MatchedNodes(BaseNode aowner) {
    owner = aowner;
  }

  public void addMatch(BranchNode n) {
    matches.add(n);
  }

  public void delMatch(BranchNode n) {
    matches.remove(n);
  }

  public void clearMatches() {
    matches.clear();
  }

  public Set getMatches() {
    return matches;
  }

  public int getMatchCount() {
    return matches.size();
  }

  /** Get the first node that is fully matched to the owner. */

  public BranchNode getFullMatch() {
    for( Iterator i=matches.iterator();i.hasNext();) {
      BranchNode fmatch = (BranchNode) i.next();
      if( fmatch.isMatch(BranchNode.MATCH_FULL))
        return fmatch;
    }
    return null;
  }
//$CUT

  public void debug( java.io.PrintWriter pw, int indent ) {
    String ind = "                                                   ".substring(0,indent+1);
    java.util.Iterator i = matches.iterator();
    while( i.hasNext() ) {
      XMLNode n = ((Node) i.next()).content;
      pw.println(ind+ (n  == null ? "(null)" : n.toString() ) );
    }
  }
//$CUT
}
