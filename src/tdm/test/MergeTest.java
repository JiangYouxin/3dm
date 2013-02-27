// $Id: MergeTest.java,v 1.2 2006-02-02 17:42:18 ctl Exp $ D
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

import junit.framework.TestCase;
import java.util.TreeMap;
import java.util.Map;
import java.util.List;
import java.io.File;
import java.util.Iterator;
import java.util.Properties;
import java.io.FileInputStream;
import tdm.lib.BaseNode;
import java.io.PrintStream;
import tdm.lib.DiffMatching;
import java.io.ByteArrayOutputStream;
import tdm.lib.BranchNode;
import tdm.lib.XMLParser;
import junit.framework.Assert;
import java.util.Arrays;
import tdm.lib.HeuristicMatching;

public class MergeTest extends TestCase {

  public static final String TEST_SET_DFILTER = System.getProperty(
      "tdm.test.merge.dirs", ".*(usecases|mergecases).*");

  public static final String TEST_SET_FFILTER = System.getProperty(
      "tdm.test.merge.file", "tdm\\.test\\.MergeTest");

  Map dirs = new TreeMap(); // Sorted for predictability of run

  int ok=0,improv=0,worse=0,fail=0;

  public MergeTest() {
    super("3dm merge test");
  }

  public void testMerge() throws Exception {
    System.out.println("Running Merge test...");
    DiffPatch.scanDataSets(dirs, DiffPatch.TEST_ROOT, TEST_SET_DFILTER,
                           TEST_SET_FFILTER);
    int COPY_TRESHOLD_ORIG = HeuristicMatching.COPY_THRESHOLD;
    for( Iterator i = dirs.entrySet().iterator(); i.hasNext();) {
      Map.Entry e = (Map.Entry) i.next();
      List l = (List) e.getValue();
      if( l.size() < 1 )
        continue;
      File dir = (File) e.getKey();
      File conffile = new File(dir,(String) l.get(0) );
      Properties p = new Properties();
      FileInputStream cin = new FileInputStream(conffile);
      p.load(cin);
      cin.close();
      for( int test=0;test<Integer.MAX_VALUE;test++) {
        String pfx = test==0 ? "" : String.valueOf(test)+".";
        if( p.getProperty(pfx+"base") == null )
          break;
        doMerge( dir, p.getProperty(pfx + "base").trim(),
                 p.getProperty(pfx + "a").trim(),
                 p.getProperty(pfx + "b").trim(),
                 p.getProperty(pfx+"facit").split("\\s+"),
                 p.getProperty(pfx+"expect").split("\\s+"),
            dir.getPath().replaceAll("[./]+",".").substring(1)+"."+test,
                 p.getProperty(pfx+"opts") == null ?
                 new String[] {} : p.getProperty(pfx+"opts").split("\\s+")
                );
         // KLUDGE: Restore initial state between invocations
         // Using a global nonfinal static for this setting was a bad idea :(
         HeuristicMatching.COPY_THRESHOLD = COPY_TRESHOLD_ORIG;
      }
    }
    System.out.println("--------------------------------------------------");
    System.out.println("Better\tOK\tWorse\tFail");
    System.out.println(""+improv+"\t"+ok+"\t"+worse+"\t"+fail);
    if( worse > 0 || fail > 0)
      System.out.println("Result became worse, please do not check in.");
    if( fail > 0 )
      Assert.fail("Test failed due to new failures.");
  }

  // Note about the maybe too tricky facit and expected commands
  // facit lists desired results in descending order of correctness
  // expected lists accepted results
  // we detect an improvement if we match a facit better than any expected
  // we did worse if we match a facit (but not <FAIL>!) worse than any expected

  public void doMerge(File dir, String basen, String an, String bn,
                      String[] facitn, String[] expectn, String id,
                      String[] opts) throws Exception {
    System.out.print(id+": ");
    File base = new File(dir, basen);
    File a = new File(dir, an);
    File b = new File(dir, bn);
    File merge = new File(DiffPatch.TEST_FOLDER_FILE, "merge.xml");
    ByteArrayOutputStream errs = new ByteArrayOutputStream();
    PrintStream err = new PrintStream(errs);
    PrintStream saved = System.err;
    try {
      System.setErr(err);
      String[] lparams =new String[] {"--merge", base.getPath(),
                                  a.getPath(), b.getPath(), merge.getPath()
      };

      String[] params = new String[opts.length+lparams.length];
      System.arraycopy(opts,0,params,0,opts.length);
      System.arraycopy(lparams,0,params,opts.length,lparams.length);
      tdm.tool.TreeDiffMerge.main(params);
      BaseNode docBase = null;
      BranchNode docA = null;
      DiffMatching m = new DiffMatching();
      XMLParser p = new XMLParser();
      docBase = (BaseNode) p.parse(merge.getPath(), m.getBaseNodeFactory());

      // Find a facit that matches
      int match = Integer.MAX_VALUE;
      for (int i = 0; i < facitn.length; i++) {
        File facit = new File(dir, facitn[i]);
        docA = (BranchNode) p.parse(facit.getPath(), m.getBranchNodeFactory());
        if (DiffPatch.treeComp(docBase, docA, false)) {
          match = i;
          break;
        }
      }
      int bestexpect = Integer.MAX_VALUE; // Best expected result
      int worstexpect = -1; // Worst expected result
      boolean exactmatch = match == Integer.MAX_VALUE;
      for( int i=0;i<expectn.length;i++) {
        if( match < Integer.MAX_VALUE &&
            expectn[i].equals( facitn[match] ) )
          exactmatch = true;
        for( int j=0;j<facitn.length;j++ ) {
          if( expectn[i].equals(facitn[j]) ) {
            bestexpect = Math.min(bestexpect, j);
            worstexpect = Math.max(worstexpect, j);
          }
        }
      }
      if( match < bestexpect ) {
        System.out.println("OK + Improved to "+facitn[match]);
        saved.println("GOOD NEWS, EVERYONE: Test "+id+" improved :).");
        improv++;
      } else if ( (worstexpect == -1 ||  match <= worstexpect)
                  && exactmatch ) {
        System.out.println("OK (by facit "+(match == Integer.MAX_VALUE ?
                           "<FAIL>" : facitn[match])+").");
        ok++;
      } else if (match==Integer.MAX_VALUE) {
        System.out.println("Regression to <FAIL>");
        saved.println("TEST FAILED: Test "+id+" regressed to failure.");
        fail++;
      } else {
        System.out.println("Regression to " + facitn[match]);
        saved.println("WARNING: Test "+id+" regressed :(.");
        worse++;
      }
      merge.delete();
    }
    catch (Exception ex) {
      err.flush();
      System.setErr(saved);
      System.out.println("3dm stderr output:");
      System.out.write(errs.toByteArray());
      System.out.println("Exception stack trace:");
      ex.printStackTrace(System.out);
      Assert.fail("3dm diff/patch excepted.");
    }
    finally {
      System.setErr(saved);
    }
  }
}
