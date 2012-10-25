package oracle.util.triage;


import java.util.jar.JarFile;
import java.util.jar.JarEntry;

import java.io.File;
import java.io.LineNumberReader;

public class UnitTestlogicReport extends UnitTestReport implements Constants {

    /** 
     Produce a unit test report from a "virtual" file.
     */
    public UnitTestlogicReport(VirtualFile vf) {
        super(vf);
    }

    /**
     Produce a unit test report from a file.
     */
    public UnitTestlogicReport(String n, Stats stats) {
        super(new File(n), stats);
    }

    /** 
     Produce a unit test report from a file.
     */
    public UnitTestlogicReport(File f, Stats stats) {
        super(f, stats);
    }

    /** 
     Produce a unit test report from an entry
     in a JAR file.
     */
    public UnitTestlogicReport(JarFile jf, JarEntry je, Stats stats, String pref) {
        super(jf, je, stats, pref);
    }

    UnitTestSuite currentSuite = null;
    UnitTest      currentTest = null;
    long          suiteBeginTimestamp = 0l;

    // List<TestLogicSegment> segments = new ArrayList<TestLogicSegment>();
    // TestLogicSegment currentSegment = null; 


    public String getFullName() {
        if (fullName == null) {
            fullName = super.getFullName();
            if (fullName == null) {
                fullName = this.toString();
            }

            // if (fullName.endsWith("/" + TESTLOGIC_LOG_FILE)) {
            //    fullName = fullName.substring(0, fullName.length() - TESTLOGIC_LOG_FILE.length() - 1);
            // }

            int pos = -1;
            if (fullName.startsWith("utp/resultout/")) {
                fullName = fullName.substring("utp/resultout/".length());
            } else if ((pos = fullName.indexOf("/utp/resultout/")) >= 0) {
                fullName = fullName.substring(pos + "/utp/resultout/".length());
            // } else  if (fullName.startsWith("results/")) {
            //    fullName = fullName.substring("results/".length());
            // } else if ((pos = fullName.indexOf("/results/")) >= 0) {
            //    fullName = fullName.substring(pos + "/results/".length());
            }
        }
        return fullName;
    }
    private String fullName;


    public String getName() {
        return getFullName();
    }

    protected void readUnitTestFile(LineNumberReader lnr) {

        if (ToplevelAnalyzer.DEBUG) {
            System.out.println("DEBUG: reading " + getName());
        }

        String line = null;
        String origLine = null;

        currentSuite = new UnitTestSuite();
        // currentSuite.setName(lName);
        // => currentSuite.setPackage(pakkage);
        // currentSuite.setDiffs(diffsFound);
        // currentSuite.setSuccs(total - diffsFound);
        // => currentSuite.setId(id);
        // => currentSuite.setTime(time);
        // => currentSuite.addProperty(String name, String value"));
 
        currentTest = new UnitTest();
        // currentTest.setName(String);
        // currentTest.setClassname(String);
        // currentTest.setSuccess(false);
        // currentTest.setMessage(message.toString());
        // => currentTest.setShortMessage(hm.get("message"));
        // => currentTest.setType(hm.get("type"));

        // initializing current suite

        boolean currentSuiteWasArchived = false;
        currentSuite.setName(getName());

        
        // currentSegment = new TestLogicSegment("prologue");

        // State variables
        boolean inTest = false;      // between BEGIN and SUCCESS / FAILURE / ERROR
        boolean inFailure = false;   // getting messages after FAILURE / ERROR

        // Variables to hold temporary parsing results
        long currentTimestamp = 0l;
        long beginTimestamp = 0l;
        boolean errorTimestamp = false;

        String currentFile = getFullName() + " (executing unspecified file)";
        StringBuffer message = null;
        StringBuffer lastJavaCompilation = new StringBuffer();
        StringBuffer embeddedTests = new StringBuffer();

        boolean inJavaCompilation = false;
        boolean stayInJavaCompilation = false;

        try {
            while ( (line = lnr.readLine()) != null) {
                origLine = line;
                stayInJavaCompilation = false;

                // Capture all Java compilations separately.
                if (line.startsWith("    [javac] ")) {
                    if (line.startsWith("    [javac] Compiling ")) {
                        lastJavaCompilation = new StringBuffer();                        
                    }
                    lastJavaCompilation.append(line); 
                    lastJavaCompilation.append("\n"); 
                    inJavaCompilation = true;
                    stayInJavaCompilation = true;

                } else if (inJavaCompilation
                           && (line.indexOf("error") >= 0
                               || line.indexOf("error") >= 0)) {
                    lastJavaCompilation.append(line); 
                    lastJavaCompilation.append("\n"); 
                    stayInJavaCompilation = true;
                } 

                if (line.startsWith("file:/")
                    || line.startsWith("==== BEGIN file:/")) {

                    // reading a file:/... designation

                    if (line.startsWith("==== BEGIN ")) {
                        line = line.substring("==== BEGIN ".length());
                    }

                    if (line.startsWith("file:/ade/")) {
                        currentFile = line.substring("file:/ade/".length());
                        int spos = currentFile.indexOf("/");
                        if (spos > 0) {
                            currentFile = currentFile.substring(spos + 1);
                        }
                    } else {
                        currentFile = line.substring("file:".length());
                    }
                    int spos = currentFile.indexOf("#");
                    if (spos > 0) {
                        currentFile = currentFile.substring(0, spos);
                    } 
                } else if (line.trim().equals("")) {

                    // skipping empty line - also terminate a failure or error message

                    if (inFailure) {
                        inFailure = false;
                        addCurrentTest(message);
                    }
                    stayInJavaCompilation = true;
                } else if ((   line.endsWith(" INFO ")
                            || line.endsWith(" WARN ")
                            || line.endsWith(" ERROR"))
                            && line.length() > 29
                            && line.charAt(7) == '-' 
                            && line.charAt(16) == ':' 
                            && line.charAt(19) == ','
                            && line.charAt(23) == ' ') {

                     // reading timestamp:  YYYY-MM-DD HH:mm:ss,SSS [INFO | WARN | ERROR]

                    currentTimestamp = Util.parseTime12(line.substring(0, 24) + " " + FARM_TIMEZONE).getTime();
                    if (line.endsWith("ERROR")) {
                        errorTimestamp = true;
                    } else {
                        errorTimestamp = false;
                    }

                    if (inFailure) {
                         // close failure message
                        inFailure = false;
                        addCurrentTest(message);
                    }
                    if (suiteBeginTimestamp == 0l) {
                        suiteBeginTimestamp = currentTimestamp;
                    }
                    stayInJavaCompilation = true;

                } else if (line.startsWith("BEGIN ")) {

                    // start a test:  BEGIN <testname>

                    currentTest.setName(line.substring("BEGIN ".length()).trim());
                    beginTimestamp = currentTimestamp;
                    message = new StringBuffer();
                    errorTimestamp = false;
                    inTest = true;

                } else if (line.startsWith("SUCCESS ")) {

                    // finish test: SUCCESS <testname>

                    currentTest.setSuccess(true);

                    if (beginTimestamp > 0l) {
                        currentTest.setStartEndTime(beginTimestamp, currentTimestamp);
                        beginTimestamp = 0l;
                    }

                    addCurrentTest(message);

                    currentTest = new UnitTest(); 
                    message = null;
                    inTest = false;

                } else if (line.startsWith("FAILURE ") 
                           || line.startsWith("ERROR ")
                           || line.startsWith("SKIP ")) {

                    // note test failure - try getting additional detail

                    currentTest.setSuccess(false);
                    errorTimestamp = false;

                    if (inTest) {
                        currentTest.setStartEndTime(beginTimestamp, currentTimestamp);
                        beginTimestamp = 0l;
                        inTest = false;
                    } else {
                        currentTest.setStartEndTime(currentTimestamp, currentTimestamp);
                        if (line.startsWith("SKIP test:")) {
                            String cn = line.substring("SKIP test:".length()).trim();
                            currentTest.setName(cn);
                            currentTest.setClassname(cn);
                            currentTest.setType("SKIP");
                        } else {
                            currentTest.setName("some issue during prep / build / teardown etc.");
                            currentTest.setClassname("n/a");
                            currentTest.setType("ERROR");
                        }
                        message = new StringBuffer();
                    } 

                    message.append(line + "\n");
                    currentTest.setClassname(currentFile);
                    embeddedTests = new StringBuffer();

                    inFailure = true;

                } else if (inTest || inFailure) {
        
                    // add test or failure detail

              
                    if (line.indexOf(": Compile failed; see the compiler error output for details.") > 0
                        && lastJavaCompilation.length() > 0) {

                        // provide last Java compiler output
                        message.append(line + "\n");
                        message.append(BEGIN_JAVA_COMPILATION + "\n");
                        message.append(lastJavaCompilation);
                        message.append(END_JAVA_COMPILATION + "\n");

                        currentTest.setShortMessage( Util.leftTrim(lastJavaCompilation.toString()) );

                        lastJavaCompilation = new StringBuffer();
                    } else if (line.startsWith("Got an failed nested [TestCase]:")) {
                        if (embeddedTests.length() > 0) {
                            embeddedTests.append(NL);
                        }
                        message = new StringBuffer( embeddedTests.toString() );
                        message.append(NL);

                        embeddedTests.append(line);
                        currentTest.setShortMessage( embeddedTests.toString() );
                    }

                    message.append(line + "\n");
                } else if (errorTimestamp) {
                    errorTimestamp = false;
                    if (canIgnoreError(line)) {
                        // skip the issue
                    } else {
                        currentTest.setSuccess(false);

                        currentTest.setClassname(currentFile);
                        currentTest.setName("an unspecified error occurred: " + line + "\n");
                        currentTest.setStartEndTime(currentTimestamp, currentTimestamp);

                        message = new StringBuffer();
                        message.append(line + "\n");
                        inFailure = true;
                    }
                } else {
                    // skip
                }

                if (!stayInJavaCompilation) {
                    inJavaCompilation = false;
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

            currentSuite.setStartEndTime( (suiteBeginTimestamp == 0l) ? currentTimestamp : suiteBeginTimestamp,
                                          currentTimestamp );
            currentSuite.deriveTotals();
            if (ToplevelAnalyzer.DEBUG) {
                System.out.println("DEBUG: " + currentSuite.getSuccs() + " sucs and " + currentSuite.getDiffs() + " difs (failure/error/skip)");
            }

            suites.add(currentSuite); 

            currentSuiteWasArchived = true;

            currentSuite = new UnitTestSuite();
            suiteBeginTimestamp = 0l;

        }

        if (Main.getSlowest() > 0) {
            for (int i = 0; i < suites.size(); i++) {
                globalStats.addSuite(suites.get(i));
                stats.addSuite(suites.get(i));
            }
        }

        if (ToplevelAnalyzer.DEBUG) {
            System.out.println("DEBUG: DONE reading " + getName() );
        }
    }


    private void addCurrentTest(StringBuffer message) {
        String msg = message.toString();
        int pos = 0;

        currentTest.setMessage(msg);
        if (currentTest.getType() != null 
            && currentTest.getType().equals("SKIP")
            && (pos = msg.indexOf("reason: ")) >= 0) {
            msg = msg.substring(pos);
            pos = msg.indexOf("\n");
            if (pos > 0) {
                msg = msg.substring(0, pos);
            }
            currentTest.setShortMessage(msg.trim());
        }

        currentSuite.addTest(currentTest);
        currentTest = new UnitTest();
    }

    private static boolean canIgnoreError(String line) {
        return line.startsWith("Unable to clean Ant project antTypes")
            || line.indexOf("] Java Result: ") > 0
            || line.indexOf("[tl-kill] parallel") > 0
            || line.indexOf("[exec] Result: ") >= 0;
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {

            String base = args[i].substring(0, args[i].length() - ".log".length());

            UnitTestlogicReport ut = new UnitTestlogicReport(args[i], new Stats());
            // ut.readUnitTestFile(args[i]);
            // ut.htmlReport("./report", base + ".html");

            System.out.println(ut.toString(FINE, false));
        }
    }
}
