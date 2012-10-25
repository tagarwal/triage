package oracle.util.triage;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;


public class UnitTestSuite implements Constants {

    public UnitTestSuite() {
    }

    public void addProperty(String pName, String value) {
        if (props.get(pName) == null) {
            propNames.add(pName);
            propsSorted = false;
        }
        props.put(pName, value);
    }

    public String[] getProperties() {
        while (!propsSorted) {
            propsSorted = true;
            for (int i = 0; i < propNames.size() - 1; i++) {
                if (propNames.get(i).compareTo(propNames.get(i + 1)) < 0) {
                    String tmp = propNames.get(i);
                    propNames.set(i, propNames.get(i + 1));
                    propNames.set(i + 1, tmp);
                    propsSorted = false;
                }
            }
        }
        return (String[]) propNames.toArray();
    }

    public String getProperty(String pName) {
        return props.get(pName);
    }

    public String getStdOut() {
        return stdOut;
    }

    public void setStdOut(String s) {
        stdOut = s;
    }

    public String getStdErr() {
        return stdErr;
    }

    public void setStdErr(String s) {
        stdErr = s;
    }

    public void setDiffs(int d) {
        totalDiffs = d;
    }

    public void setSuccs(int s) {
        totalSuccs = s;
    }

    public void deriveTotals() {
        if (Full.DEBUG) {
            System.out.println("DEBUG: " + getFullName() + ".deriveTotals()");
        }
        totalDiffs = numDiffs;
        totalSuccs = numSuccs;
    }
    

    public int getDiffs() {
        if (numDiffs == totalDiffs) {
            // things are in sync
        } else if (numDiffs == 0) {
            // copy summary if we got no details
            numDiffs = totalDiffs;
        } else {
            System.out.println("WARNING: test suite " + getFullName() + " summary has " +
                             totalDiffs + " " + JUNIT_DIFFS + " but tally gives " + numDiffs + " " + JUNIT_DIFFS + ".");
            totalDiffs = numDiffs;
        }
        return numDiffs;
    }

    public int getSuccs() {
        if (numSuccs == totalSuccs) {
        // things are in sync
        } else if (numSuccs == 0) {
            // copy summary if we got no details
            numSuccs = totalSuccs;
        } else {
            System.out.println("WARNING: test suite " + getFullName() + " summary has " +
                             totalSuccs + " " + JUNIT_SUCCS + " but tally gives " + numSuccs + " " + JUNIT_SUCCS + ".");
            totalSuccs = numSuccs;
        }
        return numSuccs;
    }

    public void setId(String s) {
        id = s;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        if (name == null) {
            if (pakkage == null) {
                return id;
            } else  {
                return pakkage;
            }
        } else {
            return name;
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPackage() {
        return pakkage;
    }

    public void setPackage(String p) {
        this.pakkage = p;
    }

    public String getFullName() {
        if (getPackage() != null) {
            return getPackage() + "-" + getName();
        } else {
            return getName();
        }
    }

    public float getTime() {
        return time;
    }

    public void setTime(float f) {
        time = f;
    }

    public void setStartEndTime(long start, long end) {
        startTime = start;
        time = (end - startTime) / 1000.0f;
    }
    private long startTime = 0l;
    

    public void addTest(UnitTest t) {
        if (tests.size() > 0
            && tests.get(tests.size() - 1).mergableWith(t)) {
            tests.get(tests.size() - 1).mergeWith(t);
        } else {
            if (t.isDiff()) {
                numDiffs ++;
            } else {
                numSuccs ++;
            }
            tests.add(t);
        }
    }


    public UnitTest[] getTests() {
        UnitTest[] uts = new UnitTest[tests.size()];
        for (int i = 0; i < tests.size(); i++) {
            uts[i] = tests.get(i);
        }
        return uts;
    }

    public String toString(int level, int suiteCount, boolean diffsOnly) {
        if (diffsOnly && getDiffs() <= 0 && !Main.showTimings()) {
            return null;
        }

        boolean showDetail = INFO < level;
        boolean showFullDetail = getDiffs() > 0 || FINER < level;

        StringBuffer sb = new StringBuffer();

        if (showDetail && suiteCount > 0) {
            sb.append("(");
            sb.append(suiteCount);
            sb.append(") -- ");
        }

        
        sb.append(getDiffs());
        if (getDiffs() == 1) {
            sb.append(" " + JUNIT_DIFF + ", ");
        } else {
            sb.append(" " + JUNIT_DIFFS + ", ");
        }

        sb.append(getSuccs());
        if (getSuccs() == 1) {
            sb.append(" " + JUNIT_SUCC);
        } else {
            sb.append(" " + JUNIT_SUCCS);
        }

        sb.append(" in suite ");
        sb.append(getFullName());

        
        if (showDetail && suiteCount > 0) {
            sb.append(" --");
        }

        if (Main.showTimings()) {
            sb.append(" ");
            sb.append(getTime());
            sb.append(" secs");
        }
        sb.append(NL);

        if (showFullDetail) {
            int count = 0;
            for (int i = 0; i < tests.size(); i++) {
                UnitTest t = tests.get(i);
                String ts = t.toString(level, diffsOnly);

                if (ts != null && !ts.trim().equals("")) {
                    count++;
                    sb.append("(");
                    if (suiteCount > 0) {
                        sb.append(suiteCount);
                        sb.append(".");
                    }
                    sb.append(count);
                    sb.append(") ");
                    sb.append(ts);
                    sb.append(NL);
                }
            }
        }
        return sb.toString();
    }

    public String toHtml(int level, int suiteCount, boolean diffsOnly) {
        if (diffsOnly && getDiffs() <= 0) {
            return null;
        }


        List<String> uary = new ArrayList<String>();
        // if (showFullDetail) {
            int count = 0;
            for (int i = 0; i < tests.size(); i++) {
                StringBuffer utb = new StringBuffer();
                UnitTest t = tests.get(i);
                String ts = t.toHtml(level, diffsOnly);

                if (ts != null && !ts.trim().equals("")) {
                    count++;
                    utb.append("(");
                    if (suiteCount > 0) {
                        utb.append(suiteCount);
                        utb.append(".");
                    }
                    utb.append(count);
                    utb.append(") ");
                    utb.append(t.getName());
                    utb.append(NL);
                    utb.append(ts);
                    utb.append(NL);
                    uary.add(utb.toString());
                }
            }
        // }


        StringBuffer sb = new StringBuffer();
        if (suiteCount > 0 || uary.size() > 1) {
            if (suiteCount > 0) {
                sb.append("(");
                sb.append(suiteCount);
                sb.append(")");
            }

            sb.append(getDiffs());
            if (getDiffs() == 1) {
                sb.append(" " + JUNIT_DIFF + ", ");
            } else {
                sb.append(" " + JUNIT_DIFFS + ", ");
            }
            sb.append(getSuccs());
            if (getSuccs() == 1) {
                sb.append(" " + JUNIT_SUCC);
            } else {
                sb.append(" " + JUNIT_SUCCS);
            }

            if (suiteCount > 0) {
                sb.append(" in suite ");
                sb.append(getFullName());
            }
            sb.append("<br>" + NL); 
        }


        if (uary.size() > 1) {
            StringBuffer ssb = new StringBuffer();
            for (int i = 0; i < uary.size(); i++) {
                if (uary.get(i) != null && !uary.get(i).equals("")) {
                    ssb.append(Util.makeLi(uary.get(i)));
                }
            }
            sb.append(Util.makeUl(ssb.toString()));
        } else if (uary.size() == 1) {
            sb.append(uary.get(0));
        } else {
            return null;
        }

        return sb.toString();
    }
     
    private boolean propsSorted = false;
    private List<String> propNames = new ArrayList<String>();
    private HashMap<String, String> props = new HashMap<String, String>();
    private List<UnitTest> tests = new ArrayList<UnitTest>();

    private String stdOut = "";
    private String stdErr = "";
    private String name;
    private String pakkage;
    private String id;
    private float time;

    private int numSuccs;
    private int numDiffs;
    private int totalSuccs;
    private int totalDiffs;
}
