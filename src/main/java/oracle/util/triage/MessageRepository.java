package oracle.util.triage;
                        

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.io.*;

/** Representation of the set of all messages
 */

public class MessageRepository implements Constants {

    private static final boolean DEBUG = false;

    /*
     * If the incidentFile is not null any messages found for 
     * "incident created" messages will link to the incidentFile.
     */
    public MessageRepository(VirtualDir vd, String incidentFile) {
        entries = new ArrayList<MessageEntry>(); 
        LineNumberReader lnr = null;
        try {
            Iterator<VirtualFile> iter = vd.getFiles(LOG_ANALYSIS_FILES);
            if (DEBUG) {
                System.out.println("Reading from virtual dir: " + vd.getName());
            }

            while (iter.hasNext()) {
                VirtualFile vf = iter.next();
                if (DEBUG) {
                    System.out.println("Reading from virtual file: " + vf.getFullName());
                }

                try {
                    lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(vf.getInputStream())));
                    String line = null;
                    while ( (line = lnr.readLine()) != null) {
                        String trim = line.trim();
                        if ( (trim.startsWith("[20")
                              && trim.indexOf(":00] [") > 0) 
                             || (trim.startsWith("####<")
                                 && trim.indexOf("> <") > 0)) {
                            Date d = null;
                            if (trim.startsWith("[20")) {
                               int pos = trim.indexOf("] [");
                               String timestamp = trim.substring(1,pos).trim();  
                               d = Util.parseDiagnosticTimestamp(trim.substring(1,pos).trim());
                            } else {
                               d = new Date(); // just used for dummy
                            }
                            if (d != null) {
                                DiagnosticEvent de = trim.startsWith("[20")
                                                     ? new DiagnosticEvent(trim, d.getTime(), vf, lnr)
                                                     : new DiagnosticWLSEvent(trim, d.getTime(), vf, lnr);
                                MessageEntry me = new MessageEntry(de, vf, incidentFile);
                                if (DEBUG) {
                                    System.out.println("Inserting ME: " + me);
                                }
                                insertMessageEntry(me);
                                if (DEBUG) {
                                    System.out.println(" -> ME inserted.");
                                }
                            }
                        }
                    }
                } catch (IOException ioe) {
                    System.out.println("SEVERE: issue reading message entries from log file " + vf.getFullName() + ": " + ioe);
                } finally {
                    if (lnr != null) {
                        try {
                            lnr.close();
                        } catch (IOException ioe) {
                          // ignore
                        }
                    }
                }
            }
        } catch (IOException ioe) {
            System.out.println("SEVERE: issue reading message entries from directory " + vd.getName() + ": " + ioe);
        }
    }

    private void insertMessageEntry(MessageEntry me) {
        if (entries.size() == 0) {
            entries.add(me);
        } else {
            long insertTime = me.getTime();
            long halfTime = 0l;

            int first = 0; 
            int last = entries.size() - 1;
            int half = (first + last) / 2;
            while (first < last) {
                if (DEBUG) { System.out.println("first=" + first + "  half=" + half + "  last=" + last); }
                halfTime = entries.get(half).getTime();
                if (insertTime < halfTime) {
                    last = half - 1;
                } else if (insertTime > halfTime) {
                    first = half + 1;
                } else { // (insertTime==halfTime)
                    me.setNextEntry(entries.get(half));
                    entries.set(half, me);
                    return;
                }
                half = (first + last) / 2;
            }
            halfTime = entries.get(first).getTime();
            if (insertTime < halfTime) {
                entries.add(first, me);
            } else if (insertTime > halfTime) {
                entries.add(first + 1, me);
            } else { // (insertTime==halfTime)
                me.setNextEntry(entries.get(first));
                entries.set(first, me);
            }
            return;
        }
    }

    private List<MessageEntry> entries;

    public static class MessageEntry {
        private MessageEntry() {
            this.nextEntry = null;
        }

        MessageEntry(long time, String location, int startLine, int endLine,
                     int level, String text, String detail,
                     boolean incidentCreatedMessage, String incidentFile) {
            this();
            this.time = time;
            this.level = level;
            this.location = location;
            this.startLine = startLine;
            this.endLine = (endLine <= 0) ? startLine : endLine;
            this.text = text;
            this.detail = detail;
            this.incidentCreatedMessage = incidentCreatedMessage;
            this.incidentFile = incidentFile;
        }

        MessageEntry(DiagnosticEvent de, VirtualFile vf, String incidentFile) {
            this(de.getStartTime(), vf.getUrl(), de.getStartLine(), de.getEndLine(),
                 de.getLevel(), de.getShortText(), de.getDetail(), 
                 de.isIncidentCreatedMessage(), incidentFile);
        }

        public void setNextEntry(MessageEntry me) {
            nextEntry = me;
        }
        public MessageEntry getNextEntry() {
            return nextEntry;
        }
        private MessageEntry nextEntry;

        public long getTime() {
            return time;
        }
        private long time;

        public String getLocation() {
            return location;
        }
        private String location;

        public int getLevel() {
            return level;
        }
        private int level;

        public int getStartLine() {
            return startLine;
        }
        private int startLine;

        public int getEndLine() {
            return endLine;
        }
        private int endLine;
        
        public String getShortText() {
            return text;
        }
        private String text;
        
        public void setDetail(String detail) {
            this.detail = detail;
        }
        public String getDetail() {
            return detail;
        }
        private String detail;

        public void setIncidentCreatedMessage(boolean incidentCreatedMessage) {
            this.incidentCreatedMessage = incidentCreatedMessage;
        }
        public boolean isIncidentCreatedMessage() {
            return incidentCreatedMessage;
        }
        private boolean incidentCreatedMessage;

        public void setIncidentFile(String incidentFile) {
            this.incidentFile = incidentFile;
        }
        public String getIncidentFile() {
            return incidentFile;
        }
        private String incidentFile;

        public String toHtml() {
            StringBuffer sb = new StringBuffer();
            toHtml(sb);
            return sb.toString();
        }

        public String toString() {
            return (new Date(getTime())).toString() + ": " + getShortText();
        }

        public void toHtml(StringBuffer sb) {
            sb.append(Util.makeColor(GRAY_COLOR, Util.toTimestampFine(getTime()).toString() + ": "));
            sb.append(Util.makeLevelIcon(getLevel()));
            sb.append(Util.makeLevelColor(getShortText(), getLevel()));
            if (getLocation() != null) {
               String loc = getLocation();
               int pos = loc.lastIndexOf("/");
               if (pos >= 0) {
                   loc = loc.substring(pos + 1);
               }
               sb.append(Util.makeRangeLink(getLocation(), " " + loc, getStartLine(), getEndLine()));
            }
            if (getIncidentFile() != null && isIncidentCreatedMessage()) {
              sb.append(Util.makeDetailLink(getIncidentFile(),
                                             Util.makeIcon(WARNING_ICON)));
                                            
            }

            sb.append("<br>\n");
            if (getDetail() != null && !getDetail().equals("")) {
               if (WARNING <= getLevel()) {
                   sb.append("<small>");
               }
               sb.append("<code>");
               sb.append(Util.escapeHtml(getDetail()));
               sb.append("</code>");
               if (WARNING <= getLevel()) {
                   sb.append("</small>");
               }
               sb.append("<br>\n");
            }

            if (getNextEntry() != null) {
                getNextEntry().toHtml(sb);
            }
        }
    }

    public String toHtml() {
        StringBuffer sb = new StringBuffer();
        sb.append("<html><head><title>Messages in chronological order</title><head>"); sb.append(NL);
        sb.append(HEADER_INCLUDES);
        sb.append("<head>"); sb.append(NL);
        sb.append("<body><h2>Messages in Chronological Order</h2>"); sb.append(NL);
  
        int spanCount = 0;
        for (MessageEntry me : entries) {
            sb.append("<span id=\"" + spanCount + "\">");
            spanCount++;
            me.toHtml(sb);
            sb.append("</span>");
        }
        sb.append("</body></html>"); sb.append(NL);
        return sb.toString();
    }

    public long[] getSpanVector() {
        long[] ary = new long[entries.size()];
        int i = 0;
        for (MessageEntry me : entries) {
             ary[i] = me.getTime();
             i++;
        }
        return ary;
    }

    public static String lookupSpan(long[] span, long timeStamp) {
        int first = 0;
        int last = span.length - 0;
        int half = (first + last) / 2;

        while (first < last) {
            if (timeStamp < span[half]) {
                last = half;
            } else if (span[half] < timeStamp) {
                first = half;
            } else { // (span[half] == timeStamp) 
                return "" + half;
            }
            half = (first + last) / 2;
        }

        if (timeStamp < span[first]) {
            return "" + first;
        }
        return "" + last;
    }


    static void createMessageFile(String location, 
                                  String target,
                                  String incidentFile) {
        PrintStream ps = null;
        try {
            MessageRepository mr = new MessageRepository(VirtualDir.create(new File(location)), incidentFile);
            if (DEBUG) System.out.println("INFO: generating html output for file browser");
            ps = new PrintStream(new FileOutputStream(new File(target)));
            ps.println(mr.toHtml());
        } catch (IOException ioe) {
            System.out.println("SEVERE: problem creating message repository for " + location + " in " + target + ": " + ioe);
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        for (String s : args) {
            MessageRepository fb = new MessageRepository(VirtualDir.create(new File(s)), null);
            System.out.println(fb.toHtml());
        }
    }

}
