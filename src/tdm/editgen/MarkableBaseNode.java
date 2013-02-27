// $Id: MarkableBaseNode.java,v 1.5 2003-01-09 13:38:45 ctl Exp $
package tdm.editgen;

import tdm.lib.BaseNode;
import tdm.lib.XMLNode;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

public class MarkableBaseNode extends BaseNode {

  protected static Map markLog = null;

  public static final int MARK_NONE = 0;
  public static final int MARK_CONTENT = 1;
  public static final int MARK_STRUCTURE = 2;

  protected int markCount = 0;
  protected int subtreeSize = 0;

  MarkableBaseNode(XMLNode aContent) {
    super( aContent );
  }

  public void beginMarkTransaction() {
    if( markLog != null )
      throw new IllegalStateException("Recursive transactions not supported");
    markLog =  new HashMap();
  }

  public void commitMarkTransaction() {
    if( markLog == null )
      throw new IllegalStateException("No transaction in progress");
    markLog =  null;
  }

  public void abortMarkTransaction() {
    if( markLog == null )
      throw new IllegalStateException("No transaction in progress");
    for( Iterator i = markLog.entrySet().iterator();i.hasNext();) {
      Map.Entry e = (Map.Entry) i.next();
      ((MarkableBaseNode) e.getKey()).markCount = ((Integer) e.getValue()).intValue();
    }
    markLog =  null;
  }

  public void mark(int mark) {
    if( markLog != null )
      markLog.put(this,new Integer(markCount));
    markCount|=mark;
  }

  public boolean isMarked() {
    return markCount > 0;
  }

  public boolean isMarkedContent() {
    return (markCount & MARK_CONTENT) != 0;
  }

  public boolean isMarkedStructure() {
    return (markCount & MARK_STRUCTURE) != 0;
  }

  public int getMark() {
    return markCount;
  }

/*
  public void unmark() {
    if(markCount==0)
      throw new IllegalStateException("Too many unmarks");
    markCount--;
  }
*/
/*  public boolean isLocked() {
    MarkableBaseNode leftSib = (MarkableBaseNode) getLeftSibling(),
      rightSib = (MarkableBaseNode) getRightSibling();
    return isMarked &&
           (leftSib != null && leftSib.isMarked
           ((childPos > 0 ) & ((MarkableBaseNode) parent.getChildAsNode(childPos-1)).isMarked) &&
           ((childPos < parent.getChildCount()-1 ) & ((MarkableBaseNode) parent.getChildAsNode(childPos+1)).isMarked);
  }
*/

  public void lock(int type) {
    lock( true, true, type );
  }

  public void lockSubtree(int type) {
    for( int i=0;i<getChildCount();i++) {
      MarkableBaseNode n = (MarkableBaseNode) getChild(i);
      n.lock(type);
      n.lockSubtree(type);
    }
  }

  public void lockLeft(int type) {
    lock( true, false, type );
  }

  public void lockRight(int type) {
    lock( false, true, type );
  }

  public void lock(boolean left, boolean right, int type) {
    MarkableBaseNode leftSib = left ? (MarkableBaseNode) getLeftSibling() : null,
      rightSib = right ? (MarkableBaseNode) getRightSibling() : null;
    mark(type);
    if( rightSib != null ) rightSib.mark(type);
    if( leftSib != null ) leftSib.mark(type);
  }

  public int getSubteeSize() {
    return subtreeSize;
  }

  public void setSubtreeSize( int aSize) {
    subtreeSize = aSize;
  }
}