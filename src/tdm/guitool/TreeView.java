// $Id: TreeView.java,v 1.7 2003-01-09 13:38:45 ctl Exp $
// PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE PROTO CODE
package tdm.guitool;

// 3dm lib
import tdm.lib.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.print.*;

import java.util.*;
import java.io.*;

// BATIK
import org.apache.batik.svggen.SVGGraphics2D;
import org.apache.batik.dom.GenericDOMImplementation;
import org.w3c.dom.Document;
import org.w3c.dom.DOMImplementation;

/**
 * Title:        Tree Diff and Merge quick proto 1
 * Description:
 * Copyright:    Copyright (c) 2000
 * Company:      HUT
 * @author Tancred Lindholm
 * @version 1.0
 */

public class TreeView extends Frame  {

  private TreeCanvas tc = null;

  public TreeView( BaseNode r1, BranchNode r2 ) {
//    root = r;
    setBounds(100,100,1100,800);
    setLayout( new BorderLayout() );
    ScrollPane sp = new ScrollPane();
    //if( opTree == null ) {
      tc = new MappingCanvas( r1, r2 );
      tc.root = r1;
/*    } else {
      tc = new TreeCanvas();
      tc.root = opTree;
    //}
*/
    tc.setSize(1000,5000);
    sp.add(tc);
    Panel p = new Panel();
    Button btnPrint = new Button("Print");
    Button btnSVG = new Button("SVG");
    add( sp, BorderLayout.CENTER );
    add( p, BorderLayout.SOUTH );
    p.add(btnPrint);
    p.add(btnSVG);
    addWindowListener( new WindowAdapter() {
        public void windowClosing( WindowEvent e ) {
          dispose();
          System.exit(0);
        }
      });
    btnPrint.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e ) {
        PrinterJob pj = PrinterJob.getPrinterJob();
                PageFormat pf = pj.pageDialog(pj.defaultPage());
     Paper p = pf.getPaper();
     p.setImageableArea( 12,12,p.getWidth()-12,p.getHeight()-12);
    pf.setPaper(p);

        pj.setPrintable(tc,pf);
         if (pj.printDialog()) {
             try {
                 pj.print();
             } catch (Exception ex) {
                 ex.printStackTrace();
             }
         }
      }
    }
    );

    btnSVG.addActionListener( new ActionListener() {
      public void actionPerformed( ActionEvent e ) {
        // Get a DOMImplementation
        DOMImplementation domImpl =
            GenericDOMImplementation.getDOMImplementation();

        // Create an instance of org.w3c.dom.Document
        Document document = domImpl.createDocument(null, "svg", null);

        // Create an instance of the SVG Generator
        SVGGraphics2D svgGenerator = new SVGGraphics2D(document);

        // Ask the test to render into the SVG Graphics2D implementation
        tc.paint(svgGenerator);

        // Finally, stream out SVG to the standard output using UTF-8
        // character to byte encoding
        boolean useCSS = false; // we want to use CSS style attribute
        try {
          Writer out = new OutputStreamWriter(System.out, "UTF-8");
          svgGenerator.stream(out, useCSS);
        } catch (Exception x) {
        }
      }
    });
  }


  class TreeCanvas extends Canvas implements Printable {
    int counter = 0,leafno=0;
    Object root = null;
    Color tagColor = new Color( 0,127,0);

    public int print(Graphics g, PageFormat pf, int pi)
                                   throws PrinterException {
     if (pi >= 1) {
         return Printable.NO_SUCH_PAGE;
     }
     ((Graphics2D) g).translate(20,20);
     ((Graphics2D) g).scale(0.5,0.4);

     paint(g);
     return Printable.PAGE_EXISTS;
   }

    public void paint( Graphics g ) {
      counter = 0;
      leafno=3;
      if( root instanceof Node )
        drawTree( (Node) root ,0,g);
      else {
        depthDelta = 128;
        //drawOpTree( (MapNode) root,0,g);
      }
    }

    // Draw control vars
    int depthDelta = 32;
    boolean mirror = false;

    protected void makePoint( Object n, int x, int y ) {
    }

    public int drawTree( Object m, int depth, Graphics g ) {
/*      counter++;
      if( counter > 5000 )
        return leafno;
 */
      if(m == null )
        return 0;
      XMLNode n = ((Node) m).getContent();
      if( n instanceof XMLTextNode ) {
        int x = depth * depthDelta, y= leafno * 12 + 6;
        g.drawOval(x-4,y-4,8,8);
        makePoint( m, x, y);
        String text = ((XMLTextNode) n).toString();
        if( text.length() > 10 )
          text = text.substring(0,9) + "...";
        if( mirror )
          g.drawString(text,x-5-g.getFontMetrics().stringWidth(text),y+4);
        else
          g.drawString(text,x+5,y+4);
        leafno++;
        return y;
      } else  {
        // it's an element node
        // first draw the children
        int x=0,y=0;
        //ElementNode en = (ElementNode) n;
/*        Vector children = null;
        if( n instanceof ElementNode )
          children = ((ElementNode) n).children;
        else if( n instanceof AreaNode )
          children = ((AreaNode) n).children; */
        Node n2 = (Node) m;
        if( n2.getChildCount() > 0 ) {
          int[] ys = new int[n2.getChildCount()];
          for( int i =0;i< n2.getChildCount();i++) {
            ys[i] = drawTree( n2.getChildAsNode(i) , depth + (mirror ? -1 : 1), g );
          }
          int midy = (ys[0] + ys[ys.length-1])/2;
          for( int i =0;i< n2.getChildCount();i++) {
            g.drawLine(depth*depthDelta,midy,(depth + (mirror ? -1 : 1))*depthDelta,ys[i]);
          }
          x = depth * depthDelta;
          y= midy;
        } else {
          x= depth * depthDelta;
          y = leafno * 12 + 6;
          leafno++;
        }
        // then myself
  //      int x = depth * 64, y= midleaf * 12 + 6;
        g.setColor( Color.black );
        g.drawRect(x-4,y-4,8,8);
        makePoint( m, x, y);
        g.setColor( tagColor );
        String label = ((XMLElementNode) n).getQName();
        if( mirror )
          g.drawString( label, x-5-g.getFontMetrics().stringWidth(label), y+4);
        else
          g.drawString( label, x+5, y+4);
        g.setColor( Color.black );
        return y;
      }
    }


  }


  class MappingCanvas extends TreeCanvas {
    private Node rootA, rootB;
    private HashMap nodesMap = null;
    private HashSet visibleMappings = new HashSet();

    abstract class VisibleMapping {
      abstract public void  draw( Graphics g );
    }

    class LineMapping extends VisibleMapping {
      private int x1,y1,x2,y2;
      int type;
      LineMapping( Point p1, Point p2, int atype ) {
        x1 = p1.x; y1 = p1.y;
        x2 = p2.x; y2 = p2.y;
        type=atype;
      }

      public void draw( Graphics g ) {
        switch(type) {
          case 1:g.setColor(Color.green);
                break;
          case 2:g.setColor(Color.cyan);
                break;
          case 3:g.setColor(Color.blue);
                break;
          default: throw new RuntimeException("Invalid type");
        }
//        g.setColor(Color.blue);
        g.drawLine(x1,y1,x2,y2);
      }
    }

    class NoMapping extends VisibleMapping {
      private int x1,y1;
      NoMapping( Point p1 ) {
        x1 = p1.x; y1 = p1.y;
      }

      public void draw( Graphics g ) {
        g.setColor(Color.red);
              g.drawLine( x1-4,y1-4,x1+4,y1+4);
              g.drawLine( x1+4,y1-4,x1-4,y1+4);
      }
    }


    public MappingCanvas( BaseNode aRootA, BranchNode aRootB ) {
      super();
      rootA = aRootA;
      rootB = aRootB;

      addMouseListener( new  MouseAdapter () {
        public void mouseClicked( MouseEvent e ) {
          // Find clicked on node
          if( nodesMap == null )
            return;
          Node n = null;
          Point p = e.getPoint();
          Graphics g = getGraphics();
          g.setColor(Color.blue);
          for( Iterator i = nodesMap.keySet().iterator();i.hasNext();) {
            Node temp = (Node) i.next();
            Point p2 = (Point) nodesMap.get(temp);
            int dx = p2.x-p.x, dy = p2.y-p.y;
            if( dx*dx+dy*dy < 16 ) {
              n = temp;
              p = p2;
              break;
            }
          }
          if( n != null ) {
            int mc = 0;
            if( n instanceof BaseNode ) {
              BaseNode nb = (BaseNode) n;
              for( java.util.Iterator i = nb.getLeft().getMatches().iterator();i.hasNext();) {
                BranchNode br = (BranchNode) i.next();
                Point dst = (Point) nodesMap.get( br );
                VisibleMapping vm = new  LineMapping(p,dst,br.getBaseMatchType());
                visibleMappings.add(vm);
                vm.draw(g);
                mc ++;
//                m.printCorr( n,n2);
              }
            } else {
              BranchNode br = (BranchNode) n;
              if( br.getBaseMatch() != null ) {
                Point dst = (Point) nodesMap.get( br.getBaseMatch()  );
                VisibleMapping vm = new  LineMapping(p,dst,br.getBaseMatchType());
                visibleMappings.add(vm);
                vm.draw(g);
                mc++;
              }
            }
            if( mc == 0 ) {
              VisibleMapping vm = new  NoMapping(p);
              visibleMappings.add(vm);
              vm.draw(g);
            }
          }
          g.setColor(Color.black);
        }
      }
      );
    }

    protected void makePoint( Object n, int x, int y ) {
        //System.out.println("Added "+x+" "+y);
     if( nodesMap != null ) {
        nodesMap.put(n,new Point(x,y));
     }

    }

    public void paint( Graphics g ) {
      nodesMap = new HashMap();
      leafno=3;
      mirror = false;
      drawTree( rootA ,0,g);
      leafno=3;
      mirror = true;
      drawTree( rootB ,32, g);
      for( Iterator i = visibleMappings.iterator();i.hasNext();)
        ((VisibleMapping) i.next()).draw(g);

    }


  }

}
