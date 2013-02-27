// $Id: XMLInlinedMatching.java,v 1.2 2004-03-09 09:41:54 ctl Exp $ D
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

import org.xml.sax.helpers.AttributesImpl;
import java.util.Vector;

/** Use when matching is explicitly encoded in the XML file */
public class XMLInlinedMatching implements Matching {

  protected BaseNode baseRoot = null;
  protected BranchNode branchRoot = null;

// Factory for BaseNode:s
  private static NodeFactory baseNodeFactory = new NodeFactory() {
    public Node makeNode(XMLNode content) {
      return new BaseNode(content);
    }
  };

// Factory for BranchNode:s
  private static NodeFactory branchNodeFactory = new NodeFactory() {
    public Node makeNode(XMLNode content) {
      return new BranchNode(content);
    }
  };

  public XMLInlinedMatching() {
    // Does nothing, only called from TriMatching constructor
  }

  /** Build matching between two trees.
   *  @param abase  Root of the base tree
   *  @param abranch Root of the branch tree
   */
  public XMLInlinedMatching(BaseNode abase, BranchNode abranch) {
    buildMatching(abase, abranch);
  }

  public void buildMatching(BaseNode base, BranchNode branch) {
    // Artificial roots always matched (otherwise BranchNode.isLeft() may fail!)
    baseRoot = base;
    branchRoot = branch;
    doBuildMatching(branch.getChild(0));
    branchRoot.setBaseMatch(baseRoot,BranchNode.MATCH_FULL);
    baseRoot.debugTree(new java.io.PrintWriter(System.out),0);
  }

  protected void doBuildMatching( BranchNode n ) {
    XMLNode c = n.getContent();
    if (c instanceof XMLTextNode) {
      throw new RuntimeException("Malformed pre-matched XML document");
     }  else {
       XMLElementNode ce = (XMLElementNode) c;
       String mTypeStr = ce.getAttributes().getValue(MATCHING_ATT_TYPE);
       String path = null;
       int type = -1;
       for( int i=0;i<MATCHING_ATT_TYPES.length && type==-1;i++) {
         if( MATCHING_ATT_TYPES[i].equals(mTypeStr) )
           type = i;
       }
       if( type == -1 )
         throw new RuntimeException("Malformed pre-matched XML document: invalid type "+mTypeStr);
       if( MATCHING_TAG_TEXT.equals(ce.getQName()) ) {
         // Remove text tag inserted by matching dumper
         path = ce.getAttributes().getValue(MATCHING_ATT_MATCH);

         BranchNode parent = n.getParent();
         if( n.getChildCount() != 1)
           throw new RuntimeException("Asset failed");
         n.setContent(n.getChild(0).getContent());
         n.removeChildren(); // Otherwise we'll recurse into the text node
       } else {
         path = ce.getAttributes().getValue(MATCHING_ATT_MATCH);
         AttributesImpl atts = (AttributesImpl) ce.getAttributes();
         atts.removeAttribute(atts.getIndex(MATCHING_ATT_TYPE));
         if( type > 0 )
           atts.removeAttribute(atts.getIndex(MATCHING_ATT_MATCH));
          ce.rehash(); // Since we mucked around with the attributes
       }

       if(type>0) {
         BaseNode baseMatch = (BaseNode) PathTracker.followPath(baseRoot,
             path);
         n.setBaseMatch(baseMatch, type);
         baseMatch.getLeft().addMatch(n);
       }

       for (int i = 0; i < n.getChildCount(); i++)
         doBuildMatching(n.getChild(i));
     }
  }

  public void getAreaStopNodes( Vector stopNodes, BranchNode n ) {
    throw new java.lang.NoSuchMethodError("Not implemented");
  }

  public BaseNode getBaseRoot() {
    return baseRoot;
  }

  public BranchNode getBranchRoot() {
    return branchRoot;
  }


  public NodeFactory getBaseNodeFactory() {
    return baseNodeFactory;
  }

  public NodeFactory getBranchNodeFactory() {
    return branchNodeFactory;
  }

  public static void dumpXMLWithMatchings(BranchNode root, XMLPrinter p) {
    dumpXMLWithMatchings(root,p, Integer.MAX_VALUE);
  }

  public static void dumpXMLWithMatchings(BranchNode root, XMLPrinter p, int levels) {
    p.startDocument();
    doDumpXMLWithMatchings(root, p, levels);
    p.endDocument();
  }

  protected static void doDumpXMLWithMatchings( BranchNode n, XMLPrinter p, int levels) {
    XMLNode c = n.getContent();
    AttributesImpl atts = new AttributesImpl();
    if (c instanceof XMLTextNode) {
      // Text node must be esacped with text tag
      setMatchAttribs(n,atts);
      XMLTextNode ct = (XMLTextNode) c;
      p.startElement("", "", MATCHING_TAG_TEXT, atts);
      p.characters(ct.getText(), 0, ct.getText().length);
      p.endElement("", "", MATCHING_TAG_TEXT);
    }
    else {
      XMLElementNode ce = (XMLElementNode) c;
      if (ce.getAttributes().getLength() > 0) // If clause is bug workaround for

        // broken SAX2 setAttributes
        atts.setAttributes(ce.getAttributes());
      setMatchAttribs(n,atts);
      p.startElement(ce.getNamespaceURI(), ce.getLocalName(), ce.getQName(),
                     atts);
      for (int i = 0; levels > 0 && i < n.getChildCount(); i++)
        doDumpXMLWithMatchings(n.getChild(i), p, levels - 1);
      p.endElement(ce.getNamespaceURI(), ce.getLocalName(), ce.getQName());
    }
  }

  private static final String MATCHING_NS = "m:";
  private static final String MATCHING_ATT_MATCH = MATCHING_NS + "match";
  private static final String MATCHING_ATT_TYPE = MATCHING_NS + "type";
  private static final String MATCHING_TAG_TEXT = MATCHING_NS + "text";
  private static final String[] MATCHING_ATT_TYPES = {
      "n", "c", "s", "f"};

  private static void setMatchAttribs(BranchNode n, AttributesImpl a) {
    if (n.hasBaseMatch()) {
      a.addAttribute("", "", MATCHING_ATT_MATCH, "CDATA",
                     PathTracker.getPathString(n.getBaseMatch()));
      a.addAttribute("", "", MATCHING_ATT_TYPE, "CDATA",
                     MATCHING_ATT_TYPES[n.getBaseMatchType()]);
    } else {
      a.addAttribute("", "", MATCHING_ATT_TYPE, "CDATA",
                     MATCHING_ATT_TYPES[0]);
    }
  }

}
