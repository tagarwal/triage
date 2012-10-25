package oracle.util.triage;

import java.util.Date;
import java.util.HashMap;
import java.io.*;

/** Representation of a build.jtl runtime log segment.
 */

public class DiagnosticEvent extends DatedFile implements Constants {
    protected static final int TEXT_MAX_LENGTH = 128;

    /* this message ID indicates that a critical error  was detected and
       an associated incident created. This should be flagged as a fatal
       error.
     */
    private static final String INCIDENT_CREATED_MESSAGE = "DFW-40104";

    public static DiagnosticEvent newDiagnosticEvent(String s, VirtualFile f, LineNumberReader lnr) {

        if (s.startsWith("[20")) {
            int pos = s.indexOf("] [");
            if (pos > 0) {
                String timestamp = s.substring(1,pos).trim();
                Date d = Util.parseDiagnosticTimestamp(timestamp);
                if (d != null) {
                    return checkMessageCounts(new DiagnosticEvent(s, d.getTime(), f, lnr));
                    
                }
            }
        }
        return null;
    }

    protected static DiagnosticEvent checkMessageCounts(DiagnosticEvent de) {
        int count = ((messageCounts.get(de.getName()) == null) 
                      ? 1
                      : messageCounts.get(de.getName()).intValue()) + 1;
        messageCounts.put(de.getName(), new Integer(count));

        if (count > eventsMaxCount) {
            return null;
        } else if (count == eventsMaxCount) {
            System.out.println("WARNING: will suppress further diagnostic events \"" + de.getName() + "\"");
            de.suppressed = true;
        }
        return de;
    }


    public static void setSuppressEventsCount(int maxCount) {
        eventsMaxCount = maxCount;
    }
    private static int eventsMaxCount = SUPPRESS_EVENTS_COUNT;

    private static HashMap<String,Integer> messageCounts = new HashMap<String, Integer>();

    public static void resetStaticVariables() {
        // eventsMaxCount = SUPPRESS_EVENTS_COUNT;
        messageCounts = new HashMap<String, Integer>();
    }

    
    protected DiagnosticEvent(String s, long ts1, long ts2, VirtualFile f, LineNumberReader lnr) {
        super(s, ts1, ts2);
        startLine = lnr.getLineNumber();
        endLine = startLine;
        url = f.getUrl();
    }

    DiagnosticEvent(String s, long timestamp, VirtualFile f, LineNumberReader lnr) {
        this("diagnostic", timestamp, timestamp, f, lnr);

    try {
        fullText = s;
        int pos = s.indexOf("] [");

        this.timestamp = s.substring(1,pos).trim();
        s = s.substring(pos + "] [".length());

        pos = s.indexOf("] [");
        component = s.substring(0,pos).trim();
        s = s.substring(pos + "] [".length());

        pos = s.indexOf("] [");
        level = s.substring(0,pos).trim();
        s = s.substring(pos + "] [".length());

        pos = s.indexOf("] [");
        error = s.substring(0,pos).trim();
        s = s.substring(pos + "] [".length());

        pos = s.indexOf("]");
        subcomponent = s.substring(0,pos);
        s = s.substring(pos + "]".length());

        if (!s.endsWith("]")) {
            pos = s.lastIndexOf("] ");
            if (pos >= 0) {
                text = s.substring(pos + "] ".length()).trim();
            } else {
                text = s;
            }
        }

        if (s.endsWith("[[")) {
            StringBuffer sb = new StringBuffer();
            try {
                String line = null;
                while ( ((line = lnr.readLine()) != null) && !line.startsWith("]]") ) {
                    sb.append(line);
                    sb.append("\n");
                    endLine++;
                }
            } catch (IOException ioe) {
                // ignore
            } 
            fullText = fullText + "\n" + sb.toString();
       }
     } catch (Exception e) {
          System.out.print("SEVERE: DiagnosticEvent(\"" + s + "\"," + this.timestamp + ", VirtualFile(" + f.getFullName() + "), <lnr>): ");
          System.out.print("component=" + component + ", ");
          System.out.print("level=" + level + ", ");
          System.out.print("error=" + error + ", ");
          System.out.println("subcomponent= " + subcomponent + ". Exception: " + e);
          e.printStackTrace();
     }
    }

    protected boolean isError() {
       return level != null && level.indexOf("ERROR") >= 0;
    }

    public int getLevel() {
        if (level == null) {
            return INFO;
        } else if (level.indexOf("WARNING") >= 0) {
            return WARNING;
        } else if (level.indexOf("FATAL") >= 0 || 
                   level.indexOf("INCIDENT") >= 0 ||
                   (isIncidentCreatedMessage())) {
            
            return FATAL;
        } else if (level.indexOf("ERROR") >= 0 || level.indexOf("SEVERE") >= 0) {
            return SEVERE;
        } else {
            return INFO;
        }
    }

    /*
     * Returns true if this is an "incident created" message, otherwise
     * false is returned.
     */
    public boolean isIncidentCreatedMessage()
    {
      return (error != null && error.equals(INCIDENT_CREATED_MESSAGE));
    }

    public String getShortText() {
        // int pos = fullText.indexOf("\n");
        // return (pos > 0) ? fullText.substring(0,pos) : fullText;
        StringBuffer sb = new StringBuffer();
        toString(sb);
        return sb.toString();
    }

    public String getDetail() {
        int pos = fullText.indexOf("\n");
        return (pos > 0) ? fullText.substring(pos + 1) : "";
        /* if (fullText.length() <= getName().length()) {
              return null;
           } else {
              return fullText;
           } */
    }

    public int getStartLine() {
        return startLine;
    }
    public int getEndLine() {
        return endLine;
    }
  
    protected String timestamp;
    protected String component;
    protected String level;
    protected String error;
    protected String subcomponent;
    protected String text;
    protected String detail;
    protected String fullText;
    protected String url;
    protected int startLine;
    protected int endLine;
    protected boolean suppressed = false;

    public String getName() {
        if (nam == null) {
           StringBuffer sb = new StringBuffer();
           toString(sb);
           nam = sb.toString();
           if (nam.length() > 80) {
               nam = nam.substring(0,77) + "...";
           }
        }
        if (suppressed) {
           return "FURTHER " +
                  (messageCounts.get(nam).intValue() - eventsMaxCount) + 
                  " MESSAGES SUPPRESSED: " + nam;
        } else {
           return nam;
        }
    }
    private String nam;

    public String getFullText() {
        return fullText;
    }

    public String getUrl() {
        int icon = isError() ? ERRMSG_ICON : TRIANGLE_ICON;
        String range = Util.makeRangeLink(url, getName(), startLine, endLine);
        if (suppressed) {
           return Util.makeHint(getFullText(), icon, 3) + range;
        } else {
           return Util.makeHint(getFullText(), icon) + range;
        }
    }
    
    private void toString(StringBuffer sb) {
         if (component != null && !component.equals("")) {
             sb.append(component);
             sb.append(" ");
         }
         if (level != null && !level.equals("")) {
             sb.append(level);
             sb.append(": ");
         }
         if (error != null && !error.equals("")) {
             sb.append(error);
             sb.append(": ");
         }
         if (subcomponent != null && !subcomponent.equals("")) {
             sb.append("[");
             sb.append(subcomponent);
             sb.append("] ");
         }
         if (text != null && !text.equals("")) {
             sb.append(text);
         }
    }
}
