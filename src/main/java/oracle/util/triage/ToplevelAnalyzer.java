package oracle.util.triage;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.LineNumberReader;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Enumeration;

import java.util.jar.JarFile;
import java.util.jar.JarEntry;

public class ToplevelAnalyzer implements Constants {

    static final boolean DEBUG = Full.DEBUG;
   // static final boolean DEBUG = true;


    public static final int UNKNOWN_DIFF_SUCC     = 0;
    public static final int JUNIT_DIFF_SUCC       = 1;
    public static final int JDEV_JUNIT_DIFF_SUCC  = 2;
    public static final int ANT_LOG_DIFF_SUCC     = 3;
    public static final int DIR_LOG_DIFF_SUCC     = 4;
    public static final int ZERO_LENGTH_DIFF_SUCC = 5;
    public static final int TESTLOGIC_DIFF_SUCC   = 6;

    public ToplevelAnalyzer(int pLevel, File fil, File filParent, boolean diffOnly, JarFile supplement) throws Exception {

        if (DEBUG) {
            System.out.println("DEBUG: ==== ToplevelAnalyzer of " + fil + " child of " + filParent + " ====");
        }

        level = pLevel; 
        topTest = new ToplevelTest(fil, filParent);
        stats = new Stats();
        archive = supplement;

        boolean showDetail = INFO < level;
        boolean showFullDetail = FINE < level;

        if (topTest.getFileName().equals("job_hang_and_killed.dif")) {

            /* --- A Hang --- */
            topTest.setKind(HANG_DIFF);

            populateHangInformation(topTest.lastModified(), topTest.getParentFile());
      
        } else if (topTest.length() == 0) {

            /* --- A Zero-Length Diff --- */
            topTest.setKind(ZERO_LENGTH_DIFF_SUCC);

        } else {

            /* --- Peruse file to see what we might have --- */
            StringBuffer sb = new StringBuffer();
            LineNumberReader lnr = topTest.getLineNumberReader();

            boolean squareBracket = false;
            String[] prefix = null;
            String subdirCheck = null;
            String testlogicLog = null;

            Set<String> workDirSet = new HashSet<String>();
            Set<String> logFileSet = new HashSet<String>();
          
            int pos;
            String line;
            while ( (line = lnr.readLine()) != null ) {
                if (line.startsWith("suc.dif.file=")) {
                    String sucDifFile = line.substring("suc.dif.file=".length());
                    if (sucDifFile.endsWith(topTest.getFileName())) {
                        if (prefix == null) {
                            prefix = new String[1];
                        }
                        prefix[0] = sucDifFile.substring(0, sucDifFile.length() - topTest.getFileName().length());
                        pos = prefix[0].indexOf("/AdfRunner/");
                        if (pos >= 0) {
                            prefix[0] = prefix[0].substring(0, pos + "/AdfRunner/".length());
                        }
                        // System.out.println("Got prefix "+prefix[0]+" from suc/dif file "+sucDifFile);
                    }
                } else if (line.startsWith("   Work.dir : ")) {
                    workDirSet.add(line.substring("   Work.dir : ".length()));
                } else if (line.startsWith("     work.dir=")) {
                    workDirSet.add(line.substring("     work.dir=".length()));
                } else if (line.startsWith("Check log file in subdir ")
                       && (pos = line.indexOf(" for detail !")) > 0) {
                    subdirCheck = line.substring("Check log file in subdir ".length(), pos);
                } else if (line.startsWith("     log.file=")) {
                    logFileSet.add(line.substring("     log.file=".length()));
                } else if (!squareBracket && line.indexOf("]") > 0 && line.trim().startsWith("[") ) {
                    squareBracket = true;
                } else if (line.startsWith("testlogic.log file: ")
                           || line.startsWith("UTP_LOG_FILE: ")) {
                    testlogicLog = (line.startsWith("UTP_LOG_FILE:")) 
                                    ? line.substring("UTP_LOG_FILE: ".length()).trim()
                                    : line.substring("testlogic.log file: ".length()).trim();
                    int lpos = testlogicLog.indexOf(",");
                    if (lpos > 0) {
                        testlogicLog = testlogicLog.substring(0, lpos);
                    }
                    lpos = testlogicLog.indexOf("/work/");
                    if (lpos > 0) {
                        testlogicLog = testlogicLog.substring(lpos + "/work/".length());
                    }

                    if (prefix == null) {
                        prefix = new String[1];
                    }
                    // int lpos = testlogicLog.lastIndexOf("/");
                    // prefix[0] = testlogicLog.substring(0,lpos);

                    prefix[0] = testlogicLog;
                    workDirSet.add(prefix[0]);
                }
                   
                sb.append(line);
                sb.append(NL);
            }
            content = sb.toString();
            lnr.close();



            /* --- ensure that our prefix -if any- contains ../work/ as well as ../oracle/work/ --- */
            if (prefix != null && testlogicLog == null) {
                pos = prefix[0].indexOf("/work/");
                if (pos >= 0) { 
                    String[] tmp = new String[2];
                    tmp[0] = prefix[0];
                    int pos2 = prefix[0].indexOf("/oracle/work/");
                    if (pos2 >= 0) {
                        tmp[1] = prefix[0].substring(0, pos2) + prefix[0].substring(pos);
                    } else {
                        tmp[1] = prefix[0].substring(0, pos) + "/oracle" + prefix[0].substring(pos);
                    }
                    prefix = tmp;
                }
            }



            if (prefix != null) {

                /* --- A JUnit test case, a TestLogic testcase, or a JUnit test case a la JDeveloper --- */
                if (testlogicLog != null) {
                    topTest.setKind(TESTLOGIC_DIFF_SUCC);
                } else {
                    topTest.setKind(JUNIT_DIFF_SUCC);
                }

                List<String> unitTestRoots = new ArrayList<String>();

                Iterator it = workDirSet.iterator();
                while (it.hasNext()) {
                    String dir = (String) it.next();
                    String pref = null;

                    if (DEBUG) {
                        System.out.println("DEBUG: workDirSet.directory: " + dir);
                        System.out.println("DEBUG: prefix: " + prefixToString(prefix));
                    }

                    // if (testlogicLog != null && dir != null && !dir.equals(null)) {
                    //    System.out.println("testlogicLog = " + testlogicLog);
                    //    System.out.println("comparePrefixAndChop(" + dir + ", " + prefixToString(prefix) + ") = '" + comparePrefixAndChop(dir, prefix) + "'");
                    // }

                    if (dir == null || dir.equals("null")) {
                        // skip
                    } else if ( testlogicLog != null || (pref = comparePrefixAndChop(dir, prefix)) != null) {
                        if (testlogicLog != null) {
                            pref = prefix[0];
                        }
                        dir = pref;

                        if (DEBUG) {
                            System.out.println("DEBUG: computed pref " + pref + " matches.");
                        }

                        if (archive == null) {
                            File root = new File(topTest.getParentFile(), dir);
                            unitTestRoots.add(root.toString());
                            addJunitTests(root);
                        } else {
                            unitTestRoots.add(dir);
                            List<String> roots = unitTestJunitJar.get(archive);
                            if (roots == null) {
                                roots = new ArrayList<String>();
                                unitTestJunitJar.put(archive, roots);
                            }
                            roots.add(dir);
                        }
                    } else if ( (pos = dir.indexOf("/jtwork/")) >= 0 ) {
                        System.out.println("Dir " + dir + " does not start with " + prefixToString(prefix) + " but contains /jtwork/. A JDev usecase.");
                        // This is possibly a JDev Unit testcase, look for jtwork.jar
                        dir = dir.substring(pos + 1);
                        JarFile jtwork = getJtworkJar(topTest.getParentFile());
                        if (jtwork == null) {
                            System.out.println("SEVERE: Unable to obtain jtwork.jar in " + topTest.getParentFile() +
                                               " for test work directory: " + dir);
                        } else {
                            topTest.setKind(JDEV_JUNIT_DIFF_SUCC);

                            unitTestRoots.add(dir);

                            List<String> roots = unitTestJunitJar.get(jtwork);
                            if (roots == null) {
                                roots = new ArrayList<String>();
                                unitTestJunitJar.put(jtwork, roots);
                            }
                            roots.add(dir);
                        }
                    } else {
                        System.out.println("SEVERE: Unable to construct work directory: " + dir +
                                           " where common prefix is " + prefixToString(prefix) );
                    }
                }

                if (DEBUG) {
                    System.out.println("DEBUG: tracking build logs.");
                }
                List<String> buildLogs = new ArrayList<String>();
                it = logFileSet.iterator();
                while (it.hasNext()) {
                    String logFile = (String) it.next();
                    String pref = null;
                    if ( (pref = comparePrefixAndChop(logFile, prefix)) != null ) {
                        logFile = pref;

                        if (archive != null && archive.getEntry(logFile) != null) {
                            buildLogs.add(logFile);
                        } else {
                            File buildFile = new File(topTest.getParentFile(), logFile);
                            if (buildFile.exists()) {
                                buildLogs.add(buildFile.toString());
                            } else {
                                if (Main.getForceTarGz()) {
                                    // nothing
                                } else {
                                    System.out.println("WARNING: unable to find build log " + logFile +
                                                       (Main.getForceTarGz() ? ""
                                                        : " (try to triage with -targz option)"));
                                }
                            }
                        }
                    } else {
                        System.out.println("WARNING: unable to determine where to look for build log " + logFile);
                    }
                }

                if (DEBUG) {
                    System.out.println("DEBUG: adding JUnit tests.");
                }
                if (archive != null
                    || topTest.getKind() == JDEV_JUNIT_DIFF_SUCC
                    || topTest.getKind() == TESTLOGIC_DIFF_SUCC) {
                    Iterator<JarFile> it2 = unitTestJunitJar.keySet().iterator();
                    while (it2.hasNext()) {
                        JarFile jf = it2.next();
                        if (DEBUG) {
                            System.out.println("DEBUG: adding tests from JAR file " + jf.getName());
                        }
                        addJarJunitTests(jf, unitTestJunitJar.get(jf));
                    }
                }

                for (int i = 0; i < unitTests.size(); i++) {
                    topTest.addDiffs(unitTests.get(i).getDiffs());
                    topTest.addSuccs(unitTests.get(i).getSuccs());
                }


                // ----------------------------------------------------------------------------- //

                sb = new StringBuffer();
                StringBuffer hb = null;
                if (Main.getHtmlStream() != null) {
                    hb = new StringBuffer();
                }

                String difSucHeader = topTest.getDiffs() + " " +
                                ((topTest.getDiffs() == 1) ? JUNIT_DIFF : JUNIT_DIFFS) + ", " +
                                topTest.getSuccs() + " " +
                                ((topTest.getSuccs() == 1) ? JUNIT_SUCC : JUNIT_SUCCS);
                if (unitTests.size() > 1) {
                    difSucHeader += ", " + unitTests.size() + " suites";
                }

                String topHeader = "";
                if (topTest.getTests() > 1) {
                    topHeader = " (" + difSucHeader + ")";
                }

                if (hb != null) {
                     // if (topTest.getTests() > 1) {
                     //    hb.append( ((DEBUG) ? "[TLA:300]" : "") + difSucHeader);
                     // }
                }

                if (unitTests.size() == 1 || (unitTests.size() > 1 && diffOnly && topTest.getDiffSuites() <= 1)) {
                    // sb.append("DETAILS OF TEST "+f);
                    // sb.append(NL);
                    if (!topHeader.equals("")) {
                        sb.append(topHeader);
                        sb.append(NL);
                    }

                } else if (unitTests.size() > 1) {
                    String header = (showFullDetail) 
                                     ? topHeader + NL + 
                                       "<<< TEST " + topTest.getFile() + " CONSISTS OF " + unitTests.size() + " TEST SUITES >>>"
                                     : topHeader;
                    sb.append(header);
                    sb.append(NL);
                    
                    if (showFullDetail || (diffOnly && topTest.getDiffs() > 0) ) {
                        int count = 0;
                        for (int i = 0; i < unitTests.size(); i++) {
                            if (showDetail 
                                || (diffOnly && unitTests.get(i).getDiffs() > 0)) {
                                count++;
                                sb.append("<");
                                sb.append(count);
                                sb.append("> ");
                                sb.append(unitTests.get(i).getName());
                                sb.append(NL);
                            }
                            // for (int i=0; i<header.length(); i++) {
                            //  sb.append('!');
                            // }
                        }
                    }
                } else {
                    if (buildLogs.size() > 0) {
                        StringBuffer ssb = new StringBuffer();
                        ssb.append(" No test results found - this may be a test build issue. ");
                        ssb.append(buildLogs.size());
                        ssb.append(" build file");
                        if (buildLogs.size() > 1) {
                            ssb.append("s");
                        }

                        sb.append(ssb);
                        sb.append(":");
                        sb.append(NL);


                        StringBuffer hhb = new StringBuffer();
                        for (int i = 0; i < buildLogs.size(); i++) {
                            String log = buildLogs.get(i);
                            String lastSteps = null;
                            if (buildLogs.size() > 1) {
                                lastSteps = (new BuildLog(log, archive)).printLastSteps(i + 1);
                            } else {
                                lastSteps = (new BuildLog(log, archive)).printLastSteps();
                            }
                            sb.append(lastSteps);
                            if (hb != null) {
                                hhb.append(Util.makeLi(Util.makeHtml(true, "code", lastSteps)) + NL);
                            }
                        }

                        if (hb != null) {
                            hb.append(((DEBUG) ? "[TLA:368]" : "") + ssb);
                            hb.append(NL);
                            hb.append(Util.makeUl(hhb.toString()));
                        }
                        
                    } else {
                        StringBuffer ssb = new StringBuffer();

                        StringBuffer seen = new StringBuffer();
                        StringBuffer notFound = new StringBuffer();
                        int notFoundCount = 0;
                        for (int i = 0; i < unitTestRoots.size(); i++) {
                           // System.out.println("unitTestRoots(" + i + ") = " + unitTestRoots.get(i));

                            if (testRootsSeen.get(unitTestRoots.get(i)) == null) {
                                notFound.append(unitTestRoots.get(i));
                                notFound.append(NL);
                                notFoundCount++;
                            } else {
                                seen.append(unitTestRoots.get(i));
                                seen.append(" already triaged in ");
                                seen.append(testRootsSeen.get(unitTestRoots.get(i)));
                                seen.append(NL);
                            }
                        }

                        if (unitTestRoots.size() == 0 
                            || notFound.length() > 0) {
                            ssb.append(" WARNING: UNABLE TO DETERMINE DETAIL TEST RESULTS FROM:");
                            ssb.append(NL);
                            ssb.append(topTest.getFile());
                            ssb.append(NL);
                            if (unitTestRoots.size() == 0) {
                                ssb.append("Missed the \"work.dir\" entries.");
                                ssb.append(NL);
                            } else if (notFound.length() > 0) {

                                if (topTest.isTestLogicCatchall()) {
                                    ssb.append("Could not find any testlogic log files ");
                                    ssb.append(NL);
                                } else {
                                    if (topTest.getKind() == TESTLOGIC_DIFF_SUCC) {
                                        ssb.append("Could not find testlogic.log. Is this a test build issue?");
                                    } else {
                                        ssb.append("Could not find any files TEST-*.xml or TESTS-*.xml");
                                    }
                                    ssb.append(NL);
                                }
                                if (topTest.getKind() == JDEV_JUNIT_DIFF_SUCC) {
                                    Iterator<JarFile> it2 = unitTestJunitJar.keySet().iterator();
                                    while (it2.hasNext()) {
                                        JarFile jf = it2.next();
                                        boolean found = false;
                                        for (int i = 0; !found && i < unitTestRoots.size(); i++) {
                                            if (unitTestJunitJar.get(jf).contains(unitTestRoots.get(i))) {
                                                found = true;
                                            }
                                        }
                                        ssb.append("from " + jf.getName() + " ");
                                        ssb.append(NL);
                                    }
                                }
                                if (notFoundCount == 1) {
                                    if (topTest.getKind() == TESTLOGIC_DIFF_SUCC) {
                                        ssb.append("did not see: ");
                                    } else { 
                                        ssb.append("under the work.dir: ");
                                    }
                                } else {
                                    if (topTest.getKind() == TESTLOGIC_DIFF_SUCC) {
                                        ssb.append("did not see the following: ");
                                    } else { 
                                        ssb.append("under the following work.dir directories:");
                                    }
                                    ssb.append(NL);
                                }
                                ssb.append(notFound);
                            }
                        }

                        if (seen.length() > 0) {
                            ssb.append(" INFO: " + seen);
                        }

                        sb.append(ssb);
                        if (hb != null) {
                            // if (unitTests.size() != 1) {
                            hb.append( ((DEBUG) ? "[TLA:453]" : "") + ssb);
                            // }
                        }
                    }
                }

                if ( showFullDetail || (diffOnly && topTest.getDiffs() > 0)) {
                    int count = 0;
                    for (int i = 0; i < unitTests.size(); i++) {
                        if (showFullDetail || (diffOnly && unitTests.get(i).getDiffs() > 0)) {
                            String ss = unitTests.get(i).toString(getLevel(), diffOnly);

                            if (ss != null) {
                                count ++;
                                if ( (showFullDetail && unitTests.size() > 1)
                                     || (diffOnly && topTest.getDiffSuites() > 1)) {
                                    sb.append("<");
                                    sb.append(count);
                                    sb.append("> ");
                                }
                                sb.append(ss);
                                sb.append(NL);
                            }
                        }
                    }
                }


                if (hb != null && unitTests.size() > 0) {
                    List<String> tests = new ArrayList<String>();
                    for (UnitTestReport ut : unitTests) {
                        String uts = ut.toHtml(FINER, diffOnly);
                        if (uts != null) {
                            tests.add(uts);
                        }
                    }
                    if (tests.size() == 1) {
                        hb.append( /*"<br>" + NL +*/ ((DEBUG) ? "[TLA:489]" : "") + tests.get(0));
                    } else {
                        StringBuffer uhb = new StringBuffer();
                        for (String uts : tests) {
                            uhb.append(Util.makeLi(uts));
                        }
                        hb.append(((DEBUG) ? "[TLA:495]" : "") + Util.makeUl(uhb.toString()));
                    }
                }

                summary = sb.toString();
                if (hb != null) {
                    htmlSummary = hb.toString();
                }
                
            } else if (topTest.getFileName().endsWith(SUCC_SUFFIX)) {

                /* --- A Suc of some other format. We do not care about it. --- */

                // will not look into any other types
                // of .suc files, since presumably these
                // will represent individual tests and
                // not any test suites.

            } else if (subdirCheck != null) {

                /* --- A diff that appears to point to its own directory. --- */

                topTest.setKind(DIR_LOG_DIFF_SUCC);

                sb = new StringBuffer();
                sb.append("LOG FILE POINTS TO THE DIRECTORY:");
                sb.append(NL);
           
                File dir = new File(topTest.getParentFile(), subdirCheck);
                sb.append(dir);
                sb.append(NL);

           
                File[] files = dir.listFiles(ERROR_FILE);
                if (files != null && files.length > 0) {
                    sb.append("You may want to check the following files:");
                    sb.append(NL);
                    for (int i = 0; i < files.length; i++) {
                        if (files[i].length() < CUTOFF_LENGTH
                            || FINE < getLevel()) {
                            sb.append(Util.readRight(files[i], getLevel()));
                            sb.append(NL);
                        } else {
                            sb.append(files[i].getName());
                            sb.append(" - ");
                            sb.append(new Date(files[i].lastModified()));
                            sb.append("  ");
                            sb.append(files[i].length());
                            sb.append(NL);
                        }
                    }
                }
                summary = sb.toString();
                htmlSummary = summary;

            } else if (squareBracket) {
                topTest.setKind(ANT_LOG_DIFF_SUCC);

                summary  = ((topTest.length() < CUTOFF_LENGTH
                        || FINE < getLevel()) 
                       ? NL + Util.readRight(topTest.getFile(), getLevel()) + NL
                       : " LOG FILE APPEARS TO BE ANT OUTPUT. " +
                         "For additional detail, see" + NL + topTest.getFile() ) + 
                      NL;

                htmlSummary = summary;
            } else {
                topTest.setKind(UNKNOWN_DIFF_SUCC);

                summary  = ((topTest.length() < CUTOFF_LENGTH
                        || FINE < getLevel()) 
                       ? NL + Util.readLeft(topTest.getFile(), getLevel()) 
                       : " UNABLE TO DETERMINE LOG FILE FORMAT. " + 
                         "For additional detail, see" + NL + topTest.getFile() ) +
                      NL;
                htmlSummary = summary;
            }
        }
    }

    public ToplevelTest getTest() {
        return topTest;
    }
    private ToplevelTest topTest;

    public Stats getStats() {
        return stats;
    }
    private Stats stats;

    public JarFile getArchive() {
        return archive;
    }
    private JarFile archive;

    private static final FilenameFilter TEST_FILE = new FilenameFilter() {
        public boolean accept(File f, String s) {
            return ((s.startsWith("TEST-") || s.startsWith("TESTS-") )
                    && s.endsWith(XML_SUFFIX))
                   || (s.endsWith(TESTLOGIC_LOG_FILE));
        }
    };

    private static final FilenameFilter SUBDIRS = new FilenameFilter() {
        public boolean accept(File f, String s) {
            return f.isDirectory() && !s.startsWith(".");
        }
    };

    private static final FileFilter ERROR_FILE = new FileFilter() {
        public boolean accept(File f) {
            return f.getName().endsWith(".err") && f.length() > 0l;
        }
    };

    private static final FilenameFilter JTWORK_FILE = new FilenameFilter() {
        public boolean accept(File f, String s) {
            return s.equals("jtwork.jar");
        }
    };


    private void addJunitTests(File root) {

        if (root.getName().endsWith(TESTLOGIC_LOG_FILE)) {
            addUnitTestLogicReport(root, stats);
        } else {
            File[] files = root.listFiles(TEST_FILE);
    
            boolean seenTESTSFile = false;
    
            for (int i = 0; files != null && i < files.length; i++) {
                if (files[i].getName().startsWith("TESTS-")) {
                    seenTESTSFile = true;
                    break;
                }
            }
    
            for (int i = 0; files != null && i < files.length; i++) {
                if (seenTESTSFile && !files[i].getName().startsWith("TESTS-")) {
                    // skip all other potential test roots
                } else if (files[i].getName().endsWith(TESTLOGIC_LOG_FILE)) {
                    addUnitTestLogicReport(files[i], stats);
                } else {
                    unitTests.add(new UnitTestReport(files[i], stats));
                }
            }
    
            files = root.listFiles(SUBDIRS);
            for (int i = 0; files != null && i < files.length; i++) {
                addJunitTests(files[i]);
            } 
        }
    }

    private static HashMap<String, String> testRootsSeen = new HashMap<String, String>();
    static void resetStaticVariables() {
        testRootsSeen = new HashMap<String, String>();
    }

    private void addUnitTestLogicReport(File f, Stats stats) {
        if (testRootsSeen.get(f.toString()) == null) {
            testRootsSeen.put(f.toString(), topTest.getName());
            unitTests.add(new UnitTestlogicReport(f, stats));
        }
    } 

    private void addJarJunitTests(JarFile jf, List<String> roots) {
        Set<String> found = new HashSet<String>();
        Enumeration enu = jf.entries();
        while (enu.hasMoreElements()) {
            JarEntry je = (JarEntry) enu.nextElement();
            String s = je.getName();
            if (   (s.endsWith(XML_SUFFIX)
                    && TEST_FILE.accept(null, s.substring(s.lastIndexOf("/") + 1)))
                || s.endsWith(TESTLOGIC_LOG_FILE) ) {
                    // if (DEBUG) { System.out.println("DEBUG: test candidate: "+s); }

                boolean match = false;
                String matchString = null;

                if (topTest.isTestLogicCatchall()) {
                    match = s.endsWith(TESTLOGIC_LOG_FILE)   
                                && testRootsSeen.get(s) == null;
                    matchString = s;
                } else {
                    for (int i = 0; i < roots.size(); i++) {
                        if (s.endsWith(TESTLOGIC_LOG_FILE)
                                && s.equals(roots.get(i))) { 
                            match = true;
                            matchString = roots.get(i);
                            break;
                        } else if (s.startsWith(roots.get(i))) {
                            match = true;
                            matchString = roots.get(i);
                            break;
                        }
                    }
                }

                String prevSeen = null;
                if (match 
                         && (prevSeen = testRootsSeen.get(matchString)) != null) {

                         // Avoid re-triaging the same test

                    if (prevSeen.equals(topTest.getName())) {
                             // avoid a silly message
                    } else {
                        System.out.println("INFO: skip triaging of " + matchString + "  in " + topTest.getName() + "."
                                                + " Previously triaged in " + testRootsSeen.get(matchString) + ".");
                    }
                   
                    match = false;
                }
                    
                if (match) {
                    if (DEBUG) {
                        System.out.println("DEBUG: FOUND MATCH: " + s);
                    }

                    found.add(matchString);
                    testRootsSeen.put(matchString, topTest.getName());

                    if (s.endsWith(TESTLOGIC_LOG_FILE)) {
                        unitTests.add(new UnitTestlogicReport(jf, je, stats, null));
                    } else {
                        unitTests.add(new UnitTestReport(jf, je, stats, null));
                    }
                } else {
                         // if (DEBUG) { System.out.println("DEBUG: no match "+s); }
                }
            }
        }

        for (int i = 0; i < roots.size(); i++) {
            if (!found.contains(roots.get(i)) && !Main.getForceTarGz()) {
                  
                if (topTest.isTestLogicCatchall() || roots.get(i).endsWith(TESTLOGIC_LOG_FILE)) {
                    System.out.println("WARNING: unable to access testlogic log file " + roots.get(i) +
                                       ((jf.getName().equals(DEFAULT_ARCHIVE_FILE)) ? ""
                                                                                    : " in " + jf.getName()) +
                                       (Main.getForceTarGz() ? ""
                                                             : " (try to triage with -targz option)"));
                } else if (jf.getName().equals(DEFAULT_ARCHIVE_FILE)) {
                    System.out.println("WARNING: No files TEST-*.xml or TESTS-*.xml found under " + roots.get(i)
                                       + (Main.getForceTarGz() ? ""
                                                               : " (try to triage with -targz option)"));
                } else {
                    System.out.println("WARNING: No files TEST-*.xml or TESTS-*.xml found under " + roots.get(i) +
                                       " in " + jf.getName()
                                       + (Main.getForceTarGz() ? ""
                                                               : " (try to triage with -targz option)"));
                }
            }
        }
    }

    private JarFile getJtworkJar(File dir) {
        File[] jarFiles = dir.listFiles(JTWORK_FILE);
        if (jarFiles == null || jarFiles.length != 1) {
            return null;
        }
        try {
            return new JarFile(jarFiles[0]);
        } catch (Exception e) {
            System.out.println("SEVERE: Unable to initialize jtwork.jar JAR from " + jarFiles[0] + ": " + e);
            e.printStackTrace();
            return null;
        }
    }

    public List<UnitTestReport> getUnitTests() {
        return unitTests;
    }
    private List<UnitTestReport> unitTests = new ArrayList<UnitTestReport>();

    public float getTime() {
        if (time < 0.0f) {
            time = 0.0f;
            for (UnitTestReport utr : getUnitTests()) {
                time += utr.getTime();
            }
        }
        return time;
    }
    private float time = -1.0f;

    private Map<JarFile, List<String>>  unitTestJunitJar = new HashMap<JarFile, List<String>>();

    String getContent() {
        return content;
    }
    private String content;

    String printTest() {
        return summary;
    }
    private String summary;

    String printHtml() {
        return htmlSummary;
    }
    private String htmlSummary;


    String printHtml(int testNumber) {
        StringBuffer sb = new StringBuffer();
        boolean isDif = topTest.getFileName().endsWith(".dif");

        String testName = testNumber + ". " + topTest.getFileName();
        if (isDif) {
            testName = "<b>" + testName + "</b>";
        }

        sb.append(
            Util.makeLi( testName +
                        ((htmlSummary == null || htmlSummary.trim().equals(""))
                         ? "" 
                         : Util.makeUl( Util.makeLi ( htmlSummary) ))
            )  );

        return sb.toString();
    }

    public int getLevel() {
        return level;
    }
    private int level;

    private void populateHangInformation(final long killTimestamp, File dir) {
        FileFilter olderFiles = new FileFilter() {
            public boolean accept(File ff) {
                return ff.isFile() && ff.lastModified() <= killTimestamp;
            }
        };
        File[] files = dir.listFiles(olderFiles);

        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int i = 0; files != null && i < files.length - 1; i++) {
                if (files[i].lastModified() > files[i + 1].lastModified()) {
                    File tmp = files[i];
                    files[i] = files[i + 1];
                    files[i + 1] = tmp;
                    sorted = false;
                }
            }
        }

        long biggestGap = 0l;
        int biggestGapIndex = -1;
        for (int i = 0; i < files.length - 1; i++) {
            long gap = files[i + 1].lastModified() - files[i].lastModified();
            if (gap > biggestGap) {
                biggestGap = gap;
                biggestGapIndex = i;
            }
        }
        String firstFile = files[biggestGapIndex].getName();
        String secondFile = files[biggestGapIndex + 1].getName();
        int pos = secondFile.lastIndexOf(".");
        if (pos > 0) {
            secondFile = secondFile.substring(0, pos + 1);
        }

        StringBuffer sb = new StringBuffer();
        sb.append("JOB HANG - Job was killed at ");
        sb.append(new Date(killTimestamp));
        sb.append(NL);
        for (int i = 0; i < files.length; i++) {
            String name = files[i].getName();
            if (name.equals(firstFile) || name.startsWith(secondFile)) {
                sb.append(name);
                sb.append(" - ");
                sb.append(new Date(files[i].lastModified()));
                sb.append(NL);
            }
        }

        summary = sb.toString();
    }

    private static final String comparePrefixAndChop(String s, String[] prefixes) {
        if (prefixes == null || s == null) {
            return null;
        } 
        for (int i = 0; i < prefixes.length; i++) {
            if (s.startsWith(prefixes[i])) {
                return s.substring(prefixes[i].length());
            }
        }
        return null;
    }

    private static final String prefixToString(String[] prefixes) {
        if (prefixes == null || prefixes.length == 0) {
            return "(empty)";
        } else {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < prefixes.length; i++) {
                sb.append(prefixes[i]);
                if (i < prefixes.length - 1) {
                    if (i < prefixes.length - 2) {
                        sb.append(", ");
                    } else {
                        sb.append(" or ");
                    }
                }
            }
            return sb.toString();
        }
    }
}
