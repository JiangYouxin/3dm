// $Id: Editgen.java,v 1.16 2006-02-03 09:05:48 ctl Exp $
package tdm.editgen;

import tdm.lib.XMLNode;
import tdm.lib.XMLElementNode;
import tdm.lib.XMLTextNode;
import tdm.lib.Node;
import tdm.lib.NodeFactory;
import tdm.lib.BaseNode;
import tdm.lib.BranchNode;
import tdm.lib.MatchedNodes;
import tdm.lib.XMLParser;
import tdm.lib.XMLNode;
import tdm.lib.XMLPrinter;
import tdm.lib.EditLog;
import java.util.Iterator;
import java.util.Set;
import java.util.HashSet;
import org.xml.sax.helpers.AttributesImpl;

public class Editgen {

  public static final String EDIT_PREFIX = "edits-";
  public static final String ALLOWED_OPS = "diumc";

  private boolean prettyPrint = true;
  private String editLogPrefix = null;
  private double probability = 0.01; // Defaultprob
  private int editCount = -1;
  private boolean useEditCount = false;
  private char[] operations = {'d','i','u','m','c'};
  private String idPrefix = "";
  private boolean useIdAttribute = false;
  private boolean updateModifiesId = false;
  private boolean copyModifiesId = false;

  private boolean printStats = true;
  private int delCount =0, insCount = 0, updCount = 0, moveCount =0, copyCount =0;

  java.util.Random rnd = new java.util.Random( 31415926L ); // repeatable runs!
  static int idCounter = 1000000;

  public static void main(String[] args) {
    Editgen e = new Editgen();
    e.editGen(/*"/home/ctl/fuego-core/xmlfs/3dm/usecases/shopping/"*/"L7.xml", "m.xml",
              new String[] {"1.xml","2.xml"});
  }

  public void editGen( String inFile, String mergeFile, String[] outfiles ) {
    // Parse infile
   MarkableBaseNode docBase=null;
   BranchNode docMerged=null;
   System.err.println("Parsing "+inFile+"...");
   try {
     XMLParser p = new XMLParser();
     docBase = (MarkableBaseNode) p.parse( inFile,baseNodeFactory);
     countSubtreeSizes(docBase); // We need subtree counts to alloc a random node
   } catch ( Exception e ) {
     System.err.println("XML Parse error in " + inFile +
                        ". Detailed exception info is:" );
     System.err.println( e.toString() );
     e.printStackTrace();
     return;
   }
   // Make merge clone
   // total merge is left tree, current working tree is right
   docMerged = clonedAndMatchedTree( docBase, true, true, false, null );
   EditLog mergeLog = new EditLog();
   // Make variants
   for( int iFile = 0; iFile < outfiles.length; iFile++ ) {
     System.err.println("Making "+outfiles[iFile]+"...");
     EditLog branchLog = new EditLog();
     BranchNode outRoot = clonedAndMatchedTree( docBase, false, true, false, null );
     ((MarkableBaseNode) docBase).mark(MarkableBaseNode.MARK_STRUCTURE | MarkableBaseNode.MARK_CONTENT);
     ((MarkableBaseNode) docBase.getChild(0)).mark(MarkableBaseNode.MARK_STRUCTURE); // Never edit root elem
     int edits = editCount == -1 ?
                 (int) (((MarkableBaseNode) docBase).getSubteeSize()*
                 (probability/outfiles.length)) :
                 editCount;
     transform( edits, (MarkableBaseNode) docBase.getChild(0),
                (MarkableBaseNode) docBase.getChild(0), mergeLog, branchLog);
     try {
       printTree( outRoot, new XMLPrinter( new java.io.FileOutputStream( outfiles[iFile]
           ), prettyPrint ));
     } catch (java.io.IOException x ) {
       System.err.println("Unable to write outfile "+outfiles[iFile] );
     }
     if( editLogPrefix != null ) {
       String editLogFile = editLogPrefix+outfiles[iFile];
       try {
         branchLog.writeEdits(new XMLPrinter( new java.io.FileOutputStream( editLogFile ),
                                              prettyPrint));
       } catch (Exception x ) {
         System.err.println("Unable to write edit log "+editLogFile );
       }
     }
   }
   // Write merge facit
   if( mergeFile != null ) {
     try {
       printTree(docMerged,
             new XMLPrinter(new java.io.FileOutputStream(mergeFile),
                            prettyPrint ));
     }
     catch (java.io.IOException x) {
       System.err.println("Unable to write outfile " + mergeFile);
     }
   }
   // Write combined log
   if( editLogPrefix != null ) {
     String mergeLogFile = editLogPrefix+mergeFile;
     try {
       mergeLog.writeEdits(new XMLPrinter( new java.io.FileOutputStream( mergeLogFile )));
     } catch (Exception x ) {
       System.err.println("Unable to write edit log "+mergeLogFile );
     }
   }
   if( printStats ) {
     System.err.println("Base tree nodes: "+((MarkableBaseNode) docBase.getChild(0)).getSubteeSize());
     System.err.println("        Inserts: "+insCount);
     System.err.println("        Deletes: "+delCount);
     System.err.println("        Updates: "+updCount);
     System.err.println("          Moves: "+moveCount);
     System.err.println("         Copies: "+copyCount);
     System.err.println("    Total edits: "+(insCount+delCount+updCount+moveCount+copyCount));
   }
  }

  int _visitCount = 0;

  public void transform( int edits, MarkableBaseNode base,MarkableBaseNode baseRoot,
                         EditLog mergeLog, EditLog branchLog ) {
    final int TRIES = 10;
    MarkableBaseNode n = null;
    for( int i=0;i<edits;i++) {
      char op = /*operations[i % operations.length];*/
          operations[(int) (rnd.nextDouble()*operations.length)];
      int tries = TRIES;
      do {
        n = getRandomNode(baseRoot,
            op=='u' ? MarkableBaseNode.MARK_CONTENT : MarkableBaseNode.MARK_STRUCTURE
            ,null);
        if( n == null ) {
          System.err.println("Ran out of free nodes to edit");
          return;
        }
      } while( !editNode(op,n,baseRoot,mergeLog,branchLog) && --tries > 0);
      if( tries == 0)
          System.err.println("Warning: Unable to perform operation "+op);
    }

  }

  public boolean editNode( char op, MarkableBaseNode base,MarkableBaseNode baseRoot,
                         EditLog mergeLog, EditLog branchLog ) {
    BranchNode n = null; // used by edit ops
    boolean after = false;
    MarkableBaseNode dest = null;
    switch(op) {
      case 'd': // Delete node
        // System.err.println("DEL");
        dest = getLargestDelTree(base);
        if( dest == null ) {
          //System.err.println("-- Nothing suitable to del found");
          return false;
        }
        _checkNotMarked(dest);
        dest.lock(MarkableBaseNode.MARK_CONTENT | MarkableBaseNode.MARK_STRUCTURE);
        dest.lockSubtree(MarkableBaseNode.MARK_CONTENT | MarkableBaseNode.MARK_STRUCTURE);
        delCount++;
        editTrees(dest,null,null,false,false,mergeLog,branchLog);
        break;
      case 'i': // Insert node
        insCount++; // System.err.println("INS");
        org.xml.sax.helpers.AttributesImpl atts = new org.xml.sax.helpers.AttributesImpl();
        atts.addAttribute("","","id","CDATA",
                          ""+newId() ); //  rnd.nextLong()+"@"+ System.currentTimeMillis());
        XMLElementNode content = new XMLElementNode("editgen-insert",atts);
        after = rnd.nextDouble() > 0.5;
        base.lock(!after,after,MarkableBaseNode.MARK_STRUCTURE);
        editTrees(null,base,new BranchNode( content),after,false,mergeLog,branchLog);
        break;
      case 'u': // Update node
        updateNode(base,mergeLog,branchLog);
        updCount++; //System.err.println("UPD");
        base.mark(MarkableBaseNode.MARK_CONTENT);
        break; // N/A for now...
      case 'm': // Move subtree
        //System.err.println("MOV");
        // Delete from base (src of move) (=delete code block)
        base.beginMarkTransaction();
        base.lock( MarkableBaseNode.MARK_STRUCTURE);
        dest = getRandomNode( baseRoot, MarkableBaseNode.MARK_STRUCTURE, base ); // NOTE! Dest must be fetched AFTER src is locked!
        // Check if No possible dest or
        // move where a copy of dst is moved
        if( dest == null || !_treeHasNoCopyOf(base,dest,true) ) {
          base.abortMarkTransaction();
          return false; // No possible dest
        }
        after = rnd.nextDouble() > 0.5;
        dest.lock(!after,after,MarkableBaseNode.MARK_STRUCTURE);
        // Text node move check
        n = dest.getLeft().getFullMatch();
        Node n2 = after ? n.getRightSibling() : n.getLeftSibling();
        if( base.getContent() instanceof XMLTextNode &&
            ( (n.getContent() instanceof XMLTextNode) ||
            ( n2 != null && n2.getContent() instanceof XMLTextNode ))) {
          base.abortMarkTransaction();
          return false;
        }
        base.commitMarkTransaction();
        moveCount++;
        editTrees(base,dest,null,after,true,mergeLog,branchLog);
        break;
      case 'c': // Copy subtree
        // NOTE: Will currently never copy as child of a node,
        // if the node does not already have children (always inserts in childlist)
        //System.err.println("CPY");
        // Lock src of copy
        base.beginMarkTransaction();
        base.lock(MarkableBaseNode.MARK_STRUCTURE);
        // Insert at dest (=insert code block)
        dest = getRandomNode( baseRoot, MarkableBaseNode.MARK_STRUCTURE, base ); // NOTE! Dest must be fetched AFTER src is locked!
        // Check if No possible dest or
        // circular copies (i.e. dest node (or parents of it) exists in copied tree)
        if( dest == null || !_treeHasNoCopyOf(base,dest,true) ) {
          base.abortMarkTransaction();
          return false;
        }
        after = rnd.nextDouble() > 0.5;
        dest.lock(!after,after,MarkableBaseNode.MARK_STRUCTURE);
        n = dest.getLeft().getFullMatch();
        n2 = after ? n.getRightSibling() : n.getLeftSibling();
        if( base.getContent() instanceof XMLTextNode &&
            ( (n.getContent() instanceof XMLTextNode) ||
            ( n2 != null && n2.getContent() instanceof XMLTextNode ))) {
//          System.err.println("-- Abort: copying text node adjacent to other text node not possible");
          base.abortMarkTransaction();
          return false;
        }
        copyCount++;
        base.commitMarkTransaction();
        editTrees(null,dest,base,after,true,mergeLog,branchLog);
        break;
    }
    return true;
  }

  protected void updateNode( MarkableBaseNode n, EditLog mergeLog,
                            EditLog branchLog  ) {
    String newId = newId();
    doUpdateNode(n,mergeLog , true, newId );
    doUpdateNode(n,branchLog , false, newId );
  }

  protected void doUpdateNode( MarkableBaseNode b, EditLog log,
                             boolean left, String newId ) {
    MatchedNodes m = left ? b.getLeft() : b.getRight();
    for( Iterator i = m.getMatches().iterator();i.hasNext();) {
      BranchNode n = (BranchNode) i.next();
      XMLNode c = n.getContent();
      if( c instanceof XMLTextNode ) {
        XMLTextNode ct = (XMLTextNode) c;
        ct.setText((ct.toString()+newId).toCharArray());
      } else {
        XMLElementNode ce = (XMLElementNode) c;
        ce.setQName( ce.getQName() + newId );
        if( useIdAttribute && updateModifiesId ) {
          int idIx = ce.getAttributes().getIndex("id");
          ( (AttributesImpl) ce.getAttributes()).setValue(idIx,newId());
        }
      }
    log.update(n);
    }
  }

  /** Return a random node from a tree. If isUnmarked is set, only returns unmarked
   * nodes. Never returns nodes inside forbiddenTree.
   */

  public MarkableBaseNode getRandomNode( MarkableBaseNode root, int markMask,
     MarkableBaseNode forbiddenTree ) {
    int pos = (int) (rnd.nextDouble() * (root.getSubteeSize() -
        (forbiddenTree == null ? 0 : forbiddenTree.getSubteeSize()) ))+1;
//    System.err.println("Pos="+pos);
    MarkableBaseNode found = doGetRandomNode( pos, root, markMask, forbiddenTree );
    if( found == null && markMask > 0 )
      found=doGetRandomNode(SCAN_UNMARKED,root,markMask, forbiddenTree ); // Unmarked scann reached end of tree, start from top
// Forbiddentreecheck
    MarkableBaseNode _n = found;
    while( _n != null ) {
      if( _n == forbiddenTree )
        throw new IllegalStateException("found in forbidden tree");
      _n = (MarkableBaseNode) _n.getParent();
    }
// endcheck
    return found;
  }

  protected static final int SCAN_UNMARKED = Integer.MIN_VALUE;
  protected MarkableBaseNode doGetRandomNode( int pos, MarkableBaseNode n,int markMask,
     MarkableBaseNode forbiddenTree ) {
    if( n == null || n == forbiddenTree )
      throw new IllegalArgumentException(n==null ? "n==null" : "recursed into forbidden tree");
    if( pos == SCAN_UNMARKED ) {
      if( (n.getMark() & markMask) == 0 )
        return n;
      else {
        MarkableBaseNode found = null;
        for( int i=0;i<n.getChildCount() && found == null;i++) {
          MarkableBaseNode child = (MarkableBaseNode) n.getChild(i);
          if( child != forbiddenTree )
            found = doGetRandomNode(pos,child,markMask,forbiddenTree);
        }
        return found;
      }
    }
    pos--;
    if( pos == 0 ) {
      if( (n.getMark() & markMask) != 0 )
        return doGetRandomNode(SCAN_UNMARKED,n,markMask, forbiddenTree);
      else
        return n;
    }
    int stSize = 0;
    MarkableBaseNode child= null;
    // BUGFIX 030116
    if( n.getChildCount() == 0 || (n.getChildCount() == 1 && n.getChild(0)==forbiddenTree))
      return null; // Out of nodes to scan
    // ENDBUGFIX
    for( int i=0;i<n.getChildCount() && pos>0;i++) {
      MarkableBaseNode candidate = ((MarkableBaseNode) n.getChild(i));
      if( candidate == forbiddenTree )
        continue;
      child = candidate;
      stSize =  child.getSubteeSize();
      pos-= stSize;
    }
    if( child == null )
      System.err.println("ARGH! pos="+(pos+stSize));
    return doGetRandomNode( pos + stSize, child, markMask, forbiddenTree );
  }

  // Get root of largest unmarked subtree of n

  protected MarkableBaseNode getLargestDelTree( MarkableBaseNode n ) {
    boolean allSubtreesDeletable = true;
    MarkableBaseNode largest = null;
    // find largest deltree from children
    for( int i=0;i<n.getChildCount();i++) {
      MarkableBaseNode delRoot = getLargestDelTree((MarkableBaseNode) n.getChild(i));
      allSubtreesDeletable &= delRoot == n.getChild(i);
      if( largest == null || (delRoot != null &&  largest.getSubteeSize() < delRoot.getSubteeSize() ))
        largest = delRoot;
    }
    if( allSubtreesDeletable && !n.isMarked() )
      return n;
    else
      return largest;
  }

  // Safety check
  private void _checkNotMarked( MarkableBaseNode n ) {
    if( n.isMarked() )
      throw new RuntimeException("ASSERT FAILED");
    for(int i=0;i<n.getChildCount();i++)
      _checkNotMarked((MarkableBaseNode) n.getChild(i));
  }

  private boolean _treeHasNoCopyOf( BaseNode root, BaseNode forbiddenNode, boolean left) {
    // Build set of forbidden nodes (=forbidden Node, or any of its parents)
    Set forbidden = new HashSet();
    while( forbiddenNode != null ) {
      forbidden.add(forbiddenNode);
      forbiddenNode = forbiddenNode.getParent();
    }
    MatchedNodes m = left ? root.getLeft() : root.getRight();
    for( Iterator i = m.getMatches().iterator();i.hasNext();) {
      BranchNode match = (BranchNode) i.next();
      if( !_treeHasNoCopyOf(match,forbidden) )
        return false;
    }
    return true;
  }

   private boolean _treeHasNoCopyOf( BranchNode root, Set forbidden) {
     if( root == null )
       return true;
     if( forbidden.contains(root.getBaseMatch()) )
       return false; //has copy throw new RuntimeException("ASSERT FAILED");
     for( int i=0;i<root.getChildCount();i++) {
       if( !_treeHasNoCopyOf(root.getChild(i), forbidden) )
         return false;
     }
     return true;
   }

  /** Function for manipulating trees. Use as described below (* means arg not used).
   * The base tree is never edited (but matches are added/removed). All operations
   * operate on entire subtrees. This function only works properly when all copied
   * subtrees are kept identical. (The function itself preserves this property)
   * DEL: editTrees(nodeToDelete,null,*,*,*);
   *   All matches for nodeToDelete in left & right are deleted, and matchings to
   *   the deleted nodes are removed.
   * INS: editTrees(null,siblingOfInsNode,BranchNode nodeToInsert,after,false);
   *   nodeToInsert is attached to both branches, either before (after flag false)
   *   or after (after flag true) siblingOfInsNode. No matchings are added.
   * MOV: editTrees(nodeToMove,targetNode,null,after,true);
   *  For both branches, the matches for nodeToMove are moved to before/after
   *  all matches for targetNode (before/after depending on the after flag)
   * CPY: editTrees(null,targetNode,BaseNode nodeToCopy,after,true);
   *  For both branches, a full match for nodeToCopy is copied to before/after
   *  all matches for targetNode (before/after depending on the after flag)
  */

  protected void editTrees( MarkableBaseNode src, MarkableBaseNode dest, Node insTree,
                            boolean after, boolean cloneInsTree, EditLog mergeLog,
                            EditLog branchLog ) {
    doEditTrees( src,dest, after, insTree, cloneInsTree, true, mergeLog );
    doEditTrees( src,dest, after, insTree, cloneInsTree, false, branchLog );
  }

  private void doEditTrees( MarkableBaseNode src,
   MarkableBaseNode dest, boolean after, Node insTree, boolean cloneInsTree,
                           boolean left, EditLog log ) {
    MatchedNodes m = null;
    if( src != null ) {
      // We should detach all matches of src
      // NOTE! for move with multiple matches, assumes all matches are identical!
      // (we only keep track of 1 deleted node, and reattach it to every copy)
      m = left ? src.getLeft() : src.getRight();
      if( dest == null )
        log.delete(src,m.getFullMatch().getParent());
      for( Iterator i = m.getMatches().iterator();i.hasNext();) {
        BranchNode match = (BranchNode) i.next();
        insTree = match; // DONT USE .getBaseMatch(), as there may be changes in the Branch!
        match.getParent().removeChild(match.getChildPos());
      }
      m.getMatches().clear();
    }
    if( dest != null ) {
      m = left ? dest.getLeft() : dest.getRight();
      for( Iterator i = m.getMatches().iterator();i.hasNext();) {
        BranchNode match = null; //DBG
        try { // DBG
          /*BranchNode*/ match = (BranchNode) i.next();
        } catch (Exception x ) { // DBG
          System.err.println("EXCEPT: "+x); //DBG
        } //DBG
        int ix = match.getChildPos() + (after ? 1: 0);
        if (!cloneInsTree) {
          if( src != null )
            throw new IllegalStateException("Currently unneccessary/unimplemented state");
          match.getParent().addChild(ix,insTree);
        } else {
//DBG          _treeHasNoCopyOf(dest,insTree instanceof BranchNode ? ((BranchNode) insTree).getBaseMatch() :
//DBG                         (BaseNode) insTree,left);
//DBG          HashSet _destSet = new HashSet();
//DBG          _destSet.add(dest);
//DBG          _treeHasNoCopyOf(insTree instanceof BranchNode ? (BranchNode) insTree:null,_destSet);
          BranchNode insRoot = clonedAndMatchedTree(
          insTree instanceof BaseNode ? (left ? ((BaseNode) insTree).getLeft().getFullMatch() :
          ((BaseNode) insTree).getRight().getFullMatch() ) : insTree
              ,left,false,
              src != null && useIdAttribute && copyModifiesId,
              dest);
          match.getParent().addChild(ix,insRoot);
          if( src == null ) {
            if( insTree instanceof BaseNode )
              log.copy(insRoot);
            else
              log.insert(insRoot);
          } else
            log.move(insRoot);
        }
      }
    }
  }

  protected BranchNode clonedAndMatchedTree( Node n, boolean left, boolean resetMatches,
                                            boolean allocNewIds, BaseNode _forbidden   ) { //DBG
    BaseNode b = n instanceof BaseNode ? (BaseNode) n : ((BranchNode) n).getBaseMatch();
//DBG        _treeHasNoCopyOf(b,_forbidden,left);
        if( b== _forbidden ) //DBG
          throw new RuntimeException("TOUCHING FORBIDDEN NODE"); // DBG
    BranchNode nc = new BranchNode( (XMLNode) n.getContent().clone() );
    if( allocNewIds && nc.getContent() instanceof XMLElementNode ) {
      AttributesImpl atts = (AttributesImpl)
          ((XMLElementNode) nc.getContent()).getAttributes();
      atts.setValue(atts.getIndex("id"),newId());
    }
    if( b != null ) { // Set matches for non-inserted nodes
      nc.setBaseMatch(b,BranchNode.MATCH_FULL);
      MatchedNodes mn = left ? b.getLeft() : b.getRight();
      if( resetMatches )
        mn.clearMatches();
      mn.addMatch(nc);
    }
    for( int i=0;i<n.getChildCount();i++)
      nc.addChild(clonedAndMatchedTree(n.getChildAsNode(i),left,resetMatches,
                                      allocNewIds, _forbidden)); //DBG
    return nc;
  }

  void printTree( Node n, XMLPrinter p ) {
    p.startDocument();
    doPrintTree(n.getChildAsNode(0),p);
    p.endDocument();
  }

  void doPrintTree( Node n, XMLPrinter p ) {
    XMLNode c = n.getContent();
    if( c instanceof XMLTextNode ) {
      XMLTextNode ct = (XMLTextNode) c;
      p.characters(ct.getText(),0,ct.getText().length);
    } else {
      XMLElementNode ce = (XMLElementNode) c;
      p.startElement(ce.getNamespaceURI(),ce.getLocalName(),ce.getQName(),ce.getAttributes());
      for( int i=0;i<n.getChildCount();i++)
        doPrintTree(n.getChildAsNode(i),p);
      p.endElement(ce.getNamespaceURI(),ce.getLocalName(),ce.getQName());
    }
  }

  public int countSubtreeSizes( MarkableBaseNode n) {
    int stSize = 0;
    for( int i=0;i<n.getChildCount();i++)
      stSize += countSubtreeSizes((MarkableBaseNode)n.getChild(i));
    stSize++;
    n.setSubtreeSize(stSize);
    return stSize;
  }

/*
  protected void markSubtree( MarkableBaseNode b )  {
    b.mark();
    for(int i=0;i<b.getChildCount();i++)
      markSubtree((MarkableBaseNode) b.getChild(i));
  }

 protected void unmarkSubtree( MarkableBaseNode b )  {
    b.unmark();
    for(int i=0;i<b.getChildCount();i++)
      unmarkSubtree((MarkableBaseNode) b.getChild(i));
  }
*/

  public void setPrettyPrint( boolean pp) {
    prettyPrint = pp;
  }

  public void setEditCount( int aCount ) {
    if( aCount < 0  )
      throw new IllegalArgumentException("Illegal edit count");
    useEditCount = true;
    editCount = aCount;
  }

  public void setProbability(double p) throws IllegalArgumentException {
    if( p < 0.0 || p>1.0 )
      throw new IllegalArgumentException("Illegal probability");
    useEditCount = false;
    probability = p;
  }

  public void setRandomGenerator( java.util.Random aRndGen ) {
    rnd = aRndGen;
  }

  // null means no log
  public void setEditLogPrefix( String aLog ) {
    editLogPrefix = aLog;
  }

  // null means don't use id attribs
  public void setIdPrefix( String aPrefix ) {
    if( aPrefix == null )
      useIdAttribute = false;
    else {
      useIdAttribute = true;
      idPrefix = aPrefix;
    }
  }

  public void setUpdateModifiesId( boolean modifies ) {
    updateModifiesId = modifies;
  }

  public void setCopyModifiesId( boolean modifies ) {
    copyModifiesId = modifies;
  }

  public void setOperations( String ops ) throws IllegalArgumentException {
    if( ops.length() == 0 ) {
      throw new IllegalArgumentException("Empty operations string");
    }
    char[] newOps = new char[ops.length()];
    for(int i=0;i<ops.length();i++) {
      char ch = ops.charAt(i);
      if( ALLOWED_OPS.indexOf(ch) == -1 )
        throw new IllegalArgumentException("Invalid char in operations string: "+ch);
      newOps[i]=ch;
    }
    operations=newOps;
  }


  // Factory for BaseNode:s
  private static NodeFactory baseNodeFactory =  new NodeFactory() {
            public Node makeNode(  XMLNode content ) {
              return new MarkableBaseNode( content  );
            }
        };

  private static NodeFactory branchNodeFactory =  new NodeFactory() {
            public Node makeNode(  XMLNode content ) {
              return new BranchNode(  content  );
            }
        };

  protected String newId() {
    return idPrefix +idCounter++;
  }

  private void _getByIds( Node n, String id, java.util.Set nodes ) {
    if( id.equals(_getId(n)) )
        nodes.add(n);
    for(int i=0;i<n.getChildCount();i++)
      _getByIds(n.getChildAsNode(i),id,nodes);
  }

  private String _getId( Node n ) {
    XMLNode c = n.getContent();
    if( c instanceof XMLElementNode ) {
      XMLElementNode ce = (XMLElementNode) c;
      return ce.getAttributes().getValue("id");
    } else {
      return ((XMLTextNode) c).toString();
    }
  }


}
