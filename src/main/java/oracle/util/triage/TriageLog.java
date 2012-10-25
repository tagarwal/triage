package oracle.util.triage;

import java.util.StringTokenizer;
import java.io.File;
import java.io.LineNumberReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.BufferedReader;

/**
 This obtains information on a Farm job.
 */


public class TriageLog implements Constants {

    TriageLog(String triageFile) {
        this.triageFile = triageFile;
    }
    private String triageFile;

    public String getTriageSummary() {
        if (summary == null) {
            populateSummaries();
        }
        return summary;
    }

    public String getShortTriageSummary() {
        if (shortSummary == null) {
            populateSummaries();
        }
        return shortSummary;
    }

    private void populateSummaries() {
        StringBuffer sb = new StringBuffer();
        StringBuffer shortSb = new StringBuffer();
        File f = new File(triageFile);
        LineNumberReader lnr = null;
        int numDiffs = 0;
        int numSucs = 0;
        try {
            lnr = new LineNumberReader(new BufferedReader(new FileReader(f)));
            String line = null;
            int pos = 0;
            int runningCount = 0;
            while ((line = lnr.readLine()) != null) {

                // Processing:
                //    *** SUITE 1752949-j2ee_srg 18 difs, 152 sucs.  ***

                if (line.startsWith("*** ") && line.endsWith(".  ***") 
                    && line.indexOf("dif") > 0
                    && line.indexOf("suc") > 0) {
                    StringTokenizer st = new StringTokenizer(line);
                    while (st.hasMoreTokens()) {
                       String token = st.nextToken();
                       if (Util.isNumeric(token)) {
                           int num = Integer.parseInt(token);
                           if (st.hasMoreTokens()) {
                               token = st.nextToken();
                               if (token.startsWith("dif")) {
                                   numDiffs = num;
                               } else if (token.startsWith("suc")) {
                                   numSucs = num;
                               }
                            }
                        }
                    }
                    // sb.append(numDiffs + " difs, " + numSucs + " sucs\n");

                // Processing:
                //    1. t.adventure_builder.build.dif - DIF ANALYSIS etc, etc.

                } else if ((pos = line.indexOf(". ")) > 0 && pos < 6
                      && Util.isNumeric(line.substring(0,pos))) {
                    int num = Integer.parseInt(line.substring(0,pos));
                    if (num == runningCount + 1) {
                        runningCount++;
                        if ((pos = line.indexOf(".dif ")) > 0) {
                            String info = line.substring(0,pos + ".dif".length()) + "\n";
                            sb.append(info);
                            shortSb.append(info);
                        } else if (line.endsWith(".dif")) {
                            sb.append(line + "\n");
                            shortSb.append(line + "\n");
                        }
                    }

                // Processing:
                //    (10) client.test.SubjPropTestCase::testGetCallerPrincipal_withPropagatedSubjectFromClient
                //    SYNOPSIS: javax.naming.NamingException: Error reading etc, etc.

                } else if (line.startsWith("(")
                            && (pos = line.indexOf(") ")) > 0 && pos < 6
                            && Util.isNumeric(line.substring(1,pos))
                            && line.indexOf("::") > 0) {
                    sb.append(" - " + line.substring(pos + ") ".length()));
                    sb.append("\n");
                    String line2 = lnr.readLine();
                    if (line2 != null
                        && line2.startsWith("SYNOPSIS: ")) {
                        sb.append(" ==> ");
                        sb.append(line2.substring("SYNOPSIS: ".length()));
                    }
                    sb.append("\n");
                 }
            }
        } catch (Exception exn) {
            System.out.println("SEVERE: error reading triage file " + f + ": " + exn);
        } finally {
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (IOException exn) {
                    // ignore
                }
            }
        }
        summary = (numDiffs == 0) ? "" : sb.toString();
        shortSummary = (numDiffs == 0) ? "" : shortSb.toString();
    }
    private String summary = null;
    private String shortSummary = null;
}
