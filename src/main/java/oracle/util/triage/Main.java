package oracle.util.triage;

import java.util.List;
import java.util.ArrayList;

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.jar.JarFile;


/**
 This class implements a diff analysis for MATs tests.
 */


public class Main implements Constants {

    /** whether or not we should attempt to show test timing information.
        Controlled by the -time option setting. */
    public static boolean showTimings() {
        return showTimings;
    }
    private static boolean showTimings = false;


    /** whether we want to force the reading of .tar.gz files
        even if a jtwork directory exists. This is much more
        thorough but also much slower. */
    public static boolean getForceTarGz() {
        return forceTarGz;
    }
    private static boolean forceTarGz = false;


    /** show baseline log diffs */
    public static boolean showBaseline() {
        return showBaseline;
    }
    private static boolean showBaseline = false;

    /** report on the slowest n tests. 
        Controlled by the -slowest <n> option setting. */
    public static int getSlowest() {
        return slowest;
    }
    private static int slowest = 0;


     /** name of the (ant) build log file to use for triage.
         In conjunction with -twork and -dir triages, this will only be used if
         there are no diffs and no succs, in which case we assume that a build error
         has occurred. */
    public static String getBuildLog() {
        return buildLog;
    }
    private static String buildLog = null;

    /** how many relevant build steps should be shown for a build log.
        By default, this should be at least 1. */
    public static int getRelevantBuildSteps() {
        return minRelevantBuildSteps;
    }
    private static int minRelevantBuildSteps = 1;

    /** should we show diffs only or both, diffs and succs */
    public static boolean getDiffsOnly(int level) {
        if (diffOnly == null) {
            return level <= FINE;
        } else {
            return diffOnly.booleanValue();
        }
    }
    private static Boolean diffOnly = null;

    /** for triaging: display both, summary and detail */
    public static boolean isSummaryDetail() {
        return summaryDetail;
    }
    private static boolean summaryDetail = false;

    /** driven by the Full feature */
    public static boolean isFull() {
        return full;
    }

    /** driven by the Full feature */
    public static void setFull(boolean b) {
        full = b;
    }
    private static boolean full = false;


    /** archive file used for Farm triage */
    public static JarFile getArchive() {
        return archive;
    }
    public static void setArchive(JarFile jf) {
        archive = jf;
    }
    private static JarFile archive = null;

    /** list of all TestLogic files used for Farm triage */
    public static List<String> getTestLogicFiles() {
        return testLogicFiles;
    }
    public static void setTestLogicFiles(List<String> files) {
        testLogicFiles = files;
    }
    private static List<String> testLogicFiles = null;

    /** name of archive file if specified on the command line */
    public static String getArchiveFile() {
        return archiveFile;
    }
    private static String archiveFile = DEFAULT_ARCHIVE_FILE;

    /** return print stream in effect for triaging information */
    public static PrintStream getOut() {
        return out;
    }
    private static PrintStream out = System.out;
    private static File outFile = null;

    /** return print stream for HTML triage, null if none */
    public static AnnotatingPrintStream getHtmlStream() {
        return htmlStream;
    }
    private static AnnotatingPrintStream htmlStream = null;

    /** return HTML file name, null if none */
    public static String getHtmlFileName() {
        return htmlFileName;
    }
    private static String htmlFileName = null;
    private static File htmlFile = null;

    /** return text URL for triaged info */
    public static String getTextUrl() {
        // omit base URL - assuming txt and html triage in same place
        return outFileName;
    }
    /** return HTML URL for triaged info */
    public static String getHtmlUrl() {
        // provide full URL information so we can get to it from text
        return (baseUrl == null) ? htmlFileName : baseUrl + htmlFileName;
    }
    private static String baseUrl = null;
    private static String outFileName = null;

    /** close HTML-relates triaging */
    public static void closeHtml() {
        if (htmlStream != null) {
            htmlStream.close();
            htmlStream = null;
            htmlFile = null;
            htmlFileName = null;
            System.out.println("INFO: skipping further HTML triage for this file");
        }
    }

    /** reset Main class for subsequent invocations via the main method. */
    static void resetStaticVariables() {
        archiveFile = DEFAULT_ARCHIVE_FILE;
        archive = null;
        diffOnly = null;
        minRelevantBuildSteps = 1;
        buildLog = null;
        slowest = 0;
        forceTarGz = false;
        summaryDetail = false;
        showTimings = false;
        out = System.out;
        outFile = null;
        outFileName = null;
        full = false;
        htmlStream = null;
        htmlFile = null;
        htmlFileName = null;
        baseUrl = null;
        testLogicFiles = null;

        AreaDescriptors.initialize();
        DatedFile.resetStaticVariables(); 
        ToplevelAnalyzer.resetStaticVariables();
        ToplevelTest.resetStaticVariables();
        DiagnosticEvent.resetStaticVariables();
        UnitTestReport.resetGlobalStats();
    }

    /** main entry point for the triage tool. */
    public static void main(String[] args) {
        if (args.length == 0
            || args[0].equalsIgnoreCase("-help")
            || args[0].equalsIgnoreCase("--help")
            || args[0].equalsIgnoreCase("help")
            || args[0].equalsIgnoreCase("-h")) {
            Messages.explain(null);
        }

        int command = NO_COMMAND;

        int level   = INFO;
        String name = null;

        String soaLabel = null;
        List<String> dteJobids = new ArrayList<String>();
        String directory = null;

        TestDescriptors tds = new TestDescriptors();
        boolean addedTestDescriptors = false;

        SuiteDescriptors sds = new SuiteDescriptors();
        boolean addedSuiteDescriptors = false;

        TriageDescriptors trds = new TriageDescriptors();
        boolean addedTriageDescriptors = false;

        List<String> fullTriageArguments = new ArrayList<String>();


        int pos = 0;
        char ch = ' ';
        int i = 0; // argument index


        /* ---------- Check whether we want to run one of the other tools ------------ */

        if (args[0].equalsIgnoreCase("-httpd")
            || args[0].equalsIgnoreCase("-full")
            || args[0].equalsIgnoreCase("-basedelta")
            || args[0].equalsIgnoreCase("-time")
            || args[0].equalsIgnoreCase("-timing")
            || args[0].equalsIgnoreCase("-mailmap")) {
            String[] tmp = new String[args.length - 1];
            for (int j = 1; j < args.length; j++) {
                tmp[j - 1] = args[j];
            }

            if (args[0].equalsIgnoreCase("-httpd")) {
                Jhttp.main(tmp);
            } else if (args[0].equalsIgnoreCase("-basedelta")) {
                DeltaDiff.main(tmp);
            } else if (args[0].equalsIgnoreCase("-full")) {
                Full.main(tmp);
            } else if (args[0].equalsIgnoreCase("-mailmap")) {
                GuidToMailIdMapper.main(tmp);
            } else {
                DatedFile.main(tmp);
            }
            return;
        } 


        
        /* ---------- Check for a command  ------------ */

        if (args[0].equalsIgnoreCase("-mats")) {
            command = MATS_TRIAGE_COMMAND;
            i++;
        } else if (args[0].equalsIgnoreCase("-twork")) {
            command = TWORK_TRIAGE_COMMAND;
            i++;
        } else if (args[0].equalsIgnoreCase("-buildlog")) {
            command = BUILD_LOG_COMMAND;
            i++;
            if (i >= args.length) {
                Messages.explain("Must specify a build log file name after " + args[0] + " command.");
            }
            buildLog = args[i];
            i++;
        } else if (args[0].equalsIgnoreCase("-farm")) {
            command = FARM_TRIAGE_COMMAND;
            i++;
            if (i >= args.length || args[i].startsWith("-")) {
                Messages.explain("Must specify the farm result directory after " + args[0] + " command.");
            }
            directory = args[i];
            if (directory.endsWith("/")) {
                directory = directory.substring(0, directory.length() - 1);
            }
            pos = directory.lastIndexOf("/");
            if (pos >= 0) {
                buildLog = directory + "/" + directory.substring(pos + 1) + ".farm.out";
            }
            i++;
        } else if (args[0].equalsIgnoreCase("-dir") || args[0].equalsIgnoreCase("-matswork")) {
            command = DIR_TRIAGE_COMMAND;
            i++;
            if (i >= args.length) {
                Messages.explain("Must specify a directory name after " + args[0] + " command.");
            }
            if (args[0].equalsIgnoreCase("-matswork")) {
                if (!args[i].startsWith("/ade/")) {
                    Messages.explain("Expected MATS work directory of the form /ade/<user>_<machine>/<work-dir>. Found " + args[i] + ".");
                }
                String view = args[i].substring("/ade/".length());
                pos = view.indexOf("/");
                if (pos < 0) {
                    Messages.explain("Expected MATS work directory of the form /ade/<user>_<machine>/<work-dir>. Found " + args[i] + ".");
                }
                String workdir = view.substring(pos);
                view = view.substring(0, pos); 
                pos = view.indexOf("_");
                if (pos < 0) {
                    Messages.explain("Expected MATS work directory of the form /ade/<user>_<machine>/<work-dir>. Found " + args[i] + ".");
                }
                String user = view.substring(0, pos);
                String host = view.substring(pos + 1);
                args[i] = "/net/" + host + "/scratch/" + user + "/adestore/views/" + view + workdir;
                try {
                    File f = new File(args[i]);
                    if (!f.exists() || !f.isDirectory()) {
                        Messages.explain("Unable to access MATS work directory " + args[i]);
                    }
                } catch (Exception exn) { 
                    Messages.explain("Unable to access MATS work directory " + args[i] + ": " + exn);
                };
            }
            directory = args[i];
            i++;

            if (!args[0].equalsIgnoreCase("-matswork")) {
                if (directory.endsWith("/")) {
                    directory = directory.substring(0, directory.length() - 1);
                }
                pos = directory.lastIndexOf("/");
                if (pos > 0) {
                    String log = directory + "/" + directory.substring(pos + 1) + ".farm.out";
                    if ((new File(log)).exists()) {
                        command = FARM_TRIAGE_COMMAND;
                        buildLog = log;
                    }
                }
            }
        } else {
            Messages.explain("Must specify either -mats, -matswork, -twork, -dir, or -buildlog command as first argument on command line.");
        }

        for (  ; i < args.length; i++) {

            /* ---------- Options ------------ */
            if (args[i].equalsIgnoreCase("-severe")
                || args[i].equalsIgnoreCase("-summary")

                || args[i].equalsIgnoreCase("-info")

                || args[i].equalsIgnoreCase("-fine")
                || args[i].equalsIgnoreCase("-detail")

                || args[i].equalsIgnoreCase("-finer")

                || args[i].equalsIgnoreCase("-finest")) {
                if (level != INFO) {
                    Messages.explain("The options -severe, -info, -fine, -finer, and -finest are mutually exclusive.");
                } else if (args[i].equalsIgnoreCase("-severe")
                        || args[i].equalsIgnoreCase("-summary")) {
                    level = SEVERE;
                } else  if (args[i].equalsIgnoreCase("-info")) {
                    level = INFO;
                } else  if (args[i].equalsIgnoreCase("-fine")
                         || args[i].equalsIgnoreCase("-detail")) {
                    level = FINE;
                } else  if (args[i].equalsIgnoreCase("-finer")) {
                    level = FINER;
                } else  /* (args[i].equalsIgnoreCase("-finest")) */  {
                    level = FINEST;
                }
            } else if (args[i].equalsIgnoreCase("-summarydetail")) {
                summaryDetail = true;
            } else if (args[i].equalsIgnoreCase("-name")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -name <name> - missing the <name> after the option.");
                } else {
                    name = args[i];
                }
            } else if (args[i].equalsIgnoreCase("-archive")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -archive <archive-file> - specifying an archive file to use.");
                } else {
                    archiveFile = args[i];
                }
            } else if (args[i].equalsIgnoreCase("-html")) {
                i++;
                if (i >= args.length || args[i].startsWith("-") || args[i].toLowerCase().indexOf(".htm") < 1) {
                    Messages.explain("Usage: -html <file>.html - missing or invalid <file>.html after the option.");
                } else if (htmlFile != null) {
                    Messages.explain("Usage: -html <file>.html - option may not be specified multiple times.");
                } else {
                    htmlFileName = args[i];
                    htmlFile = new File(htmlFileName);
                    if (htmlFile.exists() && !htmlFile.canWrite()) {
                        Messages.explain("Unable to write to HTML file " + htmlFileName + " specified in -html option.");
                    } else {
                        try {
                           htmlStream = new AnnotatingPrintStream(new PrintStream(new FileOutputStream(htmlFile)));
                        } catch (IOException ioe) {
                           Messages.explain("Error opening print stream to HTML file " + htmlFileName + ": " + ioe);
                        }
                    }
                }
            } else if (args[i].equalsIgnoreCase("-baseurl")) {
                i++;
                if (i >= args.length || args[i].startsWith("-") 
                    || !(args[i].startsWith("http://") || args[i].startsWith("https://"))) {
                    Messages.explain("Usage: -baseurl http://<base_url> - missing or invalid url.");
                } else if (baseUrl != null) {
                    Messages.explain("Usage: -baseurl http://<base_url> - option may not be specified multiple times.");
                } else {
                    baseUrl = args[i];
                }
            } else if (args[i].equalsIgnoreCase("-testdesc")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -testdesc <file> - missing the <file> containing Wiki-formatted test descriptions.");
                } else {
                    tds.addDescriptors(args[i]);
                    addedTestDescriptors = true;
                }
            } else if (args[i].equalsIgnoreCase("-suitedesc")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -suitedesc <file> - missing the <file> containing suite descriptions.");
                } else {
                    sds.addDescriptors(args[i]);
                    addedSuiteDescriptors = true;
                }
            } else if (args[i].equalsIgnoreCase("-triagedesc")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -triagedesc <file> - missing the <file> containing triage descriptions.");
                } else {
                    trds.addDescriptors(args[i]);
                    addedTriageDescriptors = true;
                }
            } else if (args[i].equalsIgnoreCase("-buildlog")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -buildlog <file> - missing the <file> containing build log.");
                } else if (buildLog != null) {
                    Messages.explain("You may only specify one build log file.");
                } else {
                   // showTimings = true;
                    buildLog = args[i];
                }
            } else if (args[i].equalsIgnoreCase("-buildsteps")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -buildsteps <number> - missing the <number> that states how many build steps to use.");
                } else {
                    int steps = 0;
                    try {
                        steps = Integer.parseInt(args[i]);
                    } catch (Exception exn) {
                        // ignore since we check below
                    }
                    if (steps <= 0) {
                        Messages.explain("Invalid number of build steps '" + args[i] + "' in -buildsteps option.");
                    } else {
                        minRelevantBuildSteps = steps;
                    }
                }
            } else if (args[i].equalsIgnoreCase("-outfile")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -outfile <file> - missing the <file> for capturing triage output.");
                } else {
                    try {
                        outFileName = args[i];
                        File f = new File(outFileName);
                        PrintStream ps = new PrintStream(f);
                        out = ps;
                        outFile = f;
                    } catch (Exception e) {
                        System.out.println("SEVERE: error creating -outfile " + args[i] + ": " + e);
                    }
                }
            } else if (args[i].equalsIgnoreCase("-forcetargz") 
                       || args[i].equalsIgnoreCase("-targz")) {
                forceTarGz = true;
            } else if (args[i].equalsIgnoreCase("-time") 
                       || args[i].equalsIgnoreCase("-times")) {
                showTimings = true;
            } else if (args[i].equalsIgnoreCase("-baseline") 
                       || args[i].equalsIgnoreCase("-basedelta")) {
                showBaseline = true;
            } else if (args[i].equalsIgnoreCase("-diffs") 
                       || args[i].equalsIgnoreCase("-diffsonly")) {
                diffOnly = new Boolean(true);
            } else if (args[i].equalsIgnoreCase("-all") 
                       || args[i].equalsIgnoreCase("-succs")) {
                diffOnly = new Boolean(false);
            } else if (args[i].equalsIgnoreCase("-slowest")) {
                i++;
                if (i >= args.length || args[i].startsWith("-")) {
                    Messages.explain("Usage: -slowest <number> - missing the <number> that states how many \"slowest\" tests to report on.");
                } else {
                    int slow = 0;
                    try {
                        slow = Integer.parseInt(args[i]);
                    } catch (Exception exn) {
                        // ignore since we check below
                    }
                    if (slow <= 0) {
                        Messages.explain("Invalid number for slowest count '" + args[i] + "' in -slowest option.");
                    } else {
                        slowest = slow;
                    }
                }
            } else if (args[i].equalsIgnoreCase("-debug")) {
                Full.DEBUG = true;
            } else if (args[i].startsWith("-")) {
                Messages.explain("Unsupported option: " + args[i]);

            /* ---------- Other Command Line Arguments  ------------ */
            } else if (command == MATS_TRIAGE_COMMAND) {
                if ( (pos = args[i].indexOf("-")) > 0
                    && '0' <= (ch = args[i].charAt(0)) && ch <= '9') {
                    String from = args[i].substring(0, pos);
                    String to = args[i].substring(pos + 1);
                    if (from.length() > to.length()) {
                        to = from.substring(0, from.length() - to.length()) + to;
                    }
    
                    try {
                        int start = Integer.parseInt(from);
                        int end = Integer.parseInt(to);
                        if (end < start) {
                            Messages.explain("Expected range of numeric DTE Jobid, found: " + args[i] + ". Start must be smaller than end.");
                        } else if (end - start > MAX_DTE_JOBID_RANGE) {
                            Messages.explain("Expected range of numeric DTE Jobid, found: " + args[i] + ". Range of " + (end - start) + " jobids appears too large.");
                        }
    
                        for (int j = start; j <= end; j++) {
                            dteJobids.add("" + j);
                        }
                    } catch (Exception e) {
                        Messages.explain("Expected range of numeric DTE Jobid, found: " + args[i] + ". The reported error is: " + e);
                    }
                } else if ( '0' <= (ch = args[i].charAt(0)) && ch <= '9') {
                    try {
                        int dummy = Integer.parseInt(args[i]);
                        dteJobids.add(args[i]);
                    } catch (Exception e) {
                        Messages.explain("Expected numeric DTE Jobid, found: " + args[i]);
                    }
                } else if (soaLabel != null) {
                    Messages.explain("Unknown argument: " + args[i] + ". This might be a SOA (or IAS) label, but that label is already set to " + soaLabel);
                } else { 
                    soaLabel = args[i]; 
                    if (!soaLabel.startsWith("SOA_") && !soaLabel.startsWith("IAS") && !soaLabel.startsWith("ASCORE") ) {
                        Messages.explain("Expected a SOA (IAS, or ASCORE) label, but found: " + soaLabel);
                    }
                }
            } else if (command == TWORK_TRIAGE_COMMAND) {
                Messages.explain("Unexpected argument: " + args[i]);
            }
        }


        if (htmlFile != null && outFile == null) {
            outFileName = htmlFileName;
            int p = htmlFileName.indexOf(".htm");
            if (p > 0) {
                outFileName = outFileName.substring(0, p);
            }
            outFileName = outFileName + ".txt";
            try {
                File f = new File(outFileName);
                PrintStream ps = new PrintStream(f);
                out = ps;
                outFile = f;
            } catch (Exception e) {
                System.out.println("SEVERE: error creating outfile " + outFileName + " with text version of triage: " + e);
            }
        }

        if (command != TWORK_TRIAGE_COMMAND && !addedTestDescriptors) {
            File tdf = new File(TRIAGE_WIKI_FILE);
            if (tdf.exists() && tdf.isFile() && tdf.canRead()) {
                tds.addDescriptors(tdf.toString());
            } else if ( (tdf = new File(TRIAGE_GLOBAL_DEFAULT_DIR + "/" + TRIAGE_WIKI_FILE)).exists()
                    && tdf.isFile() && tdf.canRead()) {
                tds.addDescriptors(tdf.toString());
            }
        }

        if (command != TWORK_TRIAGE_COMMAND && !addedSuiteDescriptors) {
            File sdf = new File(TRIAGE_SUITE_FILE);
            if (sdf.exists() && sdf.isFile() && sdf.canRead()) {
                sds.addDescriptors(sdf.toString());
            } else if ( (sdf = new File(TRIAGE_GLOBAL_DEFAULT_DIR + "/" + TRIAGE_SUITE_FILE)).exists()
                    && sdf.isFile() && sdf.canRead()) {
                sds.addDescriptors(sdf.toString());
            }
        }

        List<ResultsDir> results = new ArrayList<ResultsDir>();

        if (command == MATS_TRIAGE_COMMAND) { 
            if (dteJobids.size() == 0) {
                Messages.explain("Expected to find one or more DTE Job numbers.");
            }
            for (i = 0; i < dteJobids.size(); i++) {
                try {
                    Mats m = Mats.newMats(tds, sds, soaLabel, dteJobids.get(i));
                    if (m != null) {
                        results.add(m);
                    } else {
                      // Error was already given!
                      // System.out.println("SEVERE: unable to triage "+soaLabel+" DTE JobId: "+dteJobids.get(i));
                    }
                } catch (Exception e) {
                    System.out.println("SEVERE: issue triaging " + soaLabel + " DTE JobId: " + dteJobids.get(i) + ": " + e);
                    e.printStackTrace();
                }
            }
        } else if (command == TWORK_TRIAGE_COMMAND) {
            TWork tw = TWork.newTWork();
            if (tw != null) {
                results.add(tw);
            }
        } else if (command == DIR_TRIAGE_COMMAND) {
            Dir d = null;
            try {
                d = new Dir(directory);
            } catch (Exception e) {
                System.out.println("SEVERE: issue triaging directory " + directory + ": " + e);
                e.printStackTrace();
            }
            if (d != null) {
                results.add(d);
            }
        } else if (command == FARM_TRIAGE_COMMAND) {
            Farm f = null;
            try {
                try {
                    Farm.createJarFile(directory);
                } catch (Exception e) {
                    System.out.println("SEVERE: issue triaging farm result in " + directory + ": " + e);
                    e.printStackTrace();
                }
                f = new Farm(directory);
            } catch (Exception e) {
                System.out.println("SEVERE: issue triaging farm result in " + directory + ": " + e);
                e.printStackTrace();
            }
            if (f != null) {
                results.add(f);
            }
        } else if (command == BUILD_LOG_COMMAND) {
            System.out.println( (new BuildLog(getBuildLog(), getArchive())).printLastSteps() );
        }

        for (i = 0; i < results.size(); i++) {
            results.get(i).setLevel(level);
            if (name != null) {
                results.get(i).setSuite(name);
            }

            try {
                results.get(i).triageResults();
            } catch (Exception e) {
                ResultsDir rd = results.get(i);
                if (rd != null) {
                    System.out.println("SEVERE: issue triaging results for " + rd.getSuite() + " in " +
                                 rd.getRootDirectory() + ": " + e);
                } else {
                    System.out.println("SEVERE: issue triaging results #" + i + ": " + e);
                }
                e.printStackTrace();
            }
        }
        if (outFile != null) {
            out.close();
        }
        if (htmlStream != null) {
            htmlStream.close();
        }
    }

}
