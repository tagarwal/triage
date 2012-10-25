package oracle.util.triage;

import java.util.HashMap;
import java.util.Iterator;

import java.io.IOException;
import java.io.File;
import java.io.LineNumberReader;
import java.io.FileReader;

public class TriageDescriptors {

    private static final boolean DEBUG = false;

    public TriageDescriptors() {
    }

    public void addDescriptors(String fileName) {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(
                      new FileReader(
                          new File(fileName)));

            String line = null;
            while ( (line = lnr.readLine()) != null) {
                line = line.trim();
                if (line.equals("") || line.startsWith("||")) {
                    // skip
                } else if (line.startsWith("|")) {
                    Descriptor td = new Descriptor(line);
                    addEntry(td, fileName, lnr.getLineNumber());
                } else {
                    // skip - or should we give a warning?
                }
            }
            lnr.close();
        } catch (Exception e) {
            System.out.println("SEVERE: error in reading test descriptors from " + fileName + ": " + e);
            e.printStackTrace();
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (IOException ioe) {
                    // skip - closing resources
                }
            }
        }
    }

    private void addEntry(Descriptor td, String fName, int line) {
        String name = td.getName();
        if (name.endsWith("*")) {
            name = name.substring(0, name.length() - 1);
        }
        if (descriptors.get(name) != null) {
            System.out.println("WARNING: " + fName + ":" + line + ": " +
                         "duplicate test " + name + ": " + td + " and " + descriptors.get(name));
        }
        descriptors.put(name, td);
    }
    private HashMap<String, Descriptor> descriptors = new HashMap<String, Descriptor>();

    public Descriptor getDescriptor(String testName) {
        Descriptor d = descriptors.get(testName);
        if (d == null) {
            String match = null;
            int matchLength = 0;
            Iterator<String> it = descriptors.keySet().iterator();
            while (it.hasNext()) {
                String key = it.next();
                if (testName.startsWith(key) && key.length() > matchLength) {
                    match = key;
                    matchLength = key.length();
                }
            }
            if (match != null) {
                d = descriptors.get(match);
            }
        }
        return d;
    }

    public static class Descriptor {
        private Descriptor(String tableEntry) {
            tableEntry = cleanUp(tableEntry);
            readColumns(tableEntry);
        }

        public String getName() {
            return name;
        }

        public String getPointOfContact() {
            return pointOfContact;
        }

        public String getComponent() {
            return component;
        }

        public String getDescription() {
            return description;
        }

        public String getIntegrationPoints() {
            return integrationPoints;
        }

        public boolean isMasManaged() {
            return masManaged;
        }

        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append(getComponent());
            sb.append("::");
            sb.append(getName());
            sb.append(": ");
            sb.append(getDescription());
            sb.append(" - contact ");
            sb.append(getPointOfContact());
            return sb.toString();
        }

        private String cleanUp(String s) {
            for (int i = 0; i < SOURCE.length; i++) {
                int pos = 0;
                while ( (pos = s.indexOf(SOURCE[i])) > 0) {
                    s = s.substring(0, pos - 1) + TARGET[i] + s.substring(pos + SOURCE[i].length());
                }
            }
            return s;
        }

        private void readColumns(String s) {
            if (DEBUG) {
                System.out.println("readColumns(\"" + s + "\")");
            }

            int column = 0;
            int pos, pos2;
       
            s = s.substring(1);
            while (column <= POINT_OF_CONTACT_INDEX) {

                pos = s.indexOf("|");
                pos2 = 0;

                while ( (pos2 = s.indexOf("[", pos2)) >= 0 && pos2 < pos ) {
                    pos2 = s.indexOf("]", pos2);
                    pos = s.indexOf("|", pos2);
                }
                String col = s.substring(0, pos).trim();
                s = s.substring(pos + 1);

                if (DEBUG) {
                    System.out.println("col #" + column + "= \"" + col + "\"");
                }

          
                switch (column) {
                    case ID_INDEX: 
                        break;
                    case COMPONENT_INDEX:
                        component = col;
                        break;
                    case TEST_NAME_INDEX:
                        name = col;
                        break;
                    case DESCRIPTION_INDEX:
                        description = col;
                        break;
                    case MAS_MANAGED_INDEX: 
                        if (col.equalsIgnoreCase("yes")
                            || col.equalsIgnoreCase("y")
                            || col.equalsIgnoreCase("true")
                            || col.equalsIgnoreCase("t")) {
                            masManaged = true;
                        } else if (col.equalsIgnoreCase("no")
                            || col.equalsIgnoreCase("none")
                            || col.equalsIgnoreCase("n")
                            || col.equalsIgnoreCase("false")
                            || col.equalsIgnoreCase("f")) {
                            masManaged = true;
                        } else {
                            System.out.println("SEVERE: test " + getName() +
                                 " unable to determine whether this test is MAS managed: " + col);
                        };
                        break;
                    case INTEGRATION_POINTS_INDEX:
                        integrationPoints = col;
                        break;
                    case POINT_OF_CONTACT_INDEX:
                        pointOfContact = col;
                        break;
                    default:
                        System.out.println("SEVERE: test " + getName() + " unrecognized test info: " + col);
                };
                column++;
            }
        }

        private static final String[] SOURCE = 
            new String[] { "\\\\", "&nbsp;", "\\*", "\\|", "  ", };
        private static final String[] TARGET = 
            new String[] { "", " ", "*", " or ", " " };

        private String name;
        private String pointOfContact;
        private String component;
        private String description;
        private String integrationPoints;
        private boolean masManaged;

        private static final int ID_INDEX = 0;
        private static final int COMPONENT_INDEX = 1;
        private static final int TEST_NAME_INDEX = 2;
        private static final int DESCRIPTION_INDEX = 3;
        private static final int MAS_MANAGED_INDEX = 4;
        private static final int INTEGRATION_POINTS_INDEX = 5;
        private static final int POINT_OF_CONTACT_INDEX = 6;
    }

}
