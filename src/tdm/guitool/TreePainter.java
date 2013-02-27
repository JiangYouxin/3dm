// $Id: TreePainter.java,v 1.3 2003-01-09 13:38:45 ctl Exp $
package tdm.guitool;
// 3dm lib
import tdm.lib.*;

import java.awt.*;
import java.awt.print.*;
import java.io.*;

/**
 * Title:        3DM
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:      HUT
 * @author Tancred Lindholm
 * @version 1.0
 */

public class TreePainter extends Frame {

  Node root = null;
  public TreePainter(Node aRoot, boolean show, OutputStream file ) {
    root = aRoot;
    setLayout(new BorderLayout());
    add( new TreeCanvas(aRoot, new StdDrawer()), BorderLayout.CENTER );
    setBounds(0,0,500,500);
    addWindowListener( new java.awt.event.WindowAdapter() {
      public void windowClosing( java.awt.event.WindowEvent e ) {
        e.getWindow().dispose();
        System.exit(0);
      }
    });
    if( file != null ) {
      PrintWriter pw = new PrintWriter( file );
      pw.println("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
      pw.println("<svg fill-opacity=\"1\" color-interpolation=\"sRGB\" color-rendering=\"auto\" text-rendering=\"auto\" stroke=\"black\" stroke-linecap=\"square\" width=\"1000\" stroke-miterlimit=\"10\" stroke-opacity=\"1\" shape-rendering=\"auto\" fill=\"black\" stroke-dasharray=\"none\" font-weight=\"normal\" stroke-width=\"1\" height=\"1000\" font-family=\"&apos;Times&apos;\" font-style=\"italic\" stroke-linejoin=\"miter\" font-size=\"12\" image-rendering=\"auto\" stroke-dashoffset=\"0\">");
//      pw.println("<!DOCTYPE svg PUBLIC \"-//W3C//DTD SVG 20000303 Stylable//EN\" \"http://www.w3.org/TR/2000/03/WD-SVG-20000303/DTD/svg-20000303-stylable.dtd\">");
//      pw.println("<svg xml:space=\"preserve\" style=\"shape-rendering:geometricPrecision; text-rendering:geometricPrecision; image-rendering:optimizeQuality\" viewBox=\"0 0 1000 1000\">");
      pw.println("<g>");
      (new TreeCanvas(aRoot,new SVGDrawer(pw))).paint(null);
      pw.println("</g>");
      pw.println("</svg>");
      pw.flush();
    }
  }

  class TreeCanvas extends Canvas implements Printable {
    int counter = 0,leafno=0;
    Node root = null;
    PrimitiveDrawer pd = null;

    TreeCanvas( Node aRoot, PrimitiveDrawer aPd) {
      super();
      root = aRoot;
      pd = aPd;
    }

    public int print(Graphics g, PageFormat pf, int pi)
                                   throws PrinterException {
     if (pi >= 1) {
         return Printable.NO_SUCH_PAGE;
     }
//     ((Graphics2D) g).translate(20,20);
//     ((Graphics2D) g).scale(0.5,0.4);

     paint(g);
     return Printable.PAGE_EXISTS;
   }

    public void paint( Graphics g ) {
      visitNode( root,0,0,false,g);
      visitNode( root,0,0,true,g);
    }

    int visitNode( Node n, int leftLeaves, int depth, boolean drawNodes, Graphics g ) {
      if( n.getChildCount() == 0 ) {
        // leaf
        if(drawNodes)
           pd.drawNode( n, (double) leftLeaves, depth, g );
        return leftLeaves + 1;
      } else {
        // parent
        double[] leafPos = new double[n.getChildCount()];
        int startLeft = leftLeaves;
        // draw child trees
        for( int i=0;i<n.getChildCount();i++) {
          int oldLeft = leftLeaves;
          leftLeaves = visitNode( n.getChildAsNode(i), leftLeaves, depth + 1, drawNodes, g);
          leafPos[i] = ((double) (oldLeft+leftLeaves-1))/2.0;
        }
        // draw lines or main
        double nodePos =  ((double) (startLeft+leftLeaves-1))/2.0;
        if(drawNodes)
           pd.drawNode( n, nodePos , depth, g );
        else {
          for( int i=0;i<n.getChildCount();i++)
             pd.drawLine( depth, nodePos, leafPos[i], g );
        }
        return leftLeaves;
     }
    }
  }

  class PrimitiveDrawer {
    static final int XO = 20;
    static final int YO = 20;
    static final int LEAFSTEP = 16;
    static final int DEPTHSTEP = 64;
    static final int NODERADIUS = 5;

    protected int translateX( double x) {
      return (int) (x*LEAFSTEP+XO);
    }

    protected int translateY( int y) {
      return y*DEPTHSTEP+YO;
    }

    public void drawNode( Node n, double x, int y, Graphics g ){};
    public void drawLine( int depth, double parentPos, double childPos, Graphics g ){};
  }

  class StdDrawer extends PrimitiveDrawer {
   final Color C_NODEFILL = Color.white;

    public void drawNode( Node n, double x, int y, Graphics g ) {
      g.setColor(Color.white);
      g.fillOval(translateX(x)-NODERADIUS,translateY(y)-NODERADIUS,2*NODERADIUS,2*NODERADIUS);
      g.setColor(Color.black);
      g.drawOval(translateX(x)-NODERADIUS,translateY(y)-NODERADIUS,2*NODERADIUS,2*NODERADIUS);

      XMLNode c = n.getContent();
      String text =null;
      if( c instanceof XMLElementNode )
        text = ((XMLElementNode) c).getQName();
      else if( c instanceof XMLTextNode )
        text = new String(((XMLTextNode) c).getText());
      g.drawString(text,translateX(x)-NODERADIUS,translateY(y)+NODERADIUS);
    }

    public void drawLine( int depth, double parentPos, double childPos, Graphics g ) {
      g.drawLine(translateX(parentPos),translateY(depth),translateX(childPos),translateY(depth+1));
    }

  }

  class SVGDrawer extends PrimitiveDrawer {
    static final int TXTXO = -3;
    static final int TXTYO = +4;
    static final int XO = 20;
    static final int YO = 20;
    static final int LEAFSTEP = 24;
    static final int DEPTHSTEP = 40;
    static final int NODERADIUS = 10;

    protected int translateX( double x) {
      return (int) (x*LEAFSTEP+XO);
    }

    protected int translateY( int y) {
      return y*DEPTHSTEP+YO;
    }

    PrintWriter w = null;
    SVGDrawer( PrintWriter aw ) {
      w = aw;
    }

    public void drawNode( Node n, double x, int y, Graphics g ) {
      int cx = translateX(x), cy = translateY(y);
//      w.println("<circle cx=\""+cx+"\" cy=\""+cy+
//                "\" r=\""+NODERADIUS+"\" stroke=\"black\" fill=\"white\" />");
      w.println("<circle cx=\""+cx+"\" cy=\""+cy+
                "\" r=\""+NODERADIUS+"\" style=\"fill:#ffffff;stroke:#000000;stroke-width:1\" />");

      XMLNode c = n.getContent();
      String text =null;
      if( c instanceof XMLElementNode )
        text = ((XMLElementNode) c).getQName();
      else if( c instanceof XMLTextNode )
        text = new String(((XMLTextNode) c).getText());
//      w.println("<text x=\""+(cx+TXTXO)+"\" y=\""+(cy+TXTYO)+"\">"+text+"</text>");
      w.println("<text x=\""+(cx+TXTXO)+"\" y=\""+(cy+TXTYO)+"\" style=\"fill:#000000;font-weight:normal;font-size:12;font-family:'Times New Roman'\" >"+text+"</text>");


   }

    public void drawLine( int depth, double parentPos, double childPos, Graphics g ) {
//      w.println("<line x1=\""+translateX(parentPos)+"\" y1=\"" +translateY(depth)+
//                    "\" x2=\"" + translateX(childPos)+"\" y2=\"" + translateY(depth+1) + "\" />");
      w.println("<line x1=\""+translateX(parentPos)+"\" y1=\"" +translateY(depth)+
                    "\" x2=\"" + translateX(childPos)+"\" y2=\"" + translateY(depth+1) + "\" " +
                    "style=\"fill:none;stroke:#000000;stroke-width:1\" />");

    }
  }

}
