// $Id: Main.java,v 1.5 2006-02-03 09:05:48 ctl Exp $
package tdm.editgen;
import gnu.getopt.*;

public class Main {

  private static Editgen gen = new Editgen();
  private static String mergeFile = null;

  public static void main(String[] args) {
    System.err.println("Editgen $Revision: 1.5 $" );
    // Get command line options
    int firstFileIx = parseOpts( args );
    if( args.length - firstFileIx >= 2 ) {
      String branches[] = new String[args.length-(firstFileIx+1)];
      System.arraycopy(args,firstFileIx+1,branches,0,branches.length);
      gen.editGen(args[firstFileIx],mergeFile,branches);
    } else {
      System.err.println("Usage: editgen base edited1 edited2 ...");
    }

  }

  private static int parseOpts( String args[] ) {
    LongOpt lopts[] = {
      new LongOpt("merge",LongOpt.REQUIRED_ARGUMENT,null,'m'),
      new LongOpt("editlog",LongOpt.OPTIONAL_ARGUMENT,null,'e'),
      new LongOpt("edits",LongOpt.REQUIRED_ARGUMENT,null,'n'),
      new LongOpt("probability",LongOpt.NO_ARGUMENT,null,'p'),
      new LongOpt("operations",LongOpt.REQUIRED_ARGUMENT,null,'o'),
      new LongOpt("seed",LongOpt.REQUIRED_ARGUMENT,null,'s'),
      new LongOpt("idprefix",LongOpt.REQUIRED_ARGUMENT,null,'i'),
      new LongOpt("nopretty",LongOpt.NO_ARGUMENT,null,'u'),
      new LongOpt("idoncopy",LongOpt.NO_ARGUMENT,null,'\u0101'),
      new LongOpt("idonupdate",LongOpt.NO_ARGUMENT,null,'\u0100'),
    };
    Getopt g = new Getopt("editgen", args, "n:p:o:e::s:i:m:P:", lopts);
    int c;
    String arg;
    while ((c = g.getopt()) != -1) {
      try {
        switch(c) {
          case 'e':
            gen.setEditLogPrefix(getStringArg(g,Editgen.EDIT_PREFIX));
            break;
          case 'p':
            gen.setProbability( getDoubleArg(g,Double.MAX_VALUE) );
            break;
          case 'u':
            gen.setPrettyPrint(false);
            break;
          case 'n':
            gen.setEditCount(getIntArg(g,-1));
            break;
          case 'o':
            gen.setOperations(getStringArg(g,""));
            break;
          case 'm':
            mergeFile = getStringArg(g,"");
            break;
          case 's':
            gen.setRandomGenerator(new java.util.Random((long) getStringArg(g,"").hashCode() ) );
            break;
          case 'i':
            gen.setIdPrefix(getStringArg(g,""));
            break;
            // Longopts
          case '\u0100':
            gen.setUpdateModifiesId(true);
            break;
          case '\u0101':
            gen.setCopyModifiesId(true);
            break;
        }
      } catch (IllegalArgumentException x ) {
        System.err.println("Option error: "+x.getMessage());
      }
    }
    return g.getOptind();
  }

  static String getStringArg( Getopt g, String defval ) {
    String arg = g.getOptarg();
    if( arg == null || "?".equals(arg) )
      return defval;
    else
      return arg;
  }

  static int getIntArg( Getopt g, int defval ) {
    String arg = g.getOptarg();
    if( arg == null || "?".equals(arg) )
      return defval;
    else {
      try {
        return Integer.parseInt(arg);
      } catch (Exception e) {
        return defval;
      }
    }
  }

  static double getDoubleArg( Getopt g, double defval ) {
    String arg = g.getOptarg();
    if( arg == null || "?".equals(arg) )
      return defval;
    else {
      try {
        return Double.parseDouble(arg);
      } catch (Exception e) {
        return defval;
      }
    }
  }


}
