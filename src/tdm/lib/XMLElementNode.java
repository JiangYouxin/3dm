// $Id: XMLElementNode.java,v 1.14 2006-02-03 16:04:20 ctl Exp $ D
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
import java.util.Vector;
import java.util.Hashtable;
import java.util.Map;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.Attributes;
import java.security.MessageDigest;

/** Stores XML element nodes. */
public class XMLElementNode extends XMLNode {

/// //Probably not needed  public String nameSpace = null;
  private String name = null;
  private AttributesImpl attributes = null;
  private int nHashCode = -1;
  private byte[] attrHash = null;
//$CUT
//PROTO CODE
/*  public XMLElementNode( String aname,  Map attr ) {
    name = aname;
    attributes = new AttributesImpl();
    if( attr ==null )
      return;
    java.util.Iterator iter = attr.keySet().iterator();
    while( iter.hasNext() ) {
      String key = (String) iter.next();
      attributes.addAttribute("","",key,"",(String) attr.get(key));
    }
    makeHash();
  }
*/
//PROTO CODE ENDS
//$CUT

  public XMLElementNode( String aname, Attributes attr ) {
    name = aname;
    attributes = new AttributesImpl( attr );
    rehash();
  }

  public void rehash() {
    nHashCode = name.hashCode();
    infoSize = Measure.ELEMENT_NAME_INFO;
    MessageDigest md = getMD();
    for( int i=0;i<attributes.getLength();i++) {
      int vsize = attributes.getValue(i).length();
      infoSize += Measure.ATTR_INFO + (vsize > Measure.ATTR_VALUE_THRESHOLD ? vsize -
         Measure.ATTR_VALUE_THRESHOLD : 1 );
      md.update( calculateHash( attributes.getQName(i) ) );
      md.update( calculateHash( attributes.getValue(i) ) );
    }
    attrHash = md.digest();
  }

  /** DUMMY! Always returns "" */
  public String getNamespaceURI() {
    return "";
  }

  /** DUMMY! Always returns "" */
  public String getLocalName() {
    return "";
  }

  public String getQName() {
    return name;
  }

  public void setQName(String aName) {
    name=aName;
  }

  public Attributes getAttributes() {
    return attributes;
  }

  public void setAttributes(Attributes atts) {
    attributes=new AttributesImpl( atts );
  }

  public String toString() {
    StringBuffer sb = new StringBuffer();
    sb.append(name);
    sb.append(" {");
    if( attributes != null && attributes.getLength() > 0) {
      for( int i = 0;i<attributes.getLength();i++) {
        sb.append(' ');
        sb.append(attributes.getQName(i) );
        sb.append('=');
        sb.append(attributes.getValue(i));
      }

    }
    sb.append('}');
    return sb.toString();
  }

  public boolean contentEquals( Object a ) {
    if( a instanceof XMLElementNode )
      return ((XMLElementNode) a).nHashCode == nHashCode &&
       MessageDigest.isEqual(((XMLElementNode) a).attrHash,attrHash);
    else
      return false;
  }

//$CUT
//POSSIBLY NOT NEEDED---
/*
  public boolean compareAttributes( Attributes a, Attributes b ) {
    if( a==b )
      return true; // Either both are null, or point to same obj
    if( a==null || b== null)
      return false; // either is null, the other not
    if( a.getLength() != b.getLength() )
      return false; // Not equally many
    for( int i = 0; i<a.getLength(); i ++ ) {
      if( !a.getURI(i).equals(b.getURI(i)) ||
          !a.getLocalName(i).equals(b.getLocalName(i)) ||
          !a.getQName(i).equals(b.getQName(i)) ||
          !a.getType(i).equals(b.getType(i)) ||
          !a.getValue(i).equals(b.getValue(i)) )
        return false;
    }
    return true;
  }
*/
//ENDPOSSIBLY
//$CUT

  public int getContentHash() {
    return (attrHash[0]+(attrHash[1]<<8)+(attrHash[2]<<16)+(attrHash[3]<<24))^nHashCode;
  }

  public Object clone() {
    XMLElementNode clone = (XMLElementNode) super.clone();
    clone.attributes =  new AttributesImpl(attributes);
    return clone;
  }
}