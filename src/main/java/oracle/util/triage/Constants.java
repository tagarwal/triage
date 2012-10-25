package oracle.util.triage;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.File;
import java.io.FilenameFilter;

public interface Constants {

    /* Fully print out information above the cutoff size. */
    int CUTOFF_LENGTH = 300;

    /* How many messages with the same text to be displayed. */
    int SUPPRESS_EVENTS_COUNT = 35;

    String NL = "\n";

    int FATAL  = 0;
    int SEVERE = 1;
    int WARNING = 2;
    int INFO   = 3;
    int FINE   = 4;
    int FINER  = 5;
    int FINEST = 6;

    String DIFF_SUFFIX = ".dif";
    String SUCC_SUFFIX = ".suc";
    String JTS_SUFFIX  = ".jts";
    String JTL_SUFFIX  = ".jtl";
    String XML_SUFFIX  = ".xml";
    String WORKDIR_FILE  = "workdir.tar.gz";
    String WORKDIR_SUFFIX  = ".tar.gz";

    String TESTLOGIC_LOG_FILE = "testlogic.log";

    String DEFAULT_ARCHIVE_FILE  = "./tmp.jar";

    String TRIAGE_WIKI_FILE  = "triage.wiki";
    String TRIAGE_SUITE_FILE = "triage.suites";
    String TRIAGE_AREA_FILE  = "triage.areas";

    String DEFAULT_LABEL_PREFIX = "J2EE_MAIN_GENERIC";
    String FARM_JOB_PREFIX = "//dlsun167.us.oracle.com:7779/stapps/farm/txndumper2.jsp?pp_intg=0&pp_txn="
                             + DEFAULT_LABEL_PREFIX + "_T";
    String LABEL_BROWSER_PREFIX = "//dstintg.us.oracle.com/products/";
    String LABEL_BROWSER_INDEXFILE = "nindex.html";

    String[] TIMESTAMP_PATTERNS = new String[] {
        ".NNNN.NN.NN.NNNN.log",
        "NNNN-NN-NN_NN-NN-NN-PM.log",
        "NNNN-NN-NN_NN-NN-NNPM.log",
    };

    String[] TIMESTAMP_FORMATS = new String[] {
        "yyyy.MM.dd.HHmm",
        "yyyy-MM-dd_hh-mm-ss-aa",
        "yyyy-MM-dd_hh-mm-ssaa",
    }; 

    String MARKER_PATTERN = ".NNNN.NN.NN.NNNN.log";
    String MARKER_PLACEHOLDER = ".%TIMESTAMP%.log";

    String[] IMPORTANT_EVENTS = new String[] {
        " [FATAL",
        " [ERROR",
        " [WARNING",
    };

    String[] IMPORTANT_WLS_EVENTS = new String[] {
        " <Fatal> ", " <Severe> ",
        " <Error> ",
        " <Alert> ", " <Notice> ",
    };

    DateFormat DATE_FORMAT_1 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    DateFormat DATE_FORMAT_2 = new SimpleDateFormat("MMM dd HH:mm:ss yyyy");
    DateFormat DATE_FORMAT_3 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS");
    DateFormat DATE_FORMAT_4 = new SimpleDateFormat("yyyy.MM.dd.HHmm");
    DateFormat DATE_FORMAT_5 = new SimpleDateFormat("MMM dd yyyy HH:mm:ss z");
    DateFormat DATE_FORMAT_6 = new SimpleDateFormat("MMM dd HH:mm yyyy z");
    DateFormat DATE_FORMAT_7 = new SimpleDateFormat("yyyy.MM.dd : HH:mm:ss z");
    DateFormat DATE_FORMAT_8 = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SSS z");
    DateFormat DATE_FORMAT_9 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
    DateFormat DATE_FORMAT_10 = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z");
    DateFormat DATE_FORMAT_11 = new SimpleDateFormat("MM/dd HH:mm yyyy z");
    DateFormat DATE_FORMAT_12 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,SSS z");

    // String FARM_TIMEZONE = "UTC";
    String FARM_TIMEZONE = "-0000";
    DateFormat TIMESTAMP_FORMAT_1 = new SimpleDateFormat("HH:mm:ss");
    DateFormat TIMESTAMP_FORMAT_2 = new SimpleDateFormat("HH:mm:ss.SSS");

    // Specification of timezone for the XML files reported by the farm

    /** Epsilon value used when comparing times. */
    float EPSILON = 0.06f;


    String UNASSIGNED_AREA = "*UNASSIGNED*";

    // String[] LOG_FILES = new String[] { "build.log", "builder.log", 
    //                                    "regress.log", "SnapshotTester.log" };

    /* location of triage-related files */

   /* String TRIAGE_GLOBAL_DEFAULT_DIR = "/net/stjpg.us.oracle.com/public/erohwedd/triage"; */
   String TRIAGE_GLOBAL_DEFAULT_DIR = System.getenv("ADE_VIEW_ROOT") + "/j2ee/utl/triage";

    /** naming of failures and successes */
    String JUNIT_DIFF  = "failure";
    String JUNIT_DIFFS = "failures";
    String JUNIT_SUCC  = "success";
    String JUNIT_SUCCS = "successes";

    String T_WORK = "T_WORK";

    int NO_COMMAND           = 0;
    int DIR_TRIAGE_COMMAND   = 1;
    int TWORK_TRIAGE_COMMAND = 2;
    int MATS_TRIAGE_COMMAND  = 3;
    int BUILD_LOG_COMMAND    = 4;
    int FARM_TRIAGE_COMMAND  = 5;

    int MAX_DTE_JOBID_RANGE = 20;


    /** Maximum percentage of number of tests in a test suite to report as "slow" */
    float MAX_TEST_PERCENTAGE = 0.50f;

    /** Minimum time to be used by a test (in seconds) in order to show up in our statistics */
    float MIN_TEST_TIME = 0.050f;

    /** Mimimum gap time rquired to show up in top level test (in seconds) */
    float MIN_GAP_TIME = 60.0f;

    /** Test triaging */
    int UNKNOWN_DIFF_SUCC     = 0;
    int JUNIT_DIFF_SUCC       = 1;
    int JDEV_JUNIT_DIFF_SUCC  = 2;
    int ANT_LOG_DIFF_SUCC     = 3;
    int DIR_LOG_DIFF_SUCC     = 4;
    int ZERO_LENGTH_DIFF_SUCC = 5;
    int HANG_DIFF             = 6;

    /** Various "virtual" file types */
    int FILE_TYPE = 1;
    int ZIP_TYPE = 2;
    int TAR_TYPE = 3;
    int COMPRESSED_TAR_TYPE = 4;
    int FARM_TYPE = 5;
    // int MULTI_TYPE = 6;  // multiple virtual dirs enclosed

    int BUFF_LENGTH = 1042;

    int BLACK_COLOR = 0;
        String BLACK_COLOR_BITS = "000000";
    int RED_COLOR = 1;
        String RED_COLOR_BITS   = "990000";
    int YELLOW_COLOR = 2;
        String YELLOW_COLOR_BITS = "cc9900";
    int GREEN_COLOR = 3;
        String GREEN_COLOR_BITS = "006600";
    int BLUE_COLOR = 4;
        String BLUE_COLOR_BITS  = "000066";
    int GRAY_COLOR = 5;
        String GRAY_COLOR_BITS  = "777777";


    String[] COLORS = new String[] {
        BLACK_COLOR_BITS,
        RED_COLOR_BITS,
        YELLOW_COLOR_BITS,
        GREEN_COLOR_BITS,
        BLUE_COLOR_BITS,
        GRAY_COLOR_BITS,
    };

    String CSS_INCLUDE =                 "/jscript/mktree.css";
    String JAVASCRIPT_INCLUDE  =         "/jscript/mktree.js";
    String INITIAL_TREE_PANE_CONTENT   = "/jscript/tree.html";
    String INITIAL_DETAIL_PANE_CONTENT = "/jscript/detail.html";

    String HEADER_INCLUDES = "<script language=\"JavaScript\" src=\"" + JAVASCRIPT_INCLUDE + "\"></script>" + NL
                           + "<link rel=\"stylesheet\" href=\"" + CSS_INCLUDE + "\">" + NL;

     String BULLET_ICON_FILE =   "/jscript/bullet.gif";
     String CHECK_ICON_FILE =    "/jscript/check.gif";
     String INFO_ICON_FILE =     "/jscript/info.gif";
     String LINK_ICON_FILE =     "/jscript/link.gif";
     String MINUS_ICON_FILE =    "/jscript/minus.gif";
     String PLUS_ICON_FILE =     "/jscript/plus.gif";
     String STOP_ICON_FILE =     "/jscript/stop.gif";
     String ERRMSG_ICON_FILE =   "/jscript/errmsg.gif";
     String TRIANGLE_ICON_FILE = "/jscript/tri.gif";
     String RED_X_ICON_FILE =    "/jscript/x.gif";
     String ZOOM_IN_ICON_FILE  = "/jscript/zoom_in.gif";
     String ZOOM_OUT_ICON_FILE = "/jscript/zoom_out.gif";
     String TEXT_ICON_FILE =     "/jscript/text.gif";
     String ENDTEXT_ICON_FILE =  "/jscript/endtext.gif";
     String TREE_ICON_FILE =     "/jscript/tree.gif";
     String LETTER_ICON_FILE =   "/jscript/letter.gif";
     String WARNING_ICON_FILE =  "/jscript/warning.gif";

     int BULLET_ICON =   0;
     int CHECK_ICON =    1;
     int INFO_ICON =     2;
     int LINK_ICON =     3;
     int MINUS_ICON =    4;
     int PLUS_ICON =     5;
     int STOP_ICON =     6;
     int ERRMSG_ICON =   7;
     int TRIANGLE_ICON = 8;
     int RED_X_ICON =    9;
     int ZOOM_IN_ICON  = 10;
     int ZOOM_OUT_ICON = 11;
     int TEXT_ICON     = 12;
     int ENDTEXT_ICON  = 13;
     int TREE_ICON     = 14;
     int LETTER_ICON   = 15;
     int WARNING_ICON  = 16;

     String[] HTML_RESOURCES = new String[] {
        BULLET_ICON_FILE,
        CHECK_ICON_FILE,
        INFO_ICON_FILE,
        LINK_ICON_FILE,
        MINUS_ICON_FILE,
        PLUS_ICON_FILE,
        STOP_ICON_FILE,
        ERRMSG_ICON_FILE,
        TRIANGLE_ICON_FILE,
        RED_X_ICON_FILE,
        ZOOM_IN_ICON_FILE,
        ZOOM_OUT_ICON_FILE,
        TEXT_ICON_FILE,
        ENDTEXT_ICON_FILE,
        TREE_ICON_FILE,
        LETTER_ICON_FILE,
        WARNING_ICON_FILE,

        CSS_INCLUDE,
        JAVASCRIPT_INCLUDE,
        INITIAL_TREE_PANE_CONTENT,
        INITIAL_DETAIL_PANE_CONTENT,
    };


    public static String[] ABSOLUTE_DIR_PREFIXES = new String[] {
        "/net/",
        "/ade/",
        "/ade_autofs/",
        "/scratch/",
    };

    public static String[] ZIP_INFIXES = new String[] {
        ".jar::",
        ".zip::",
    };

    
    int MAX_HINT_CHAR_PER_LINE = 80;
    int MAX_HINT_PIXEL_PER_LINE = 550;
    int MAX_HINT_NUM_LINES = 15;

    int MAX_HINT_CHARACTERS = MAX_HINT_NUM_LINES * MAX_HINT_CHAR_PER_LINE;


    // Have at most 32 MB in a log file
    long MIN_BIG_FILE = 1024l * 1024l;
    // long MAX_INPUT_STREAM_SIZE = 16l * MIN_BIG_FILE;
    long MAX_INPUT_STREAM_SIZE = 32l * MIN_BIG_FILE;


    /* this filter covers files that get archived  */
    FilenameFilter TRIAGE_FILES = new FilenameFilter() {

            public boolean accept(File f, String s) {
                 int pos = s.lastIndexOf("/");
                 String name = (pos >= 0) ? s.substring(pos + 1) : s;
                 String path = (pos > 0)  ? s.substring(0,pos) : "";

                 if (LOG_ANALYSIS_FILES.accept(f, s)) {
                     // include all files required for log analysis
                     return true;
                 } else if (f != null && f.getParentFile() != null &&
                            f.getParentFile().getName().startsWith("incdir_")) {
                    // include incident files, when not via a TAR stream
                    // this can include binary files that shouldn't be
                    // processed by the MessageRepository class
                    return true;
                 } else if (name.equals("readme.txt") || 
                            name.endsWith(".dmp")) {
                   // include incident files, when via a TAR stream
                   // this can include binary files that shouldn't be
                   // processed by the MessageRepository class
                   return true;
                 }

                 return false;
             }
     };

    /* this filter covers files handles by MessageRepository */
    FilenameFilter LOG_ANALYSIS_FILES = new FilenameFilter() {

            public boolean accept(File f, String s) {
                 int pos = s.lastIndexOf("/");
                 String name = (pos >= 0) ? s.substring(pos + 1) : s;
                 String path = (pos > 0)  ? s.substring(0,pos) : "";

                 if (name.endsWith(XML_SUFFIX)
                     && (name.startsWith("TEST-")
                         || name.startsWith("TESTS-")
                         || name.startsWith("build")
                         || name.indexOf("build") > 0)) {
                     return true;
                 } else if (name.endsWith(JTS_SUFFIX)
                            || name.endsWith(JTL_SUFFIX)) {
                     return true;
                 } else if (Util.isLogFile(name)) {
                     return true;
                 } else if (name.endsWith(".suc")
                            || name.endsWith(".dif")
                            || name.endsWith(".tlg")) {
                     return true;
                 } else if (name.endsWith(".farm.out")
                            || name.endsWith(".history.log")) {
                     return true;
                 } else if (name.endsWith(".properties")) {
                     return true;
                 } else if (name.equalsIgnoreCase("labels.txt")) {
                     return true;
                 }

                 return false;
             }
     };

     String[] TRACE_PREFIXES_TO_ELIDE = new String[] {
          "org.apache.tools.ant.",
          "junit.",
          "sun.reflect.",
          "java.lang.",
          "org.testlogic.",
          "net.sf.antcontrib.",
          "com.oopsconsultancy.xmltask.",
     };

     String BEGIN_JAVA_COMPILATION = "---------- LAST JAVA COMPILATION ----------";
     String END_JAVA_COMPILATION   = "---------- END  JAVA COMPILATION ----------";

}
