// $Id: XMLParser.java,v 1.10 2006-02-03 11:26:57 ctl Exp $ D
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

import org.xml.sax.XMLReader;
import org.xml.sax.Attributes;
import org.xml.sax.helpers.AttributesImpl;
import org.xml.sax.InputSource;
import org.xml.sax.helpers.XMLReaderFactory;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.SAXException;
import java.util.Stack;

/** 3DM wrapper for a generic XML SAX parser. */

public class XMLParser extends DefaultHandler {

  /** Default parser name. */
  private static final String DEFAULT_PARSER_NAME = "org.apache.xerces.parsers.SAXParser";
  private  XMLReader xr = null;

  public XMLParser() throws Exception {
    this(DEFAULT_PARSER_NAME);
  }

  /** Create new parser using supplied SAX parser class name. */
  public XMLParser( String saxParserName ) throws Exception {
    try {
      xr = XMLReaderFactory.createXMLReader();
    } catch (Exception e ) {
      xr = XMLReaderFactory.createXMLReader(saxParserName);
    }
    xr.setContentHandler(this);
    xr.setErrorHandler(this);
    try {
     xr.setFeature("http://xml.org/sax/features/namespaces",false);
     xr.setFeature("http://xml.org/sax/features/validation",false);
    } catch (SAXException e) {
     throw new Exception("Error setting features:" + e.getMessage());
    }

  }

  // Parser state
  private String currentText = null;
  private Node currentNode = null;
  private NodeFactory factory = null;
  private Stack treestack = new Stack();


  /** Parse an XML file. Returns a parse tree of the XML file.
   *  @param file Input XML file
   *  @param aFactory Factory for creating nodes in the tree.
   */
  public Node parse( String file, NodeFactory aFactory ) throws ParseException,
          java.io.FileNotFoundException, java.io.IOException {
    factory = aFactory;
    //FileReader r = new FileReader(file);
    try {
      InputSource is = new InputSource( new java.io.InputStreamReader( new java.io.FileInputStream(file), "utf-8" ));
      //is.setEncoding("utf-8");
      xr.parse(is);
    } catch ( org.xml.sax.SAXException x ) {
      throw new ParseException(x.getMessage());
    }
    Node root = currentNode;
    // Don't leave a ptr to the parsed tree; it can't be GC'd then!
    currentNode = null;
    factory = null; // forget factory and allow GC
    return root;
  }

   public void startDocument () {
     currentNode = factory.makeNode( new XMLElementNode("$ROOT$",
       new AttributesImpl() ) );
   }

   public void endDocument () {
   }

   public void startElement (String uri, String name,
                             String qName, Attributes atts) {
     if( currentText != null )
       currentNode.addChild( factory.makeNode(
        new XMLTextNode( currentText.trim().toCharArray() )  ) );
     currentText = null;
     Node n = factory.makeNode( new XMLElementNode( qName, atts ) );
     currentNode.addChild(  n  );
     treestack.push( currentNode );
     currentNode = n;
///       //         System.out.println("Start element: {" + uri + "}" + name);
   }


   public void endElement (String uri, String name, String qName)
   {
     if( currentText != null ) {
       currentNode.addChild(factory.makeNode(
           new XMLTextNode(currentText.trim().toCharArray())));
     }
     currentText = null;
     currentNode = (Node) treestack.pop();
   }


   public void characters (char ch[], int start, int length)
   {
     // The method trims whitespace from start and end of character data
     boolean lastIsWS = currentText == null || currentText.endsWith(" ");
     boolean hasNonWs = false;
     StringBuffer sb = new StringBuffer();
     for (int i = start; i < start + length; i++) {
       if (Character.isWhitespace(ch[i])) {
         if (lastIsWS)
           continue;
         sb.append(" ");
         lastIsWS = true;
       }
       else {
         sb.append(ch[i]);
         lastIsWS = false;
         hasNonWs = true;
       }
     }
     String chars = sb.toString();
     // BUGFIX060203: Whitespace-only character event may be ignored
     // The buggy code below ate all ws-only char events inside a
     // string. Removing it caused currentText to sometimes be  " ", which
     // we want to appear as currentText=null. The solution to this
     // is the hasNonWs flag
     // NOTE: The bug appeared on patching back the diff
     // usecases/reveiew/branch{1,2}. However, it only appeared on
     // one of my machines, probably due to different parser implementations
     // NOTE2: The whole whitespace mangling being done here should be
     // thrown away; it was originally a hack for handling inconsequent use of
     // whitespace in the test cases. It will probably just mess up real
     // cases.
     /*  BUGGY CODE: if (chars.trim().length() == 0)
              return;*/
     if (currentText != null)
       currentText += chars;
     else if (hasNonWs) {
       currentText = chars;
       // else do nothing, i.e. only ws will cause currentText = null
     }
   }

}
