//$Id: Measure.java,v 1.12 2005-10-19 18:22:12 ctl Exp $ D
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

import org.xml.sax.Attributes;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;

/** Node similarity measurement. This class is used to calculate the
 *  content, child list and matched child list distance between nodes.
 */

public class Measure {

  // Maximum distance. The distance is normalized between 0 and MAX_DIST
  public static final double MAX_DIST = 1.0;
  // Distance to return by childListDistance if both nodes have 0 children
  public static final double ZERO_CHILDREN_MATCH = 1.0;
  // info bytes in an element name ($c_e$ in thesis)
  public static final int ELEMENT_NAME_INFO = 1;
  // info bytes in the presence of an attribute ($c_a$ in thesis)
  public static final int ATTR_INFO = 2;
  // attribute values less than this have a info size of 1 ($c_v$ in thesis)
  public static final int ATTR_VALUE_THRESHOLD = 5;
  // text nodes shorter than this have an info size of 1 ($c_t$ in thesis)
  public static final int TEXT_THRESHOLD = 5;

///  public static final int DISTBUF_SIZE = 8192;
///  public final int[] DISTBUF = new int[DISTBUF_SIZE];
//$CUT
  // Tester
  public static void main(String[] args) {
    String a = "return stringDist( a, b, a.length()+b.length() );";
    String b = "return stzingDist( a, b, a.length()+b.length() );";
    System.out.println("a="+a);
    System.out.println("b="+b);
    System.out.println("Dist = " + (new Measure()).qDist(a,b));
  }
//$CUT
  public Measure() {
  }

  // State variables. State is used internally to allow the class to be extended
  // to calculate the distance between the combined contents of several nodes.
  // Currently, only parwise distance calculation is visible outside the class.

  // Mismatched info bytes
  private int mismatched = 0;
  // Total info bytes
  private int total = 0;
  // set to true if total mismatch occurs (e.g. text and element node compared)
  private boolean totalMismatch = false;
  // penalty term ($c_p in thesis)
  private static final int C= 20;

  /** Return content distance between nodes. */
  public double getDistance(Node a, Node b ) {
    try {
      if( a!=null && b!= null )
        includeNodes( a, b );
      double penalty = Math.max(0.0,1.0-((double) total)/((double) C));
      double distance= penalty+(1.0-penalty)*((double) (mismatched))/
                        ((double) total);
      return totalMismatch ? 1.0 : distance;
    } finally {
      resetDistance();
    }
  }

  // Currently not used. used to read distance without adding more nodes
  private double getDistance() {
    return getDistance(null,null);
  }

  private void resetDistance() {
    mismatched = 0;
    total = 0;
    totalMismatch = false;
  }

  // Add node pair internal to state
  private void includeNodes( Node a, Node b ) {
    if( a== null || b== null || totalMismatch)
      return;
    XMLNode ca = a.getContent(), cb = b.getContent();
    if( ca instanceof XMLElementNode && cb instanceof XMLElementNode ) {
      XMLElementNode ea = (XMLElementNode) ca,eb=(XMLElementNode) cb;
      total+=ELEMENT_NAME_INFO;
      mismatched += ea.getQName().equals(eb.getQName()) ? 0 : ELEMENT_NAME_INFO;
      Attributes aa =  ea.getAttributes(), ab = eb.getAttributes();
      for( int i=0;i<aa.getLength();i++)  {
        int index = ab.getIndex(aa.getQName(i));
        if( index != -1 ) {
          String v1 =  aa.getValue(i), v2 = ab.getValue(index);
          int amismatch = stringDist( v1,v2,1.0 );
          int info = (v1.length() > ATTR_VALUE_THRESHOLD ? v1.length() : 1 ) +
                     (v2.length() > ATTR_VALUE_THRESHOLD ? v2.length() : 1 );
          mismatched += amismatch > info ? info : amismatch;
          total+=info;
        } else {
          mismatched += ATTR_INFO;
          total += ATTR_INFO;
        }
      }
      // Scan for deleted from b
      for( int i=0;i<ab.getLength();i++) {
        if( aa.getIndex(ab.getQName(i)) == -1 ) {
          mismatched += ATTR_INFO;
          total += ATTR_INFO;
        }
      }
    } else if ( ca instanceof XMLTextNode && cb instanceof XMLTextNode ) {
      int info = ca.getInfoSize() + cb.getInfoSize() / 2,
        amismatch = stringDist( ((XMLTextNode) ca).getText(),
                                ((XMLTextNode) cb).getText(),1.0 ) / 2;
      mismatched += amismatch > info  ? info : amismatch;
      total+=info;
    } else
      totalMismatch = true;
  }
//$CUT
/*

  private  TokenComparator stringComp = new TokenComparator() {
      int getLength( Object o ) {
        return ((String) o).length();
      }
      boolean equals( Object a, int ia, Object b, int ib ) {
        return ((String) a).charAt(ia)==((String) b).charAt(ib);
      }
    };

  private TokenComparator charArrayComp = new TokenComparator() {
      int getLength( Object o ) {
        return ((char[] ) o).length;
      }
      boolean equals( Object a, int ia, Object b, int ib ) {
        return ((char[]) a)[ia] ==((char[]) b)[ib];
      }
    };

  private TokenComparator nodeChildComp = new TokenComparator() {
      int getLength( Object o ) {
        return ((Node ) o).getChildCount();
      }
      boolean equals( Object a, int ia, Object b, int ib ) {
        return ((Node) a).getChildAsNode(ia).getContent().contentEquals(
          ((Node) b).getChildAsNode(ib).getContent());
      }
    };

  private TokenComparator matchedNodeChildComp = new TokenComparator() {
      int getLength( Object o ) {
        return ((Node ) o).getChildCount();
      }
      boolean equals( Object a, int ia, Object b, int ib ) {
        return ((BaseNode) a).getChild(ia) == ((BranchNode) b).getBaseMatch();
      }
    };
*/
//$CUT

  public int stringDist( String a, String b,double limit ) {
    return qDist(a,b);
  }

  public int stringDist( char[] a, char[] b,double limit ) {
    return qDist(a,b);
///    System.out.println("A="+new String(a,0,a.length));
///    System.out.println("B="+new String(b,0,b.length));
///    System.out.println("dist="+d);
///    System.out.println("sdist="+stringDist( a, b,(int) a.length+b.length, charArrayComp ));*/
///    return d;
  }

  public double childListDistance( Node a, Node b ) {
    if( a.getChildCount()== 0 && b.getChildCount() == 0)
      return ZERO_CHILDREN_MATCH; // Zero children is also a match!
    else {
      char[] ac = new char[a.getChildCount()];
      char[] bc = new char[b.getChildCount()];
      for( int i=0;i<a.getChildCount();i++)
        ac[i]=(char) (a.getChildAsNode(i).getContent().getContentHash()&0xffff);
      for( int i=0;i<b.getChildCount();i++)
        bc[i]=(char) (b.getChildAsNode(i).getContent().getContentHash()&0xffff);
      return ((double) stringDist(ac,bc,1.0))
                       / ((double) a.getChildCount() + b.getChildCount());
/// /*      return ((double) stringDist(a,b,a.getChildCount()+b.getChildCount(),nodeChildComp))
///                       / ((double) a.getChildCount() + b.getChildCount());
///
/// */
    }
  }

  public int matchedChildListDistance( BaseNode a, BranchNode b ) {
    char[] ac = new char[a.getChildCount()];
    char[] bc = new char[b.getChildCount()];
    // NOTE! i+1's as 0=-0! (Which would yield equality for all child lists of
    // length 1). Using i instead of i+1 was a bug that took some time to find
    for( int i=0;i<a.getChildCount();i++)
      ac[i]=(char) (i+1);
    for( int i=0;i<b.getChildCount();i++) {
      BaseNode m = b.getBaseMatch();
      if( m!= null && m.getParent() == a )
        bc[i] = (char) m.getChildPos();
      else
        bc[i] = (char) -(i+1);
    }
    return stringDist(ac,bc,1.0);
  }

//$CUT
/*  // Directly adapted from [Myers86]

  private int stringDist( Object a, Object b, int max, TokenComparator tc ) {
//DBG    if( 1==1 ) return max/2;
//    if( 1==1) return (int) Math.round( Math.random()*tc.getLength(a)*.4 );
    //if( max > 50 )
    //  System.out.println("max="+max);
    //max = max > 10 ? 10 : max;
    int arraySize = 2*max+1;
    int v[] = null;
    if( true ) throw new RuntimeException("DISABLED---!");
    if( arraySize <= DISTBUF_SIZE )
      v = DISTBUF; // Use preallocated buffer (speedup!)
    else
      v = new int[2*max+1];
    int x=0,y=0;
    final int VBIAS = max, N = tc.getLength(a), M=tc.getLength(b);
    v[VBIAS+1]=0;
    for(int d=0;d<=max;d++) {
      for( int k=-d;k<=d;k+=2 ) {
        if( k==-d || ( k!=d && v[k-1+VBIAS] < v[k+1+VBIAS] ) )
          x = v[k+1+VBIAS];
        else
          x = v[k-1+VBIAS]+1;
        y=x-k;
        while( x < N && y < M && tc.equals(a,x,b,y)  ) {
          x++;
          y++;
        }
        v[k+VBIAS]=x;
        if( x >= N && y>= M )
          return d;
      }
    }
  return Integer.MAX_VALUE; // D > max
  }


  abstract class TokenComparator {
    abstract int getLength( Object o );
    abstract boolean equals( Object a, int ia, Object b, int ib );
  }
*/
//$CUT

  // q-Gram Distance [Ukkonen92]
  // The implementation could use some heavy optimization. It's really a
  // textbook example of inefficient coding...

  class Counter {
    public int count = 1;
  }

  private static final int INIT_CAPACITY=2048;

  // Which gram to use. Yes, I know its ugly to have as
  // a state var, as it changes. But it origibally didn't :)
  private static int Q=4;

  // Hsh tables used to store q-Grams
  private Map aGrams = new HashMap(INIT_CAPACITY);
  private Map bGrams = new HashMap(INIT_CAPACITY);

  protected int qDist( String a , String b ) {
    decideQ(a.length()+b.length());
    buildQGrams(a,aGrams);
    buildQGrams(b,bGrams);
    return calcQDistance();
  }

  protected int qDist( char[] a , char[] b ) {
    decideQ(a.length+b.length);
    buildQGrams(a,aGrams);
    buildQGrams(b,bGrams);
    return calcQDistance();
  }

  protected void buildQGrams(String a, Map grams) {
    grams.clear();
/// /*    if( a.length() < Q )
///         grams.put(a,new Counter());
///     else {*/
    for( int i=0;i<a.length();i++) {
    String gram = a.substring(i,i+Q > a.length() ? a.length() : i+Q );
    if( grams.containsKey(gram) )
      ((Counter) grams.get(gram)).count++;
    else
      grams.put(gram,new Counter());
    }
  }

  protected void buildQGrams(char[] a, Map grams) {
    grams.clear();
    for( int i=0;i<a.length;i++) {
      int count = i + Q > a.length ? a.length -i : Q;
      String gram = new String(a,i,count);
      if( grams.containsKey(gram) )
        ((Counter) grams.get(gram)).count++;
      else
        grams.put(gram,new Counter());
    }
  }

  // Decide with q-gram to use, based on the length of the content
  protected int decideQ( int total ) {
   int q = 1;
   if( total > 150 )
      q = 4;
    else if( total > 50 )
      q = 2;
    return q;
  }

  protected int calcQDistance() {
    int dist = 0;
    // first, loop over agrams
    for( Iterator i = aGrams.keySet().iterator();i.hasNext();) {
      Object gramA = i.next();
      int countA = ((Counter) aGrams.get(gramA)).count;
      int countB = 0;
      if( bGrams.containsKey(gramA) )
        countB = ((Counter) bGrams.get(gramA)).count;
      else
///        System.out.println("Not in B: " +gramA.toString());
      dist += Math.abs(countA-countB);
    }
    // And add any grams present in b but not in a
    for( Iterator i = bGrams.keySet().iterator();i.hasNext();) {
      Object gramB = i.next();
      if( !aGrams.containsKey(gramB) ) {
        dist += ((Counter) bGrams.get(gramB)).count;
///        System.out.println("Not in A: " +gramB.toString());
      }
    }
    return dist;
  }
}