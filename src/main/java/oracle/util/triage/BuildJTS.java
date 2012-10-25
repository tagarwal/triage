package oracle.util.triage;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;

/** Representation of a build.jts runtime log segment.
 */

public class BuildJTS extends DatedFile implements Constants {

    private static final boolean DEBUG = false;

    public BuildJTS(VirtualFile root) {
        super(root);
    }

    public void setSuiteName(String s) {
        suiteName = s;
    }

    public String getSuiteName() {
        return suiteName;
    }
    
    public void setTarget(String s) {
        target = s;
    }

    public String getTarget() {
        return target;
    }
    
    public void setId(int n) {
        id = n;
    }

    public int getId() {
        return id;
    }
    public void setAntFile(String s) {
        antFile = s;
    }

    public String getAntFile() {
        return antFile;
    }

    public void setWorkDir(String s) {
        workDir = s;
    }

    public String getWorkDir() {
        return workDir;
    }

    public void setLogFile(String s) {
        logFile = s;
    }

    public String getLogFile() {
        return logFile;
    }

    public void setParentSuite(String s) {
        parentSuite = s;
    }

    public String getParentSuite() {
        return parentSuite;
    }

    public void setParentTarget(String s) {
        parentTarget = s;
    }

    public String getParentTarget() {
        return parentTarget;
    }
  

/*
    public void setUnitTestSuite(UnitTestSuite uts) {
        unitTestSuite = uts;
    }
    public UnitTestSuite getUnitTestSuite() {
        return unitTestSuite;
    }

    public boolean contains(BuildJTS jts) {
        return getStartTime() <= jts.getStartTime()
            && jts.getEndTime() <= getEndTime();
    }

    private UnitTestSuite unitTestSuite;
*/

    private int id;
    private String suiteName;
    private String target;
    private String workDir;
    private String antFile;
    private String logFile;
    private String parentSuite;
    private String parentTarget;

    public String getFullName() {
        StringBuffer sb = new StringBuffer();
        sb.append(getSuiteName());
        if (getTarget()!=null 
            && !getTarget().equals("null")
            && !getTarget().equals("[default]")) {
            sb.append("::");
            sb.append(getTarget());
        }
        return sb.toString();
    }

    static void readJTSFiles(VirtualFile f) {
        BuildJTS jst = new BuildJTS(f);
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(f.getInputStream())));
            String line = null;
            while ( (line=lnr.readLine()) != null) {
                if (line.startsWith("           id=")) {
                    try {
                        jst.setId(Integer.parseInt(line.substring("           id=".length())));
                    } catch (Exception exn) {
                        exn.printStackTrace();
                    }
                } else if (line.startsWith("   suite.name=")) {
                    jst.setSuiteName(line.substring("   suite.name=".length()));
                } else if (line.startsWith("       target=")) {
                    jst.setTarget(line.substring("       target=".length()));
                } else if (line.startsWith("     work.dir=")) {
                    jst.setWorkDir(line.substring("     work.dir=".length()));
                } else if (line.startsWith("     ant.file=")) {
                    jst.setAntFile(line.substring("     ant.file=".length()));
                } else if (line.startsWith("     log.file=")) {
                    jst.setLogFile(line.substring("     log.file=".length()));
                } else if (line.startsWith("   start.time=")) {
                    jst.setStartTime(line.substring("   start.time=".length()));
                } else if (line.startsWith("     end.time=")) {
                    jst.setEndTime(line.substring("     end.time=".length()));
                } else if (line.startsWith(" parent.suite=")) {
                    jst.setParentSuite(line.substring(" parent.suite=".length()));
                } else if (line.startsWith("parent.target=")) {
                    jst.setParentTarget(line.substring("parent.target=".length()));
                    theUniverse.addChild(jst);
                    jst = new BuildJTS(f);
                }
            }
            lnr.close();
         } catch (Exception e) {
            e.printStackTrace();
            if (lnr!=null) {
                try {
                    lnr.close();
                } catch (IOException ioe) {
                    // ignore closing resource
                }
            }
         }
    }

}
