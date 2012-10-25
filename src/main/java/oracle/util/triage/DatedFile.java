package oracle.util.triage;
                        

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;

/** Representation of a build.jts runtime log segment.
 */

public class DatedFile implements Constants, Cloneable {

    private static final boolean DEBUG = false;

    public DatedFile(VirtualFile root) {
        this.name = root.getName();
        this.dir = root.getParent(); 
        this.length = root.length(); 
        if (this.name == null) {
           this.name = "** Unknown **";
           this.url = this.name;
           System.err.println("SEVERE: Unknown name for DatedFile!");
           (new Exception()).printStackTrace();
        } else {
           this.url = root.getUrl();
        }
    }

    public DatedFile(String name, long start, long end) {
        this(name, start, end, null);
    }

    public DatedFile(String name, long start, long end, String url) {
        this.name = name;
        setStartTime(start);
        setEndTime(end);
        if (this.name == null) {
           this.name = "** Unknown **";
           System.err.println("SEVERE: Unknown name for DatedFile!");
           (new Exception()).printStackTrace();
        }
        this.url = url;
    }

    public DatedFile(String name, long start, long end, String url, int startLine, int endLine) {
        this(name, start, end, url);
        this.startLine = startLine;
        this.endLine = endLine;
    }

    private DatedFile() {
        this.startTime = Long.MIN_VALUE;
        this.endTime = Long.MAX_VALUE;
        this.name = "N/A";
        this.dir = "N/A"; 
        this.url = this.name; 
    }

    private boolean isUniverse() {
        return startTime == Long.MIN_VALUE;
    }

    public DatedFile clone() throws CloneNotSupportedException {
        DatedFile theClone = (DatedFile) super.clone();
        theClone.children = new ArrayList<DatedFile>();
        return theClone;
    }

    public String getFullName() {
        if (isUniverse()) {
            return "Total";
        } else if (getDir() == null) {
            return getName(); 
        } else {
            return getDir() + "/" + getName(); 
        }
    }    

    public String getDir() {
        return dir;
    }    

    public String getName() {
        return name + getFragment(); 
    }    

    public long getLength() {
        return length;
    }    

    public String getUrl() {
        String prefix = "";
        if (name.endsWith(".suc")) {
            prefix = Util.makeIcon(CHECK_ICON);
        } else if (name.endsWith(".dif")) {
            prefix = Util.makeIcon(RED_X_ICON);
        }

        String suffix = "";
        if (name.equals("build.jtl")) {
            suffix = Util.makeDetailLink(url.substring(0,url.length() - "xml".length()) + "xml",
                                         Util.makeIcon(LINK_ICON));
        } else if (name.equals("testlogic.log")) {
            suffix = Util.makeDetailLink(url.substring(0,url.length() - "log".length()) + "html",
                                         Util.makeIcon(LINK_ICON));
        }

        if (isUniverse()) {
            return getFullName();
        } else {
            String name = this.name;
            if (name.startsWith("build.")
                && (getDir() != null || url != null)) {
                int pos;
                String dir = url;
                if (dir == null) {
                    dir = getDir();
                } else {
                    pos = dir.lastIndexOf("/");
                    if (pos > 0) {
                        dir = dir.substring(0,pos);
                    } 
                }
                
                pos = dir.indexOf("jtwork/");
                if (pos > 0) {
                   dir = dir.substring(pos + "jtwork/".length());
                   name = dir + "/" + name;
                }
            }

            String nm = name + getFragment();

            if (url == null || getLength() == 0l) {
                return prefix + nm + suffix;
            } else {
                return prefix + Util.makeRangeLink(url, nm, startLine, endLine) + suffix;
            }
        }
    }

    public String getArea() {
        return area;
    }    

    public void setArea(String a) {
        area = a;
    }    

    public String getOwner() {
        return owner; 
    }    

    public void setOwner(String o) {
        owner = o; 
    }    

    public void setStartTime(String s) {
        startTime = Util.parseTime1(s).getTime();
    }
    
    public void setStartTime(long l) {
        startTime = l;
    }
    
    public long getStartTime() {
        return startTime;
    }

    public void setEndTime(String s) {
        endTime = Util.parseTime1(s).getTime();
    }
    
    public void setEndTime(long l) {
        endTime = l;
    }
    
    public long getEndTime() {
        return endTime;
    }

    public boolean isTest() {
        return false;
    }

    public int getNumber() {
        return number;
    }

    public boolean isDiagnosticLog() {
        return name.endsWith(".history.log")
            || (name.endsWith(".log")
                && name.indexOf("diagnostic") >= 0);
    }

    public boolean contains(DatedFile bf) {
        return getStartTime() <= bf.getStartTime()
            && bf.getEndTime() <= getEndTime();
    }

    public float getTime() {
        long delta = 0L;
        if (isUniverse()) {
            if (children.size() > 0) {
                delta = children.get(children.size() - 1).getEndTime() - children.get(0).getStartTime();
            }
        } else {
            delta = getEndTime() - getStartTime();
        }
        return deltaDateToSeconds(delta);
    }

    public float getTestTime() {
        return getChildrenTestTime();
    }

    public DatedFile[] getChildren() {
        DatedFile[] chn = new DatedFile[children.size()];
        for (int i = 0; i < children.size(); i++) {
           chn[i] = children.get(i);
        }
        return chn;
    }

    public DatedFile getParent() {
        if (isUniverse()) {
            return this;
        } else {
            return theUniverse.getParentFor(this);
        }
    }
    
    private DatedFile getParentFor(DatedFile df) {
        if (df.getStartTime() < getStartTime()
            || getEndTime() < df.getEndTime()) {
            return null;
        }

        for (int i = 0; i < children.size(); i++) {
             DatedFile child = children.get(i);
             if (df == child) {
                 return this;
             } else if (child.getStartTime() <= df.getStartTime()
                        && df.getEndTime() <= child.getEndTime()) {
                 return child.getParentFor(df);
             }
        }
        return null;
    }

    public static boolean showDetails() {
        return details;
    }
    private static boolean details = false;

    public static Date getSubmitTime() {
        return submitTime;
    }
    private static Date submitTime = null;

    public static List<DatedFile> getRoots(String area) {
         List<DatedFile> roots = new ArrayList<DatedFile>();
         theUniverse.getRoots(roots, area);
         return roots;
    }

    private void getRoots(List<DatedFile> roots, String area) {
        if (area.equals(getArea())) {
           roots.add(this);
        } else if (getArea() == null || getArea() == UNASSIGNED_AREA) {
           for (int i = 0; i < children.size(); i++) {
               children.get(i).getRoots(roots, area);
           }
        }
    }

    protected float getChildrenTestTime() {
        float childrenTestTime = 0.0f;
        for (int i = 0; i < children.size(); i++) {
            childrenTestTime += children.get(i).getTestTime();
        }
        return childrenTestTime;
    }

    public float getChildrenTime() {
        long delta = 0L;
        for (int i = 0; i < children.size(); i++) {
            DatedFile child = children.get(i);
            delta += child.getEndTime() - child.getStartTime();
        }
        return deltaDateToSeconds(delta);
    }

    public void addChildren(DatedFile[] elems) {
        for (DatedFile elem : elems) {
            addChild(elem);
        }
    }

    public void addChild(DatedFile elem) {
        if (isUniverse() && elem.isDiagnosticLog()) {
            for (DatedFile child : elem.children) {
                 addChild(child);
            }
        } else {
            boolean done = false;
            for (int i = 0; !done && i < children.size(); i++) {
                 DatedFile child = children.get(i);
                 if (child.contains(elem)) {
                     child.addChild(elem);
                     done = true;
                } else if (elem.contains(child)) {
                     children.set(i, elem);
                     elem.addChild(child);
                     while (i + 1 < children.size() && elem.contains(children.get(i + 1))) {
                         elem.addChild(children.get(i + 1));
                         children.remove(i + 1);
                     }
                     done = true;
                } else if (child.getEndTime() <= elem.getStartTime()) {
                     // can skip to next one
                } else if (elem.getEndTime() <= child.getStartTime()) {
                     children.add(i, elem);
                     done = true;
                } else {
                    // we have an overlap 

                    // let's add all of the children of this node
                    if (elem.children.size() > 0) {
                        if (DEBUG) {
                            System.out.println("INFO: Overlap - adding children of " + elem.toShortString());
                        }
                        addChildren(elem.getChildren());
                        elem.children = new ArrayList<DatedFile>();

                        // now try again
                        addChild(elem);

                    } else { 
               
                        // Split the node.
                        try {
                         if (child.getEndTime() <= elem.getEndTime()) {
                             DatedFile secondElem = (DatedFile)elem.clone();
                             elem.setFragment(1);
                             elem.setEndTime(child.getEndTime());

                             secondElem.setFragment(2); 
                             secondElem.setStartTime(child.getEndTime() + 1);

                             if (true) {
                                 System.out.println("INFO: Splitting to " + elem.toShortString() + 
                                                    " and " + secondElem.toShortString());
                             }

                             child.addChild(elem);
                             addChild(secondElem);
                            
                          } else { // elem.getEndTime() < child.getEndTime()
                              DatedFile secondElem = (DatedFile)elem.clone();
                              elem.setFragment(1);
                              elem.setEndTime(child.getStartTime() - 1);

                              secondElem.setFragment(2); 
                              secondElem.setStartTime(child.getStartTime());
      
                              if (true) {
                                  System.out.println("INFO: Splitting to " + elem.toShortString() + 
                                                     " and " + secondElem.toShortString());
                              }

                              child.addChild(secondElem);
                              addChild(elem);
                          }
                       } catch (CloneNotSupportedException cns) {
                           System.out.println("SEVERE: Unable to break up " + child.toShortString() + " to avoid " +
                                              "overlap with " + elem.toShortString() + " - error: " + cns);
                       }
                   }
                   done = true;
                }
            }
            if (!done) {
               children.add(elem);
            }
        }
    }
    private List<DatedFile> children = new ArrayList<DatedFile>();

    private void flatten(float threshold, List<DatedFile> flattenList) {
        if (isTest()) {
            flattenList.add(this);
        } else {
            float selfTime = getTime() - getChildrenTime();
            List<DatedFile> cumulator = null;
            if (selfTime >= threshold || isUniverse()) {
                flattenList.add(this);
                if (children.size() > 0) { 
                    cumulator = new ArrayList<DatedFile>();
                }
            } else {
                cumulator = flattenList;
            }

            for (int i = 0; i < children.size(); i++) {
                children.get(i).flatten(threshold, cumulator);
            }

            if ((selfTime >= threshold || isUniverse()) && children.size() > 0) {
                children = cumulator;
            }
        }
    }

    public static void flatten() {
        theUniverse.flatten(1.0f + EPSILON, new ArrayList<DatedFile>());
    }


    private int number(int n) {
        number = n;
        n++;
        for (int i = 0; i < children.size(); i++) {
             n = children.get(i).number(n);
        }
        return n;
    }

    public static void number() {
        theUniverse.number(0);
    }
   
    private long startTime;
    private long endTime;

    private int startLine = -1;
    private int endLine = -1;

    private String dir;
    private String name;
    private String url;
    private String area;
    private String owner;
    private int number;
    private long length = -1l; // different from 0 so that we can link "virtual" entries.

    public String toString() {
        StringBuffer sb = new StringBuffer();
        toString("", sb);
        return sb.toString();
    }

    public String toShortString() {
        return getFullName()
               + " " 
               + Util.toTimestamp(getStartTime())
               + "-"
               + Util.toTimestamp(getEndTime());
    }

    private void setFragment(int n) {
        if (fragment == null) {
            fragment = "" + n;
        } else {
            fragment = fragment + "." + n;
        }
    }

    private String getFragment() {
        return (fragment == null) ? "" : "[" + fragment + "]";
    }
    private String fragment = null;

    private void toString(String indent, StringBuffer sb) {
        float elapsedTime = getTime();
        float childTime = getChildrenTime();
        float testTime = getTestTime();
        float selfTime = elapsedTime - childTime;
        float nontestTime = elapsedTime - testTime;

        sb.append((new Date(getStartTime())) + " - " + (new Date(getEndTime())) + "\n");

        if (getArea() != null) {
           String prefix = (getArea() + "        ").substring(0,8);
           sb.append("[" + prefix + "]");
        } else {
           sb.append("[Unassign]");
        }
        sb.append(indent);
        sb.append(getFullName());
        sb.append(" time: ");

        if ( Math.abs(elapsedTime) < EPSILON ) {
           sb.append(Util.seconds(0.0f));
           if (isTest()) {
               sb.append(" testing");
           } else {
               sb.append(" self");
           }
        } else if ( (elapsedTime - testTime) < EPSILON) {
           // Testing only time
           sb.append(Util.seconds(testTime));
           sb.append(" testing");
        } else if ( Math.abs(elapsedTime - selfTime) < EPSILON ) {
           sb.append("total ");
           sb.append(Util.seconds(elapsedTime));
           sb.append(" self");
        } else {
           sb.append("total ");
           sb.append(Util.seconds(elapsedTime));
           sb.append(" = ");

           if ( testTime > EPSILON ) {
               // Testing only time
               sb.append(Util.seconds(testTime));
               sb.append(" testing ");
               if ( Math.abs( nontestTime ) > EPSILON ) {
                  sb.append(" + ");
               }
           }
           if ( selfTime > EPSILON ) {
               sb.append(Util.seconds(selfTime));
               sb.append(" self ");
               if (Math.abs(nontestTime - selfTime) > EPSILON) {
                   sb.append(" + ");
               }
           }
           if (Math.abs(nontestTime - selfTime) > EPSILON) {
               sb.append(Util.seconds(nontestTime - selfTime));
               sb.append(" children (no testing)");
           }
        }

        sb.append(NL);
        String newIndent = indent + "  ";
        for (int i = 0; i < children.size(); i++) {
            children.get(i).toString(newIndent, sb);
            if (isUniverse() && i < children.size() - 1) {
               float gap = deltaDateToSeconds(children.get(i + 1).getStartTime() - children.get(i).getEndTime());
               if (gap > 1.0f + EPSILON) {
                   sb.append("[Unassign]");
                   sb.append(newIndent);
                   sb.append("GAP time: " + Util.seconds(gap));
                   sb.append(NL);
               }
            }
        }
    }

    private static float deltaDateToSeconds(long delta) {
        return (delta) * 1.0f / 1000.0f;
    }

    public static void setMaxCsvLevel(int m) {
        MAX_CSV_LEVEL = m;
    }

    public static int getMaxCsvLevel() {
        return MAX_CSV_LEVEL;
    }
    private static int MAX_CSV_LEVEL = 5;


    public String toHtml() {
        StringBuffer sb = new StringBuffer();
        sb.append("<HTML><HEAD><TITLE>Build Log and Times</TITLE></HEAD>"); sb.append(NL);
        sb.append(HEADER_INCLUDES);

        sb.append("<BODY><H1>Build Log and Times</H1>"); sb.append(NL);
        sb.append("<H2>Summary</H2>"); sb.append(NL);
        sb.append(AreaDescriptors.getHtmlStats());
        sb.append(NL);

        sb.append("<H2>Detail</H2>"); sb.append(NL);
        sb.append("<a href=\"#\" onclick=\"expandTree('tree1'); return false;\">Expand all</a> - ");
        sb.append("<a href=\"#\" onclick=\"collapseTree('tree1'); return false;\">collapse all</a>");
        sb.append(NL);

        sb.append("<ul class=\"mktree\" id=\"tree1\">"); sb.append(NL);
        toHtml(null, sb);
        sb.append("</ul>"); sb.append(NL);

        sb.append("</BODY></HTML>"); sb.append(NL);
        return sb.toString();
    }

    protected void toHtml(String pref, StringBuffer sb) {
        sb.append("<li id=\"n" + getNumber() + "\">");
        if (pref != null) {
            sb.append(pref);
        }

        if (!isUniverse() 
            && (getArea() == null || getArea() == UNASSIGNED_AREA)) {
            StringBuffer subSb = new StringBuffer();
            subSb.append(Util.toTimestamp(getStartTime()));
            if (getStartTime() != getEndTime()) {
               subSb.append("-");
               subSb.append(Util.toTimestamp(getEndTime()));
            }
            subSb.append(" ");
            subSb.append(getUrl());
            sb.append(Util.makeColor(GRAY_COLOR, subSb.toString()));
        } else {
            sb.append(getUrl());
        }

        if (getTime() > EPSILON) {
            sb.append(" ");
            sb.append(Util.secs(getTime())); 
            if (isTest()) {
                sb.append(" test");
            } else if (getChildrenTime() <= EPSILON) {
                // this is "self" time - no need to print it here
            } else {
                float testTime = getTestTime();
                      testTime = (testTime > EPSILON) ? testTime : 0.0f;
                float selfTime = getTime() - getChildrenTime();
                      selfTime = (selfTime > EPSILON) ? selfTime : 0.0f;
    
                if (testTime > 0.0f || selfTime > 0.0f) {
                    sb.append(" (");
                    if (testTime > 0.0f) {
                        sb.append(Util.secs(testTime)); 
                        sb.append(" test");
                        if (selfTime > 0.0f) {
                            sb.append(", ");
                        }
                    }
                    if (selfTime > 0.0f) {
                        sb.append(Util.secs(selfTime)); 
                        sb.append(" self");
                    }
                    sb.append(")");
                 }
            }
        }

        sb.append(NL);

        if (children.size() > 0) {
            sb.append("<ul>"); sb.append(NL);
            for (int i = 0; i < children.size(); i++) {
                String prefix = null;
                if (isUniverse()  && 0 < i) {
                    float gap = deltaDateToSeconds(children.get(i).getStartTime() - children.get(i - 1).getEndTime());
                    if (gap > MIN_GAP_TIME) {
                       // sb.append("<li>" + Util.makeColor(GRAY_COLOR, "GAP") );
                       prefix = "<i>" + Util.secs(gap) + "</i> - ";
                       // sb.append("</li>");
                       // sb.append(NL);
                    }
                }
                children.get(i).toHtml(prefix, sb);
            }
            sb.append("</ul>");
        }
        sb.append("</li>");
        sb.append(NL);
    }

    public String toCSV() {
        StringBuffer sb = new StringBuffer();
        sb.append("Sequence,Area,");
        for (int i = 0; i < getMaxCsvLevel(); i++) {
            sb.append("Level");
            sb.append(i + 1);
            sb.append(",");
        }
        sb.append("TotalTime,TestingTime");
        sb.append(NL);

        String[] prefixes = new String[getMaxCsvLevel() + 1];
        for (int i = 0; i < prefixes.length; i++) {
             prefixes[i] = "";
        }
        seqCount = 0;

        toCSV(sb, 0, prefixes);
        return sb.toString();
    }
    private static int seqCount = 0;

    private void toCSV(StringBuffer sb, int level, String[] prefixes) {
        if (level > getMaxCsvLevel()) {
            return;
        }
        prefixes[level] = getFullName();

        seqCount++;
        sb.append(seqCount);
        sb.append(",");

        if (getArea() != null) {
           sb.append("\"");
           sb.append(getArea());
           sb.append("\"");
        }
        sb.append(",");

        for (int i = 1; i < prefixes.length; i++) {
             sb.append("\""); sb.append(prefixes[i]); sb.append("\",");
        }
        sb.append(getTime()); sb.append(",");
        sb.append(getTestTime());
        sb.append(NL);

        for (int i = 0; i < children.size(); i++) {
           children.get(i).toCSV(sb, level + 1, prefixes);
           if (isUniverse() && i < children.size() - 1) {
              float gap = deltaDateToSeconds(children.get(i + 1).getStartTime() - children.get(i).getEndTime());
              if (gap > 1.0f + EPSILON) {
                  seqCount++;
                  sb.append(seqCount);
                  sb.append(",\"GAP\",");
                  for (int j = 1; j < getMaxCsvLevel(); j++) {
                       sb.append("\"\",");
                  }
                  sb.append(gap);
                  sb.append(",0.0");
                  sb.append(NL);
              }
           }
       }
       prefixes[level] = "";
    }

    public static DatedFile theUniverse = new DatedFile();

    static void resetStaticVariables() {
        theUniverse = new DatedFile();
        seqCount = 0;
        MAX_CSV_LEVEL = 5;
    }

    /*
     public static void readFiles(File dir) {
         File[] datedFiles = dir.listFiles(Util.DATED_FILES);
         for (int i=0; datedFiles!=null && i<datedFiles.length; i++) {
             readFile(datedFiles[i]);
         }

         datedFiles = dir.listFiles(Util.DIR_FILES);
         for (int i=0; datedFiles!=null && i<datedFiles.length; i++) {
             readFiles(datedFiles[i]);
         }
    }

    private static void readFile(File f) {
        DatedFile dated = null;

        if (f.getName().endsWith(JTS_SUFFIX)) {
            BuildJTS.readJTSFiles(f);
        } else if (f.getName().endsWith(JTL_SUFFIX)) {
            BuildJTL.readJTLFiles(f);
        } else if (f.getName().endsWith(XML_SUFFIX)) {
            TestResults.readTestResults(f);
        } else if (f.getName().endsWith(".log")) {
            DatedLog.readDatedLog(f);
        } else {
            System.out.println("ERROR: unrecognized log file: "+f);
        }
    }
    */

    public static void readFiles(File dir) {
        try {
            VirtualDir vd = VirtualDir.create(dir);

            Iterator<VirtualFile> it = vd.getFiles(Util.DATED_FILES);
            while (it.hasNext()) {
                VirtualFile vf = it.next();
                if (vf.getName().endsWith(JTS_SUFFIX)) {
                    BuildJTS.readJTSFiles(vf);
                } else if (vf.getName().endsWith(JTL_SUFFIX)) {
                    BuildJTL.readJTLFiles(vf);
                } else if (vf.getName().endsWith(XML_SUFFIX)
                           && (vf.getName().startsWith("TESTS-")
                               || vf.getName().startsWith("TEST-"))) {
                    TestResults.readTestResults(vf);
                } else if (vf.getName().equals(TESTLOGIC_LOG_FILE)) {
                    TestlogicResults.readTestResults(vf);
                } else if (vf.getName().endsWith(XML_SUFFIX)
                           && (vf.getName().equals(dir.getName() + XML_SUFFIX))) {
                    FarmResults.readFarmResults(vf, getSubmitTime());
                } else if (vf.getName().endsWith(XML_SUFFIX)) {
                } else if (vf.getName().endsWith(".history.log")) { 
                    DatedHistory.readDatedHistory(vf);
                } else if ((vf.length() > 0l)
                           && (vf.getName().endsWith(".log") 
                               || vf.getName().endsWith(".err") 
                               || vf.getName().endsWith(".tlg") 
                               || vf.getName().endsWith(".farm.out")
                               || vf.getName().endsWith(".out"))) {
                    DatedLog.readDatedLog(vf);
                } else if (vf.getName().endsWith(SUCC_SUFFIX) 
                           || vf.getName().endsWith(DIFF_SUFFIX)) { 
                    DatedLog.readDatedLog(vf);
                } else {
                    // System.out.println("WARNING: unrecognized log file: "+vf.getFullName());
                }
            }
        } catch (IOException ioe) {
           ioe.printStackTrace();
           System.out.println("SEVERE: error " + ioe + " when reading from " + dir);
        }
    }
    public static void main(String[] args) {

        if (args.length == 0) {
           Messages.explainTiming("SEVERE: must provide directory with regression results for timing analysis.\n");
           return;
        }
        /* StringBuffer sb = new StringBuffer("oracle.util.triage.DatedFile.Main ");
        for (int i=0; i<args.length; i++) {
             sb.append(" \"" + args[i] + "\"");
        }
        System.out.println(sb); */
             
        boolean readAreaDescriptors = false;
        boolean doFlatten = false;
        File csvFile = null;
        File htmlFile = null;
        List<String> dirsToCheck = new ArrayList<String>();

        seqCount = 0;
        theUniverse = new DatedFile(); 
        AreaDescriptors.initialize();


        /***************** COMMAND LINE OPTIONS PROCESSING *******************/

        for (int i = 0; i < args.length; i++) {
            if (args[i].equalsIgnoreCase("-areadesc")) {
                i++;
                if (i < args.length) {
                    AreaDescriptors.addDescriptors(args[i]);
                    readAreaDescriptors = true;
                } else {
                    System.out.println("SEVERE: Descriptor file name missing in -areadesc <descriptor-file> option.");
                }
            } else if (args[i].equalsIgnoreCase("-csv")) {
                i++;
                if (i < args.length) {
                    csvFile = new File(args[i]);
                    if (!csvFile.getName().endsWith(".csv")) {
                        System.out.println("WARNING: CSV file " + csvFile + " does not have '.csv' extension.");
                    }
                } else {
                    System.out.println("SEVERE: CSV file name is missing in -csv <file.csv> option.");
                }
            } else if (args[i].equalsIgnoreCase("-csvMaxLevel")) {
                i++;
                if (i < args.length) {
                  int level = 0;
                  try {
                     level = Integer.parseInt(args[i]);
                  } catch (Exception e) {
                     // Ignore
                  }
                  if (level <= 0) {
                    System.out.println("SEVERE: -csvMaxLevel " + args[i] +
                                       " - must specify positive integer for max level.");
                  } else {
                      setMaxCsvLevel(level);
                  }
                } else {
                    System.out.println("SEVERE: -csvMaxLevel <level> - level is missing for this option.");
                }
            } else if (args[i].equalsIgnoreCase("-html")) {
                i++;
                if (i < args.length) {
                    htmlFile = new File(args[i]);
                    if (!htmlFile.getName().endsWith(".htm") && !htmlFile.getName().endsWith(".html")) {
                        System.out.println("WARNING: HTML file " + htmlFile + " does not have '.html' extension.");
                    }
                } else {
                    System.out.println("SEVERE: HTML file name is missing in -html <file.html> option.");
                }
            } else if (args[i].equalsIgnoreCase("-submit") || args[i].equalsIgnoreCase("-submitted")) {
                i++;
                if (i < args.length) {
                    int year = 1900 + (new Date()).getYear();
                    // System.out.println("INFO: supplied submission time: " +  args[i] + " " + year + " " + FARM_TIMEZONE );
                    submitTime = Util.parseTime6( args[i] + " " + year + " " + FARM_TIMEZONE );
                } else {
                    System.out.println("SEVERE: UTC Submission time 'MMM DD HH:MM' missing in -submit <time> option.");
                }
            } else if (args[i].equalsIgnoreCase("-flatten")) {
                doFlatten = true;
            } else if (args[i].equalsIgnoreCase("-details") || args[i].equalsIgnoreCase("-detail")) {
                details = true;
            } else {
                dirsToCheck.add(args[i]);
            }
        }


        /***************** READ AREA DESCRIPTIONS *******************/

        if (!readAreaDescriptors) {
            File adf = new File(TRIAGE_AREA_FILE);
            if (adf.exists() && adf.isFile() && adf.canRead()) {
                AreaDescriptors.addDescriptors(adf.toString());
                readAreaDescriptors = true;
            } else if ( (adf = new File(TRIAGE_GLOBAL_DEFAULT_DIR + "/" + TRIAGE_AREA_FILE)).exists()
                    && adf.isFile() && adf.canRead()) {
                AreaDescriptors.addDescriptors(adf.toString());
                readAreaDescriptors = true;
            } else {
               if (DEBUG) {
                  System.out.println("NO good. Also: " + TRIAGE_GLOBAL_DEFAULT_DIR + "/" + TRIAGE_AREA_FILE + " did not work.");
               }
            }
        }


        /***************** NOW CHECK WORK and LOG DIRECTORIES *******************/

        if (dirsToCheck.size() == 0) {
           System.out.println("SEVERE: Must list one or more directories (such as $T_WORK) " +
                              " or log area to extract information from.");
        } else {
            for (int i = 0; i < dirsToCheck.size(); i++) {
                File f = new File(dirsToCheck.get(i));
                if (!VirtualDir.isVirtualDir(f)) {
                    System.out.println("SEVERE: " + f + " must be a real or virtual (e.g. workdir.tar.gz or jtwork.jar) directory.");
                } else {
                    readFiles(f);
                }
            }
        }

        /***************** POST-PROCESSING *******************/

        // If we could find the area descriptions, let's make sure we apply
        // these to all of the nodes.
        if (readAreaDescriptors) {
            if (DEBUG) { System.out.println("ADDING AREAS TO THE UNIVERSE"); }
            AreaDescriptors.addArea(theUniverse);
            if (DEBUG) { System.out.println("ADDING OWNERS TO THE UNIVERSE"); }
            AreaDescriptors.addOwner(theUniverse);
        }

        // If we were told to flatten the hierarchy, lets do so.
        if (doFlatten) {
            System.out.println("Flattening reporting depth.");
            flatten();
        }

        // The area descriptions may have told us to create some virtual
        // top-level tasks. Lets do so.
        AreaDescriptors.createToplevelTasks();

        // Now we expect no more changes to happen to the nodes in our
        // system. Thus we can number everything (in chronological order).
        number();


        /***************** OUTPUT ROUTINES *************************/


        // Text format

        if (csvFile == null && htmlFile == null) {
            System.out.println(theUniverse.toString());
        }


        // CSV and HTML format as requested

        final int CSV_INDEX = 0;
        final int HTML_INDEX = 1;
        final int LAST_INDEX = HTML_INDEX;

        File[] outFiles   = new File[LAST_INDEX + 1];
        String[] formats  = new String[LAST_INDEX + 1];

        outFiles[CSV_INDEX]  = csvFile;
        formats[CSV_INDEX]   = "CSV";
        
        outFiles[HTML_INDEX] = htmlFile;
        formats[HTML_INDEX]  = "HTML";

        for (int i = 0; i <= LAST_INDEX; i++) {
            if (outFiles[i] != null) {
                PrintStream ps = null;

                try {
                   ps = new PrintStream(new FileOutputStream(outFiles[i]));

                   DatedFile topNode = theUniverse;
                   if (topNode.children.size() == 1) {
                       topNode = topNode.children.get(0);
                   }
                   if (i == CSV_INDEX) {
                       ps.println(topNode.toCSV());
                   } else if (i == HTML_INDEX) {
                       ps.println(topNode.toHtml());
                   }
                   ps.close();
                   System.out.println("INFO: Wrote summary in " + formats[i] + " format to " + outFiles[i]);
                } catch (IOException ioe) {
                   System.out.println("SEVERE: Error in writing " + formats[i] + " file " + outFiles[i] + ": " + ioe);
                   try {
                      if (ps != null) {
                          ps.close();
                      }
                   } catch (Exception e) {
                      // ignore, closing resources
                   }
                }
            }
        } 

        // Some more stats in text format

        if (csvFile == null && htmlFile == null) {
            System.out.println();
            System.out.println(AreaDescriptors.getStats());
        }
   }

}
