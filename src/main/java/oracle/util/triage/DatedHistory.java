package oracle.util.triage;

import java.util.Date;
import java.io.*;

/** Representation of the <testsuite>.history.log log file.
 */

public class DatedHistory extends DatedFile implements Constants {

    private static final boolean DEBUG = false;

    private DatedHistory(long timestamp, int startLine, VirtualFile f) {
        super("history", timestamp, timestamp);
        this.startLine = startLine;
        this.endLine = startLine;
        this.dir = f.getParent();
        this.name = f.getName();
        this.url = f.getUrl();
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return dir + "/" + name;
    }

    public String getUrl() {
        return Util.makeHint(getSummary(), INFO_ICON)
             + Util.makeRangeLink(url, getName(), startLine, endLine);
    }

    public boolean isDiagnosticLog() {
        return false;
    }

    public String getSummary() {
        StringBuffer sb = new StringBuffer();
        if (top != null) {
            sb.append(top); sb.append("\n");
        }
        if (cpu != null) {
            sb.append(cpu); sb.append("\n");
        }
        if (mem != null) {
            sb.append(mem); sb.append("\n");
        }
        if (swap != null) {
            sb.append(swap); sb.append("\n");
        }
        return sb.toString();
    }


    private String name;
    private String dir;
    private String url;
    private int startLine;
    private int endLine;
    private String cpu;
    private String top;
    private String mem;
    private String swap;

    static void readDatedHistory(VirtualFile f) {
        if (suppressDatedHistory) {
            return;
        }

        if (f.getName() == null || !f.getName().endsWith(".history.log")) {
            System.out.println("SEVERE: Note a <testsuite>.history.log file! Skipping!");
            return;
        }
         
        LineNumberReader lnr = null;
        DatedHistory dh = null;

        try {
            lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(f.getInputStream())));
   
            String line = null;

            while ( (line = lnr.readLine()) != null) {
                String trim = line.trim();
                if (trim.startsWith("DATE: ")) {
                    if (dh != null) {
                        theUniverse.addChild(dh);
                        dh = null;
                    }

                    String tstamp = trim.substring("DATE: ".length()) + " " + FARM_TIMEZONE;
                    Date d = Util.parseTime5(tstamp);
                    if (d != null && d.getTime() != 0l) { 
                        dh = new DatedHistory(d.getTime(), lnr.getLineNumber(), f);
                    }
                } else if (trim.startsWith("top - ")) {
                    if (dh != null) {
                        dh.top = trim;
                   }
                } else if (trim.startsWith("Cpu")) {
                    if (dh != null) {
                        dh.cpu = trim;
                   }
                } else if (trim.startsWith("Mem: ")) {
                    if (dh != null && dh.mem == null) {
                        dh.mem = trim;
                   }
                } else if (trim.startsWith("Swap: ")) {
                    if (dh != null && dh.swap == null) {
                        dh.swap = trim;
                   }
                } else if (trim.startsWith("=====================================================================")) {
                    if (dh != null) {
                        dh.endLine = lnr.getLineNumber();
                   }
                }
            }
            if (dh != null) {
                theUniverse.addChild(dh);
                dh = null;
            }
            lnr.close();
      } catch (IOException ioe) {
          System.out.println("SEVERE: Error in reading " + f);
          ioe.printStackTrace();
          if (lnr != null) {
              try {
                  lnr.close();
              } catch (Exception e) {
                  // ignore
              }
          }
       }
    }

    static void setSuppressDatedHistory(boolean b) {
        suppressDatedHistory = b;
    }
    private static boolean suppressDatedHistory = false;

}
