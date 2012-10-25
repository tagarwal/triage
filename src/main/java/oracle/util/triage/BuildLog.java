package oracle.util.triage;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

/** Representation of a build.jtl log file.
 */

public class BuildLog implements Constants {

    private static final boolean DEBUG = false;

    ///// List of prefixes of log information that will be deleted ////.
    private static String[] LINES_TO_DELETE = new String[] {
        "Removed: ",
    };

    ///// List of prefixes that signify nested ant invocations /////
    private static String[] NESTED_PREFIX = new String[] {
        "     [exec] ",
        "     [java] ",
    };

    private static final String LF = "\n";


    /** Create a BuildLog based on a build file */
    public BuildLog(String s, JarFile archive) {
        this.setName(s);
        File f = new File(s);
        LineNumberReader lnr = null;
        ZipEntry ze = null;

        List<String> lines = new ArrayList<String>();

        try {
            if (archive != null && (ze = archive.getEntry(getName())) != null) {
                lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(archive.getInputStream(ze))));
            } else {
                lnr = new LineNumberReader(new BufferedReader(new FileReader(f)));
            }


            String line = null;
            while ( (line = lnr.readLine()) != null ) {
                if (toRemove(line)) {
                    // skip this line
                } else {
                   lines.add(line);
                }
            }

        } catch (IOException ioe) {
            ioe.printStackTrace();
            System.out.println("SEVERE: Error while reading build log file " + f + ": " + ioe);
            try {
                if (lnr != null) lnr.close();
            } catch (Exception e) {
              // closing resources - ignore
            }
        }
        steps = readSteps(lines, 1);
    }

    ///////////////////////////
    //// Memmber functions ////
    ///////////////////////////

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
    private String name;

    public List<Step> getSteps() {
        return steps;
    }
    private List<Step> steps = new ArrayList<Step>();

    /** Print the last steps in build file #index */
    public String printLastSteps(int index) {
        int numStepsToShow = Main.getRelevantBuildSteps();
        Step[] lastSteps = getLastSteps( getSteps(), numStepsToShow );
        StringBuffer sb = new StringBuffer();

        sb.append("#### Build log ");
        if (index > 0) {
            sb.append("#"); sb.append(index); sb.append(" - ");
        }
        sb.append(getName());
        if (lastSteps.length > 1) {
            sb.append(" - last ");
            sb.append(lastSteps.length);
            sb.append(" steps: ####");
        } else {
            sb.append(" - last step: ####");
        }
        sb.append(NL);

        for (Step st : lastSteps) {
            sb.append( st.print("", numStepsToShow + 1) );
        }
        sb.append("#### End of Build Log ");
        if (index > 0) {
            sb.append("#"); sb.append(index); sb.append(" ");
        }
        sb.append("####"); sb.append(NL);

        return sb.toString();
    }

    /** Print the last steps in the build file */
    public String printLastSteps() {
        return printLastSteps(0);
    }

    public String summarize() {
        StringBuffer sb = new StringBuffer();
        for (Step s : getSteps()) {
            sb.append(s.summarize(""));
        }
        return sb.toString();
    }


    ////////////////////////////////////
    //// Static processing routines ////
    ////////////////////////////////////

    private static boolean toRemove(String line) {
        for (int i=0; i<LINES_TO_DELETE.length; i++) {
            if (line.startsWith(LINES_TO_DELETE[i])) {
                return true;
            }
        }
        return false;
    }

    public static List<Step> readSteps(List<String> lines, int lineNo) {
        List<Step> result = new ArrayList<Step>();

        Step step   = new Step( 1 );
        boolean inMainArea = true;
        boolean lastLineBlank = false;

        lineNo--;

        for (String line : lines) {
            lineNo ++;
            if (lastLineBlank && isStart(line)) {
                int stepStart = lineNo; 
                int stepEnd   = stepStart - 1;
                if (stepEnd > 0) {
                    step.setEndLine(stepEnd);
                    result.add(step);
                }
                String stepName = getStepName(line);
                Date stepTime   = getStepTime(line);
                step = new Step(stepStart, stepName, stepTime, line);
                inMainArea = true;
                lastLineBlank = false;
            } else if (isBlank(line)) {
                lastLineBlank = true;
                if (inMainArea) {
                    step.addMain(line);
                } else {
                    step.addSuffix(line);
                }
            } else if (inMainArea && isMain(line)) {
                step.addMain(line);
            } else {
                inMainArea = false;
                step.addSuffix(line);
            }
        }
        step.setEndLine(lineNo);
        result.add(step);

        return result;
    }

    private static int getNestedPrefix(String line) {
        for (int i=0; i<NESTED_PREFIX.length; i++) {
            if (line.startsWith(NESTED_PREFIX[i])) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isStart(String line) {
        int pos = line.indexOf(":");
        if (pos > 0) {
            if (isAntTargetName(line.substring(0, pos - 1))) {
                String suffix = line.substring(pos + 1).trim();
                return suffix.equals("") || suffix.startsWith("@");
            }
        }
        return false;
    }

    private static String getStepName(String line) {
        int pos = line.indexOf(":");
        return (pos >= 0) ? line.substring(0, pos) : null;
    }

    private static Date getStepTime(String line) {
        int pos = line.indexOf("@");
        if (pos >= 0) {
            try {
                String stamp = line.substring(pos + 1).trim();
                return Util.parseTime1(stamp);
            } catch (Exception e) {
                System.out.println("Need to improve parsing date: \"" + line.substring(pos + 1).trim() + "\"");
                return null;
            }
        } else {
            return null;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.equals(s.trim());
    }

    private static boolean isMain(String line) {
        String t = line.trim();
        if (!t.startsWith("[")) {
            return false;
        }
        int pos1 = t.indexOf("]");
        if (pos1 < 0) return false;
        int pos2 = t.indexOf(" ");
        if (pos2 > 0 && pos2 < pos1) return false;
        return true;
    }

    private static boolean isAntTargetName(String s) {
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if ( ('a' <= ch && ch <= 'z')
                 || ('0' <= ch && ch <= '9')
                 || ch == '.'
                 || ch == '-'
                 || ch == '_' ) {
                 // accepted
            } else {
                return false;
            }
        }
        return true;
    }


    /** Return the last numSteps steps from steps */
    public static Step[] getLastSteps(List<Step> steps,  int numSteps) {
        int numRelevantSteps = 0;
        int first = steps.size() - 1;

        if (first < 0) {
            return new Step[] {};
        }

        while ( numRelevantSteps < numSteps && first > 0) {
            if (steps.get(first).isRelevant()) {
                numRelevantSteps ++;
            }
            if (numRelevantSteps < numSteps) {
                first--;
            }
        }
        Step[] res = new Step[steps.size() - first];
 
        for (int i = 0; i < res.length; i++) {
            res[i] = steps.get(first + i);
        }
        return res;
    }


    public static class Step {

        public Step(int startLine, String stepName, Date stepTime, String line) {
            first = startLine;
            last  = startLine - 1;
            name  = stepName;
            time  = stepTime;
            header = line;
            body   = new ArrayList<String>();
            suffix = new ArrayList<String>();
            nestedPrefix = null;
        }

        public boolean isRelevant() {
            if (getMainSize() == 0 
                || getName().equals("only-test-report")) {
                return false;
            }
            return true;
        }

        public Step(int startLine) {
           this(startLine, null, null, "");
        }

        public void addMain(String s) {
            body.add(s);
        }

        public void addSuffix(String s) {
            suffix.add(s);
        }

        public void setEndLine(int lineNo) {
            last = lineNo;
            _init();
        }

        public Date getTime() {
            return time;
        }

        public String getName() {
            return name;
        }

        public int getStartLine() {
            return first;
        }

        public int getEndLine() {
            return last;
        }

        public int getSize() {
            return getHeaderSize() + getMainSize() + getSuffixSize();
        }

        public int getMainSize() {
            if (mainSize < 0) {
                mainSize = 0;
                if (nestedPrefix != null) {
                    for (Step s : nestedSteps) {
                        mainSize += s.getSize();
                    }
                }
                for (String s : body) {
                    mainSize += s.trim().length();
                }
            }
            return mainSize;
        }
        private int mainSize = -1;
     
        public int getSuffixSize() {
            if (suffixSize < 0) {
                suffixSize = 0;
                for (String s : suffix) {
                    suffixSize += s.trim().length();
                }
            }
            return suffixSize;
        }
        private int suffixSize = -1;

        public int getHeaderSize() {
            return header.length();
        }

        public String toString() {
            throw new IllegalArgumentException();
        }

        public String print(String prefix, int numStepsToShow) {
            _init();
            StringBuffer sb = new StringBuffer();
            if (DEBUG) {
               sb.append(prefix); sb.append("DEBUG: HEADER "); sb.append(LF);
            }
            sb.append(prefix); sb.append(header);  sb.append(LF);

            if (nestedSteps != null) {
                if (DEBUG) {
                    sb.append(prefix); sb.append("DEBUG: BODY NESTED STEPS"); sb.append(LF);
                }
                if (numStepsToShow <= 0) {
                    for (Step s : nestedSteps) {
                       sb.append( s.print(prefix + nestedPrefix, numStepsToShow) );
                    }
                } else {
                    Step[] toShow = getLastSteps(nestedSteps, numStepsToShow);
                    int omitted = nestedSteps.size() - toShow.length;
                    if (omitted == 1) {
                        sb.append(prefix + nestedPrefix ); sb.append(" ... omitted a build step ..."); sb.append(LF);
                    } else if (omitted > 1) {
                        sb.append(prefix + nestedPrefix ); sb.append(" ... omitted " + omitted + " build steps ..."); sb.append(LF);
                    }
                    for (Step s : toShow) {
                       sb.append( s.print(prefix + nestedPrefix, numStepsToShow) );
                    }
                }
                if (DEBUG) {
                    sb.append(prefix); sb.append("DEBUG: BODY NESTED TRAIL"); sb.append(LF);
                }
            } else {
                if (DEBUG) {
                    sb.append(prefix); sb.append("DEBUG: BODY"); sb.append(LF);
                }
            }
            for (String s : body) {
                sb.append(prefix); sb.append(s); sb.append(LF);
            }

            if (DEBUG) {
                sb.append(prefix); sb.append("DEBUG: SUFFIX"); sb.append(LF);
            }
            for (String s : suffix) {
                sb.append(prefix); sb.append(s); sb.append(LF);
            }
            return sb.toString();
        }

        private int first;
        private int last;
        private Date time;
        private String name;
        private String header;
        private String nestedPrefix = null;
        private List<Step> nestedSteps;
        private List<String> body;
        private List<String> suffix;
        private boolean initialized = false;

        private void _init() {
            if (initialized) {
                return;
            }
            initialized = true;
            int pref = -1;
            if (body.size() > 0 
                && (pref = getNestedPrefix(body.get(0))) >= 0) {
                int i = 0;
                List<String> nested = new ArrayList<String>();
                List<String> bodyTail = new ArrayList<String>();

                nestedPrefix = NESTED_PREFIX[pref];
                for (i=0; i<body.size() && body.get(i).startsWith(nestedPrefix); i++) {
                    nested.add(body.get(i).substring(nestedPrefix.length()));
                }
                for ( ; i<body.size(); i++) {
                    bodyTail.add(body.get(i));
                }
                nestedSteps = readSteps(nested, getStartLine()+1); 
                body = bodyTail;
                for (Step s : nestedSteps) {
                    s._init();
                }
            }
        }
 

        public String summarize(String pref) {
            StringBuffer sb = new StringBuffer();
            sb.append(pref + "Step " + getName() + " " + getStartLine() + "-" + getEndLine() + ": @ " + getTime());
            sb.append(pref + "    main   = " + getMainSize() + " characters" + LF);
            if (nestedPrefix != null) {
                sb.append(pref + "   " + nestedSteps.size() + " nested steps:");
                for (Step s : nestedSteps) {
                    sb.append(s.summarize(pref + nestedPrefix));
                }
            }
            sb.append(pref + "    suffix = " + getSuffixSize() + " characters" + LF);
            return sb.toString();
        }
    }

    public static void main(String[] args) {
        for (int i = 0; i < args.length; i++) {
            BuildLog bl = new BuildLog(args[i], null);
            System.out.println("**** FILE: " + args[i] + " ****");
            System.out.println(bl.summarize());
            System.out.println("********* LAST STEP: *********");
            System.out.println(bl.printLastSteps(0));
        }
    }

}
