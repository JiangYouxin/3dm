// $Id: BranchNode.java,v 1.16 2006-02-06 11:37:04 ctl Exp $ D
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

import java.util.Iterator;

/**
 *  Node in a branch tree. In addition to the
 *  functionality provided by the node class, BranchNode adds matchings to a
 *  node in the base tree.
 */

public class BranchNode extends Node {

  // Match types
  // NOTE: If you change these, remember to change MATCHING_ATT_TYPES also!
  public static final int MATCH_FULL = 3;
  public static final int MATCH_CONTENT = 1;
  public static final int MATCH_CHILDREN = 2;

  protected MatchedNodes partners = null;
  protected BaseNode baseMatch = null;
  protected int matchType = 0;

  protected BranchNode() {
    super();
  }

  public BranchNode( XMLNode aContent ) {
    super();
    content = aContent;
  }

  public BranchNode getChild( int ix ) {
    return (BranchNode) children.elementAt(ix);
  }

  public BranchNode getParent() {
    return (BranchNode) parent;
  }

  public void setPartners(MatchedNodes p) {
    partners = p;
  }

  public MatchedNodes getPartners() {
    return partners;
  }

  public void setBaseMatch(BaseNode p, int amatchType) {
    if( amatchType < MATCH_CONTENT || amatchType > MATCH_FULL )
      throw new IllegalArgumentException();
    baseMatch = p;
    matchType = amatchType;
  }

  public void setMatchType( int amatchType ) {
    if( amatchType < MATCH_CONTENT || amatchType > MATCH_FULL )
      throw new IllegalArgumentException();
    matchType = amatchType;
  }

  public void delBaseMatch() {
    baseMatch = null;
    matchType = 0;
  }

  public int getBaseMatchType() {
    return matchType;
  }

  public boolean hasBaseMatch() {
    return baseMatch != null;
  }

  public BaseNode getBaseMatch() {
    return baseMatch;
  }

  /** Tells if this node is in the left tree. */
  public boolean isLeftTree() {
    //  Assumes that at least the root is matched.
    if( baseMatch != null )
      return baseMatch.getLeft().getMatches().contains(this);
    else
      return getParent().isLeftTree();
  }

  public boolean isMatch( int type) {
    return ((matchType & type) != 0);
  }


  /** Find a node partner of given type. */

  public BranchNode getFirstPartner( int typeFlags ) {
    // Remeber to check both steps! The canidate's match type is only from base
    // if A should match B structurally we need
    // A---------Base---------B
    //    struct      struct
    if( ( matchType & typeFlags) == 0 )
      return null;
    MatchedNodes m= getPartners();
    if( m == null )
      return null;
    for( Iterator i = m.getMatches().iterator();i.hasNext();) {
      BranchNode candidate = (BranchNode) i.next();
      if((candidate.matchType & typeFlags) != 0)
        return candidate;
    }
    return null;
  }
//$CUT
  public void debug( java.io.PrintWriter pw, int indent ) {
    super.debug(pw, indent);
    String ind = "                                                           ".substring(0,indent+1);
    pw.println(ind+(partners != null ? "Partners are:" : "(no partners)"));
    if(partners != null ) {
      partners.debug(pw,indent+1);
      pw.println(ind+"---");
    }
  }
//$CUT
}
