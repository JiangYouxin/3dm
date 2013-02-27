// $Id: Diff.java,v 1.21 2006-02-06 11:20:28 ctl Exp $ D
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

import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import java.util.Vector;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Iterator;
import java.util.List;
import java.io.IOException;

/** Produces the diff between two naturally matched trees.
 *  Collapsing multiple copy-ops using the run attribute is not implemented in
 *  this version. <b>NOTE: Format of diff root tag changed in CVS rev
 *  1.16</b> Current code reads old diffs, but new diffs are not
 *  necesarily readable by old code!
 */

public class Diff extends DiffAlgorithm {

  // Instance vars
  private Configuration cf = DEFAULT_CONFIG;
  private Object branchRoot;

  private ContentHandler ch=null; // NOTE: Can't run multiple diffs concurrently!
  private NodeIndex index = null;
  private Matching m = null;
  private static final Attributes EMPTY_ATTS = new AttributesImpl();

  /*private PRIVATIZE LATER*/ static final Configuration DEFAULT_CONFIG = new Configuration();

//  static final Set RESERVED;

// These need to be removed when there are no longer any external dependencies!
  static final String DIFF_NS = DEFAULT_CONFIG.DIFF_NS;
  public static final String DIFF_COPY_TAG = DIFF_NS+"copy";
  public static final String DIFF_INS_TAG = DIFF_NS+"insert";
  public static final String DIFF_ESC_TAG = DIFF_NS+"esc";
  public static final String DIFF_ROOT_TAG = "diff";

  public static final String DIFF_CPYSRC_ATTR = "src";
  public static final String DIFF_CPYDST_ATTR = "dst";
  public static final String DIFF_CPYRUN_ATTR = "run";
  public static final String DIFF_ROOTOP_ATTR = "op";

  public static final String DIFF_ROOTOP_INS = "insert";
//endkill

  /** Construct a diff operating on the matched trees passed to the constructor.
   *  Note that the matching contains pointers to the base and new trees.
   *  @param am Matching between trees to diff
   */
  public Diff(Matching am) {
    this( am, new BFSIndex(am.getBaseRoot()));
  }

  public Diff(Matching am, NodeIndex aIx) {
    index = aIx;
    m = am;
//    se = new TdmSequence(index);
    branchRoot = am.getBranchRoot();
  }

  public Diff(Configuration aCf,
              Object aBranchRoot) {
   cf = aCf;
   branchRoot = aBranchRoot;
 }

/*  public Diff(Matching am, NodeIndex aIndex) {
    m=am;
    index = aIndex;
  }*/

  /** Encode the diff between the trees passed to the constructor.
   *  @param ch Output encoder for the diff
   */
  public void diff(ContentHandler ch) throws SAXException {
    this.ch = ch;
    try {
      diff(branchRoot);
    } catch (IOException x) {
      throw new SAXException(x);
    }
  }


  // BFS Enumeration of nodes. Useful beacuse adjacent nodes have subsequent ids
  // => diff can use the "run" attribute more often

  static class BFSIndex implements NodeIndex, IdIndex {

    protected Map nodeToNumber = new HashMap();
    protected Map numberToNode = new HashMap();
    private Object rootId = null;

    public BFSIndex( Node root ) {
      int id = 0;
      LinkedList queue = new LinkedList();
      queue.add(root);
      while( !queue.isEmpty() ) {
        Node n = (Node) queue.removeFirst();
        nodeToNumber.put(n,new Long(id));
        numberToNode.put(String.valueOf(id),n);
        for( int i=0;i<n.getChildCount();i++)
          queue.add(n.getChildAsNode(i));
        id++;
      }
      rootId=getId(root).toString();
    }

    public Object getId(Object n) {
      return nodeToNumber.get(n);
    }

    public Node lookup(Object id) {
      return (Node) numberToNode.get(id.toString());
    }

    public Object getRootId() {
      return rootId;
    }

  }

  public static class Configuration {

    protected Set RESERVED=null;

    protected String DIFF_NS =""; //diff:"; //XDiFf-"; // should be solved using proper namespaces!
    protected String DIFF_COPY_TAG = DIFF_NS+"copy";
    protected String DIFF_INS_TAG = DIFF_NS+"insert";
    protected String DIFF_ESC_TAG = DIFF_NS+"esc";
    protected String DIFF_ROOT_TAG = "diff";

    protected String DIFF_CPYSRC_ATTR = "src";
    protected String DIFF_CPYDST_ATTR = "dst";
    protected String DIFF_CPYRUN_ATTR = "run";
    protected String DIFF_ROOTOP_ATTR = "op";

    protected final String DIFF_ROOTOP_INS = "insert";

    protected boolean useQName = true;

    public Configuration() {
      init();
    }

    public Configuration( String aNameSpace,
                          String aCopyTag,
                          String aInsTag,
                          String aEscTag,
                          String aRootTag,
                          String aCopySrcAttr,
                          String aCopyDstAttr,
                          String aCopyRunAttr,
                          String aRootOpAttr,
                          boolean aUseQNames ) {
      DIFF_NS = aNameSpace;
      DIFF_COPY_TAG = aCopyTag;
      DIFF_INS_TAG = aInsTag;
      DIFF_ESC_TAG = aEscTag;
      DIFF_ROOT_TAG = aRootTag;
      DIFF_CPYSRC_ATTR = aCopySrcAttr;
      DIFF_CPYDST_ATTR = aCopyDstAttr;
      DIFF_CPYRUN_ATTR = aCopyRunAttr;
      DIFF_ROOTOP_ATTR = aRootOpAttr;
      useQName = aUseQNames;
      init();
    }

    protected void init() {
      RESERVED = new HashSet();
      RESERVED.add(DIFF_COPY_TAG);
      RESERVED.add(DIFF_INS_TAG);
      RESERVED.add(DIFF_ESC_TAG);
    }
  }

/*
  public interface TreeAbstraction {
    public List getStopNodes( Object changeNode );
    public Object lookupBase( Object changeNode );
    public void content( Object node, boolean start) throws SAXException; // return open tagname
    //public boolean needsEscape( Object changeNode );
    public Iterator getChildIterator(Object changeNode);
    //public String identify(Object changeNode);
    //public String getRun(Object start, Object end);
    public boolean appends( Object baseTail, Object baseNext );
  }*/


  // 3dm XML implementation

  public List getStopNodes(Object changeNode) {
    Vector v = new Vector();
    m.getAreaStopNodes(v, (BranchNode) changeNode);
    return v;
  }

  public Object lookupBase(Object changeNode) {
    return ( (BranchNode) changeNode).getBaseMatch();
  }

  public Object getChangeRoot() {
    return m.getBranchRoot();
  }

  protected void content(Object branch, boolean open) throws IOException {
    try {
      if (branch instanceof DiffOperation) {
        DiffOperation op = (DiffOperation) branch;
        AttributesImpl rootAtts = new AttributesImpl();
        switch (op.getOperation()) {
          case DiffOperation.ROOT_INSERT:
            addAttribute(rootAtts, cf.DIFF_ROOTOP_ATTR, cf.DIFF_ROOTOP_INS);
          case DiffOperation.ROOT_COPY:
            boolean hasNamespace = cf.DIFF_NS != null &&
                cf.DIFF_NS.length() > 0;
            if (open) {
              ch.startDocument();
              if (hasNamespace)
                ch.startPrefixMapping("diff", cf.DIFF_NS);
              startElem(ch, cf.DIFF_ROOT_TAG, rootAtts);
            } else {
              endElem(ch, cf.DIFF_ROOT_TAG);
              if (hasNamespace)
                ch.endPrefixMapping("diff");
              ch.endDocument();
            }
            break;
          case DiffOperation.COPY:
            if (open)
              openCopy(op.getSource(), op.getDestination(), op.getRun(), ch);
            else
              endElem(ch, cf.DIFF_COPY_TAG);
            break;
          case DiffOperation.INSERT:
            if (open) {
              AttributesImpl atts = new AttributesImpl();
              if (op.getDestination() != null)
                addAttribute(atts, cf.DIFF_CPYDST_ATTR,
                             identify(op.getDestination()));
              startElem(ch, cf.DIFF_INS_TAG, atts);
            } else
              endElem(ch, cf.DIFF_INS_TAG);
            break;
          default:
            throw new UnsupportedOperationException("Unknown diffop: " +
                op.getOperation());
        }
        return;
      }
      XMLNode content = ( (BranchNode) branch).getContent();
      if (content instanceof XMLTextNode) {
        if (!open)
          return;
        XMLTextNode ct = (XMLTextNode) content;
        ch.characters(ct.getText(), 0, ct.getText().length);
      } else {
        // Element node
        XMLElementNode ce = (XMLElementNode) content;
        // FIXME: Esscape code is broken! (it never worked anyway)
//        if( needsEscape(branch) )
//          startElem(ch,cf.DIFF_ESC_TAG,EMPTY_ATTS);
        if (open)
          ch.startElement(ce.getNamespaceURI(), ce.getLocalName(),
                          ce.getQName(),
                          ce.getAttributes());
        else
          ch.endElement(ce.getNamespaceURI(), ce.getLocalName(), ce.getQName());
//        if( needsEscape(branch) )
//          endElem(ch,cf.DIFF_ESC_TAG);
      }
    } catch ( SAXException x) {
      throw new IOException(x.getMessage());
    }
  }

  // Workaround QName hell

  protected void addAttribute(AttributesImpl a, String name, String value) {
    if (cf.useQName)
      a.addAttribute("", "", name, "CDATA", value);
    else
      a.addAttribute("" /*cf.DIFF_NS*/, name, "", "CDATA", value);
  }

  protected void startElem(ContentHandler c, String name, Attributes atts) throws
      SAXException {
    if (cf.useQName)
      c.startElement("", "", name, atts);
    else
      c.startElement(cf.DIFF_NS, name, "", atts);
  }

  protected void endElem(ContentHandler c, String name) throws
      SAXException {
    if (cf.useQName)
      c.endElement("", "", name);
    else
      c.endElement(cf.DIFF_NS, name, "");
  }

  protected void openCopy(Object src, Object dst, Long run, ContentHandler ch) throws
      SAXException {
    AttributesImpl copyAtts = new AttributesImpl();
    addAttribute(copyAtts, cf.DIFF_CPYSRC_ATTR, identify(src));
    if (dst != DiffOperation.NO_VALUE)
      addAttribute(copyAtts, cf.DIFF_CPYDST_ATTR, identify(dst));
    if (run != DiffOperation.NO_VALUE)
      addAttribute(copyAtts, cf.DIFF_CPYRUN_ATTR, run.toString());
    startElem(ch, cf.DIFF_COPY_TAG, copyAtts);
  }

  protected void closeCopy(ContentHandler ch) throws SAXException {
    endElem(ch, cf.DIFF_COPY_TAG);
  }

  public boolean needsEscape(Object changeNode) {
    XMLNode content = ( (BranchNode) changeNode).getContent();
    if (content instanceof XMLElementNode &&
        DEFAULT_CONFIG.RESERVED.contains( ( (XMLElementNode) content).getQName()))
      return true;
    return false;
  }

  public Iterator getChildIterator(Object changeNode) {
    return ( (BranchNode) changeNode).children.listIterator();
  }

  public boolean appends(Object tail, Object next) {
    return ( (Number) index.getId(next)).longValue() ==
        ( (Number) index.getId(tail)).longValue() + 1;
  }

  public String identify(Object node) {
    return index.getId(node).toString();
  }

}
