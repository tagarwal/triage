package oracle.util.triage;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;

/** Representation of a single test.
 */

public class UnitTestResults extends DatedFile implements Constants {

    public UnitTestResults(UnitTest[] uts, long startTime, long endTime) {
        super("", startTime, endTime);
        tests = uts;
    }

    public boolean isTest() {
        return true;
    }

    public float getTestTime() {
        return getTime();
    } 

    public UnitTest[] getTests() {
        return tests;
    }
    private UnitTest[] tests;

    protected void toHtml(StringBuffer sb) {
        if (tests==null || tests.length==0) {
            return;
        }
        sb.append("<li id=\"n"+getNumber()+"\">");
        for (int i=0; i<tests.length; i++) {
             if (i>0) {
                 sb.append("- ");
             }
             sb.append(tests[i].getFullName());
             sb.append(" ");
             sb.append(Util.secs(tests[i].getTime()));
             if (i<tests.length-1) {
                 sb.append("<br>");
                 sb.append(NL);
             }
         }
         sb.append("</li>");
         sb.append(NL);
    }
}
