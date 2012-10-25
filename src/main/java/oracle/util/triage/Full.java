package oracle.util.triage;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.PrintStream;
import java.io.IOException;

/**
 This class implements a full triage and runtime analysis for farm and DTE jobs.
 */


public class Full implements Constants {

    private static void explain(String message) {
        if (message != null) {
            System.out.println("SEVERE: " + message);
        }
        System.out.println("Synopsis: java -jar triage.jar -full <arguments>  performs an extensive triage");
        System.out.println("and runtime analysis of a test run and can be invoked in the following ways:");
        System.out.println("-full NNNNNNNN                    - triages all tests for farm job NNNNNNN");
        System.out.println("-full NNNNNNNN:suite1,...,suiteN  - dto. but only shows particular suites");
        System.out.println("-full <farm-results-dir>              - triages all tests in a farm job");
        System.out.println("-full <farm-results-dir>/<suite-dir>  - triages a particular test in a farm job");
        System.out.println("-full <a-twork-dir>               - triages a local regressions (/ade, /scratch, /net area)");
        System.out.println();
        System.out.println("-full -srg <label>                - triages the srg submitted for J2EE label <label>");
        System.out.println("-full -lrg <label>                - triages all lrgs submitted for J2EE label <label>");
        System.out.println("-full -lrg <label>:suite1,...,suiteN  - dto. but only for certain suites");
        System.out.println("-full -cts <label>                - triages the lrgs submitted for J2EE label <label>");
        System.out.println("-full -cts <label>:suite1,...,suiteN  - dto. but only for certain suites");
        System.out.println();
        System.out.println("-full -dte MMMMMMM                - triages the DTE job MMMMMMM");
        System.out.println("-full -rb  RRRRRRR                - triages the Release Builder job RRRRRRR");
        System.out.println();
        System.out.println("Additional flags that can be employed:");
        System.out.println("-update              - do _not_ re-triage tests that have already been triaged.");
        System.out.println("-nogzip              - do _not_ compress generated .html files with gzip");
        System.out.println("-logrecords NNN      - display at most NNN records of a log message (default " +
                           SUPPRESS_EVENTS_COUNT + ")");
        System.out.println("-nohistory            - do _not_ display history records of computer, process, and port status");
        System.out.println("-noshowrunning        - do not show running and waiting tests suites");
        System.out.println("-nofilebrowser        - do not create an HTML page to browser all log/test files");
        System.out.println("-nomessagebrowser     - do not create an HTML page with all messages chronologically sorted");
        System.out.println("-noincidentreport     - do not create an HTML page detailing incidents");
        System.out.println();
        System.out.println("The following output is produced by the triage tool:");
        System.out.println("   index.html, a top-level index to jobs, test suites and triage files.");
        System.out.println("for every job <nnnn> and suite <suite> of that job that has completed:");
        System.out.println("   <nnnn>triage-<suite>.txt   - the triaging results (suc/dif summary)");
        System.out.println("   <nnnn>run-<suite>.html.gz  - a full runtime analysis (gzipped)");
        System.out.println("   <nnnn>-<suite>.jar         - ancillary information (logs etc.) referenced by the run.");
        System.out.println();
        System.out.println("In order to access the full triaging information:");
        System.out.println("- be sure to use Firefox (IE has performance issues with these pages)");
        System.out.println("- start an HTTP server as follows in the directory with your results:");
        System.out.println("     java -jar triage.jar -httpd . 8080");
        System.out.println("  Or adjust the current directory '.' and/or the port 8080 as desired.");
        
        if (message != null) {
            System.exit(1);
        } else {
            System.exit(0);
        }
    }

    private static int processOption(String[] args, int idx) {
        if (args[idx].equalsIgnoreCase("-help")
            || args[idx].equalsIgnoreCase("-h")) {
            explain(null);
        } else if (args[idx].equalsIgnoreCase("-debug")) {
            DEBUG = true;
        } else if (args[idx].equalsIgnoreCase("-gzip")) {
            gzip = true;
        } else if (args[idx].equalsIgnoreCase("-nogzip")) {
            gzip = false;
        } else if (args[idx].equalsIgnoreCase("-history")) {
            DatedHistory.setSuppressDatedHistory(false);
        } else if (args[idx].equalsIgnoreCase("-nohistory")) {
            DatedHistory.setSuppressDatedHistory(true);
        } else if (args[idx].equalsIgnoreCase("-update")) {
            updating = true;
        } else if (args[idx].equalsIgnoreCase("-noupdate")) {
            updating = false;
        } else if (args[idx].equalsIgnoreCase("-showrunning")) {
            showrunning = true;
        } else if (args[idx].equalsIgnoreCase("-noshowrunning")) {
            showrunning = false;
        } else if (args[idx].equalsIgnoreCase("-messagebrowser")) {
            messagebrowser = true;
        } else if (args[idx].equalsIgnoreCase("-nomessagebrowser")) {
            messagebrowser = false;
        } else if (args[idx].equalsIgnoreCase("-incidentreport")) {
            incidentreport = true;
        } else if (args[idx].equalsIgnoreCase("-noincidentreport")) {
            incidentreport = false;
        } else if (args[idx].equalsIgnoreCase("-filebrowser")) {
            filebrowser = true;
        } else if (args[idx].equalsIgnoreCase("-nofilebrowser")) {
            filebrowser = false;
        } else if (args[idx].equalsIgnoreCase("-logrecord")
                   || args[idx].equalsIgnoreCase("-logrecords")
                   || args[idx].equalsIgnoreCase("-logs")) {
            idx++;
            if (args.length <= idx || !Util.isNumeric(args[idx])) {
                explain("-logrecords <count> is missing the max count of log records");
            } else {
                try {
                    DiagnosticEvent.setSuppressEventsCount(Integer.parseInt(args[idx]));
                } catch (Exception e) {
                    System.out.println("SEVERE: should not occur: " + e);
                }
            }
        } else {
            explain("unknown option: " + args[idx]);
        }
        return idx;
    }
    private static boolean updating = false;
    private static boolean showrunning = true;
    private static boolean filebrowser = true;
    private static boolean messagebrowser = true;
    private static boolean incidentreport = true;
    static boolean DEBUG = false;

    public static void main(String[] args) {
        if (args.length == 0) {
            explain("must provide at least one argument.");
        }

        triages = new ArrayList<Full>();
        for (int i = 0; i < args.length; i++) {
            FarmInfo fi = null;

            if (args[i].equalsIgnoreCase("-dte")) {
                i++;
                if (args.length <= i) {
                    System.out.println("SEVERE: expected -dte <dte-jobid> but <dte-jobid> is missing!");
                } else if (!Util.isNumeric(args[i])) {
                    System.out.println("SEVERE: expected -dte <dte-jobid> but found: -dte " + args[i]);
                } else {
                    fi = FarmInfo.newDteInfo(Integer.parseInt(args[i]));
                }
            } else if (args[i].equalsIgnoreCase("-srg")
                        || args[i].equalsIgnoreCase("-lrg") 
                        || args[i].equalsIgnoreCase("-cts")) {
                String option = args[i];

                i++;
                if (args.length <= i) {
                    System.out.println("SEVERE: expected " + option + " <label> but <label> is missing!");
                } else {
                    String suites = null;
                    String label = args[i];
                    int pos = label.indexOf(":");
                    if (pos > 0) {
                        suites = label.substring(pos + 1);
                        label = label.substring(0, pos);
                    }
                         
                    FarmLrg fl = FarmLrg.newFarmLrg(label);
                    if (fl != null) {
                        int id = (option.equalsIgnoreCase("-srg"))
                                   ? fl.getSrgFarmId()
                                   :  ((option.equalsIgnoreCase("-lrg"))
                                       ? fl.getLrgFarmId() : fl.getCtsFarmId());
                        if (id != 0) {
                            fi = FarmInfo.newFarmInfo("" + id +
                                                       ((suites == null) ? "" : ":" + suites));
                            String prefix = option.substring(1).toUpperCase() + " " + Util.makeLabelLink(label);
                            fi.setHeaderPrefix(prefix);
                        } else {
                            System.out.println("SEVERE: unable to determine farm job id for " + option + " test suite of " + label);
                        }
                    }
                }
            } else if (args[i].startsWith("-")) {
                i = processOption(args, i);
            } else {
                fi = FarmInfo.newFarmInfo(args[i]);
            }

            if (fi != null) {
                List<FarmInfo.RegressionSuite> suites = fi.getRegressionSuites();
                for (FarmInfo.RegressionSuite suite : suites) {
                    Full f = new Full(suite);
                    if (f.triage()) {
                        triages.add(f);
                    };
                };
            }
        }

        if (triages.size() == 0) {
            System.out.println("WARNING: since no triage results are available, none will be written.");
        } else {
            writeNoFramesHtml();
            writeIndexHtml();
        }
    }
    private static boolean gzip = true;
    private static ArrayList<Full> triages = new ArrayList<Full>();

    static void resetStaticVariables() {
        gzip = true;
        triages = new ArrayList<Full>();
    }


    private Full(FarmInfo.RegressionSuite rs) {
        this.info = rs.getFarmInfo();
        this.regression = rs;
    }

    private boolean triage() {
        int status = regression.getStatus();
        if (status != FarmInfo.FINISHED && status != FarmInfo.ABORTED) {
            System.out.println("INFO: suite " + regression.getSuite() + " from job " + info.getFarmJobId() +
                               ((status == FarmInfo.RUNNING)
                                   ? " is still running."
                                   : ((status == FarmInfo.FAILED)
                                       ? " - submission has failed." 
                                       : ((status == FarmInfo.WAITING)
                                          ? " - waiting in queue " : " status UNKNOWN."))));
            return (showrunning) ? true : false;
        }

        String anId = (info.getFarmJobId() == 0) ? "" : "" + info.getFarmJobId() + "-";

        outFile = "triage-" + anId + regression.getSuite() + ".txt";
        outHtml = "triage-" + anId + regression.getSuite() + ".html";
        archiveFile = (info.isFarm()) 
                      ? anId + regression.getSuite() + ".jar"
                      : null;
        htmlFile = "run-" + anId + regression.getSuite() + ".html";
        browserFile = "browse-" + anId + regression.getSuite() + ".html";
        messageFile = "msg-" + anId + regression.getSuite() + ".html";
        incidentFile = "incident-" + anId + regression.getSuite() + ".html";

        File f = null;

        if (updating && (new File(outFile)).exists()) {
            System.out.println("INFO: skipping triaging into " + outFile + " since it already exists.");
            regression.setTriageFile(outFile);
            if ( (new File(outHtml)).exists()
                 || (new File(outHtml + ".gz")).exists()) {
                regression.setHtmlFile(outHtml);
            }
            if ( (new File(browserFile)).exists()
                 || (new File(browserFile + ".gz")).exists()) {
                regression.setBrowserFile(browserFile);
            }
            if ( (new File(messageFile)).exists()
                 || (new File(messageFile + ".gz")).exists()) {
                regression.setMessageFile(messageFile);
            }
            if ( (new File(incidentFile)).exists()
                 || (new File(incidentFile + ".gz")).exists()) {
                regression.setIncidentFile(incidentFile);
            }

        } else if ( !(new File(regression.getResultLocation()).exists()) ) {
            System.out.println("WARNING: skipping triaging into " + outFile + " since " +
                               regression.getResultLocation() + " does not (yet) exist");

        } else {
            System.out.println("INFO: Triaging into " + outFile +
                                ((info.isFarm()) ? " and building archive " + archiveFile 
                                                 : ""));
             //test results " + regression.getResultLocation() + " ***";
            Main.resetStaticVariables();
            Main.setFull(true);

            String[] args = new String[] {
                (info.isFarm()) ? "-farm" : "-dir" ,  regression.getResultLocation(),
                "-name", anId + regression.getSuite(),
                "-detail",
                "-diffsonly", 
                "-outfile", outFile,
                "-html", outHtml,
                (info.isFarm()) ? "-archive" : "-targz", (info.isFarm()) ? archiveFile : "-targz",
                "-targz",
            };
            Main.main(args);
            regression.setTriageFile(outFile);

            if ((new File(outHtml)).exists()) {
                System.out.println("INFO: adding HTML triage " + outHtml);
                if (gzip) {
                    Gzip.gzip(new File(outHtml));
                };
                regression.setHtmlFile(outHtml);
            }

            if (filebrowser) {
                System.out.println("INFO: Creating HTML file " + browserFile + " for browsing log files.");
                if (info.isFarm()) {
                    FileBrowser.createFileBrowser(archiveFile, browserFile);
                } else {
                    FileBrowser.createFileBrowser(regression.getResultLocation(), browserFile);
                }
                if (gzip) {
                    Gzip.gzip(new File(browserFile));
                };
                regression.setBrowserFile(browserFile);
            }


            if (messagebrowser) {
                System.out.println("INFO: Creating HTML file " + messageFile + " with chronological message repository.");
                if (info.isFarm()) {
                    MessageRepository.createMessageFile(archiveFile, 
                                                        messageFile,
                                                        incidentFile);
                } else {
                    MessageRepository.createMessageFile(
                      regression.getResultLocation(), messageFile, incidentFile);
                }
                if (gzip) {
                    Gzip.gzip(new File(messageFile));
                };
                regression.setMessageFile(messageFile);
            }

            if (incidentreport) {
                System.out.println("INFO: Creating HTML file " + incidentFile + 
                                   " with details of incidents");
                int incidentCount = 0;
                if (info.isFarm()) {
                   incidentCount = IncidentReporter.createIncidentReport(
                                      archiveFile, incidentFile);
                } else {
                   incidentCount = IncidentReporter.createIncidentReport(
                     regression.getResultLocation(), incidentFile);
                }
                if (gzip) {
                    Gzip.gzip(new File(incidentFile));
                };
                regression.setIncidentFile(incidentFile);
                regression.setIncidentCount(incidentCount);
            }

        }

        if (updating && ( (new File(htmlFile).exists())
                          || (new File(htmlFile + ".gz")).exists())) {
            System.out.println("INFO: skipping generation of " + htmlFile + ((gzip) ? ".gz" : "") +
                               " since it already exists." );
            regression.setRunFile(htmlFile);
        } else if (  !(f = new File((info.isFarm())
                                     ? archiveFile 
                                     : regression.getResultLocation())
                      ).exists() ) {
            System.out.println("WARNING: skipping generation of " + htmlFile + " since " + f +
                               " does not exist");
        } else {
            System.out.println("INFO: Generating run details into " + htmlFile);
            Main.resetStaticVariables();
            Main.setFull(true);

            String[] args = new String[] {
                "-timing",  (info.isFarm()) ? archiveFile : regression.getResultLocation(),
                "-detail",
                "-html", htmlFile,
            };
            Main.main(args);
            if (gzip) {
                Gzip.gzip(new File(htmlFile));
            };
            regression.setRunFile(htmlFile);
        }
        return true;
    }
    private String outFile;
    private String outHtml;
    private String archiveFile;
    private String htmlFile;
    private String browserFile;
    private String messageFile;
    private String incidentFile;
    private FarmInfo info;
    private FarmInfo.RegressionSuite regression;

    private String toHtml() {
        return regression.getSuite() + " " + regression.getTriageLink();
    }

    private String makeJobLink() {
        String id = FARM_JOB_PREFIX + "" + info.getFarmJobId();
        return Util.makeTargetLink(id, "" + info.getFarmJobId(), "tree");
    }

    private static void writeNoFramesHtml() {
        File outfile = new File("noframes.html");
        if (outfile.exists()) {
            System.out.println("WARNING: overwriting " + outfile);
        }
        try {
            PrintStream ps = new PrintStream(outfile);
            ps.println("<html><head><title>Triaging Results</title>");
            ps.println(HEADER_INCLUDES);
            ps.println("</head><body>");
            String lastJob = "?";
            for (Full full : triages) {
                if (!lastJob.equals(full.info.getHtmlHeader())) {
                    if (!lastJob.equals("?")) {
                        ps.println("</ul>");
                    }
                    ps.println("<h4>" + full.info.getHtmlHeader() + "</h4>");
                    ps.println("<ul>");
                }
                ps.println("<li>" + full.toHtml() + "</li>");
                lastJob = full.info.getHtmlHeader();
            }
            if (!lastJob.equals("?")) {
                ps.println("</ul>");
            }
            ps.println("</body></html>");
            ps.close();
        } catch (IOException ioe) {
            System.out.println("SEVERE: error writing to " + outfile + ": " + ioe);
        }
    }

    private static void writeIndexHtml() {
        File outfile = new File("index.html");
        if (outfile.exists()) {
            System.out.println("WARNING: overwriting " + outfile);
        }
        try {
            PrintStream ps = new PrintStream(outfile);
            ps.println("<html>");
            ps.println("<head>");
            ps.println(HEADER_INCLUDES);
            ps.println("</head>");
            ps.println("<frameset cols=\"15%,*\">");
            ps.println("  <frame src=\"noframes.html\">");
            ps.println("  <frameset rows=\"90%,*\">");
            ps.println("     <frame src=\"" + INITIAL_TREE_PANE_CONTENT   + "\" name=\"tree\">");
            ps.println("     <frame src=\"" + INITIAL_DETAIL_PANE_CONTENT + "\" name=\"detail\">");
            ps.println("  </frameset>");
            ps.println("</frameset>");
            ps.println("<noframes>");
            ps.println("<body>Your browser does not support frames.");
            ps.println("Start with <a href=\"noframes.html\">noframes.html</a>.");
            ps.println("</body>");
            ps.println("</noframes>");
            ps.println("</html>");
            ps.close();
        } catch (IOException ioe) {
            System.out.println("SEVERE: error writing to " + outfile + ": " + ioe);
        }
    }
}
