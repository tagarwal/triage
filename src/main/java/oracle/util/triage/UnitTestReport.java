package oracle.util.triage;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.InputStreamReader;

public class UnitTestReport implements Constants {

    /** 
     Produce a unit test report from a "virtual" file.
     */
    public UnitTestReport(VirtualFile vf) {
        fullName = vf.getFullName();
        if (fullName == null) {
            fullName = vf.toString();
        }

        name = vf.getName();
        if (name == null) {
           name = fullName;
        }

        this.stats = new Stats();

        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new InputStreamReader(vf.getInputStream()));
            readUnitTestFile(lnr);
        } catch (Exception e) {
            System.out.println("SEVERE: error opening " + vf.getFullName() + ": " + e);
            e.printStackTrace();
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (Exception e2) {
                    // ignore
                }
            }
        } 
    }

    /** 
     Produce a unit test report from a file.
     */
    public UnitTestReport(String n, Stats stats) {
        this(new File(n), stats);
    }

    /** 
     Produce a unit test report from a file.
     */
    public UnitTestReport(File f, Stats stats) {
        fullName = f.toString();
        name = f.getName();
        if (name == null) {
           name = fullName;
        }
        this.stats = stats;

        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new FileReader(f));
            readUnitTestFile(lnr);
        } catch (Exception e) {
            System.out.println("SEVERE: error opening " + f + ": " + e);
            e.printStackTrace();
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (Exception e2) {
                    // ignore
                }
            }
        } 
    }

    protected Stats stats;

    protected static Stats globalStats = new Stats();

    public static Stats getGlobalStats() {
        return globalStats;
    }

    public static void resetGlobalStats() {
        globalStats = new Stats();
    }

    /** 
     Produce a unit test report from an entry
     in a JAR file.
     */
    public UnitTestReport(JarFile jf, JarEntry je, Stats stats, String pref) {
        name = je.getName();
        this.stats = stats;
 
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(
                  new InputStreamReader(
                      jf.getInputStream(je)));
            readUnitTestFile(lnr);
        } catch (Exception e) {
            System.out.println("SEVERE: error opening entry " + je + " from JAR file " + jf + ": " + e);
            e.printStackTrace();
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (Exception e2) {
                    // ignore
                }
            }
        }
    }

    private String name;
    private String fullName;
    private String jarfileName;

    protected void readUnitTestFile(LineNumberReader lnr) {
        String line = null;
        String origLine = null;

        currentSuite = new UnitTestSuite();
        currentTest = new UnitTest();
        boolean currentSuiteWasArchived = false;
        try {
            while ( (line = lnr.readLine()) != null) {
                origLine = line;
                line = line.trim();

                if (line.startsWith("<testsuite ")) {
                    currentSuite = new UnitTestSuite();
                    currentSuiteWasArchived = false;
                    line = readTestSuite(lnr, line.substring("<testsuite ".length()));
                } 

                if (line.startsWith("</testsuite>")) {
                    suites.add(currentSuite); 
                    currentSuiteWasArchived = true;
                }

                if (line.startsWith("<testcase ")) {
                    currentTest = new UnitTest();
                    line = readTestCase(lnr, line.substring("<testcase ".length()));
                } 

                if (line.startsWith("</testcase>")) {
                    currentSuite.addTest(currentTest); 
                }

                if (line.startsWith("<failure ")) {
                    readDiff(lnr, line.substring("<failure ".length()), "</failure>");
                }

                if (line.startsWith("<error ")) {
                    readDiff(lnr, line.substring("<error ".length()), "</error>");
                }

                if (line.startsWith("<property ")) {
                    line = readProperty(lnr, line.substring("<property ".length()));
                }

                if (line.startsWith("<system-out>")) {
                    readStdOut(line);
                }

                if (line.startsWith("<system-err>")) {
                    readStdErr(line);
                }
            }
            lnr.close();


        } catch (Exception e) {
            System.out.println("SEVERE: error processing " + getName() + ": " + e + "\n" +
                          " at " + getName() + ":" + ((lnr == null) ? " " : (lnr.getLineNumber() + ": ")) + origLine);

            e.printStackTrace();
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (Exception e2) {
                    // freeing resorces - ignore
                }
            }
        }

        if (!currentSuiteWasArchived) {
            suites.add(currentSuite); 
            currentSuiteWasArchived = true;
        }

        if (Main.getSlowest() > 0) {
            for (int i = 0; i < suites.size(); i++) {
                globalStats.addSuite(suites.get(i));
                stats.addSuite(suites.get(i));
            }
        }

    }

    private UnitTestSuite currentSuite;
    private UnitTest currentTest;
    protected List<UnitTestSuite> suites = new ArrayList<UnitTestSuite>();

    public UnitTestSuite[] getSuites() {
        UnitTestSuite[] res = new UnitTestSuite[suites.size()];
        for (int i = 0; i < suites.size(); i++) {
            res[i] = suites.get(i);
        }
        return res;
    }

    private String readProperty(LineNumberReader lnr, String l)
        throws IOException {
        HashMap<String, String> hm = new HashMap<String, String>();
        String res = parseAttributes(lnr, l, hm);
        currentSuite.addProperty(hm.get("name"), hm.get("value"));
        return res;
    }

    private String readTestCase(LineNumberReader lnr, String l)
        throws IOException {
        HashMap<String, String> hm = new HashMap<String, String>();
        String res = parseAttributes(lnr, l, hm);

        currentTest.setName(hm.get("name"));
        currentTest.setClassname(hm.get("classname"));
        currentTest.setTime(hm.get("time"));
        currentTest.setSuccess(true);

        return res;
    }

    private void readDiff(LineNumberReader lnr, String line, String endToken) 
        throws IOException {
        HashMap<String, String> hm = new HashMap<String, String>();
        StringBuffer message = new StringBuffer();
        int pos = 0;

        line = parseAttributes(lnr, line, hm);
        currentTest.setSuccess(false);
        currentTest.setShortMessage(hm.get("message"));
        currentTest.setType(hm.get("type"));


        while ( line != null && (pos = line.indexOf(endToken)) < 0) {
            message.append(line);
            message.append(NL);
            line = lnr.readLine();
        }

        if (pos >= 0) {
            message.append(line.substring(0, pos));
        }

        currentTest.setMessage(message.toString());
    }

    private String readTestSuite(LineNumberReader lnr, String l)
        throws IOException {
        HashMap<String, String> hm = new HashMap<String, String>();
        String res = parseAttributes(lnr, l, hm);

        int diffsFound = Integer.parseInt(hm.get("errors"));
        diffsFound += Integer.parseInt(hm.get("failures"));
        int total = Integer.parseInt(hm.get("tests"));
        String lName = hm.get("name");
        String pakkage = hm.get("package");
        String id = "id";
        float time = Float.parseFloat(hm.get("time"));

        currentSuite.setName(lName);
        currentSuite.setPackage(pakkage);
        currentSuite.setDiffs(diffsFound);
        currentSuite.setSuccs(total - diffsFound);
        currentSuite.setId(id);
        currentSuite.setTime(time);

        reportTime += time;

        return res;
    }

    private String readStdOut(String l) {
        return l;
    }

    private String readStdErr(String l) {
        return l;
    }


    private String parseAttributes(LineNumberReader lnr, String line, HashMap<String, String> hm)
        throws IOException {
        int pos = 0;
        while ( (pos = line.indexOf("=\"")) > 0) {
            String lName = line.substring(0, pos).trim();
            line = line.substring(pos + "=\"".length());
            pos = line.indexOf("\"");
            while (pos < 0) {
                String nextLine = lnr.readLine();
                if (nextLine == null) {
                    // This is a corrupt file. Force-close the property!
                    nextLine="\"";
                }
                line = line + "\\n" + nextLine;
                pos = line.indexOf("\"");
            }
            String value = line.substring(0, pos);
            line = line.substring(pos + 1);
            hm.put(lName, value);
        }

        line = line.trim();
        if (line.startsWith(">")) {
            line = line.substring(1);
        }

        return line;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return fullName;
    }

    public String getJarfileName() {
        return name;
    }

    public float getTime() {
        return reportTime;
    }
    private float reportTime;

    public int getSuccs() {
        if (succs < 0) {
            init();
        }
        return succs;
    }

    public int getDiffs() {
        if (diffs < 0) {
            init();
        }
        return diffs;
    }

    public int getDiffSuites() {
        if (diffSuites < 0) {
            init();
        }
        return diffSuites;
    }

    protected void init() {
        diffs = 0;
        diffSuites = 0;
        succs = 0;
        for (int i = 0; i < suites.size(); i++) {
            succs += suites.get(i).getSuccs();
            int d = suites.get(i).getDiffs();
            if (d > 0) {
                diffs += d;
                diffSuites++;
            }
        }
    }
    private int diffs = -1;
    private int diffSuites = -1;
    private int succs = -1;


    public String toString(int level, boolean diffOnly) {
        Stats st = new Stats();

        if (diffs < 0 || succs < 0 || diffSuites < 0) {
            init();
        }

        if (Main.getSlowest() > 0) {
            for (int i = 0; i < suites.size(); i++) {
                st.addSuite(suites.get(i));
            }
        }

        if (diffOnly && getDiffs() == 0 && !Main.showTimings() && Main.getSlowest() <= 0) {
            return null;
        }

        boolean showDetail = INFO < level;
        boolean showFullDetail = FINE < level;

        StringBuffer sb = new StringBuffer();

        String title = "--- SUMMARY OF " + getName() + " ---";
        StringBuffer tb = new StringBuffer();
        for (int i = 0; i < title.length(); i++) {
            tb.append('-');
        }

        if (showDetail) {
            sb.append(NL);
            sb.append(tb);
            sb.append(NL);
        }
        sb.append(title);
        if (Main.showTimings()) {
            sb.append(" ");
            sb.append(getTime());
            sb.append(" secs");
        }
        sb.append(NL);
        if (showDetail) {
            sb.append(tb);
            sb.append(NL);
        }

        if (suites.size() != 1) {
            sb.append(getDiffSuites());
            if (getDiffSuites() == 1) {
                sb.append(" suite has " + JUNIT_DIFFS + " (out of ");
            } else {
                sb.append(" suites have " + JUNIT_DIFFS + " (out of ");
            }
            sb.append(suites.size());
            if (suites.size() == 1) {
                sb.append(" suite). ");
            } else  {
                sb.append(" suites). ");
            }
        }

        sb.append("Total: ");
        sb.append(diffs);
        if (diffs == 1) {
            sb.append(" " + JUNIT_DIFF + ", ");
        } else {
            sb.append(" " + JUNIT_DIFFS + ", ");
        }
        sb.append(succs);
        if (succs == 1) {
            sb.append(" " + JUNIT_SUCC + ".");
        } else {
            sb.append(" " + JUNIT_SUCCS + ".");
        }
        sb.append(NL);

        if (showDetail) {
            sb.append(NL);
        }

        int count = (suites.size() <= 1 ) ? -1 : 0;

        for (int i = 0; i < suites.size(); i++) {
            if (suites.get(i).getDiffs() > 0 
                || showFullDetail
                || (Main.getSlowest() > 0 && count >= 0)) {

                count++;
                String sstr = suites.get(i).toString(level, count, diffOnly);
                if (sstr != null) {
                    sb.append(sstr);
                }

                if (Main.getSlowest() > 0 && count > 0) {
                    Stats st2 = new Stats();
                    st2.addSuite(suites.get(i));
                    String slowest = st2.toString();
                    if (slowest != null) {
                        sb.append("> Slowest tests in subsuite <");
                        sb.append(NL);
                        sb.append(slowest);
                        sb.append(NL);
                    }
                }
            }
        }

        if (showDetail) {
            sb.append("---  End SUMMARY OF ");
            sb.append(getName());
            sb.append(" ---");
        }
        return sb.toString();
    }

    public String toHtml(int level, boolean diffOnly) {
        if (diffs < 0 || succs < 0 || diffSuites < 0) {
            init();
        }

        if (diffOnly && getDiffs() == 0) {
            return null;
        }


        List<String> ssl = new ArrayList<String>();

        int count = (suites.size() <= 1 ) ? -1 : 0;
        for (int i = 0; i < suites.size(); i++) {
            if (suites.get(i).getDiffs() > 0) {
                count++;
                String sstr = suites.get(i).toHtml(level, count, diffOnly);
                if (sstr != null) {
                    ssl.add(sstr);
                }
            }
        }


        StringBuffer sb = new StringBuffer();
        // sb.append("<h3>Triage of " + getName() + "</h3>" + NL);

        if (suites.size() != 1) {
            sb.append(getDiffSuites());
            if (getDiffSuites() == 1) {
                sb.append(" suite has " + JUNIT_DIFFS + " (out of ");
            } else {
                sb.append(" suites have " + JUNIT_DIFFS + " (out of ");
            }
            sb.append(suites.size());
            if (suites.size() == 1) {
                sb.append(" suite). ");
            } else  {
                sb.append(" suites). ");
            }
        }

        if (ssl.size() > 1) {
            sb.append("Total: ");
            sb.append(diffs);
            if (diffs == 1) {
                sb.append(" " + JUNIT_DIFF + ", ");
            } else {
                sb.append(" " + JUNIT_DIFFS + ", ");
            }
            sb.append(succs);
            if (succs == 1) {
                sb.append(" " + JUNIT_SUCC + ".");
            } else {
                sb.append(" " + JUNIT_SUCCS + ".");
            }
            sb.append("<br>" + NL);

            StringBuffer ssb = new StringBuffer();
            for (int i = 0; i < ssl.size(); i++) {
                 if (ssl.get(i) != null && !ssl.get(i).equals("")) {
                    ssb.append(Util.makeLi(ssl.get(i)));
                 }
            }
            sb.append(Util.makeUl(ssb.toString()));
        } else if (ssl.size() == 1) {
            sb.append(ssl.get(0));
        } else {
            return null;
        }

        return sb.toString();
    }


    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {

            String base = args[i].substring(0, args[i].length() - XML_SUFFIX.length());

            UnitTestReport ut = new UnitTestReport(args[i], new Stats());
            // ut.readUnitTestFile(args[i]);
            // ut.htmlReport("./report", base + ".html");

            System.out.println(ut.toString(FINE, false));
        }
    }
}
