package oracle.util.triage;

import java.util.Date;
import java.util.TimeZone;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.text.ParseException;


import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.FileFilter;
import java.io.FilenameFilter;

public class Util implements Constants {

    private static final boolean DEBUG = false;

    public static String unHTML(String s) {
        if (s != null) {
            for (int i = 0; i < SOURCE.length; i++) {
                int pos;
                while ( (pos = s.indexOf(SOURCE[i])) >= 0 ) {
                    s = s.substring(0, pos) + TARGET[i] + s.substring(pos + SOURCE[i].length());
                }
            }
        }
        return s;
    }

    private static final String[] SOURCE = 
        new String[] { "&lt;", "&gt;", "&amp;", };
    private static final String[] TARGET = 
        new String[] { "<", ">", "&", };


    /** Use this to find a good spot to cut off the beginning of some text. */
    public static String leftTrim(String s) {
        if (CUTOFF_LENGTH < s.length()) {
            s = s.substring(s.length() - CUTOFF_LENGTH, s.length());

            int[] starts = getStarts(s);
            int   pos    = s.length();

            for (int i = 0; i < starts.length; i++) {
                if (0 <= starts[i] && starts[i] < pos) {
                    pos = starts[i];
                }
            }
            if (0 <= pos && pos < s.length()) {
                s =  s.substring(pos + 1).trim();
            }
            s = "... " + s;
        }
        return s;
    }

    /** Instead of a cutoff "in the middle", try to find a good "first point". */
    private static int[] getStarts(String s) {
        return new int[] { s.indexOf("\n"), s.indexOf("\\n"), s.indexOf(" "), s.indexOf(","),
                           s.indexOf("."), s.indexOf(":"), };
    }


    /** Use this to find a good spot to cut off the remainder of some text. */
    public static String rightTrim(String s) {
        if (CUTOFF_LENGTH < s.length()) {
            s = s.substring(0, CUTOFF_LENGTH);

            int[] ends = getEnds(s);
            int   pos  = ends[0];
            boolean stop = STOP_BEFORE[0];

            for (int i = 1; i < ends.length; i++) {
                if (pos < ends[i]) {
                    pos = ends[i];
                    stop = STOP_BEFORE[i]; 
                }
            }
            if (pos > 0) {
                s =  s.substring(0, (stop) ? pos + 1 : pos).trim();
            }
            s = s + " ...";
        }
        return s;
    }

    /** Instead of a cutoff "in the middle", we try to find a good "last point". */
    private static int[] getEnds(String s) {
        return new int[] { s.lastIndexOf("\n"), s.lastIndexOf("\\n"), s.lastIndexOf(" "), s.lastIndexOf(","), 
                           s.lastIndexOf("."), s.lastIndexOf(")"),  };
    }

    /** Should we cut before that point? */
    private static final boolean[] STOP_BEFORE =
           new boolean[] { true, true, true, false, false, false, };


    public static String readRight(File f, int level) {
        return read(f, level, true);
    }

    public static String readLeft(File f, int level) {
        return read(f, level, false);
    }

    private static String read(File f, int level, boolean toRight) {
        String s = readFile(f);
        if (level <= FINE) {
            return "-- " + f.getName() + " - " + (new Date(f.lastModified())) + " - " + f.length() + " --" + NL +
                ( (toRight) ? rightTrim(s) : leftTrim(s)) + NL ;
        } else {
            return "-- " + f + " - " + (new Date(f.lastModified())) + " - " + f.length() + " --" + NL +
                 s + NL +
                 "-- END OF " + f + " --";
        }
    }

    private static String readFile(File f) {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new FileReader(f));
            StringBuffer sb = new StringBuffer((int) f.length());
            String line;
            while ( (line = lnr.readLine()) != null) {
                sb.append(line);
                sb.append(NL);
            }
            lnr.close();
            return sb.toString();
        } catch (Exception e) {
            System.out.println("SEVERE: Error reading file " + f + ": " + e);
            e.printStackTrace();
        }
        if (lnr != null) {
            try {
                lnr.close();
            } catch (Exception e2) {
                // closing resources - ignore
            }
        }
        return "[Unable to read file]";
    }

    
    public static final FilenameFilter SUC_DIF_FILES = new FilenameFilter() {
        public boolean accept(File f, String s) {
            return s.endsWith(SUCC_SUFFIX) || s.endsWith(DIFF_SUFFIX);
        }
    };

    public static final FilenameFilter DATED_FILES = new FilenameFilter() {
        public boolean accept(File f, String s) {
            int pos = s.lastIndexOf("/");
            if (pos >= 0) {
                s = s.substring(pos + 1);
            }
            boolean res = s.endsWith(JTS_SUFFIX)
                || (s.endsWith(XML_SUFFIX)
                    && (s.startsWith("TEST-")
                        || s.startsWith("TESTS-")
                        || (f != null
                            && s.equals(f.getParentFile().getName() + XML_SUFFIX))
                    ))
                || (s.endsWith(".farm.out")
                    // && f != null
                    // && (f.getParentFile().getName() + ".farm.out").equals(s)
                    )

                || s.endsWith(JTL_SUFFIX)
                // || (s.endsWith(".log")
                //    && (isLogFile(s)
                //        || AreaDescriptors.getArea(standardized(s)) != null))
                || s.endsWith(".log")
                || s.endsWith(".err")
                || s.endsWith(".tlg")
                || s.endsWith(SUCC_SUFFIX)
                || s.endsWith(DIFF_SUFFIX)
                || s.startsWith("testlogic")
                || s.startsWith(".testlogic")
                || s.endsWith(".out")
                ;
            return res;
        }
    };

    static boolean isMessageFile(String s) {
        return s != null
               && (s.endsWith(".log")
                   || s.endsWith(".err")
                   || s.endsWith(".html")
                   || s.endsWith(".xml")
                   || s.endsWith(".out"));
    }

    static boolean isLogFile(String s) {
        /*
        for (int i = 0; i < LOG_FILES.length; i++) {
            if (s.equals(LOG_FILES[i])) {
                return true;
            }
         }
        return false;
        */
        return s != null
               && (isMessageFile(s)
                   || s.endsWith(".farm.out"));
    }


    private static int hasTimestampPattern(String s) {
        for (int tries = 0; tries < TIMESTAMP_PATTERNS.length; tries++) {
            boolean match = false;
            char p, ch;
            if (s.length() > TIMESTAMP_PATTERNS[tries].length()) {
                match = true;
                for (int i = 0, j = s.length() - TIMESTAMP_PATTERNS[tries].length();
                    match 
                    && i < TIMESTAMP_PATTERNS[tries].length();
                    i++, j++) {
                    p = TIMESTAMP_PATTERNS[tries].charAt(i);
                    if (j < 0) {
                        match = false;
                    } else {
                        ch = s.charAt(j);
                        if ( (p == ch)
                            || (p == 'N' && '0' <= ch && ch <= '9')
                            || (p == 'A' && ch == 'P')
                            || (p == 'P' && ch == 'A')) {
                          // match!
                        } else {
                            match = false;
                        }
                    }
                }
            }
            if (DEBUG) { System.out.println("hasTimestampPattern(\"" + s + "\")=" + match); }
            if (match) {
                return tries;
            }
        }
        return -1;
    }

    private static DateFormat[] timestampFormats = new DateFormat[TIMESTAMP_FORMATS.length];
    static {
        for (int i = 0; i < timestampFormats.length; i++) {
            timestampFormats[i] = new  SimpleDateFormat(TIMESTAMP_FORMATS[i]);
            timestampFormats[i].setTimeZone(TimeZone.getTimeZone(FARM_TIMEZONE));
        }
    };

    public static String standardized(String fileName) {
        int match = hasTimestampPattern(fileName);
        return (match >= 0)
               ? fileName.substring(0, fileName.length() - TIMESTAMP_PATTERNS[match].length())
                 + TIMESTAMP_PATTERNS[match]
               : fileName;
    }

    public static Date getTimestamp(String fileName) {
        int match = hasTimestampPattern(fileName);
        if (match >= 0) {
            int startIndex = fileName.length() - TIMESTAMP_PATTERNS[match].length();
            String date = fileName.substring(startIndex, startIndex + TIMESTAMP_FORMATS[match].length());
            Date d = parseTime(date, timestampFormats[match]); 
            if (DEBUG) {
                System.out.println("TIMESTAMP from file " + fileName + " with date " + date + " is: " + toTimestamp(d.getTime()));
            }
            return d;
        } else {
            return null;
        }
    }

    static {
        TIMESTAMP_FORMAT_1.setTimeZone(TimeZone.getTimeZone(FARM_TIMEZONE));
    };
    public static String toTimestamp(long l) {
        return TIMESTAMP_FORMAT_1.format(new Date(l));
    }

    static {
        TIMESTAMP_FORMAT_2.setTimeZone(TimeZone.getTimeZone(FARM_TIMEZONE));
    };
    public static String toTimestampFine(long l) {
        return TIMESTAMP_FORMAT_2.format(new Date(l));
    }


    static {
        DATE_FORMAT_10.setTimeZone(TimeZone.getTimeZone("GMT"));
    };
    public static String toDateTime(long l) {
        return DATE_FORMAT_10.format(new Date(l));
    }


    public static final FileFilter DIR_FILES = new FileFilter() {
        public boolean accept(File f) {
            return f.isDirectory();
        }
    };

    public static Date parseTime1(String s) {
        return parseTime(s, DATE_FORMAT_1);
    }

    public static Date parseTime2(String s) {
        return parseTime(s, DATE_FORMAT_2);
    }

    public static Date parseTime3(String s) {
        return parseTime(s, DATE_FORMAT_3);
    }

    private static Date parseTime4(String s) {
        return parseTime(s, DATE_FORMAT_4);
    }

    public static Date parseTime5(String s) {
        return parseTime(s, DATE_FORMAT_5);
    }

    public static Date parseTime6(String s) {
        return parseTime(s, DATE_FORMAT_6);
    }

    public static Date parseTime7(String s) {
        return parseTime(s, DATE_FORMAT_7);
    }

    public static Date parseTime8(String s) {
        return parseTime(s, DATE_FORMAT_8);
    }

    public static Date parseTime9(String s) {
        return parseTime(s, DATE_FORMAT_9);
    }

    private static Date parseTime11(String s) {
        return parseTime(s, DATE_FORMAT_11);
    }

    public static Date parseShortTime11(String s) {
        int year = (new Date()).getYear();
        return parseTime(s + " " + year + " " + FARM_TIMEZONE, DATE_FORMAT_11);
    }

    public static Date parseTime12(String s) {
        return parseTime(s, DATE_FORMAT_12);
    }

    private static Date parseTime(String s, DateFormat df) {
        try {
            df.setLenient(true);
            return df.parse(s);
        } catch (ParseException pe) {
            System.out.println("ERROR: unable to parse date from: " + s);
            pe.printStackTrace();
            return new Date(0L);
        }
    }         


    private static final int   SECONDS_PER_MINUTE_INT = 60;
    private static final int   MINUTES_PER_HOUR_INT = 60;
    private static final int   SECONDS_PER_HOUR_INT = SECONDS_PER_MINUTE_INT * MINUTES_PER_HOUR_INT;
    private static final float SECONDS_PER_MINUTE_FLOAT = 1.0f * SECONDS_PER_MINUTE_INT;
    private static final float ROUND_OFF_TO_UNIT_FLOAT = 0.5f;
    private static final float ROUND_OFF_TO_FRACTION_FLOAT = 0.05f;
    private static final int   SECONDS_TO_MINUTE_DISPLAY_THRESHOLD = 600; // seconds
    private static final int   SECONDS_DISPLAY_CUTOFF = 10;               // minutes
    private static final int   ROUND_OFF_TO_MINUTE_INT = 30; // seconds
    private static final float SECONDS_TO_MILLISECONDS_FLOAT = 1000.0f;
 
    public static String seconds(float sec) {
        if (sec >= SECONDS_PER_MINUTE_FLOAT) {
            sec += ROUND_OFF_TO_UNIT_FLOAT;
            return Integer.toString((int) sec) + " secs";
        }
        String res = (sec + ROUND_OFF_TO_FRACTION_FLOAT) + "0";
        int pos = res.indexOf(".");
        if (pos >= 0) {
            res = res.substring(0, pos + 2);
            if (res.endsWith(".0")) {
                res = res.substring(0, res.length() - 2);
            }
        }
        return res + " secs";
    }

    public static String secs(float sec) {
        if (sec >= SECONDS_PER_MINUTE_FLOAT) {
            int seconds = (int) (sec + ROUND_OFF_TO_UNIT_FLOAT);
            if (seconds >= SECONDS_TO_MINUTE_DISPLAY_THRESHOLD) {
                seconds += ROUND_OFF_TO_MINUTE_INT;
            } 
            int hours = seconds / SECONDS_PER_HOUR_INT;
            seconds -= hours * SECONDS_PER_HOUR_INT;
            int minutes = seconds / SECONDS_PER_MINUTE_INT;
            seconds -= minutes * SECONDS_PER_MINUTE_INT;
            if (hours >= 1) {
                return hours + "h" + minutes + "m";
            } else if (minutes >= SECONDS_DISPLAY_CUTOFF) {
                return minutes + "m";
            } else if (minutes >= 1) {
                return (seconds == 0) ? minutes + "m"
                                    : minutes + "m" + seconds + "s";
            } else {
                return seconds + "s";
            }
        } else {
            String res = (sec + ROUND_OFF_TO_FRACTION_FLOAT) + "0";
            int pos = res.indexOf(".");
            if (pos >= 0) {
                res = res.substring(0, pos + 2);
                if (res.endsWith(".0")) {
                    res = res.substring(0, res.length() - 2);
                }
            }
            return res + "s";
        }
    }

    public static long millis(float f) {
        return (long) (f * SECONDS_TO_MILLISECONDS_FLOAT);
    }


    public static String chop(String s, String prefix, String endTag) {
        if (s.startsWith(prefix)) {
            s = s.substring(prefix.length());
            int pos = s.indexOf(endTag);
            return (pos < 0) ? null
                           : s.substring(0, pos);
        } else {
            return null;
        }
    }

    public static boolean isImportantEvent(String s) {
        for (int i = 0; i < IMPORTANT_EVENTS.length; i++) {
            if (s.indexOf(IMPORTANT_EVENTS[i]) >= 0) {
               return true;
            }
        }
        return false;
    }

    public static boolean isImportantWLSEvent(String s) {
        for (int i = 0; i < IMPORTANT_EVENTS.length; i++) {
            if (s.indexOf(IMPORTANT_WLS_EVENTS[i]) >= 0) {
               return true;
            }
        }
        return false;
    }


    public static Date parseDiagnosticTimestamp(String s) {
        int pos = s.indexOf("T");
        if (pos > 0) {
            s = s.substring(0,pos) + " " + s.substring(pos + 1);
        }

        pos = s.indexOf("+");
        if (pos < 0) {
            pos = s.lastIndexOf("-");
        }
        if (pos > 0) {
            s = s.substring(0,pos) + " GMT" + s.substring(pos);
        }
        return parseTime9(s);
    }

    public static String javaScriptCleanUp(String s, int lineWidth, int maxSize) {
       StringBuffer sb = new StringBuffer();
       int charCount = 0; int i;
       for (i = 0; i < s.length() && i < maxSize; i++) {
           charCount++;
           char ch = s.charAt(i);
           if (ch == '\'') {
               sb.append("&#39;");
           } else if (ch == '"') {
               sb.append("&quot;");
           } else if (ch == ' ') {
               sb.append("&nbsp;");
           } else if (ch == '&') {
               sb.append("&amp;");
           } else if (ch == '<') {
               // sb.append("&lt;");
               sb.append("{");
           } else if (ch == '>') {
               // sb.append("&gt;");
               sb.append("}");
           } else if (ch == '(') {
               sb.append("&#40;");
           } else if (ch == ')') {
               sb.append("&#41;");
           } else if (ch == '\\') {
               if (i + 1 < s.length() && s.charAt(i + 1) == 'n') {
                   i++;
                   sb.append("<br>");
                   charCount = 0; 
               } else {
                   sb.append("&#92;");
               }
           } else if (ch == '\n') {
               sb.append("<br>");
               charCount = 0; 
           } else {
               sb.append(ch);
           }
           if (charCount == lineWidth) {
               sb.append("<br>");
               charCount = 0;
           }
       }
       if (i > maxSize) { 
           sb.append("...");
       }
       return sb.toString();
    }

    public static String escapeHtml(String s) {
       StringBuffer sb = new StringBuffer();
       int charCount = 0; int i;
       for (i = 0; i < s.length() && i < s.length(); i++) {
           charCount++;
           char ch = s.charAt(i);
           if (ch == ' ') {
               sb.append("&nbsp;");
           } else if (ch == '&') {
               sb.append("&amp;");
           } else if (ch == '<') {
               sb.append("&lt;");
           } else if (ch == '>') {
               sb.append("&gt;");
           } else if (ch == '\n') {
               sb.append("<br>\n");
               charCount = 0; 
           } else {
               sb.append(ch);
           }
       }
       return sb.toString();
    }

    public static String makeIcon(int icon) {
        return "<img src=\"" + HTML_RESOURCES[icon] + "\">";
    }

    private static String makeIcon(int icon, int multiplicity) {
        String single = makeIcon(icon);
        StringBuffer res = new StringBuffer();
        for (int i = 0; i < multiplicity; i++) {
             res.append(single);
        }
        return res.toString();
    }

    public static String makeLevelIcon(int level) {
        if (level > INFO) {
            return "";
        } else if (level == INFO) {
            return "";
        } else if (level == WARNING) {
            return makeIcon(TRIANGLE_ICON);
        } else if (level == SEVERE) {
            return makeIcon(RED_X_ICON);
        } else { // (level < SEVERE) 
            return makeIcon(RED_X_ICON, 3);
        }
    }

    // public static String makeHint(String text, String alternate,  int icon, int multiplicity) {
    //     return (text.length() > MAX_HINT_CHARACTERS) 
    //            ?  makeHint(alternate, icon, multiplicity)
    //            : makeHint(text, icon, multiplicity);
    // }

    public static String makeHint(String text, String alternate,  int icon) {
        return (text.length() > MAX_HINT_CHARACTERS) 
               ?  makeHint(alternate, icon)
               : makeHint(text,icon);
    }

    public static String makeHint(String text,  int icon, int multiplicity) {
        return "<a href=\"#\" class=\"hintanchor\" onMouseover=\"showhint('"
                 + javaScriptCleanUp(text,
                                     MAX_HINT_CHAR_PER_LINE,
                                     MAX_HINT_CHARACTERS) 
                 + "', this, event, '" + MAX_HINT_PIXEL_PER_LINE + "px')\">" 
                 + makeIcon(icon, multiplicity) + "</a>";
    }

    public static String makeHint(String text,  int icon) {
        return "<a href=\"#\" class=\"hintanchor\" onMouseover=\"showhint('"
                 + javaScriptCleanUp(text,
                                     MAX_HINT_CHAR_PER_LINE,
                                     MAX_HINT_CHARACTERS) 
                 + "', this, event, '" + MAX_HINT_PIXEL_PER_LINE + "px')\">" 
                 + makeIcon(icon) + "</a>";
    }

    public static String makeColor(int color, String text) {
        return "<font color=\"#" + COLORS[color] + "\">" + text + "</font>";
    }

    public static String makeLevelColor(String text, int level) {
        if (level >= INFO) {
            return text;
        } else if (level == WARNING) {
            return "<b>" +  text + "</b>";
        } else { // (level <= SEVERE
            return "<b>" + makeColor(RED_COLOR,  text) + "</b>";
        }
    }

    public static String makeLink(String url, String text) {
        return makeTargetLink(url, text, null);
    }

    public static String makeDetailLink(String url, String text) {
        return makeTargetLink(url, text, "detail");
    }

    public static String makeTargetLink(String url, String text, String target) {
        return "<a href=\"http:" + url + "\"" +
               ((target == null) ? ""
                               : " target=\"" + target + "\"") + ">" + text + "</a>";
    }

    public static String makeRangeLink(String url, String text, int start, int end) {
        String range = "";
        if (start > 0) {
           range = "?rg="
                   + start
                   + ((end < 0) 
                       ? "-"
                       : ((start == end)
                           ? ""
                           : "-" + end));
        }
        return makeDetailLink(url + range, text)
               + ((range.equals(""))
                   ? ""
                   : makeDetailLink(url, makeIcon(ZOOM_OUT_ICON)));
    }

    public static String makeJobLink(int jobId) {
        return makeTargetLink(FARM_JOB_PREFIX + jobId, "" + jobId, "tree");
    }

    public static String makeLabelLink(String label) {
        String url = label;
        if (url.indexOf("_") < 0) {
            url = DEFAULT_LABEL_PREFIX + "_" + url;
        }
        url = LABEL_BROWSER_PREFIX + url.replaceAll("_","/") + "/" + LABEL_BROWSER_INDEXFILE;
 
        return makeTargetLink(url, label, "tree");
    }

    public static String makeDteLink(int dteId) {
        return "" + dteId;
    }


    public static String makeRelativeUrl(String url) {
        if (url == null || url.indexOf(":") >= 0) {
            return url;
        }
        int pos = url.lastIndexOf("/");
        return (pos >= 0) 
               ? url.substring(pos + 1)
               : url;
    }



    private static final long KILO_FACTOR = 1024l;
    private static final long KILOBYTE_THRESHOLD = 2 * KILO_FACTOR;
    private static final long MEGABYTE_THRESHOLD = 2 * (KILO_FACTOR * KILO_FACTOR);
    private static final long GIGABYTE_THRESHOLD = 2 * (KILO_FACTOR * KILO_FACTOR * KILO_FACTOR);

    public static String makeFileSize(long l) {
        if (l < KILOBYTE_THRESHOLD) {
            return "" + l + "B";
        } else if (l < MEGABYTE_THRESHOLD) {
            return "" + (l / KILO_FACTOR) + "KB";
        } else if (l < GIGABYTE_THRESHOLD) {
            return "" + (l / KILO_FACTOR / KILO_FACTOR) + "MB";
        } else {
            return "" + (l / KILO_FACTOR / KILO_FACTOR / KILO_FACTOR) + "GB";
        }
    }

    public static boolean isNumeric(String s) {
        if (s == null || s.equals("")) return false;
        for (int i = 0; i < s.length(); i++) {
             char ch = s.charAt(i);
             if (ch < '0' || ch > '9') {
                 return false;
             }
        }
        return true;
    }


    public static String makeHtml(boolean escape, String tag, String s) {
        tag = (tag == null) ? "" : tag;
        int pos = tag.indexOf(",");
        if (pos > 0) {
           s = makeHtml(escape, tag.substring(pos + 1), s);
           tag = tag.substring(0,pos);
        } else {
           if (escape) {
               s = escapeHtml(s);
           }
        }
        tag = tag.trim();
        return (tag.equals("")) ? s : ("<" + tag + ">\n" + s + "</" + tag + ">\n");
    }


    public static void resetNumber() {
        num = 0;
    }
    private static int num = 0;

    public static String makeLi(String s) {
        if (s != null && !s.trim().equals("")) {
            int number = num;
            num++;
            return "<li id=\"n" + number + "\">" + s + "</li>" + NL;
        } else {
            return "";
        }
    }
    public static String makeUl(String s) {
        if (s != null && !s.trim().equals("")) {
            return "<ul>" + NL + s + NL + "</ul>" + NL;
        } else {
            return "";
        }
    }
}
