package oracle.util.triage;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.io.StringReader;

import java.util.List;
import java.util.ArrayList;


public class ToplevelTest implements Constants {

    public ToplevelTest(File f) {
        this(f, null);
    }

    public ToplevelTest(File f, File fParent) {
        if (f == null) {
            f = new File("Unreported Potential Issues in Testlogic");
            testLogicCatchall = true;
            parentFile = fParent;
            lastModified = System.currentTimeMillis();
        } 

        addSeen(f);

        file = null;
        prefix = null;

        // If we have a test named:  foo-test.{suc,dif}
        // check if we have a file:  foo/test.{suc,dif}
        // If so, then create a UnitTest with prefix "foo/".

        String nam = f.getName();
        int pos = nam.indexOf("-");
        if (pos > 0) {
            File alt = new File(f.getParent() + "/" + nam.substring(0, pos) 
                                + "/" + nam.substring(pos + 1));
            if (alt.exists() && alt.canRead()) {
                file = alt;
                addSeen(file);
                prefix = nam.substring(0, pos) + File.pathSeparator;
            }
        }

        if (file == null) {
            file = f;
        }

        diffKind = (isTestLogicCatchall()) 
                   ? ToplevelAnalyzer.TESTLOGIC_DIFF_SUCC
                   : UNKNOWN_DIFF_SUCC;
        if (getFileName().endsWith(DIFF_SUFFIX)) {
            hasDiff = true;
        }
    }
    private File file;
    private File parentFile;
    private long lastModified;
    private String prefix;
    private int diffs;
    private int diffSuites;
    private int diffKind;
    private int succs;
    private boolean hasDiff;
    private boolean testLogicCatchall = false;

    private static final String TESTLOGIC_CATCHALL = "testlogic.log file:  /ade/some_view/work/utp/resultout/\n";

    public static boolean wasSeen(File f) {
        return seen.contains(makeCanonical(f));
    }

    private static String makeCanonical(File f) {
        try {
            return f.getCanonicalPath();
        } catch (IOException ioe) {
            return f.getParent() + "/" + f.getName();
        }
    }

    private static void addSeen(File f) {
        seen.add(makeCanonical(f));
    }
    private static List<String> seen = new ArrayList<String>();

    static void resetStaticVariables() {
        seen = new ArrayList<String>();
    };

    public boolean isTestLogicCatchall() {
        return testLogicCatchall;
    }

    public boolean isDiff() {
        return hasDiff;
    }

    public int getKind() {
        return diffKind;
    }

    void setKind(int kind) {
        diffKind = kind;
    }

    public File getFile() {
        return file;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getName() {
        return (prefix == null)
               ? getFile().getName()
               : prefix + "/" + getFile().getName();
    }

    void addSuccs(int s) {
        succs += s;
    }

    public int getSuccs() {
        if (succs == 0 && !isDiff()) {
            return 1;
        }
        return succs;

    }

    void addDiffs(int d) {
        diffs += d;
        if (d > 0) {
            diffSuites ++;
            hasDiff = true;
        }
    }

    public int getDiffs() {
        if (diffs == 0 && isDiff()) {
            return 1;
        }
        return diffs;
    }

    public int getTests() {
        return getDiffs() + getSuccs();
    }

    public int getDiffSuites() {
        if (diffSuites == 0 && isDiff()) {
            return 1;
        }
        return diffSuites;
    }

    public String getFileName() {
        return getFile().getName();
    }

    public File getParentFile() {
        if (isTestLogicCatchall()) {
            return parentFile;
        }
        return getFile().getParentFile();
    }

    public long lastModified() {
        if (isTestLogicCatchall()) {
            return lastModified;
        }
        return getFile().lastModified();
    }

    public long length() {
        if (isTestLogicCatchall()) {
            return (long)(TESTLOGIC_CATCHALL.length());
        }
        return getFile().length();
    }

    public LineNumberReader getLineNumberReader() throws IOException {
        if (isTestLogicCatchall()) {
            return new LineNumberReader(new StringReader(TESTLOGIC_CATCHALL));
        }
        return new LineNumberReader(new BufferedReader(new FileReader(getFile())));
    }
}
