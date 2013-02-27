// $Id: TriMatching.java,v 1.16 2003-01-30 09:24:59 ctl Exp $ D
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

/** Matching between a base and two branch trees. */
public class TriMatching  {

  private BranchNode leftRoot = null;
  private BranchNode rightRoot = null;
  private BaseNode baseRoot = null;

  public TriMatching( BranchNode left, BaseNode base, BranchNode right) {
    this(left,base,right,HeuristicMatching.class,HeuristicMatching.class);
  }
  /** Create matching */
  public TriMatching( BranchNode left, BaseNode base, BranchNode right,
                      Class leftMatchType, Class rightMatchType  ) {
    Matching m = null;
    try {
      m = (Matching) rightMatchType.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Fatal Error instantiating matching class "+rightMatchType.getName());
    }
    m.buildMatching( base, right );
    leftRoot =left;
    rightRoot = right;
    baseRoot = base;
    swapLeftRight( base );
    try {
      m = (Matching) leftMatchType.newInstance();
    } catch (Exception e) {
      throw new RuntimeException("Fatal Error instantiating matching class "+leftMatchType.getName());
    }

    m.buildMatching( base, left );
    setPartners( left, false );
    setPartners( right, true );
  }

  // Swap left and right matching fields in base nodes. The superclass
  // always fills in left matchings, so we need to call this when making
  // the right (no pun intended) matchings
  protected void swapLeftRight( BaseNode base ) {
    base.swapLeftRightMatchings();
    for( int i=0;i<base.getChildCount();i++)
      swapLeftRight(base.getChild(i));
  }

  // Set partner fields of branch nodes
  protected void setPartners( BranchNode n, boolean partnerInLeft ) {
    BaseNode baseMatch = n.getBaseMatch();
    if( baseMatch != null ) {
      if( partnerInLeft )
        n.setPartners(baseMatch.getLeft());
      else
        n.setPartners(baseMatch.getRight());
    } else
        n.setPartners(null);
    for( int i=0;i<n.getChildCount();i++)
      setPartners(n.getChild(i),partnerInLeft);
  }

  public BranchNode getLeftRoot() {
    return leftRoot;
  }

  public BranchNode getRightRoot() {
    return rightRoot;
  }

  public BaseNode getBaseRoot() {
    return baseRoot;
  }

}