// $Id: XMLTextNode.java,v 1.14 2006-02-03 16:04:20 ctl Exp $
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

import java.security.MessageDigest;

/** Stores XML element nodes. */
public class XMLTextNode extends XMLNode {

  private char[] text=null;
  private byte[] cHash = null;

  public XMLTextNode( String srctext ) {
    this( srctext.toCharArray() );
  }

  public XMLTextNode( char[] srctext ) {
    this( srctext,0,srctext.length);
  }

  public XMLTextNode( char[] srctext, int first, int length ) {
    text = new char[length];
    System.arraycopy(srctext,first,text,0,length);
//    System.out.println("NEW TN:"+new String(text));
    cHash = calculateHash(text);
    infoSize = text.length > Measure.TEXT_THRESHOLD ? text.length -
                Measure.TEXT_THRESHOLD : 1;
  }

  public boolean contentEquals( Object a ) {
    if( a instanceof XMLTextNode )
      return MessageDigest.isEqual(cHash,((XMLTextNode) a).cHash);
    else
      return false;
  }

  public char[] getText() {
    return text;
  }

  public void setText(char[] aText) {
    text = aText;
  }

  public String toString() {
    return new String(text);
  }

  public int getContentHash() {
    return cHash[0]+(cHash[1]<<8)+(cHash[2]<<16)+(cHash[3]<<24);
  }

  public Object clone() {
    return super.clone();
  }

}