package oracle.util.triage;

import java.io.*;

/** Representation of a build.jtl runtime log segment.
 */

public class TestResults extends DatedFile implements Constants {

    public TestResults(VirtualFile root) {
        super(root);
    }

    public String getFullName() {
        return /* getDir() + "/" + */ getName();
    }

    public boolean isTest() {
        return true;
    }

    public float getTestTime() {
        return getTime();
    } 

    public UnitTestSuite[] getSuites() {
        return suites;
    }
    public void setSuites(UnitTestSuite[] suites) {
        this.suites = suites;
    }
    private UnitTestSuite[] suites;


    public String getUrl() {
        return url;
    }
    /*private*/ String url;

    private static String computeUrl(UnitTestReport utr, VirtualFile f) {
         boolean success = (utr.getSuccs() > 0) && (utr.getDiffs() == 0); 

         String summary = ((success) ? "SUCCESS: " : "FAILURE: ") 
                          + utr.getDiffs() + " dif, " + utr.getSuccs() + " suc";
         if (!success) {
             summary += "\n";
             if (utr.getDiffs() > 0) {
                 summary = summary + utr.toString(INFO , true);
             } else {
                 summary = summary + "Build failure - check .tlg or .log file.";
             }
         }
         return Util.makeHint(summary, (success) ? CHECK_ICON : RED_X_ICON)
              + Util.makeDetailLink(f.getUrl(), f.getName()); 
    }

    static void readTestResults(VirtualFile f) {
        TestResults tr = new TestResults(f);
        tr.setEndTime(f.lastModified());
        tr.setStartTime(f.lastModified());
        if (!tr.getName().startsWith("TESTS-")) {
            // ensure we are not double-counting

            UnitTestReport utr = new UnitTestReport(f);
/*
            LineNumberReader lnr = null;
            try {
                lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(f.getInputStream())));
    
                String line=null;
                while ( (line=lnr.readLine()) != null) {
                    String trim = line.trim();
                    if (trim.startsWith("<testsuite ")) {
                         String name = trim.substring("<testsuite ".length());
                         int pos=name.indexOf("\"");
                         name = trim.substring(0,pos);
                         pos = trim.indexOf(" time=\"");
                         if (pos > 0) {
                             String time = trim.substring(pos+" time=\"".length());
                             pos = time.indexOf("\"");
                             time = time.substring(0,pos);
                             try {
                                 long delta = (long) (Float.parseFloat(time) * 1000.0f);
                                 tr.setStartTime(tr.getEndTime() - delta);
                             } catch (Exception e) {
                                 System.out.println("SEVERE: Unable to parse line "+lnr.getLineNumber()+
                                                     " from "+f+" for testsuite timing.");
                             }
                         }
                    }
                } 
                lnr.close();
            } catch (IOException ioe) {
                System.out.println("SEVERE: Error in reading "+f);
                ioe.printStackTrace();
                if (lnr!=null) {
                   try {
                       lnr.close();
                   } catch (Exception e) {
                       // ignore
                   }
                }
            }
*/
            
            float time = 0.0f;
            tr.setStartTime(tr.getEndTime() - Util.millis(utr.getTime()));

            if (showDetails()) {
                tr.setSuites(utr.getSuites());
                long timeStamp = tr.getStartTime();
                time = 0.0f;
                for (UnitTestSuite uts : utr.getSuites()) {
                     UnitSuiteResult usr = new UnitSuiteResult(uts, timeStamp + Util.millis(time));
                     time += uts.getTime();
                     tr.addChild(usr);
                }
            }

            tr.url = computeUrl(utr, f);
            theUniverse.addChild(tr);
        }
    }

}
