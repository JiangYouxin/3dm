// $Id: DiffAlgorithm.java,v 1.1 2006-02-06 11:08:57 ctl Exp $
package tdm.lib;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

public abstract class DiffAlgorithm {

//  private static final Object NO_DST_REQUIRED = new Object(); // Must not be null!
    // (that -> nullptrex in sequence

  public DiffAlgorithm() {
  }


  /* generate diff starting from branchRoot */
  protected void diff(Object branchRoot) throws IOException {
    boolean rootHasMatch = lookupBase(branchRoot) != null;
    if (rootHasMatch) {
      List stopNodes = getStopNodes(branchRoot);
      DiffOperation op = new DiffOperation(DiffOperation.ROOT_COPY,
                                           DiffOperation.NO_VALUE,
                                           DiffOperation.NO_VALUE,
                                           DiffOperation.NO_VALUE);
      content(op, true);
      copy(stopNodes);
      content(op, false);
    } else {
      DiffOperation op = new DiffOperation(DiffOperation.ROOT_INSERT,
                                           DiffOperation.NO_VALUE,
                                           DiffOperation.NO_VALUE,
                                           DiffOperation.NO_VALUE);
      content(op, true);
      insert(branchRoot);
      content(op, false);
    }
  }

  protected void copy(List stopNodes) throws
      IOException {
    // Find stopnodes
    Sequence s = new Sequence();
    for (Iterator i = stopNodes.iterator(); i.hasNext(); ) {
      Object stopNode = i.next();
      Object dst = lookupBase(stopNode);
      // BUGFIX 030115
      if (!emitChildList(s, stopNode, dst, false)) {
        DiffOperation op = new DiffOperation(DiffOperation.INSERT,
                                             DiffOperation.NO_VALUE, dst,
                                             DiffOperation.NO_VALUE);
        content(op, true);
        content(op, false);
      }
      // ENDBUGFIX
    }
  }


  protected void insert(Object branch) throws
      IOException {
    // Element node
    Sequence s = new Sequence();
    content(branch,true);
    emitChildList(s, branch, DiffOperation.NO_VALUE, true); // NO dst required
    content(branch,false);
  }


  private boolean emitChildList(Sequence s, Object parent,
                                Object dst, boolean insMode) throws
      IOException {

    Vector children = new Vector();
    for (Iterator i = getChildIterator(parent); i.hasNext(); ) {
      children.add(i.next());
    }
    for (int ic = 0; ic < children.size(); ic++) {
      boolean lastStopNode = ic == children.size() - 1;
      Object child = children.elementAt(ic);
      Object baseMatch = lookupBase(child);
      if (baseMatch != null) {
        List childStopNodes = getStopNodes(child);
        Object src = lookupBase(child);
        if (childStopNodes.size() == 0 && !lastStopNode) {
          if (s.isEmpty()) {
            s.init(src, dst);
            continue;
          } else if (s.appends(src, dst)) {
            s.append(src);
            continue;
          }
        }
        // Did not append to sequence (or @ last stopnode) => emit sequence
        if (!s.appends(src, dst)) {
          // Current does not append to prev seq -> output prev seq + new
          // in separate tags
          if (!s.isEmpty()) {
            DiffOperation op = new DiffOperation(DiffOperation.COPY, s.src,
                                                 s.dst, new Long(s.run));
            content(op, true);
            content(op, false);
          }
          if (childStopNodes.size() > 0 || lastStopNode) {
            DiffOperation op = new DiffOperation(DiffOperation.COPY, src,
                                                 dst, new Long(1l));
            content(op, true);
            copy(childStopNodes);
            content(op, false);
            s.setEmpty(); // Reset sequence
          } else
            s.init(src, dst);
        } else { // appends to open sequence (other reason for seq. break)
          s.append(src);
          DiffOperation op = new DiffOperation(DiffOperation.COPY, s.src, s.dst,
                                               new Long(s.run));
          content(op, true);
          copy(childStopNodes);
          content(op, false);
          s.setEmpty(); // Reset sequence
        }

      } else { // endif has base match
        if (!s.isEmpty()) {
          DiffOperation op = new DiffOperation(DiffOperation.COPY, s.src, s.dst,
                                               new Long(s.run));
          content(op, true);
          content(op, false);
          s.setEmpty();
        }
        if (!insMode) {
          // Insert tree...
          // SHORTINS = Concatenate several <ins> tags to a single one
          if (ic == 0 || (lookupBase(children.elementAt(ic - 1)) != null)) // SHORTINS
            content(new DiffOperation(DiffOperation.INSERT,
                                      DiffOperation.NO_VALUE, dst,DiffOperation.NO_VALUE ), true);
        }
        insert(child);
        if (!insMode) {
          if (lastStopNode || (lookupBase(children.elementAt(ic + 1)) != null)) // SHORTINS
            content(new DiffOperation(DiffOperation.INSERT,
                                      DiffOperation.NO_VALUE,
                                      DiffOperation.NO_VALUE, DiffOperation.NO_VALUE), false);
        }
      }
    } // endfor children
    return children.size() > 0;
  }

  class Sequence {
    Object src = null;
    Object dst = null;
    Object tail = null;
    long run = -1;

    void setEmpty() {
      run = -1;
    }

    boolean isEmpty() {
      return run == -1;
    }

    void init(Object asrc, Object adst) {
      src = asrc;
      tail = asrc;
      dst = adst;
      run = 1;
    }

    void append(Object aSrc) {
      run++;
      tail = aSrc;
    }

    boolean appends(Object asrc, Object adst) {
      return!isEmpty() && adst.equals(dst) &&
         DiffAlgorithm.this.appends(tail, asrc); // Nasty bug
    }

  }

  public static class DiffOperation {

    public static final int ROOT_COPY = 1;
    public static final int ROOT_INSERT = 2;
    public static final int COPY = 3;
    public static final int INSERT = 4;
    public static final Long NO_VALUE = new Long(Long.MIN_VALUE); // If a field has no value

    private int operation;
    private Object source;
    private Object destination;
    private Long run;

    public int getOperation() {
      return operation;
    }

    public Object getSource() {
      return source;
    }

    public Object getDestination() {
      return destination;
    }

    public Long getRun() {
      return run;
    }

    protected DiffOperation(int aOperation, Object aSource, Object aDestination,
                            Long aRun) {
      if( aSource ==null || aDestination == null || aRun == null )
        throw new IllegalArgumentException();
      operation = aOperation;
      source = aSource;
      destination = aDestination;
      run = aRun;
    }
  }

  // Implementations override these

  protected abstract List getStopNodes(Object changeNode);

  protected abstract Object lookupBase(Object changeNode);

  protected abstract void content(Object node, boolean start) throws
      IOException;

  protected abstract Iterator getChildIterator(Object changeNode);

  protected abstract boolean appends(Object baseTail, Object baseNext);

}
