// $Id: Matching.java,v 1.24 2004-03-09 09:41:54 ctl Exp $
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

public interface Matching {

  public void buildMatching( BaseNode base, BranchNode branch );
  public void getAreaStopNodes( Vector stopNodes, BranchNode n );
  public BaseNode getBaseRoot();
  public BranchNode getBranchRoot();

  public NodeFactory getBaseNodeFactory();
  public NodeFactory getBranchNodeFactory();
}
