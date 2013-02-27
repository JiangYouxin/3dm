// $Id: XMLPrinter.java,v 1.11 2006-02-06 11:57:59 ctl Exp $
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

import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.Attributes;

/**
 * Class for outputting XML. The class has two modes: prettyprint and not. In prettyprint
 * mode it indents tags and contents according to level, most likely introducing
 * additional whitespace in content. In no-prettyprint no new whitespace is introduced in
 * the content, but the output is still quite readable (i.e. not a single line).
 * The algorithm is to not introduce any linebreaks if there is any content between tags (open as
 * well as close).
 */
public class XMLPrinter extends DefaultHandler {

  private static final int STATE_CHARS = 0;
  private static final int STATE_TAG = 1;
  private int state = -1;

  int indent = 0;
  private boolean prettyPrint = false;
  private static final String IND =
  "                                                                              ";
  private java.io.PrintWriter pw = null;

/*  public XMLPrinter( java.io.PrintWriter apw ) {
    pw=apw;
  }

  public XMLPrinter( java.io.PrintWriter apw, boolean aPrettyPrint ) {
    pw=apw;
    prettyPrint = aPrettyPrint;
  }
*/

  public XMLPrinter( java.io.OutputStream out ) {
    this(out,false);
  }

  public XMLPrinter(  java.io.OutputStream out, boolean aPrettyPrint ) {
    try {
      pw=new java.io.PrintWriter( new java.io.OutputStreamWriter( out, "utf-8" ));
    } catch (java.io.UnsupportedEncodingException x ) {
      System.err.println("Internal error: unknow encoding: utf-8");
      System.exit(-1);
    }
    prettyPrint = aPrettyPrint;
  }

   ////////////////////////////////////////////////////////////////////
   // Event handlers.
   ////////////////////////////////////////////////////////////////////


   public void startDocument ()
   {
      childcounter =HAS_CONTENT;
      pw.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"+
        (prettyPrint ? "\n": ""));
      state = STATE_TAG;
   }


   public void endDocument ()
   {
       //System.out.println("End document");
     if(!prettyPrint)
       pw.println();
     pw.flush();
   }

    java.util.Stack csstack = new java.util.Stack();
   Integer childcounter = null;
   public void startElement (String uri, String name,
                             String qName, Attributes atts)

   {
    if( childcounter == null ) {
       printWithNL(">",prettyPrint );
      childcounter =HAS_CONTENT;
    }
    StringBuffer tagopen = new StringBuffer();
    if( state == STATE_TAG && !prettyPrint)
      tagopen.append("\n");
    tagopen.append('<');
    tagopen.append( qName );
  //    tagopen.append(' ');
    if( atts != null && atts.getLength() != 0 ) {
      for( int i = 0;i<atts.getLength();i++ ) {
        tagopen.append(' ');
        tagopen.append(atts.getQName(i));
        tagopen.append('=');
        tagopen.append('"');
        tagopen.append(toEntities(atts.getValue(i)));
        tagopen.append('"');
      }
    }
    csstack.push( childcounter );
    childcounter = null;
//      if( assumeNoChildren )
//        tagopen.append("/>");
//      else
//        tagopen.append('>');
    pw.print((prettyPrint ? IND.substring(0,indent) : "")  + tagopen.toString());
    indent ++;
    state = STATE_TAG;
   }


   public void endElement (String uri, String name, String qName)
   {
      indent--;
        if( childcounter == null )
          printWithNL(" />",prettyPrint);
        else {
          if( state == STATE_TAG && !prettyPrint)
            pw.println();
          printWithNL((prettyPrint ? IND.substring(0,indent) : "") + "</"+qName+">",
                      prettyPrint );
        }
      childcounter = (Integer) csstack.pop();
      state = STATE_TAG;
   }

   protected void printWithNL( String s, boolean appendNL ) {
     if( appendNL )
       pw.println(s);
      else
      pw.print(s);
   }

   final Integer HAS_CONTENT = new Integer(0);

   public void characters (char ch[], int startpos, int length) {
     state = STATE_CHARS;
     if (childcounter != HAS_CONTENT)
       printWithNL(">", prettyPrint);
     childcounter = HAS_CONTENT;
     if (length == 0)
       return;
     String chars = toEntities(ch, startpos, length);
     /*
            int start=0,next=-1;
            do {
       next=chars.indexOf("\n",start);
       if( next==-1)
         pw.println(chars.substring(start));
       else {
         pw.println(chars.substring(start,next));
         start=next+1;
       }
            } while( next != -1 );*/
     printWithNL(chars, prettyPrint);
     //System.err.println("OUT:"+chars);
   }

   private static String toEntities(String str) {
      if (str.length() == 0) {
          return ""; // avoid instance for empty strings.
      }
      char data[] = str.toCharArray();
      return toEntities(data, 0, data.length);
   }

   private static String toEntities(char data[], int off, int len) {
      if (len == 0) {
          return ""; // avoid instance for empty strings.
      }
      StringBuffer b = new StringBuffer();
      int end = off + len;
      int scan = off;
      while (scan < end) {
         char c = data[scan++];
         switch (c) {
         case '&': b.append(data, off, scan - off - 1).append("&amp;"); break;
         case '<': b.append(data, off, scan - off - 1).append("&lt;"); break;
         case '>': b.append(data, off, scan - off - 1).append("&gt;"); break;
         case '\'': b.append(data, off, scan - off - 1).append("&apos;"); break;
         case '"': b.append(data, off, scan - off - 1).append("&quot;"); break;
         default: continue;
         }
         off = scan;
      }
      if (off < scan) {
         b.append(data, off, scan - off);
      }
      return b.toString();
   }

   public void print( Node root ) {
     print( root, false );
   }

   public void print( Node root, boolean fragment ) {
     XMLNode c = root.getContent();
     if( !fragment )
       startDocument();
     if( c instanceof XMLTextNode ) {
       char[] text = ( (XMLTextNode) c).getText();
       characters( text, 0, text.length);
     } else {
       startElement("","",((XMLElementNode) c).getQName(),((XMLElementNode) c).getAttributes());
       for( int i=0;i<root.getChildCount();i++) {
         print(root.getChildAsNode(i),true);
       }
       endElement("","",((XMLElementNode) c).getQName());
     }
     if( !fragment )
       endDocument();
   }
}
