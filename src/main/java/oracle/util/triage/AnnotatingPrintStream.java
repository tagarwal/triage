package oracle.util.triage;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class AnnotatingPrintStream extends PrintStream  implements Constants {

    public AnnotatingPrintStream(OutputStream os) {
        super(os);
    }
    private boolean initialized = false;

    private void init() {
        if (initialized) {
            return;
        } else {
            initialized = true;
        }

        if (Main.getArchiveFile() != null
            && Main.getArchiveFile() != DEFAULT_ARCHIVE_FILE) {
            ZipFile zipFile = null;
            try {
                zipFile = new ZipFile(Main.getArchiveFile());
                Enumeration en = zipFile.entries();
                while (en.hasMoreElements()) {
                    ZipEntry zipEntry = (ZipEntry) en.nextElement();
                    map.put(zipEntry.getName(), 
                            "<a href=\"http:" + Main.getArchiveFile() + "::" + zipEntry.getName() + "\">"
                            + zipEntry.getName() 
                            + "</a>");
                    if (zipEntry.getName().startsWith("/")) {
                        map.put(zipEntry.getName().substring(1), 
                                "<a href=\"http:" + Main.getArchiveFile() + "::" + zipEntry.getName() + "\">"
                                + zipEntry.getName().substring(1) 
                                + "</a>");
                    }
                }

                entries = new String[map.keySet().size()];
                int i = 0;
                for (String s : map.keySet()) {
                    if (s.length() < minLength) {
                        minLength = s.length();
                    }
                    entries[i] = s;
                    i++;
                }
                
                boolean done = false;
                while (!done) {
                    done = true;
                    for (i=0; i<entries.length-1; i++) {
                        if (entries[i].length() < entries[i+1].length()) {
                            String tmp = entries[i];
                            entries[i] = entries[i+1];
                            entries[i+1] = tmp;
                            done = false;
                        }
                    }
                }
                // System.out.println("INFO: initialized " + entries.length + " entries from archive " + Main.getArchiveFile() + " with minimum length " + minLength);
            } catch (Exception exn) {
                System.out.println("SEVERE: unable to make references to archive " + Main.getArchiveFile() + ": " + exn);
            } finally {
                if (zipFile != null) {
                    try {
                        zipFile.close();
                    } catch (Throwable t) {
                        // ignore
                    }
                }
            }
        }
    }

    private String[] entries;
    private HashMap<String, String> map = new HashMap<String, String>();
    private int minLength = Integer.MAX_VALUE;

    public void println(String s) {
        print(s);
        super.println();
    }

    public void print(String s) {
        init(); 
        if (inPrint || map.size() == 0) {
            super.print(s);
        } else {
            inPrint = true; // avoid recursive invocations
            annotate(s);
            inPrint = false;
        }
    }
    private boolean inPrint = false;

    private void annotate(String s) {
        int pos = s.indexOf(NL);
        while (pos >= 0) {
            String trans = s.substring(0,pos);
            super.println(transform(trans));
            s = s.substring(pos+1);
            pos = s.indexOf(NL);
        }
        super.print(transform(s));
    }


    private String transform(String s) {
        if (s.length() < minLength) {
            return s;
        }
        int pos = 0;
        for (String key : entries) {
            if ( (pos = s.indexOf(key)) >= 0) {
                return s.substring(0, pos) + map.get(key) + s.substring(pos + key.length());
            }
        }
        return s;
    }
}
