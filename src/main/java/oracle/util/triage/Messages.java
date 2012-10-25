package oracle.util.triage;


/**
 This class implements a diff analysis for MATs tests.
 */
public class Messages implements Constants {


    static void explainTriaging(String pref) {
        if (pref == null) {
            pref = "";
        }
        System.out.println(pref + "Synopsis for triaging a test suite to standard output:");
        System.out.println("   java -jar triage.jar [-twork|-dir <dir>|-farm <dir>|-buildlog <file>|-mats...] [options]");
        System.out.println("Where [options] is as follows:");
        System.out.println("   -twork      - triages the test results found in $T_WORK");
        System.out.println("   -dir  <dir> - triages the test results found in directory <dir>");
        System.out.println("   -farm <dir> - triages farm test results from directory <dir>");
        System.out.println("   -buildlog <file> - triages <file> as a build log. This can also specified in addition to");
        System.out.println("                      -twork/-dir/-farm to reference a corresponding build log");
       
/*
        System.out.println("           - triages MATS test results if you do the following:");
        System.out.println("             (1) Go to http://stjpg.us.oracle.com/Wiki.jsp?page=AS11J2EEMatsPreflight");
        System.out.println("             (2) Use the link:  http://release-builder4.us.oracle.com");
        System.out.println("             (3) Pick the desired J2EE label");
        System.out.println("             (4) Obtain DTE JOB Ids from the \"Track Install Jobs\" column.");
        System.out.println("             (5) Obtain SOA (or IAS) LABEL by clicking one of the \"MATS Test Shiphome Reports\" links.");
        System.out.println("           Note: you can also specify ranges of job ids, such as 1237-1244 or 1237-44.");
*/
        System.out.println("Options for test triage:");
        System.out.println("  -name <name>        - (re-)naming of the top-level test suite");
        System.out.println("  -outfile <file>.txt - text version of the triage goes to <file>.txt instead of stdout.");
        System.out.println("  -html <file>.html   - creating HTML version of triage in <file>.html");
        System.out.println("                        Implicitly defines -outfile <file>.txt unless already specified.");
        System.out.println("                        Use with -archive and -targz generates additional pages for browsing");
        System.out.println("                        result area files and messages, and timing information.");
        System.out.println("  -baseurl <base_url> - base URL to use for accessing <file>.html");
        System.out.println("  -archive <jar-file> - saves log files into <jar-file> (otherwise uses ./tmp.jar)");
        System.out.println("                        with -html option, this triggeres generation of additional HTML reports");
        System.out.println("  -suitedesc   <file> - specify a file with suite descriptions.");
        System.out.println("                        (Default: " + TRIAGE_GLOBAL_DEFAULT_DIR + "/" +
                                                    TRIAGE_SUITE_FILE + ")");
        System.out.println("  -testdesc    <file> - specify a file with (Wiki-formatted) test descriptions.");
        System.out.println("                        (Default: " + TRIAGE_GLOBAL_DEFAULT_DIR + "/" +
                                                    TRIAGE_WIKI_FILE + ")");
/*
        System.out.println("  -triagedesc  <file> - specify a file with test descriptions and triage patterns.");
        System.out.println("                        (Default: "+TRIAGE_GLOBAL_DEFAULT_DIR+"/"+
                                                    TRIAGE_AREA_FILE+")");
*/
/*
        System.out.println("  -time               - print out the time taken by tests");
        System.out.println("  -slowest     <num>  - identify the <num> slowest tests (use -summary,");
*/
        System.out.println("  -buildsteps  <num>  - when a build error occurs, we display the last <num> of ");
        System.out.println("                        relevant build steps (default 1).");
        // System.out.println();
        System.out.println("Reporting detail can be set by using _one_ of the following options:");
        System.out.println("  -severe  - list names of failures only.  Synonym:  -summary");
        System.out.println("  -info    - (default) provide failures with detail synopsis and assignment");
        System.out.println("  -fine    - provide full details and test info for failures, list names of suc(esse)s");
        System.out.println("  -detail  - synonym for -fine");
        System.out.println("  -finer   - provide test suite information (even if no failure in suite)");
        System.out.println("  -finest  - provide information on individual tests (even if no test failure)");
        System.out.println("  -summarydetail  - special mode that performs a summary triage and then detail triage");
        System.out.println("The following options can be used in addition to any of the above.");
        System.out.println("  -diffs   - suppress printing of successful suites or tests (even with -finer, -finest)");
        System.out.println("  -targz   - force reading of the .tar.gz file when availabe (thorough but much slower)");
        System.out.println("  -baseline - show link to baseline log comparison on HTML page (linked page must be created separately.)");
/*
        System.out.println("ToDo:");
        System.out.println("  -html <dir>             - option to generate html files");
        System.out.println("  -junit <dir1>...<dirn>  - command to triage all JUnit results in subdirs.");
        System.out.println("  -aimeweb <??>...        - command to triage aimeweb results");
        System.out.println("  -farm <??>...           - command to triage farm results");
        System.out.println("  -???                    - command(s) to compare different runs");
        System.out.println("  -???                    - ability for \"auto-triaging\" for assignments.");
        System.out.println("  -???                    - ability to utilize patterns for triaging");
*/
    }

    static void explainFullTriage(String pref) {
        if (pref == null) {
            pref = "";
        }
        System.out.println(pref + "Synopsis for triaging one or more test suites into a Web site:");
        System.out.println("   java -jar triage.jar -full  <farmid> ... <dir> ...[options]");
        System.out.println("   -full <farmid> - triages farm job <farmid> (which can be a -mats job)");
        System.out.println("   -full <dir>    - triages local job where <dir> points to the $T_WORK directory");
        System.out.println("NOTES: this command will write files into the current directory.  Thus typically");
        System.out.println("       you will want to start out in an empty directory for this.");
        System.out.println("       Subsequently use -httpd option on the triage command to start the HTTP server.");
        System.out.println("       Run -full without additional arguments to see a synopsis of other options.");
    }

    static void explainTiming(String pref) {
        if (pref == null) {
            pref = "";
        }
        System.out.println(pref + "Synopsis for testing displaying the time that various testing activities take:");
        System.out.println("   java -jar triage.jar -timing [options] <directory_to_report_on>");
        System.out.println("Options for reporting of test times:");
        System.out.println("  -html <file>   - generates HTML file (which can be served with -httpd option)");
        System.out.println("  -csv <file>    - generates a CSV file of events for reading by Excel etc.");
        System.out.println("  -csvMaxLevel <level>  - specify maximum nesting level for CSV file. (Default 5)");
        System.out.println("  -submit \"MMM DD HH:MM\" - specify time of farm submission in UTC time.");
        System.out.println("                           The format corresponds to what is reported with farm showjobs -detail command.");
        System.out.println("  -areadesc - description of areas with assignable tests or targets to account time.");
        System.out.println("              (default: " + TRIAGE_GLOBAL_DEFAULT_DIR + "/" + TRIAGE_AREA_FILE + ")");
        System.out.println("  -flatten       - prune out seemingly superfluous nodes to reduce nesting.");
        System.out.println("  -detail        - display additional details, such as runtimes for individual tests.");
    }

    static void explainBaseDelta(String pref) {
        if (pref == null) {
            pref = "";
        }
        System.out.println(pref + "Synopsis for comparison of logfile against a baseline logfile.");
        System.out.println("   java -jar triage.jar -basedelta  [options]   <baseline-log-file>  <log-file>");
        // if (message == null) {
        System.out.println("Shows lines in log-file that are not in baseline-log-file.");
        System.out.println("Various heuristic filtering is applied to reduce redundant information. This includes:");
        System.out.println("* reducing known settings (host, label, view) to a token");
        System.out.println("* reducing various patterns (timestamps, durations, URLs) to tokens");
        System.out.println("* omitting lines with certain patterns, and");
        System.out.println("* removing duplications in the generated report.");
            // System.out.println("");
        // }
        System.out.println("Input: <baseline-log-file> and <log-file> are referenced as files or as URLs.");
        System.out.println("       URLs to HTML triage pages are also supported.");
        System.out.println("Output: Lines in <log-file> that are NOT in <baseline-log-file> are shown on stdout");
        System.out.println("        with '!' prefix. Additional context lines are provided around them.");
        // System.out.println("");
        System.out.println("Options:");
        System.out.println("  -verbose or -v    - verbose output: will not omit lines or remove dups in output.");
        System.out.println("  -debug or -d      - debug output: display processed line patterns.");
        System.out.println("  -context <number> - how many lines of context to show (default: " + DeltaDiff.contextLength + ")");
        System.out.println("  -html <htmlfile>  - generates HTML output into htmlfile instead of writing to stdout.");
        System.out.println("  -rules <rulefile> - take rules from <rulefile>. If it does not exist, generate template into it.");
        System.out.println("  -prepend-rules <rulefile> - prepend rules from <rulefile> to existing rules.");
    }

    static void explainHttpd(String pref) {
        if (pref == null) {
            pref = "";
        }
        System.out.println(pref + "Synopsis for http server:");
        System.out.println("   java -jar triage.jar -httpd <directory> <port>");
        System.out.println("Starts a micro-HTTP server that serves pages starting at the root <directory>.");
    }
    
    static void explainMailMap(String pref) {
        if (pref == null) {
            pref = "";
        }
        System.out.println(pref+ "Synopsis for Mapping a list of Guids and mailIds to the proper MailIds");
        System.out.println("   java -jar triage.jar -mailmap  [Options] <List of comma or space-separated guids, guidMailIds, etc>");
        System.out.println("Options:");        
        System.out.println("  -comma or -c    - Output is a Comma-separated list of mailIds(default)");
        System.out.println("  -space or -s    - Output is a Space-separated list of mailIds");
    }


    private static void explainAll() {
        System.out.println("Triage v1.6.4 from 05-Oct-2012 is a tool that can:");
        System.out.println("   (A) triage test suite regression results to standard output");
        System.out.println("   (B) triage (Farm) regression results into a web site; perform farm work archive extraction.");
        System.out.println("   (C) compare log file(s) to baseline log file(s) producing text or html output");
        System.out.println("   (D) serve up HTTP pages, including those from (B) and (C)");
        System.out.println("   (E) Map a list of Guids and mailIds, to the associated proper user mailIDs");
        System.out.println("The initial command line option determines which of these functionalities is desired.");
        System.out.println("See also: http://stjpg.us.oracle.com/Wiki.jsp?page=TriageTool e-mail: Ekkehard.Rohwedder@oracle.com");
        explainTriaging("\n(A) ");
        explainFullTriage("\n(B) "); 
        explainBaseDelta("\n(C) "); 
        explainHttpd("\n(D) ");
        explainMailMap("\n(E)");

    }


    public static void explain(String message) {

        if (isHelp(message)) {
            // skip
        } else if (message.startsWith("SEVERE:")) {
            System.out.println(message);
        } else {
            System.out.println("SEVERE: " + message);
        }

        if (isHelp(message)) {
            explainAll();
            System.exit(0);
        } else {
            System.exit(1);
        }
    }

    private static boolean isHelp(String s) {
        if (s == null
            || s.equalsIgnoreCase("help")
            || s.equalsIgnoreCase("-help")
            || s.equalsIgnoreCase("--help")
            || s.equalsIgnoreCase("h")
            || s.equalsIgnoreCase("-h")
            || s.equalsIgnoreCase("--h")) {
            return true;
        }
        return false;
    }

}
