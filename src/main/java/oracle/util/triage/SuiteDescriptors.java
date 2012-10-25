package oracle.util.triage;

import java.util.HashMap;

import java.io.IOException;
import java.io.File;
import java.io.LineNumberReader;
import java.io.FileReader;

public class SuiteDescriptors {

    private static final boolean DEBUG = false;

    public SuiteDescriptors() {
    }

    public void addDescriptors(String fileName) {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(
                      new FileReader(
                          new File(fileName)));

            String line = null;
            int pos;
            while ( (line = lnr.readLine()) != null) {
                line = line.trim();
                if (line.equals("") || line.startsWith("#")) {
                    // skip
                } else if ( (pos = line.indexOf("=")) > 0) {
                    String key = line.substring(0, pos - 1).trim();
                    String value = line.substring(pos + 1).trim();
         
                    if (entries.get(key) != null) {
                        System.out.println("WARNING: " + fileName + ":" + lnr.getLineNumber() + ": " +
                               "duplicate definition of suite " + key + ": " + value + ". " +
                               "Was already mapped to: " + entries.get(key) + ". Entry ignored.");
                    } else {
                        entries.put(key, value);
                    }
                } else {
                    // skip - or should we give a warning?
                }
            }
            lnr.close();
        } catch (Exception e) {
            System.out.println("SEVERE: error in reading suite descriptors from " + fileName + ": " + e);
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

    private HashMap<String, String> entries = new HashMap<String, String>();

    public String getDescriptor(String testName) {
        return entries.get(testName);
    }

}
