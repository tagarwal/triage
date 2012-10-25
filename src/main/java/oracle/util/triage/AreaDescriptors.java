package oracle.util.triage;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import java.io.IOException;
import java.io.File;
import java.io.LineNumberReader;
import java.io.FileReader;

public class AreaDescriptors implements Constants {

    private static final boolean DEBUG = false;

    public static void initialize() {
        descriptors = new HashMap<String, Descriptor>();
        addedUnassignedAreaDescriptor = false;
    }

    public static void addDescriptors(String fileName) {
        if (DEBUG) {
            System.out.println("Add Descriptors: " + fileName);
        }
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(
                      new FileReader(
                          new File(fileName)));

            String currentArea = null;
            String line = null;
            boolean inArea = false;
            Descriptor areaDescriptor = null;

            while ( (line = lnr.readLine()) != null) {
                line = line.trim();
                if (line.equals("") || line.startsWith("#")) {
                    // skip
                } else if (line.startsWith("AREA ")) {
                    if (inArea) {
                       throw new Exception("Missing END_AREA tag.");
                    } 
                    inArea = true;
                    areaDescriptor = new Descriptor(line.substring("AREA ".length()).trim());
                } else if (line.startsWith("OWNER ")) {
                    if (!inArea) {
                       throw new Exception("Missing AREA tag.");
                    } 
                    areaDescriptor.addOwner(line.substring("OWNER ".length()).trim());
                } else if (line.startsWith("CREATE_TOPLEVEL_TASK ")) {
                    if (!inArea) {
                       throw new Exception("Missing AREA tag.");
                    } 
                    areaDescriptor.setToplevelTask(line.substring("CREATE_TOPLEVEL_TASK ".length()).trim());
                } else if (line.equals("EXCLUDE_TIME_REPORTING")) {
                    if (!inArea) {
                       throw new Exception("Missing AREA tag.");
                    } 
                    areaDescriptor.setExcludeTimeReporting(true);
                } else if (line.equals("END_AREA")) {
                    if (!inArea) {
                       throw new Exception("Missing AREA tag.");
                    } 
                    inArea = false;
                    addDescriptor(areaDescriptor);
                    if (DEBUG) {
                        System.out.println(areaDescriptor.toString());
                    }
                } else if (inArea) {
                    if (line.endsWith(MARKER_PLACEHOLDER)) {
                        line = line.substring(0, line.length() - MARKER_PLACEHOLDER.length())
                             + MARKER_PATTERN;
                    }
                    areaDescriptor.addTarget(line);
                } else {
                    throw new Exception("Unrecognized entry: '" + line + "'");
                }
            }
            lnr.close();
            if (inArea) {
                throw new Exception("Missing END_AREA tag.");
            }
        } catch (Exception e) {
            System.out.println("SEVERE: error in reading area definition from " + fileName + ": " + e);
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (IOException ioe) {
                    // skip - closing resources
                }
            }
        }
    }

    private static void addDescriptor(Descriptor areaDescriptor) {
        String[] areas = areaDescriptor.getTargets();
        for (String area : areas) {
             descriptors.put(area, areaDescriptor);
        }
    }
    private static HashMap<String, Descriptor> descriptors = new HashMap<String, Descriptor>();
    private static boolean addedUnassignedAreaDescriptor = false;

    public static String getArea(String targetOrFile) {
        String a = getArea2(targetOrFile);
        if (DEBUG) {
           System.out.println("getArea(" + targetOrFile + ")=" + a);
        }
        return a;
    }
    public static String getArea2(String targetOrFile) {
        Descriptor d = descriptors.get(Util.standardized(targetOrFile));
        return (d == null) ? null : d.getArea();
    }

    public static String getOwner(String targetOrFile) {
        Descriptor d = descriptors.get(Util.standardized(targetOrFile));
        return (d == null) ? null : d.getOwner();
    }

    public static class Descriptor {
        private Descriptor(String area) {
            this.area = area;
        }

        private String area;
        private String owner;
        private List<String> targets = new ArrayList<String>();
        private String toplevelTask;
        private boolean excludeTimeReporting = false;

        public String getArea() {
            return area;
        }

        public String getOwner() {
            return (owner == null) ? "N/A" : owner;
        }

        public String getToplevelTask() {
            return toplevelTask;
        }

        void setExcludeTimeReporting(boolean b) {
            excludeTimeReporting = b;
        }

        public boolean getExcludeTimeReporting() {
            return excludeTimeReporting;
        }

        void setToplevelTask(String task) {
            toplevelTask = task;
        }

        public void addOwner(String s) {
            if (owner == null) {
                owner = s;
            } else {
                owner += ", " + s;
            }
        }

        public void addTarget(String s) {
            targets.add(s);
        }

        public String[] getTargets() {
            String[] tgts = new String[targets.size()];
            for (int i = 0; i < targets.size(); i++) {
                 tgts[i] = targets.get(i);
            }
            return tgts;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("AREA " + getArea() + " (" + getOwner() + ") - ["); 
            String[] tgts = getTargets();
            for (int i = 0; i < tgts.length; i++) {
                 sb.append(tgts[i]);
                 if (i < tgts.length - 1) {
                     sb.append(",");
                 }
            }
            sb.append("]"); 
            return sb.toString();
        }

        public void addTime(float f) {
            time += f;
        }
   
        public float getTime() {
            return time;
        }
        private float time = 0.0f;
    }

    public static void addArea(DatedFile df) {
        if (!addedUnassignedAreaDescriptor) {
            Descriptor ua = new Descriptor(UNASSIGNED_AREA);
            ua.addTarget(UNASSIGNED_AREA);
            addDescriptor(ua);
            addedUnassignedAreaDescriptor = true;
        }

        String name = Util.standardized(df.getFullName());
        String area = getArea(name);
        if (area != null) {
            if (DEBUG) {
               System.out.println("AREA for " + name + " is " + area);
            }
            addArea(df, area);
            descriptors.get(name).addTime(df.getTime());
        } else {
            if (DEBUG) {
               System.out.println("No AREA for " + name + " - marked UNASSIGNED");
            }
            float selfTime = df.getTime() - df.getChildrenTime();
            descriptors.get(UNASSIGNED_AREA).addTime(selfTime);
            for (DatedFile child : df.getChildren()) {
                addArea(child);
            }
        }
    }

    private static void addArea(DatedFile df, String area) {
        if (df.getArea() == null) {
            df.setArea(area);
            for (DatedFile child : df.getChildren()) {
                addArea(child, area);
            }
        } else {
            System.out.println("WARNING: " + df.getFullName() + " already assigned to area " +
                               df.getArea() + " - will not reset.");
        }
    }

    public static void addOwner(DatedFile df) {
        String name = df.getFullName();
        String owner = getOwner(name);
        if (owner != null) {
            addOwner(df, owner);
        } else {
            for (DatedFile child : df.getChildren()) {
                addOwner(child);
            }
        }
    }

    private static void addOwner(DatedFile df, String owner) {
        if (df.getOwner() == null) {
            df.setOwner(owner);
            for (DatedFile child : df.getChildren()) {
                addOwner(child, owner);
            }
        } else {
            System.out.println("WARNING: " + df.getFullName() + " already assigned to " + df.getArea() + " - will not reset.");
        }
    }

    public static String getStats() {
        return getStats(false);
    }

    public static String getHtmlStats() {
        return getStats(true);
    }

    private static List<Descriptor> sortedDescriptors() {
        Iterator<Descriptor> it = descriptors.values().iterator();
        List<Descriptor> lst = new ArrayList<Descriptor>();

        while (it.hasNext()) {
            Descriptor d = it.next();
            boolean done = false;
            for (int i = 0; !done && i < lst.size(); i++) {
                if (d.getTime() > lst.get(i).getTime()) {
                    lst.add(i, d);
                    done = true;
                } else if (d == lst.get(i)) {
                    done = true;
                }
            }
            if (!done) {
                lst.add(d);
            }
        }
        return lst;
    }

    public static void createToplevelTasks() {
        List<Descriptor> lst = sortedDescriptors();
        for (int i = 0; i < lst.size(); i++) {
             // Does this area need one (or more) toplevel tasks?
             if (lst.get(i).getToplevelTask() != null) {
                 String area = lst.get(i).getArea();
                 String taskName = lst.get(i).getToplevelTask();
                 List<DatedFile> roots = DatedFile.getRoots(area);

                 DatedFile lastParent = null;
                 DatedFile lastParentsFirstChild = null;
                 DatedFile lastParentsLastChild = null;
                 int childCount = 0;
                 int toplevelTaskCount = 0;

                 // Determine the parents and check whether they are all the same
                 for (int j = 0; j < roots.size(); j++) {
                     DatedFile parent = roots.get(j).getParent();
                     if (j == 0) {
                        lastParent = parent;
                        lastParentsFirstChild = roots.get(j);
                        lastParentsLastChild = roots.get(j);
                        childCount = 1;
                     } else if (parent == lastParent) {
                        lastParentsLastChild = roots.get(j);
                        childCount++;
                     }

                     if (j > 0 && (lastParent != parent || j == roots.size() - 1)) {
                        if (childCount > 1) {
                            toplevelTaskCount++;
                            String name = (toplevelTaskCount <= 1)
                                           ? taskName 
                                           : taskName + toplevelTaskCount;
                            DatedFile newChild = new DatedFile(name,
                                                              lastParentsFirstChild.getStartTime(),
                                                              lastParentsLastChild.getEndTime());
                            newChild.setArea(area);
                            lastParent.addChild(newChild);
                        }

                        if (lastParent != parent) {
                            lastParent = parent;
                            lastParentsFirstChild = roots.get(j);
                            lastParentsLastChild = roots.get(j);
                            childCount = 1;
                        }
                     }
                 }
             }
        }
    }

    private static String getStats(boolean html) {
        StringBuffer sb = new StringBuffer();
        if (html) {
           sb.append("<table><tr><th>Time</th><th>Area</th><th>Owners</th><th>Roots</th></tr>");
           sb.append(NL);
        } else {
           sb.append("Statistics: Time, Area, Owner");
           sb.append(NL);
        }

        List<Descriptor> lst = sortedDescriptors();
        for (int i = 0; i < lst.size(); i++) {
            Descriptor d = lst.get(i);
            if (d.getTime() > EPSILON) {
                if (html) {
                    String area = d.getArea();
                    String owner = d.getOwner();
                    if (area == null || area == UNASSIGNED_AREA) {
                        area = Util.makeColor(GRAY_COLOR, area );
                        owner = Util.makeColor(GRAY_COLOR, owner );
                    }
                    sb.append("<tr><td>" + Util.secs(d.getTime()) +
                              "</td><td>" + area +
                              "</td><td>" + owner +
                              "</td><td>");
                    if (!d.getExcludeTimeReporting()) {
                        List<DatedFile> roots = DatedFile.getRoots(d.getArea());
    
                        // Sort in decreasing time
                        boolean sorted = false;                    
                        while (!sorted) {
                           sorted = true;
                           for (int j = 0; j < roots.size() - 1; j++) {
                                if (roots.get(j).getTime() < roots.get(j + 1).getTime()) {
                                    DatedFile tmp = roots.get(j);
                                    roots.set(j, roots.get(j + 1));
                                    roots.set(j + 1, tmp);
                                    sorted = false;
                                }
                            }
                        }
    
                        for (int j = 0; j < roots.size(); j++) {
                             DatedFile df = roots.get(j);
                             sb.append(Util.secs(df.getTime()) +
                                       ": <a href=\"#\" onclick=\"expandToItem('tree1'," +
                                       "'n" + df.getNumber() + "'); return false;\">" + df.getFullName() + "</a>");
                             if (j < roots.size() - 1) {
                                 sb.append(", ");
                             }
                        }
                    }
                    sb.append("</td></tr>");
                    sb.append(NL);
                } else {
                    sb.append(d.getTime() + ", " + d.getArea() + ", " + d.getOwner());
                    sb.append(NL);
                }
            }
        }
        if (html) {
            sb.append("</table>");
            sb.append(NL);
        }

        return sb.toString();
    }

}
