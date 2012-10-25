package oracle.util.triage;

import java.io.*;

/** Representation of a build.jtl runtime log segment.
 */

public class DiagnosticWLSEvent extends DiagnosticEvent implements Constants {

    public static DiagnosticEvent newDiagnosticEvent(String s, VirtualFile f, LineNumberReader lnr) {

        if (s.startsWith("####<")) {
            int pos = s.indexOf("> <");
            if (pos > 0) {
                return checkMessageCounts(new DiagnosticWLSEvent(s, System.currentTimeMillis(), f, lnr));
            }
        }
        return null;
    }

    
    DiagnosticWLSEvent(String s, long timestamp, VirtualFile f, LineNumberReader lnr) {
        super("diagnostic", timestamp, timestamp, f, lnr);

        fullText = s;
        int pos = s.indexOf("> <");

        this.timestamp = s.substring(4,pos).trim();
        s = s.substring(pos + "> <".length());

        pos = s.indexOf("> <");
        level = s.substring(0,pos).trim().toUpperCase();   // <Info> or <Error> or <Alert> or <Notice> ...
        s = s.substring(pos + "> <".length());

        pos = s.indexOf("> <");                              // <Server> or <Socket> or <IIOP> or <Security> or <Diagnostics> or <Log Management>- more like the category
        component = s.substring(0,pos).trim();
        s = s.substring(pos + "> <".length());

        pos = s.indexOf("> <");
        String theHost = s.substring(0,pos).trim();        // <stacd13> etc.
        s = s.substring(pos + "> <".length());

        pos = s.indexOf("> <");
        String theComponent = s.substring(0,pos).trim();   // <Adminserver> or <>
        s = s.substring(pos + "> <".length());
        if (!theComponent.equals("")) {
            component = component + ": " + theComponent;
        }

        pos = s.indexOf("> <");
        String theThread = s.substring(0,pos).trim(); // <main> or <[ACTIVE] ...> or <[STANDBY]...> ...
        s = s.substring(pos + "> <".length());

        pos = s.indexOf("> <");
        if (pos >= 0) {
            String ignore1 = s.substring(0,pos).trim(); // <<WLS KERNEL>> or <>
            s = s.substring(pos + "> <".length());
        }

        pos = s.indexOf("> <");
        if (pos >= 0) {
            String ignore2 = s.substring(0,pos).trim(); // <> ?
            s = s.substring(pos + "> <".length());
        }

        pos = s.indexOf("> <");
        if (pos >= 0) {
            String ignore3 = s.substring(0,pos).trim(); // <> ?
            s = s.substring(pos + "> <".length());
        }

        pos = s.indexOf("> <");
        if (pos >= 0) {
            String theTimestamp = s.substring(0,pos).trim(); // <1226014951570>
            long l = 0l;
            try {
                l = Long.parseLong(theTimestamp);
                // set timestamp for this event
                setStartTime(l);
                setEndTime(l);
            }  catch (Exception _exe) {
                // ignore
            }
            s = s.substring(pos + "> <".length());
        }

        pos = s.indexOf("> <");
        if (pos >= 0) {
            error = s.substring(0,pos).trim(); //  <BEA-001135>
            s = s.substring(pos + "> <".length());
        }

        // <Resuming the JDBC service.> or over multiple lines:
        // <Nov 6, 2008 3:42:32 PM oracle.as.jmx.framework.PortableMBeanFactory setJMXFrameworkProviderClass
        // INFO: JMX Portable Framework initialized with platform SPI "class oracle.as.jmx.framework.wls.spi.JMXFrameworkProviderImpl"> 
 
        if (s.endsWith(">")) {
            text = s.substring(0, s.length() - 1);
        } else {
            text = s + "...";
            StringBuffer sb = new StringBuffer();
            try {
                String line = null;
                boolean found = false;
                while ( ((line = lnr.readLine()) != null) && !found) {
                    sb.append(line);
                    sb.append("\n");
                    endLine++;
                    found = line.trim().endsWith(">");
                }
            } catch (IOException ioe) {
                // ignore
            } 
            fullText = fullText + "\n" + sb.toString();
       }
    }

}
