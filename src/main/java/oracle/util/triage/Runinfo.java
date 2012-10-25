package oracle.util.triage;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;

/** Representation of a build.jts runtime log segment.
 */

public class Runinfo implements Constants {

    private static final boolean DEBUG = false;

    private static String INDEX_FILE   = "index.html";
    private static String SUMMARY_FILE = "summary.html";
    private static String DETAIL_FILE  = "detail.html";
    private static String CSV_FILE     = "runinfo.csv";
    private static String TRIAGE_FILE  = "triage.log";

    private static void writeIndexFile() {
        PrintStream ps = null;
        try {
            ps = new PrintStream(new FileOutputStream(new File(INDEX_FILE)));
            ps.println("<HTML><HEAD><TITLE>Run Information</TITLE></HEAD>");
            ps.println("<BODY><H1>Run Information</H1>");
            ps.println("<UL>");
            ps.println("<li><a href=\"" + SUMMARY_FILE + "\">Run Summary</a></li>");
            ps.println("<li><a href=\"" + CSV_FILE + "\">Run Summary in CSV format</a></li>");
            ps.println("<li><a href=\"" + DETAIL_FILE + "\">Run Detail</a></li>");
            ps.println("<li><a href=\"" + TRIAGE_FILE + "\">Result triage file</a></li>");
            ps.println("<li>TBD. - other log files</li>");
            ps.println("</UL>");
            ps.println("</HTML>");
            ps.close();
        } catch (IOException ioe) {
            System.out.println("SEVERE: Error writing file "+INDEX_FILE+": "+ioe);
            if (ps!=null) {
                try { ps.close(); 
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private static String[] newArguments(String[] l1, String[] l2) {
        String[] res = new String[l1.length + l2.length];
        for (int i=0; i<l1.length; i++) {
             res[i] = l1[i];
        }
        for (int i=0; i<l2.length; i++) {
             res[i + l1.length] = l2[i];
        }
        return res;
    }

    public static void main(String[] args) {
        String[] summaryArgs = new String[] {
                    "-flatten",
                    "-html", SUMMARY_FILE,
                    "-csv",      CSV_FILE,
                 };
        DatedFile.main( newArguments( summaryArgs, args) );
 
        String[] detailArgs = new String[] {
                    "-detail", DETAIL_FILE,
                    "-html", DETAIL_FILE,
                 };
        DatedFile.main( newArguments( detailArgs, args) );

        writeIndexFile();
    }

}
