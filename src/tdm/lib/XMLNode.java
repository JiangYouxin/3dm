// $Id: XMLNode.java,v 1.12 2006-02-06 11:39:38 ctl Exp $ D
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
import java.io.IOException;
import org.xml.sax.SAXException;

/** Class for storing content of XML nodes. Supports fast equality comparison
 *  using MD5 hash codes, and automatic calculation of node infoSize. */

public abstract class XMLNode implements Cloneable {

  protected int infoSize = 0;

  public XMLNode() {
  }

  public int getInfoSize() {
    return infoSize;
  }

  public abstract boolean contentEquals( Object a );
  /** Get 32-bit hash code */
  public abstract int getContentHash();

  protected MessageDigest getMD() {
    try{
      return MessageDigest.getInstance("MD5");
    } catch ( java.security.NoSuchAlgorithmException e ) {
      System.err.println("MD5 hash generation not supported -- aborting");
      System.exit(-1);
    }
    return null;
  }

  protected byte[] calculateHash(char[] data) {
    MessageDigest contentHash = getMD();
   contentHash.reset();
    for( int i=0;i<data.length;i++) {
      contentHash.update((byte) (data[i]&0xff));
      contentHash.update((byte) (data[i]>>8));
    }
    return contentHash.digest();
  }

  protected byte[] calculateHash(String data) {
    MessageDigest contentHash = getMD();
    contentHash.reset();
    for( int i=0;i<data.length();i++) {
      contentHash.update((byte) (data.charAt(i)&0xff));
      contentHash.update((byte) (data.charAt(i)>>8));
    }
    return contentHash.digest();
  }

  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException x ) {
      return null;
    }
  }

  public interface Externalizer {
    public void startNode(XMLNode n) throws IOException, SAXException;
    public void endNode(XMLNode n) throws IOException, SAXException;
  }

  public interface Merger {
    /** Merge contents. Only called if both branch 1 and 2 differ from base */
    public XMLNode merge( XMLNode baseNode, XMLNode aNode, XMLNode bNode );
  }
}
