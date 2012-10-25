package oracle.util.triage;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.HashSet;
import java.util.StringTokenizer;
import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 This obtains information on a Farm job.
 */


public class FarmInfo implements Constants {

    private static final String FARM_COMMAND_LINE = "farm showjobs -detail -job ";
    private static final String DTE_COMMAND_LINE  = "/usr/local/packages/aime/dte/DTE2.0/bin/getJobInfo -t ";

    public static final int RUNNING = 1;  // job submitted, or job running and not yet completed
    public static final int FINISHED = 2; // job completed and results available
    public static final int ABORTED = 3;  // job timed out or was aborted. (partial) results available
    public static final int FAILED = 4;   // job not submittable
    public static final int WAITING = 5;  // job waiting in queue


    private FarmInfo(String location) throws Exception {
        System.out.println("NOTE: If this is a farm job, please provide the Farm ID for better triage!");
        this.onFarm = false;
        RegressionSuite rs = new RegressionSuite(this, location);
        suites.add(rs);
    }

    private FarmInfo(int farmJobId, String suiteFilter) throws Exception {
        this(farmJobId, 0, 0, suiteFilter);
    }

    private FarmInfo(int farmJobId) throws Exception {
        this(farmJobId, 0, 0, null);
    }

    private FarmInfo(int farmJobId, int dteJobId,  int rbJobId, String suiteList) throws Exception {
        this.farmJobId = farmJobId;
        this.dteJobId = dteJobId;
        this.rbJobId = rbJobId;

        this.onFarm = true;

        String jobStr = "" + farmJobId;
        String cmd = FARM_COMMAND_LINE + jobStr;

        initFilter(suiteList);

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;
            int pos = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();

                if (line.startsWith(jobStr)) {
                    // 1727780 MAR 12 12:31 farms         aime_sshot_farmsubmit finished             4
                    pos = "1727780 ".length();
                    int width = "MAR 12 12:31".length();
                    dateSubmitted = line.substring(pos, pos + width).trim();
                } else if (line.startsWith("Results location  : ")) {
                    pos = "Results location  : ".length();
                    resultsLocation = line.substring(pos).trim();
                } else if ((pos = line.indexOf(" : fin : ")) > 0) {
                    String suite = line.substring(0, pos).trim();

                    if (isFilterSuite(suite)) {
                        String sucsDifs = line.substring(pos + " : fin : ".length()).trim();
                        pos = sucsDifs.indexOf(" sucs, ");
                        int sucs = Integer.parseInt(sucsDifs.substring(0, pos).trim());
    
                        sucsDifs = sucsDifs.substring(pos + " sucs, ".length());
                        pos = sucsDifs.indexOf(" difs in ");
                        int difs = Integer.parseInt(sucsDifs.substring(0, pos).trim());
    
                        pos = sucsDifs.indexOf(" hrs [");
                        sucsDifs = sucsDifs.substring(pos + " hrs [".length());
                        pos = sucsDifs.indexOf(" - ");
                        int pos2 = sucsDifs.indexOf("]");
    
                        long start = Util.parseShortTime11( sucsDifs.substring(0, pos) ).getTime(); 
                        long end   = Util.parseShortTime11( sucsDifs.substring(pos + " - ".length(), pos2) ).getTime(); 
                        RegressionSuite rs = new RegressionSuite(this, suite, FINISHED);
                        rs.setStartTime(start);
                        rs.setEndTime(end);
                        rs.setSucs(sucs);
                        rs.setDifs(difs);
                        suites.add(rs);
                        if (Full.DEBUG) {
                            System.out.println("added new RegressionSuite(" + suite + "," + "FINISHED)");
                        }
                    }
                } else if ((pos = line.indexOf(" : run : ")) > 0) {
                    String suite = line.substring(0, pos).trim();
                    if (isFilterSuite(suite)) {
                        RegressionSuite rs = new RegressionSuite(this, suite, RUNNING);
                        suites.add(rs);
                    }
                } else if ((pos = line.indexOf(" : failed : ")) > 0) {
                    String suite = line.substring(0, pos).trim();
                    String cause = line.substring(pos + " : failed : ".length()).trim();
                    if (isFilterSuite(suite)) {
                        RegressionSuite rs = new RegressionSuite(this, suite, ABORTED);
                        rs.setCause(cause);
                        suites.add(rs);
                    }
                } else if ((pos = line.indexOf(" : invalid lrg")) > 0) {
                    String suite = line.substring(0, pos).trim();
                    if (isFilterSuite(suite)) {
                        RegressionSuite rs = new RegressionSuite(this, suite, FAILED);
                        suites.add(rs);
                    }
                } else if ((pos = line.indexOf(" : not started")) > 0) {
                    String suite = line.substring(0, pos).trim();
                    if (isFilterSuite(suite)) {
                        RegressionSuite rs = new RegressionSuite(this, suite, WAITING);
                        suites.add(rs);
                    }
                }
            }

            // wait for child to return
            process.waitFor();

            if (suiteFilter.size() > 0 && suites.size() == 0) {
                System.out.println("WARNING: did not see any suite from " + suiteList);
            }

            if (this.dteJobId > 0) {
                setHeaderPrefix("DTE " + Util.makeDteLink(dteJobId));
            }
        } catch (Exception e) {
            System.out.println("SEVERE: issue when checking job " + getName() + ": " + e);
        }
    }

    public boolean isFilterSuite(String s) {
        if (suiteFilter.size() == 0) {
            return true;
        } else {
            return suiteFilter.contains(s);
        }
    }

    private void initFilter(String filterList) {
        if (filterList == null || filterList.equals("")) {
            return;
        }
        StringTokenizer st = new StringTokenizer(filterList);
        while (st.hasMoreTokens()) {
            String s = st.nextToken();
            suiteFilter.add(s);
            if (!s.startsWith("lrg")) {
                suiteFilter.add("lrg" + s);
            }
        }
    }
    private Set<String> suiteFilter = new HashSet<String>();

    public boolean isFarm() {
        return onFarm;
    }
    private boolean onFarm = false;

    static FarmInfo newFarmInfo(String location) {
        if (location.endsWith("/")) {
            location = location.substring(0, location.length() - 1);
        }

        try {
            int pos;
            if (Util.isNumeric(location)) {
                // Farm job id:  1735678
                int jobId = Integer.parseInt(location);
                return new FarmInfo(jobId);
            } else if ((pos = location.indexOf(":")) > 0 && Util.isNumeric(location.substring(0, pos))) {
                int jobId = Integer.parseInt(location.substring(0, pos));
                String suite = location.substring(pos + 1);
                return new FarmInfo(jobId, suite);
            } else if ( (pos = jobIdDirectory(location)) > 0) {
                 // Farm job results directory:
                 //    /net/stdmlina42/farm_results/J2EE_MAIN_GENERIC_T1735116
                 // or individual farm test suite directory:
                 //    /net/stdmlina42/farm_results/J2EE_MAIN_GENERIC_T1735116/lrgcoresrg
                String jobIdStr = location.substring(pos);
                System.out.println("jobIdStr(1)=" + jobIdStr);
                String suite = null;
                int dte = 0;

                int pos2 = jobIdStr.indexOf("/");
                if (pos2 > 0) {
                    suite = jobIdStr.substring(pos2 + 1);
                    jobIdStr = jobIdStr.substring(0, pos2);
                    System.out.println("jobIdStr(2)=" + jobIdStr);

                    pos2 = suite.indexOf(".");
                    if ((pos2 > 0) && Util.isNumeric(suite.substring(0, pos2))) {
                        dte = Integer.parseInt(suite.substring(0, pos2));
                         // System.out.println("dte="+dte);
                        suite = null;
                    }
                }
                int jobId = Integer.parseInt(jobIdStr);

                 // System.out.println("jobIdStr(2)="+jobIdStr);
                if (suite != null && suite.equals("")) {
                    suite = null;
                }
                return new FarmInfo(jobId, dte, 0, suite);

            // } else {
            //     // other farm jobs
            } else {
                 // Local or nfs-accessible directory:
                 //   $T_WORK
                 // or
                 //   /net/stbbo03/scratch/vgoel/view_storage/vgoel_j2ee_main_view/oracle/work
                return new FarmInfo(location);
            }
        } catch (Exception e) {
            System.out.println("SEVERE: Unable to determine test results or farm job from: " + location + ": " + e);
            e.printStackTrace();
            return null;
        }
    }


    private int farmJobId;
    private int dteJobId;
    private int rbJobId;
    private String privateName;

    private String dateSubmitted; // TODO: make this a long or Date
    private String resultsLocation;
    private List<RegressionSuite> suites = new ArrayList<RegressionSuite>();

    public String getName() {
        StringBuffer sb = new StringBuffer();
        if (getRbJobId() > 0) {
            sb.append("RB" + getRbJobId() + ", ");
        }
        if (getDteJobId() > 0) {
            sb.append("DTE" + getDteJobId() + ", ");
        }
        if (getFarmJobId() > 0) {
            sb.append("Farm " + getDteJobId());
        }
        if (getPrivateName() != null) {
            sb.append(getPrivateName());
        }
        return sb.toString();  
    }

    public void setHeaderPrefix(String prefix) {
        headerPrefix = prefix;
    }
    private String headerPrefix = null;

    public String getHtmlHeader() {
        if (getFarmJobId() > 0) {
            return ((headerPrefix == null) ? "" : headerPrefix + ": ") 
                   + Util.makeJobLink(getFarmJobId());
        } else if (headerPrefix != null) {
            return headerPrefix;
        } else {
            return "Desktop run";
        }
    }


    public int getFarmJobId() {
        return farmJobId;
    }

    public int getDteJobId() {
        return dteJobId;
    }

    public int getRbJobId() {
        return rbJobId;
    }

    public String getPrivateName() {
        return privateName;
    }

    public List<RegressionSuite> getRegressionSuites() {
        return suites;
    }

    public String getResultsLocation() {
        return resultsLocation;
    }

    public String getDateSubmitted() {
        return dateSubmitted;
    }


    private static String getSuiteFromResultLocation(String location) {
        int pos = location.lastIndexOf("/");
        return location.substring(pos + 1);
    }

    private static int getJobIdResultsLocation(String location) {
        try {
            int pos = location.lastIndexOf("/");
            String id = location.substring(0, pos);
            pos = id.lastIndexOf("/");
            id = id.substring(pos + 1);
            pos = id.lastIndexOf("_T");
            id = id.substring(pos + "_T".length());
            return Integer.parseInt(id);
        } catch (Exception e) {
            System.out.println("SEVERE: unable to determine Farm Job ID from: " + location + " - " + e);
            return 0;
        }
    }


    private static int jobIdDirectory(String s) {
        int idx = 0; 
        int pos = s.indexOf("_T", 0);
        if (Full.DEBUG) {
            System.out.println("jobIdDirectory(" + s + ")");
        }
        while (pos > 0) {
            pos += "_T".length();
            String jobIdStr = s.substring(pos);
            if (Full.DEBUG) {
                System.out.println("- jobIdStr: " + jobIdStr + " (pos=" + pos + ")");
            }
            int pos2 = 0;
            if ((pos2 = jobIdStr.indexOf("/")) > 0) {
                jobIdStr = jobIdStr.substring(0, pos2);
            }
            if (Full.DEBUG) {
                System.out.println("- jobIdStr': " + jobIdStr);
            }
            if (Util.isNumeric(jobIdStr)) {
                if (Full.DEBUG) {
                    System.out.println("- jobIdDirectory(" + s + ") returns " + pos);
                }
                return pos;
            }
            idx = pos;
            pos = s.indexOf("_T", idx);
        }
        if (Full.DEBUG) {
            System.out.println("- jobIdDirectory(" + s + ") returns -1");
        }
        return -1;
    }

    public static class RegressionSuite {
        RegressionSuite(FarmInfo parent, String name, int status) {
            String s = getViewName(name);
            this.origName = name;
            if (!s.equals("local")) {
                this.name = s;
            } else {
                this.name = name;
            }
            this.status = status;
            this.parent = parent;

            if (Full.DEBUG) {
                System.out.println("name=" + this.name + ", status=" + status + ", parent=" + parent);
            }
        }

        RegressionSuite(FarmInfo fi, String directory) {
            this.name = getViewName(directory);
            this.status = FINISHED;
            this.startTime = 0l;
            this.endTime = 0l;
            this.localLocation = directory;
            this.parent = fi;
            try {
                File[] files = (new File(directory)).listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.isDirectory()) {
                             // skip
                        } else if (f.getName().endsWith(".suc")) {
                            this.sucs ++;
                        } else if (f.getName().endsWith(".dif")) {
                            this.difs ++;
                        }
                    }
                }
            } catch (Exception e) {
                System.out.println("SEVERE: Problem accessing local result location at " + directory + ": " + e);
            }
        }
        private String localLocation;
        private String origName;

        public String getSuite() {
            return name;
        }
        private String name;

        public int getStatus() {
            return status;
        }
        private int status;

        public void setStartTime(long l) {
            startTime = l;
        }
        public long getStartTime() {
            return startTime;
        }
        private long startTime;

        public void setEndTime(long l) {
            endTime = l;
        }
        public long getEndTime() {
            return endTime;
        }
        private long endTime;

        public void setCause(String s) {
            cause = s;
        }
        public String getCause() {
            return cause;
        }
        private String cause;

        public void setSucs(int i) {
            sucs = i;
        }
        public int getSucs() {
            return sucs;
        }
        private int sucs;

        public void setDifs(int i) {
            difs = i;
        }
        public int getDifs() {
            return difs;
        }
        private int difs;

        public String getResultLocation() {
            if (parent == null || !parent.isFarm()) {
                return localLocation;
            } else if (origName != null) {
                return parent.getResultsLocation() + "/" + origName;
            } else {
                return parent.getResultsLocation() + "/" + getSuite();
            }
        }

        public FarmInfo getFarmInfo() {
            return parent;
        }
        private FarmInfo parent;

        public void setTriageFile(String f) {
            triageFile = f;
        }
        public String getTriageFile() {
            return triageFile;
        } 
        private String triageFile;

        public void setHtmlFile(String f) {
            htmlFile = f;
        }
        public String getHtmlFile() {
            return htmlFile;
        } 
        private String htmlFile;

        public void setRunFile(String f) {
            runFile = f;
        }
        public String getRunFile() {
            return runFile;
        } 
        private String runFile;

        public void setBrowserFile(String f) {
            browserFile = f;
        }
        public String getBrowserFile() {
            return browserFile;
        } 
        private String browserFile;

        public void setMessageFile(String f) {
            messageFile = f;
        }
        public String getMessageFile() {
            return messageFile;
        }
        private String messageFile;

        public void setIncidentFile(String f) {
            incidentFile = f;
        }
        public String getIncidentFile() {
            return incidentFile;
        } 
        private String incidentFile;

        public void setIncidentCount(int count) {
            incidentCount = count;
        }
        public int getIncidentCount() {
            return incidentCount;
        }
        private int incidentCount = 0;

        public String getTriageLink() {
            String triage = "";
            if (getStatus() == RUNNING) {
                triage = Util.makeColor(BLUE_COLOR, "running");
            } else if (getStatus() == WAITING) {
                triage = "waiting";
            } else if (getStatus() == FAILED) {
                triage = Util.makeColor(RED_COLOR, "failed (invalid lrg)");
            } else if (getStatus() == ABORTED) {
                triage = Util.makeColor(RED_COLOR, "failed (" + getCause() + ")");
            } else if (getStatus() == FINISHED) {
                if (getSucs() == 0 && getDifs() == 0) {
                    triage = Util.makeColor(RED_COLOR, "build failure");
                } else if (getDifs() == 0) {
                    triage = Util.makeColor(GREEN_COLOR, "" + getSucs() + " sucs");
                } else if (getSucs() == 0) {
                    triage = Util.makeColor(RED_COLOR, "" + getDifs() + " difs");
                } else {
                    triage = Util.makeColor(RED_COLOR, "" + getDifs() + " difs") + ", "
                            + Util.makeColor(GREEN_COLOR, "" + getSucs() + " sucs");
                }
            }
            if (getTriageFile() != null) {
                triage = Util.makeTargetLink(getHtmlFile(), triage, "tree");
                TriageLog tl = new TriageLog(getTriageFile());
                String summary = tl.getTriageSummary();
                if (summary != null && !summary.equals("")) {
                    triage = Util.makeHint(summary, tl.getShortTriageSummary(), INFO_ICON) + triage;
                }
            } else if (getEndTime() != 0l) {
                 // long delta = (System.currentTimeMillis() - getEndTime()) / 1000l / 60l / 60l;
                 // triage = triage + " <small>(finished " + delta + " minutes ago)</small>";
            }

            if (getBrowserFile() != null) {
                triage = triage + Util.makeTargetLink(getBrowserFile(),
                                                       Util.makeIcon(TEXT_ICON),
                                                       "tree");
            }

            if (getRunFile() != null) {
                triage = triage + Util.makeTargetLink(getRunFile(),
                                                       Util.makeIcon(TREE_ICON),
                                                       "tree");
            }

            if (getMessageFile() != null) {
                triage = triage + Util.makeTargetLink(getMessageFile(),
                                                       Util.makeIcon(LETTER_ICON),
                                                       "tree");
            }

            if (getIncidentFile() != null && getIncidentCount() > 0) {
                triage = triage + Util.makeTargetLink(getIncidentFile(),
                                                       Util.makeIcon(WARNING_ICON),
                                                       "tree");
            }

            return triage;
        }
    }

    static String getViewName(String directory) {
        int pos = 0;
        String origDirectory = directory;
        if (directory.startsWith("/ade/")) {
            directory = directory.substring("/ade/".length());
            pos = directory.indexOf("/");
            if (pos > 0) {
                directory = directory.substring(0, pos);
            }
        } else if ((pos = directory.indexOf("view_storage/")) >= 0) {
            directory = directory.substring(pos + "view_storage/".length());
            pos = directory.indexOf("/");
            if (pos > 0) {
                directory =  directory.substring(0, pos);
            }

        } else if ((pos = directory.indexOf(".")) > 0 
            && Util.isNumeric(directory.substring(0, pos))) {
            directory = directory.substring(pos + 1);

        } else {
            directory = "local";
        }

        if (Full.DEBUG) {
            System.out.println("getViewName(" + origDirectory + ") = " + directory);
        }

        return directory;
    }



    static FarmInfo newDteInfo(int dteId) {
        String jobStr = "" + dteId;
        String cmd = DTE_COMMAND_LINE + jobStr;
        FarmInfo fi = null;
        int farmId = 0;

        try {
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream()));

            String line = null;
            int pos = 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Farm Submit ID : ")) {
                    String farmIdStr = line.substring("Farm Submit ID : ".length()).trim();
                    pos = farmIdStr.indexOf(" ");
                    if (pos > 0) {
                        farmIdStr = farmIdStr.substring(0, pos);
                    }
                    farmId = Integer.parseInt(farmIdStr);
                    
                }
            }

            // wait for child to return
            process.waitFor();

            if (farmId != 0) {
                fi = new FarmInfo(farmId, dteId, 0, null);
            }
        } catch (Exception e) {
            System.out.println("SEVERE: Unable to determine farm job from: " + dteId + ": " + e);
        } finally {
            return fi;
        }
    }

}
