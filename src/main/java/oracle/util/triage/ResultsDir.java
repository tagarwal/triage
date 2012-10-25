package oracle.util.triage;

import java.util.List;
import java.util.ArrayList;

import java.io.File;


/**
 This class implements a diff analysis for MATs tests.
 */


public class ResultsDir implements Constants {

    public ResultsDir(String directory, String suite) {
      this(new File(directory), suite);
    }

    public ResultsDir(File directory, String suite) {
        rootDirectory = directory;
        setSuite(suite);
    }
    private File rootDirectory;
    private String suite;

    public File getRootDirectory() {
        return rootDirectory;
    }

    public int getFarmJob() {
        initResultsDir();
        return farmJob;
    }
    private int farmJob;


    public File getBuildFile() {
        initResultsDir();
        return buildFile;
    }
    private File buildFile;

    private void initResultsDir() {
        if (initResultsDir) {
            return;
        }
        File parent = rootDirectory.getParentFile();
        File build = new File(parent.toString() + "/build/compile.log");
        if (build.exists() && build.canRead()) {
            buildFile = build;
        }
        String txnId = parent.getName();
        int pos =txnId.lastIndexOf("_T");
        if (pos > 0) {
            String job = txnId.substring(pos + "_T".length());
            try {
                farmJob = Integer.parseInt(job);
            } catch (Exception e) {
                // ignore - cannot get jobId
            }
        }
        initResultsDir = true;
    }
    private boolean initResultsDir = false;

    public String getSuite() {
        if (getSuiteDescriptors() != null) {
            String alias = null;
            if ( (alias = getSuiteDescriptors().getDescriptor(suite)) != null) {
                return alias + " [=" + suite + "]";
            }
        }

        return suite;
    }
    private boolean processedSuite = false; 

    public void setSuite(String suite) {
        this.suite = suite;
    }

    public String getSuiteDesignation() {
        return suiteDesignation;
    }

    public void setLevel(int level) {
        this.level = level;
    }

    public int getLevel() {
        return level;
    }

    public void setTestDescriptors(TestDescriptors testDescriptors) {
        this.testDescriptors = testDescriptors;
    }

    public TestDescriptors getTestDescriptors() {
        return testDescriptors;
    }

    public void setSuiteDescriptors(SuiteDescriptors suiteDescriptors) {
        this.suiteDescriptors = suiteDescriptors;
    }

    public SuiteDescriptors getSuiteDescriptors() {
        return suiteDescriptors;
    }

    public void setSuiteDesignation(String suiteDesignation) {
        this.suiteDesignation = suiteDesignation;
    }

    public void triageResults() throws Exception {

        if (Main.getArchiveFile() != null 
            && Main.getArchiveFile() != DEFAULT_ARCHIVE_FILE
            && Main.getHtmlStream() != null) {
            int pos = Main.getHtmlFileName().lastIndexOf(".");
            browserFile = Main.getHtmlFileName().substring(0, pos) + "-files.html";
            messageFile = Main.getHtmlFileName().substring(0, pos) + "-messages.html";
            incidentFile = Main.getHtmlFileName().substring(0, pos) + "-incidents.html";
            timingFile  = Main.getHtmlFileName().substring(0, pos) + "-timing.html";
            if (Main.showBaseline()) {
                baselineFile = Main.getHtmlFileName().substring(0, pos) + "-basedelta.html";
            }
            archive = Main.getArchiveFile();
        }

        if (incidentFile != null) {
          // generate incident report before preparing top level report in
          // order to determine number of incidents found. this will be
          // reported on the top page if there's at least one incident
          System.out.println("INFO: Creating HTML file " + incidentFile + 
                             " with details of incidents.");
          incidentCount = IncidentReporter.createIncidentReport(archive,
                                                                incidentFile);
          Gzip.gzip(new File(incidentFile));
        }

        if (Main.isSummaryDetail()) {
            triageResults(INFO);
           
            DatedFile.resetStaticVariables();
            DiagnosticEvent.resetStaticVariables();
            ToplevelAnalyzer.resetStaticVariables();
            ToplevelTest.resetStaticVariables();
            UnitTestReport.resetGlobalStats();

            if (Main.getHtmlStream() != null) {
                Main.closeHtml();
            }

            Main.getOut().println();
            Main.getOut().println("The detail triage is appended below.");
            Main.getOut().println();
            triageResults(FINE);
        } else {
            triageResults(getLevel());
        }

        if (browserFile != null) {
            System.out.println("INFO: Creating HTML file " + browserFile + " for browsing result areas.");
            FileBrowser.createFileBrowser(archive, browserFile);
            Gzip.gzip(new File(browserFile));

            System.out.println("INFO: Creating HTML file " + messageFile + " with chronological message repository.");
            MessageRepository.createMessageFile(archive, 
                                                messageFile, 
                                                incidentFile);
            Gzip.gzip(new File(messageFile));

            System.out.println("INFO: Generating timing details into " + timingFile);
            Main.resetStaticVariables();
            Main.setFull(true);
            String[] args = new String[] {
                "-timing",  archive,
                "-detail",
                "-html", timingFile,
            };
            Main.main(args);
            Gzip.gzip(new File(timingFile));
        }
    }

    private String browserFile = null;
    private String messageFile = null;
    private String incidentFile = null;
    private int incidentCount = 0;
    private String timingFile = null;
    private String baselineFile = null;
    private String archive = null;

    private void triageResults(int level) throws Exception {
        List<File> dirsToCheck = new ArrayList<File>();

        UnitTestReport.resetGlobalStats();

        dirsToCheck.add(getRootDirectory());
        File[] files = getRootDirectory().listFiles(Util.DIR_FILES);
        for (int i = 0; files != null && i < files.length; i++) {
            dirsToCheck.add(files[i]);
        }
    
        List<File> testFiles = new ArrayList<File>();
        for (int i = 0; i < dirsToCheck.size(); i++) {
            files = dirsToCheck.get(i).listFiles(Util.SUC_DIF_FILES);
            for (int j = 0; files != null && j < files.length; j++) {
                testFiles.add(files[j]);
            }
        }

        File[] tests = new File[testFiles.size()];
        for (int i = 0; i < testFiles.size(); i++) {
            tests[i] = testFiles.get(i);
        }

        int succs = 0;
        int diffs = 0;
        List<String> diffNames = new ArrayList<String>();
      
        for (int i = 0; i < tests.length; i++) {
            if (tests[i].getName().endsWith(SUCC_SUFFIX)) {
                succs++;
            } else {
                diffs++;
            }
        }

        String sucsAndDifs = diffs +
                        ((diffs == 1) ? " dif, " : " difs, ") + succs +
                        ((succs == 1) ? " suc" : " sucs");
        String header = ((getSuiteDesignation() == null)
                        ? "" : (getSuiteDesignation() + " ") ) +
                        "SUITE " + getSuite() + " " + sucsAndDifs + ". "; 
        String textHeader = header;

        if (SEVERE < level) {
            textHeader = "*** " + header + " ***";
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < textHeader.length(); i++) {
                sb.append('*');
            }
            textHeader = sb.toString() + NL + textHeader + NL + sb.toString();
        }
        if (Main.getHtmlStream() != null) {
            Main.getOut().println("HTML version: " + Main.getHtmlUrl());
        }
        Main.getOut().print(textHeader);

        if (Main.getHtmlStream() != null) {
            Main.getHtmlStream().println("<HTML><HEADER><TITLE>" + header + "</TITLE><BODY><H1>" + header + "</H1>" + NL);
            Main.getHtmlStream().println(HEADER_INCLUDES);
        }

        if (SEVERE < level) {
            Main.getOut().println();
            Main.getOut().println("Results at: " + getRootDirectory());
            if (diffs > 0) {
                // 
            } else if (succs > 0) {
                Main.getOut().print("No diffs. ");
            } else if (Main.getBuildLog() != null) {
                Main.getOut().println("Build failure. Triaging build log.");
                Main.getOut().print( (new BuildLog(Main.getBuildLog(), Main.getArchive())).printLastSteps() );
                Main.getOut().print(" ");
            } else {
                Main.getOut().print("Build failure. ");
            }
        }

        if (Main.getHtmlStream() != null) {

            if (diffs == 0 && succs > 0) {
                Main.getHtmlStream().print("<large><b>No diffs.</b></large><p>" + NL);
            } else if (diffs > 0) {
                Main.getHtmlStream().print("<large><b>Test failure: " + sucsAndDifs + "</b></large><p>" + NL);
            } else {
                Main.getHtmlStream().println("<large><b>Build failure.</b></large><p>" + NL);
            }

            if (getFarmJob() > 0) {
                Main.getHtmlStream().print("<b>Farm job: " + Util.makeJobLink(getFarmJob()) + "</b><br>" + NL);
            }
            Main.getHtmlStream().print("<b>Farm results:</b> <code>" + getRootDirectory() + "</code><br>" + NL);


            boolean haveContent = false;
            Main.getHtmlStream().print(" <b>Links: [[</b>");
            if (browserFile != null) {
                if (haveContent) { Main.getHtmlStream().print(" <b>|</b>"); };
                Main.getHtmlStream().print(" <a href=\"" + Util.makeRelativeUrl(browserFile) + "\">browse result area</a>");
                haveContent = true;
            }
            if (baselineFile != null) {
                if (haveContent) { Main.getHtmlStream().print(" <b>|</b>"); };
                Main.getHtmlStream().print(" <a href=\"" + Util.makeRelativeUrl(baselineFile) + "\">show baseline log delta</a>");
                haveContent = true;
            }
            if (messageFile != null) {
                if (haveContent) { Main.getHtmlStream().print(" <b>|</b>"); };
                Main.getHtmlStream().print(" <a href=\"" + Util.makeRelativeUrl(messageFile) + "\">messages</a>");
                haveContent = true;
            }
            if (timingFile != null) {
                if (haveContent) { Main.getHtmlStream().print(" <b>|</b>"); };
                Main.getHtmlStream().print(" <a href=\"" + Util.makeRelativeUrl(timingFile) + "\">timing</a>");
                haveContent = true;
            }
            {
                if (haveContent) { Main.getHtmlStream().print(" <b>|</b>"); };
                Main.getHtmlStream().print(" <a href=\"" + Util.makeRelativeUrl(Main.getTextUrl()) + "\">text version</a>");
                haveContent = true;
            }
            Main.getHtmlStream().println(" <b>]]</b><p>" + NL);

            // Main.getHtmlStream().print("<b>Files:</b><br>");
            Main.getHtmlStream().print("<table>");
            if (incidentFile != null && incidentCount > 0)
            {
              Main.getHtmlStream().print("<tr><td><b>Incidents</b></td><td>");
              Main.getHtmlStream().print("<a href=\"" + Util.makeRelativeUrl(incidentFile) + "\">" + incidentCount + "</a>");
              Main.getHtmlStream().print("</td</tr>");
            }

            if (Main.getTextUrl() != null) {
                Main.getHtmlStream().print("<tr><td><b>Farm log</b></td><td>");
                // String consoleOutput = Main.getTextUrl();
                // System.out.println("consoleOutput = " + consoleOutput);
                // System.out.println("makeRelativeUrl(consoleOutput) = " + Util.makeRelativeUrl(consoleOutput));
                String consoleOutput = "/" + getRootDirectory().getName() + ".farm.out";
                Main.getHtmlStream().print(consoleOutput);
                Main.getHtmlStream().print("</td></tr>" + NL);
            }

            if (getBuildFile() != null) {
                Main.getHtmlStream().print("<tr><td><b>Build log</b></td>");
                Main.getHtmlStream().print("<td>/compile.log</td></tr>");
            }

            if (Main.getTestLogicFiles() != null) {
                boolean firstLine = true;
                for (String tlFile : Main.getTestLogicFiles()) {
                     Main.getHtmlStream().print("<tr><td>" + ((firstLine) ? "<b>TestLogic</b>" : "") + "</td><td>" + tlFile + "</td></tr>" + NL);
                     firstLine = false;
                }
            }

            Main.getHtmlStream().print("</table><p>" + NL);

            Main.getHtmlStream().println("<b>Results:</b> ");
            Main.getHtmlStream().println("<a href=\"#\" onclick=\"expandTree('tree1'); return false;\">Expand all</a> - ");
            Main.getHtmlStream().println("<a href=\"#\" onclick=\"collapseTree('tree1'); return false;\">collapse all</a><br>");
            Main.getHtmlStream().println("<ul class=\"mktree\" id=\"tree1\">" + sucsAndDifs + NL);

            if (diffs == 0 && succs == 0 && Main.getBuildLog() != null) {
                Main.getHtmlStream().print(
                        Util.makeLi("Last step in build log" + NL +
                           Util.makeUl( 
                              Util.makeLi(
                                Util.makeHtml(true, "pre", (new BuildLog(Main.getBuildLog(), Main.getArchive())).printLastSteps())
                            )    )  )  );
            }
        }

        if (INFO <= level) {
            Main.getOut().println();
        }

        if (INFO < level || diffs > 0) {
            sortFiles(tests);
        }

        int count = 0;
        for (int i = 0; i < tests.length; i++) {
            if (ToplevelTest.wasSeen(tests[i])) {
                // skip this one
            } else if (tests[i].getName().endsWith(DIFF_SUFFIX)) {
                count++;
                triageTest(count, tests[i], level);
            } else {
                if (Main.getHtmlStream() != null || INFO < level) {
                    count++;
                    triageTest(count, tests[i], level);
                } else if (Main.showTimings() || Main.getSlowest() > 0) {
                    count++;
                    triageTest(count, tests[i], level);
                }
            }
        }

        if (tests.length > 0) {
            // Triage TestLogic catchall
            triageTest(tests.length + 1, null, tests[tests.length - 1].getParentFile(), level);
        }

        if (Main.getSlowest() > 0) {
            String slow = UnitTestReport.getGlobalStats().toString();
            if (slow != null) {
                if (level < INFO) {
                    Main.getOut().println();
                }
                Main.getOut().println(">>>> SLOWEST TESTS OVERALL <<<<");
                Main.getOut().println(slow);
            }
        }

       
        if (Main.showTimings()) {
            Main.getOut().println(getTime());
        } else if (level < INFO && Main.getSlowest() <= 0) {
            Main.getOut().println();
        }

        if (Main.getHtmlStream() != null) {
            Main.getHtmlStream().println("</ul>" + NL);
            Main.getHtmlStream().println("</BODY></HTML>" + NL);
        }
    }

    private static void sortFiles(File[] files) {
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < files.length - 1; i++) {
                if (compare(files[i], files[i + 1]) > 0) {
                    File tmp = files[i];
                    files[i] = files[i + 1];
                    files[i + 1] = tmp;
                    sorted = false;
                }
            }
        }
    }

    private static int compare(File f, File g) {
        if (f.getName().endsWith(DIFF_SUFFIX)) {
            if (g.getName().endsWith(SUCC_SUFFIX)) {
                return -1;
            }
        } else if (f.getName().endsWith(SUCC_SUFFIX)) {
            if (g.getName().endsWith(DIFF_SUFFIX)) {
                return 1;
            }
        }
        return f.getName().compareTo(g.getName());
    }

    private void triageTest(int no, File f, int level) {
        triageTest(no, f, null, level);
    }

    private void triageTest(int no, File f, File fParent, int level) {
        if (level == SEVERE) {
            if (Main.showTimings() || Main.getSlowest() > 0) {
                try {
                    ToplevelAnalyzer da = new ToplevelAnalyzer(level, f, fParent,
                                                               Main.getDiffsOnly(level),
                                                               Main.getArchive());
                    addTime(da);
                    Main.getOut().println(no + ". " + da.getTest().getName());
                } catch (Exception exn) { 
                     // ignore
                    exn.printStackTrace();
                }
            } else if (f != null && f.getName().endsWith(DIFF_SUFFIX)) {
                Main.getOut().println(no + ". " + f.getName());
            } else if (f == null) {

                // TestLogic Catchall check

                try {
                    ToplevelAnalyzer da = new ToplevelAnalyzer(level, f, fParent,
                                                               Main.getDiffsOnly(level),
                                                               Main.getArchive());
                    addTime(da);
                    if (da.getTest().isDiff()) {
                        Main.getOut().println(no + ". " + da.getTest().getName());
                    }
                } catch (Exception exn) { 
                     // ignore
                    exn.printStackTrace();
                }
            }
            return;
        }

        try {
            ToplevelAnalyzer da = new ToplevelAnalyzer(level, f, fParent,
                                                       Main.getDiffsOnly(level),
                                                       Main.getArchive());
            addTime(da);

            boolean hasDiff = (f == null) ? da.getTest().isDiff()
                                        :  f.getName().endsWith(DIFF_SUFFIX);

            if (f == null && !hasDiff) {
                // this is the TestLogic Catchall category
                // when there is no diff to report we just bail out!
                return;
            }

            if (INFO < level 
                || hasDiff
                || Main.showTimings() 
                || Main.getSlowest() > 0) {


                if (no > 0) {
                    Main.getOut().print(no);
                    Main.getOut().print(". ");
                }

                Main.getOut().print( (f == null) 
                                      ? "TestLogic Catchall"
                                      : f.getName() );
            }
 
            if (Main.showTimings()) {
                Main.getOut().print(" time: " + da.getTime() + " secs");
            }


            if (hasDiff) {
                if (INFO < level && getTestDescriptors() != null) {
                    String baseName = (f == null) ? "TestLogic Catchall"
                                                : f.getName().substring(0, f.getName().length() - DIFF_SUFFIX.length());
                    TestDescriptors.Descriptor d = getTestDescriptors().getDescriptor(baseName);
                    if (d != null) {
                        Main.getOut().println(" contact: " + d.getPointOfContact() + ", component: " + d.getComponent() +
                                  " - " + d.getDescription());
                    }
                }
            }

            String s = da.printTest();
         
            if (hasDiff) {

                if (Main.getHtmlStream() != null) {
                    Main.getHtmlStream().println(da.printHtml(no));
                }

                if (s == null || s.trim().equals("")) {
                    Main.getOut().print(" - Dif analysis N/A. ");
                    Main.getOut().println("See original dif at: " + f);
                } else {
                    Main.getOut().print(" - DIF ANALYSIS");
                    Main.getOut().println(s);
                }
            } else {
                if (Main.getHtmlStream() != null) {
                    Main.getHtmlStream().println(da.printHtml(no));
                }

                if (s == null || s.trim().equals("")) {
                    Main.getOut().println();
                } else {
                    Main.getOut().print(" -");
                    if (FINE < level) {
                        Main.getOut().print(" DETAILS");
                        Main.getOut().println(s);
                    } else {
                        Main.getOut().print(s);
                    }
                }
            }

            if (Main.getSlowest() > 0) {
                String stats = da.getStats().toString();
                if (stats != null) {
                    Main.getOut().println(">>> Slowest tests in top-level suite " + f.getName() + " <<<");
                    Main.getOut().println(stats);
                }
            }
        } catch (Exception e) {
            Main.getOut().println("SEVERE: problem analyzing diff/suc file " + f + ": " + e);
            e.printStackTrace();
        }
    }

    public String getTime() {
        StringBuffer sb = new StringBuffer();
        sb.append("Total time: ");
        sb.append(time);
        sb.append(" secs");
        if (unknownTime > 0) {
            sb.append(" - time for ");
            sb.append(unknownTime);
            sb.append(" tests is not known.");
        }
        return sb.toString();
    }
    private float time = 0.0f;
    private int   unknownTime = 0;

    private void addTime(ToplevelAnalyzer tla) {
        if (tla.getTime() < 0.0f) {
            unknownTime++;
        } else {
            time += tla.getTime();
        }
    }


    private TestDescriptors testDescriptors;
    private SuiteDescriptors suiteDescriptors;
    private int level = INFO;
    private String suiteDesignation;
}
