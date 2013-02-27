// $Id: DiffPatch.java,v 1.2 2006-02-02 17:42:17 ctl Exp $ D
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

package tdm.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import junit.framework.Assert;
import junit.framework.TestCase;
import tdm.lib.BaseNode;
import tdm.lib.BranchNode;
import tdm.lib.DiffMatching;
import tdm.lib.XMLParser;

/** Tests diff and patch functionality. The test scans for directories
 * that have test files in them, and then runs a diff+patch cycle for
 * each pair of files in each directory. The patched file is then compared
 * to the original using 3dm's tree model (tdbm.lib.*Nodes)
 */

public class DiffPatch extends TestCase {

  public static final File TEST_FOLDER_FILE = new File(System.getProperty(
      "tdm.test.tmp", "tmp"));

  public static final File TEST_ROOT = new File(System.getProperty(
      "tdm.test.root", "."));

  public static final String TEST_SET_DFILTER = System.getProperty(
      "tdm.test.diffpatch.dirs", ".*(usecases|mergecases).*");

  public static final String TEST_SET_FFILTER = System.getProperty(
      "tdm.test.diffpatch.files", ".*\\.(xml|html)");

  Map dirs = new TreeMap(); // Sorted for predictability of run

  public DiffPatch() {
    super("3dm diff-patch cycle test");
  }

  public void testDiffPatch() throws Exception {
    System.out.println("Running diff+patch test...");
    Assert.assertTrue(TEST_FOLDER_FILE.exists() || TEST_FOLDER_FILE.mkdirs());
    scanDataSets(dirs,TEST_ROOT,TEST_SET_DFILTER,TEST_SET_FFILTER);
    for( Iterator i = dirs.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry) i.next();
      List l = (List) e.getValue();
      for( int j=0;j<l.size()*l.size();j++) {
        int f1 = j/l.size();
        int f2= j%l.size();
        if( f1 != f2 )
          diffPatch((File) e.getKey(),(String) l.get(f1),(String) l.get(f2));
      }
    }
  }

  public void diffPatch( File root, String f1, String f2) throws IOException {
    System.out.println(""+root+": "+f1+","+f2);
    ByteArrayOutputStream errs = new ByteArrayOutputStream();
    PrintStream err = new PrintStream(errs);
    PrintStream saved = System.err;
    File src=(new File(root,f1));
    File dst=(new File(root,f2));
    File diff = (new File(TEST_FOLDER_FILE,"diff.xml"));
    File patched = (new File(TEST_FOLDER_FILE,"patched.xml"));
    try {
      System.setErr(err);
      tdm.tool.TreeDiffMerge.main(new String[]
        {"--diff",
         src.getPath(),
         dst.getPath(),
         diff.getPath()
      });
      /*{
        // Uncomment  and edit diff.xml when stopped to test test :)
        if (Math.random() > 0.9)
          Thread.sleep(30000);
      }*/
      tdm.tool.TreeDiffMerge.main(new String[]
        {"--patch",
         src.getPath(),
         diff.getPath(),
         patched.getPath()
      });
      BaseNode docBase=null;
      BranchNode docA=null;
      DiffMatching m = new DiffMatching();
      XMLParser p = new XMLParser();
      docBase = (BaseNode) p.parse(patched.getPath(), m.getBaseNodeFactory());
      docA = (BranchNode) p.parse( dst.getPath(),m.getBranchNodeFactory());
      treeComp(docBase,docA,true);
      System.out.println("OK! (Sizes: base="+src.length()+
                         ", dest="+dst.length()+
                         ", diff="+diff.length()+
                         ", patchedDiffToBase="+(patched.length()-dst.length()));
      // Test that rdiff is the empty diff
      diff.delete();
      patched.delete();
    } catch ( Exception ex ) {
      err.flush();
      System.setErr(saved);
      System.out.println("3dm stderr output:");
      System.out.write(errs.toByteArray());
      System.out.println("Exception stack trace:");
      ex.printStackTrace(System.out);
      Assert.fail("3dm diff/patch excepted.");
    } finally {
      System.setErr(saved);
    }
  }

  public static boolean treeComp(BaseNode n, BranchNode m, boolean validate ) {
    if( !n.getContent().contentEquals(m.getContent()) ) {
      Assert.assertFalse("Differing content.", validate);
      return false;
    }
    if( n.getChildCount() != m.getChildCount() ) {
      Assert.assertFalse("Differing child count.",validate);
      return false;
    }
    for( int i = 0; i< n.getChildCount();i++) {
      if( !treeComp(n.getChild(i), m.getChild(i), validate ) )
        return false;
    }
    return true;
  }

  public static void scanDataSets(Map dirs, File root, String df, String ff) {
    if( root.isDirectory() ) {
      File e[] = root.listFiles();
      for( int i=0;i<e.length;i++) {
        if(e[i].getPath().matches(df) )
           scanDataSets(dirs, e[i],df,ff);
      }
    } else if (root.getName().matches(ff ) ) {
      File dir = root.getParentFile();
      List l = (List) dirs.get(dir);
      if( l== null ) {
        l= new LinkedList();
        dirs.put(dir,l);
      }
      l.add(root.getName());
      Collections.sort(l); // Well, a little overkill to do it every time
    }
  }
}
