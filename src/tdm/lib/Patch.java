// $Id: Patch.java,v 1.14 2006-02-06 10:04:46 ctl Exp $ D
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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.AttributesImpl;

/** Patching algorithm.
 *  See the {@link patch(BaseNode, BranchNode, org.xml.sax.ContentHandler)
 *  patch} method for usage.
 */

public class Patch {

  public static final Node DONT_EXPAND_COPY = new BaseNode(null);

  public Patch() {
  }

  /** Patch a tree.
   *  @param base Tree to patch
   *  @param diff Parse tree of XML diff file, as produced by the Diff class
   *  @param ch Content handler that the patched tree is output to
   */
  public void patch( Node base, BranchNode diff, ContentHandler ch )
      throws ParseException, SAXException{
    patch( base, diff, ch, new Diff.BFSIndex(base));
  }

  public void patch( Node base, BranchNode diff, ContentHandler ch, IdIndex index )
    throws ParseException, SAXException {
    BranchNode patched = patch( base, diff, index );
    ch.startDocument();
    dumpTree( patched.getChild(0), ch );
    ch.endDocument();
  }

  private void dumpTree( BranchNode n, ContentHandler ch ) throws SAXException {
    if( n.getContent() instanceof XMLTextNode ) {
      char [] text = ((XMLTextNode) n.getContent()).getText();
      ch.characters(text,0,text.length);
    } else {
      XMLElementNode en = (XMLElementNode) n.getContent();
      ch.startElement("","",en.getQName(),en.getAttributes());
      for( int i=0;i<n.getChildCount();i++)
        dumpTree(n.getChild(i),ch );
      ch.endElement("","",en.getQName());
    }
  }

  protected BranchNode patch( Node base, BranchNode diff, IdIndex index ) throws
          ParseException  {
    BranchNode patch = new BranchNode( new XMLElementNode("$DUMMY$",
      new org.xml.sax.helpers.AttributesImpl() ) );
    // Getchild(0) to skip diff tag
    // Note that we need an $DUMMY$ node to which $ROOT$ is attached as an only child
    // we cant use <root> as start of copy, since root elem may be changed!
    diff=diff.getChild(0); // Jump over $ROOT$ (fakeroot)... need to kill this eventually
    XMLNode dc = diff.getContent();
    if( !(dc instanceof XMLElementNode) ||
        !Diff.DIFF_ROOT_TAG.equals(((XMLElementNode) dc).getQName()) )
        throw new ParseException("Invalid root tag for diff");
    String op = ((XMLElementNode) dc).getAttributes().getValue(Diff.DIFF_ROOTOP_ATTR);
    if( op == null || op.length() == 0 )
      // Default op, which is copy
      copy( patch,diff,base, index.getRootId(), index);
    else if( Diff.DIFF_ROOTOP_INS.equals(op) )
      insert( patch, diff, index );
    else
      throw new ParseException("Invalid rootop for diff: "+op);
    return patch.getChild(0);
  }

  // diff => a command or just some nodes to insert
  // patch = the parent node, under which the subtree produced by the command
  // shall be inserted

  protected void insert( BranchNode patch, BranchNode diff, IdIndex index) throws
    ParseException {
    XMLNode cmdcontent = diff.getContent();
    if( cmdcontent instanceof XMLTextNode ||
      !Diff.DEFAULT_CONFIG.RESERVED.contains(((XMLElementNode) cmdcontent).getQName())) {
      // Simple insert operation
      BranchNode node = new BranchNode( cmdcontent );
      patch.addChild(node);
      for( int i=0;i<diff.getChildCount();i++)
        insert(node,diff.getChild(i),index); // Recurse to next level
    } else {
      // Other ops..
      XMLElementNode ce = (XMLElementNode) cmdcontent;
      if( ce.getQName().equals(Diff.DIFF_ESC_TAG) ||
          ce.getQName().equals(Diff.DIFF_INS_TAG) ) {
        // BUGFIX 030115
	  //if( diff.getChildCount() == 0)
          //throw new ParseException("DIFFSYNTAX: insert/esc has no subtree " +
          //                          ce.toString() );
        //ENDBUGFIX
        for( int i=0;i<diff.getChildCount();i++) // SHORTINS
          insert( patch, diff.getChild(i), index );
      } else {
        // Copy operation
        Node srcRoot = null;
        String src = null;
        try {
          src = ce.getAttributes().getValue(Diff.DIFF_CPYSRC_ATTR);
          srcRoot = index.lookup(src) ;
        } catch ( Exception e ) {
            throw new ParseException(
              "DIFFSYNTAX: Invalid parameters in command " + ce.toString() );
        }
        copy( patch, diff, srcRoot,src, index );
      } // Copyop
    }
  }

  // Called on copy tag
  protected void copy( BranchNode patch, BranchNode diff, Node srcRoot,Object src,
                      IdIndex index )
                      throws ParseException {
    // Gather the stopnodes for the copy
    Vector dstNodes = new Vector();
    Map stopNodes = new HashMap();
    long nsrc=-1;
    long run = 1;
    try {
      String runS = ((XMLElementNode) diff.getContent()).getAttributes().
          getValue(Diff.DIFF_CPYRUN_ATTR);
      if (runS != null)
        run = Long.parseLong(runS);
      if( run > 1 )
        nsrc = Long.parseLong(src.toString());
    } catch ( Exception e ) {
      throw new ParseException("DIFFSYNTAX: Invalid run count/non-number src for copy tag " +
                               diff.toString() );
    }

    for( int i = 0; i < diff.getChildCount(); i++ ) {
      XMLNode content = diff.getChild(i).getContent();
      Node stopNode = null;
      // Sanity check
      if( content instanceof XMLTextNode || (
        !((XMLElementNode) content).getQName().equals(Diff.DIFF_COPY_TAG) &&
        !((XMLElementNode) content).getQName().equals(Diff.DIFF_INS_TAG) ) )
        throw new ParseException("DIFFSYNTAX: Only copy or insert commands may"+
                                 " appear below a copy command");
      XMLElementNode stopCommand = (XMLElementNode) content;
      try {
        stopNode = index.lookup(
                                stopCommand.getAttributes().
                                getValue(Diff.DIFF_CPYDST_ATTR));
      } catch ( Exception e ) {
        throw new ParseException("DIFFSYNTAX: Invalid parameters in command " +
                                 stopNode.toString() );
      }
      dstNodes.add( stopNode );
      if( !stopNodes.containsKey(stopNode) )
        stopNodes.put(stopNode,null);
    }
    // Run copy. stopNode values are at the same time filled in to point to the
    // created nodes

    for( long iRun=1;iRun<run;iRun++) {
      dfsCopy(patch,srcRoot,null,src);
      src = String.valueOf(nsrc+iRun);
      srcRoot = index.lookup(src);
    }
    dfsCopy( patch, srcRoot , stopNodes, src );
    // Recurse for each diff child
    for( int i = 0; i < diff.getChildCount(); i++ ) {
      // DEBUG
      /*BranchNode _n = (BranchNode) stopNodes.get( dstNodes.elementAt(i) );
      if(  _n == null  ) {
        BaseNode _s = (BaseNode) dstNodes.elementAt(i);
        System.err.println("Internal failure: Stop node not created");
        System.err.println("src path=" + PathTracker.getPathString(srcRoot) );
        System.err.println("stop path=" + PathTracker.getPathString(_s ));
      }*/
      //ENDDEBUG
      insert( (BranchNode) stopNodes.get( dstNodes.elementAt(i) ),
              diff.getChild(i), index);
    }
  }

  protected void dfsCopy( BranchNode dst, Node src, Map stopNodes, Object srcId ) {
    if( src == DONT_EXPAND_COPY ) {
      if( stopNodes != null && stopNodes.size() > 0 )
        throw new IllegalStateException("Can't use noexpand with truncated subtrees");
      AttributesImpl atts = new AttributesImpl();
      atts.addAttribute("","",Diff.DIFF_CPYSRC_ATTR,"CDATA",srcId.toString());
      BranchNode node = new BranchNode(
        new XMLElementNode( Diff.DIFF_COPY_TAG, atts ));
      dst.addChild(node);
    } else
      dfsCopyTree(dst,src,stopNodes);
  }

  protected void dfsCopyTree( BranchNode dst, Node src, Map stopNodes ) {
    BranchNode copied = new BranchNode( src.getContent() );
    dst.addChild( copied );
    if( stopNodes != null && stopNodes.containsKey(src) ) {
      stopNodes.put(src,copied);
      return; // We're done in this branch
    }
    for( int i =0;i<src.getChildCount();i++)
      dfsCopyTree( copied, src.getChildAsNode(i), stopNodes );
  }

}
