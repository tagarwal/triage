package oracle.util.triage;

import java.io.*;
import java.util.regex.*;
import java.util.*;
import java.net.*;

public class DeltaDiff {


   public static final int LONGEST_LINE_LENGTH = 1024 * 32;  // suppress lines longer than 32k

   private static boolean DEBUG   = false;
   private static boolean VERBOSE = false;


   private static final String SUPPRESSED = "";          // Content of lines that is suppressed.


   private static final String FONT_START_RED = "<FONT color=#FF0000>";
   private static final String FONT_END       = "</FONT>";


   static int contextLength = 3;  // How many context lines to show before and after actual deltas

   private static int numDigits = 6;  // How many digits to show in a line number.


   private static final String[][] INFOS = new String[][] {
       new String[]{ " VIEW_LOCATION ",        "${VIEW_LOCATION}" },
       new String[]{ " VIEW_LABEL ",           "${VIEW_LABEL}" },
       new String[]{ " VIEW_NAME ",            "${VIEW_NAME}" },
       new String[]{ "=\t\tTransaction ",      "${TRANSACTION_NAME}" },
       new String[]{ " HOST_NAME ",            "${HOST_NAME}" },
       new String[]{ " [exec] jobReqID=",      "${JOB_REQ_ID}" },
       new String[]{ " [echo] DB_SID  =",      "${DB_SID}" },
       new String[]{ " <{(SHORT_HOST_NAME)}>", "${HOST_NAME}" },     // Pseudo entry. Leave this last.
       new String[]{ " <{(UC_HOST_NAME)}>",    "${HOST_NAME}" },     // Pseudo entry. Leave this last.
       new String[]{ " <{(FARM_JOBID)}>",      "${FARM_JOBID}" },    // Pseudo entry. Leave this last.
       new String[]{ " <{(LABEL_DATE)}>",      "${LABEL_DATE}" },    // Pseudo entry. Leave this last.
       new String[]{ " <{(OECPERF_SID)}>",     "${OECPERF_SID}" },   // Pseudo entry. Leave this last.
       new String[]{ " <{(ORA_SID)}>",         "${ORA_SID}" },       // Pseudo entry. Leave this last.
       new String[]{ " <{(RCU_DB_SID)}>",      "${RCU_DB_SID}" },    // Pseudo entry. Leave this last.
       new String[]{ " <{(LDAPOD_VIEW)}>",     "${LDAPOID_VIEW}" },  // Pseudo entry. Leave this last.
       new String[]{ " <{(USER_NAME)}>",       "${USER_NAME}" },     // Pseudo entry. Leave this last. Must be after LDAPOID_VIEW, etc.
   };
   private static final int VIEW_LOCATION_INDEX   = 0;
   private static final int VIEW_LABEL_INDEX      = VIEW_LOCATION_INDEX + 1;
   private static final int VIEW_NAME_INDEX       = VIEW_LABEL_INDEX + 1;
   private static final int TRANSACTION_NAME_INDEX = VIEW_NAME_INDEX + 1;
   private static final int HOST_NAME_INDEX       = TRANSACTION_NAME_INDEX + 1;
   private static final int JOB_REQ_ID_INDEX      = HOST_NAME_INDEX + 1;
   private static final int DB_SID_INDEX          = JOB_REQ_ID_INDEX + 1;

   private static final int START_PSEUDO_INDEX    = DB_SID_INDEX + 1;

   private static final int SHORT_HOST_NAME_INDEX = START_PSEUDO_INDEX;
   private static final int UC_HOST_NAME_INDEX    = SHORT_HOST_NAME_INDEX + 1;
   private static final int FARM_JOBID_INDEX      = UC_HOST_NAME_INDEX + 1;
   private static final int LABEL_DATE_INDEX      = FARM_JOBID_INDEX + 1;
   private static final int OECPERF_SID_INDEX     = LABEL_DATE_INDEX + 1;
   private static final int ORA_SID_INDEX         = OECPERF_SID_INDEX + 1;
   private static final int RCU_DB_SID_INDEX      = ORA_SID_INDEX + 1;
   private static final int LDAPOID_VIEW_INDEX    = RCU_DB_SID_INDEX + 1;
   private static final int USER_NAME_INDEX       = LDAPOID_VIEW_INDEX + 1;

   private static final String[] INFOS_PREFIX = new String[INFOS.length];
   static {
        for (int i=0; i<START_PSEUDO_INDEX; i++) {
             INFOS_PREFIX[i] = INFOS[i][0].substring(1);
        };
   };


   private static DeltaProperties props = null;
   private static String ruleFile = null;
   private static String prependRuleFile = null;

   public DeltaDiff(String logfile) {
      this.fileArgument = logfile;

      if (props == null) {
          props = new DeltaProperties(ruleFile, prependRuleFile);
      }

      determineLogFiles();
      
      populateInfos();
   }
   private String fileArgument;  // the passed-in file argument
   private String fileUrl;       // the url to show the file
   private String[] logFiles;    // the actual set of log files.
   private String[] logFileUrls;    // the actual set of log files.

   private String shortFileName; // short name for the file
                                 // -> only populated when a delta is computed

   private void determineLogFiles() {
       if (fileArgument.endsWith(".html")) {
          // First see if we can determine ${FARM_JOBID} and ${JOB_REQ_ID} from the HTML file
          LineNumberReader lnr = null;
          try {
              String line = null;
              lnr = createLineNumberReader(fileArgument);
              while ((line = lnr.readLine()) != null) {
                  if  (line.startsWith("<b>Results at:</b> <code>") && line.indexOf("</code>") > 0) {
                      int pos = line.indexOf("</code>");
                      line = line.substring("<b>Results at:</b> <code>".length(), pos).trim();

                      pos = line.lastIndexOf("/");
                      if (pos > 0) {
                          String testSuite = line.substring(pos+1);
                          line = line.substring(0, pos);
                          
                          pos = testSuite.indexOf(".");
                          if (pos > 0) {
                              String jobReqId = testSuite.substring(0, pos).trim();
                              setProperty(JOB_REQ_ID_INDEX, jobReqId);
                          }
                      
    
                          pos = line.lastIndexOf("/");
                          line = line.substring(pos+1);
                          pos = line.lastIndexOf("_T");
                          if (pos > 0) {
                              String farmJobid = line.substring(pos+"_T".length()).trim();
                              setProperty(FARM_JOBID_INDEX, farmJobid);
                          }
                      }
                      break;
                  }
              }
          } catch (IOException ioe) {
              throw new IllegalArgumentException("Unable to read HTML file " + fileArgument + ": " + ioe.getMessage());
          } finally {
              if (lnr != null) {
                  try {
                      lnr.close();
                  } catch (IOException ioe) {
                      // ignore
                  }
              }
          }

          // Now compute the names of the logfiles
          // System.out.println("Computing logfiles for " + fileArgument);
          Pattern[] patterns = props.getHtmlToLogPatterns();
          boolean found = false;

          int patternIndex = 0;
          for (Pattern p : patterns) {
              Matcher m = p.matcher(fileArgument);
              if (m.matches()) {
                  // System.out.println("Found match! Creating patterns.");

                  String[] logPatterns = props.getHtmlToLogReplacements()[patternIndex];
                  logFiles = new String[logPatterns.length];

                  int logFileIndex = 0;
                  for (String logPattern : logPatterns) {

                      // determine name of this logFile
                      logFiles[logFileIndex] = logPattern;
                      int pos = 0;

                      for (int i=1; i<=m.groupCount(); i++) {
                          // replace \<number>   with the group number <number>in the match
                          String group = m.group(i);
                          logFiles[logFileIndex] = replaceAll(logFiles[logFileIndex], "\\" + i, m.group(i));
                      }
                      // replace ${FARM_JOBID}, ${JOB_REQ_ID}
                      logFiles[logFileIndex] = replaceAll(logFiles[logFileIndex], "${FARM_JOBID}", getProperty(FARM_JOBID_INDEX));
                      logFiles[logFileIndex] = replaceAll(logFiles[logFileIndex], "${JOB_REQ_ID}", getProperty(JOB_REQ_ID_INDEX));
                      // System.out.println("Changed logfile to: " + logFiles[logFileIndex]);

                      logFileIndex++;
                  }
                  found = true;
                  break;
              }
              patternIndex++;
          }
          if (!found) {
              throw new IllegalArgumentException("Unable to determine log file(s) for HTML file: " + fileArgument);
          } 
      } else {
          logFiles = new String[] { fileArgument, };
      }
   }

   private static LineNumberReader createLineNumberReader(String fileOrUrl) throws IOException {
        Reader r = null;
        if (fileOrUrl.startsWith("http:") 
            || fileOrUrl.startsWith("https:") 
            || fileOrUrl.startsWith("ftp:") 
            || fileOrUrl.startsWith("file:")) {
            // this is an URL
            r = new InputStreamReader((new URL(fileOrUrl)).openStream());
        } else {
            r = new FileReader(new File(fileOrUrl));
        }
        return new LineNumberReader(new BufferedReader(r));
   }


   private static String replaceAll(String s, String src, String tgt) {
       if (s == null || tgt == null || src == null || src.equals("")) {
           return s;
       }
       StringBuffer sb = new StringBuffer();
       int pos = 0;
       int start = 0;
       while ((pos = s.indexOf(src, start)) >= 0) {
           sb.append(s.substring(start,pos));
           sb.append(tgt);
           start = pos + src.length();
           pos = start;
       }
       sb.append(s.substring(start));
       return sb.toString();
   }


   private void populateNamesWithDelta(DeltaDiff other) {
       if (other == null) {
           return;
       }
       shortFileName = makeShortName(fileArgument, other.fileArgument);
       other.shortFileName = makeShortName(other.fileArgument, fileArgument);

       fileUrl = "<A HREF=\"" + fileArgument + "\">" + shortFileName + "</A>";
       other.fileUrl = "<A HREF=\"" + other.fileArgument + "\">" + other.shortFileName + "</A>";

       logFileUrls = new String[logFiles.length];
       other.logFileUrls = new String[other.logFiles.length];
       for (int i=0; i<logFiles.length; i++) {
           String sfn = makeShortName(logFiles[i], other.logFiles[i]);
           logFileUrls[i] = "<A HREF=\"" + logFiles[i] + "\">" + sfn + "</A>";
       }

       for (int i=0; i<other.logFiles.length; i++) {
           String sfn = makeShortName(other.logFiles[i], logFiles[i]);
           other.logFileUrls[i] = "<A HREF=\"" + other.logFiles[i] + "\">" + sfn + "</A>";
       }
   }

   private static String makeShortName(String s1, String s2) {
       return makeShortName(s1, s2, SHORT_START);
   }

   private static String makeShortName(String s1, String s2, int segment) {
       int pos1 = s1.lastIndexOf("/");
       int pos2 = s2.lastIndexOf("/");

       String comp1 = null;
       if (pos1 >= 0) {
           comp1 = s1.substring(pos1+1);
           s1 = s1.substring(0,pos1);
       } else {
           comp1 = s1;
           s1 = "";
       }

       String comp2 = null;
       if (pos2 >= 0) {
           comp2 = s2.substring(pos2+1);
           s2 = s2.substring(0,pos2);
       } else {
           comp2 = s2;
           s2 = "";
       }

       if (comp1.equals("") 
           || (comp1.indexOf(":") >= 0 && comp1.indexOf("::") < 0)) {
           return "";
       } else if (comp1.equals(comp2)) {
           if (segment == SHORT_START) {
               String res = makeShortName(s1, s2, SHORT_MIDDLE);
               if (res.endsWith("/") || comp1.startsWith("/")) {
                   return res + comp1;
               } else {
                   return res + "/" + comp1;
               }
           } else if (segment == SHORT_MIDDLE) {
               return makeShortName(s1, s2, SHORT_MIDDLE);
           } else {
               return comp1; 
           }
      } else {
           if (segment == SHORT_START) {
               // return makeShortName(s1, s2, SHORT_END) + "/" + comp1;
               if (comp1.startsWith("/")) {
                   return "..." + comp1;
               } else {
                   return ".../" + comp1;
               }
           } else if (segment == SHORT_MIDDLE) {
               String prefix = makeShortName(s1, s2, SHORT_END);
               if (!prefix.endsWith("/") && !comp1.startsWith("/")) {
                   return prefix + "/" + comp1;
               } else {
                   return prefix + comp1;
               }
           } else {
              if (comp1.startsWith("/")) {
                  return "..." + comp1;
              } else {
                  return ".../" + comp1;
              }
           }
      }
   }
   private static final int SHORT_START = 1;
   private static final int SHORT_MIDDLE = 2;
   private static final int SHORT_END = 3;


   private void populateInfos() {
       String LABEL = null;
       String PRODUCT = null;
       String HOSTNAME = null;
       String PRODUCT_HOME = null;
       String LABEL_ID = null;
       String OECPERF_SID = null;
       String ORA_SID = null;
       String RCU_DB_SID = null;
       String LDAPOID_VIEW = null;

       for (String file : logFiles) {
           LineNumberReader lnr = null;
           try {
               String line = null;
               int pos = 0;
               int numMatches = 0;
               lnr = createLineNumberReader(file);
               while ( (line=lnr.readLine()) != null ) {
                   if (line.length() > LONGEST_LINE_LENGTH) {
                       line = "[LINE SUPPRESSED: longer than " + (LONGEST_LINE_LENGTH / 1024) + "k]";
                   }
                   for (int i = 0; i < START_PSEUDO_INDEX; i++) {
                        if (INFO_MATCHES[i] != null) {
                            // skip - already populated
                        } else if ( (pos = line.indexOf(INFOS[i][0])) >= 0
                                    || line.startsWith(INFOS_PREFIX[i])) {
                            // System.out.println("POSSIBLE MATCH: " + INFOS[i][0] + " found in '" + line + "'");
                            if (line.startsWith(INFOS_PREFIX[i])) {
                               pos = -1;
                            }
    
                            // process match
                            if (INFOS[i][0].endsWith("=") || INFOS[i][0].startsWith("=")) {
                                INFO_MATCHES[i] = line.substring(pos + INFOS[i][0].length()).trim();
                                numMatches++;
                            } else {
                                pos = line.indexOf(":", pos + INFOS[i][0].length());
                                if (pos > 0) {
                                    // TODO: show debug info
                                    INFO_MATCHES[i] = line.substring(pos+1).trim();
                                    numMatches++;
                                }
                            }
                        }
                   }
    
                   // if (numMatches == INFO_MATCHES.length) { break; }
    
                   // iFarm regressions do not show the ADE settings in the log. Try an alternative.
                   if (line.startsWith("LABEL                ")) {
                       LABEL = line.substring(5).trim();
                   } else if (line.startsWith("PRODUCT              ")) {
                       PRODUCT = line.substring(7).trim();
                   } else if (line.startsWith("HOSTNAME             ")) {
                       HOSTNAME = line.substring(8).trim();
                   } else if (line.startsWith("PRODUCT_HOME         ")) {
                       PRODUCT_HOME = line.substring(12).trim();
                   } else if (line.startsWith("\t\tLabel ID ")) {
                       LABEL_ID = line.substring(11).trim();
                       pos = LABEL_ID.lastIndexOf("_T");
                       if (pos > 0) {
                          LABEL_ID = LABEL_ID.substring(pos + 2);
                       }
                   } else if (line.startsWith("ORA_SID=")) {
                       ORA_SID = line.substring("ORA_SID=".length()).trim();
                   } else if ((pos = line.indexOf(" rcu.db.sid=")) >= 0) {
                       RCU_DB_SID = line.substring(pos + " rcu.db.sid=".length()).trim();
                   } else if (line.indexOf("OECPERF_") >= 0 
                              && (pos = line.indexOf("_DB_SID=")) >= 0) {
                       if (line.indexOf("Command not found") < 0) {
                           OECPERF_SID = line.substring(pos + "_DB_SID=".length()).trim();
                       }
                   } else if (line.endsWith("_ldapoid")
                              && (pos = line.indexOf("[exec] VIEW_NAME     : ")) > 0) {
                       LDAPOID_VIEW = line.substring(pos + "[exec] VIEW_NAME     : ".length()).trim();
                   }
               }
           } catch (IOException ioe) {
               System.out.println("SEVERE: unable to read " + file + ": " + ioe);
               System.exit(1);
           } finally {
               if (lnr != null) {
                   try { lnr.close();
                   } catch (IOException ioe) {
                       // skip
                   }
               }
           }
       }

       // Use alternative settings if needed.
       if (getProperty(VIEW_LABEL_INDEX) == null) {
           setProperty(VIEW_LABEL_INDEX, LABEL);
       }
       if (getProperty(HOST_NAME_INDEX) == null) {
          setProperty(HOST_NAME_INDEX, HOSTNAME);
       }
       if (getProperty(VIEW_NAME_INDEX) == null) {
           if (PRODUCT != null && PRODUCT_HOME != null && PRODUCT_HOME.endsWith("/" + PRODUCT)) {
               String view = PRODUCT_HOME.substring(0, PRODUCT_HOME.length() - PRODUCT.length() - 1);
               int pos = view.lastIndexOf("/");
               if (pos >= 0) {
                   view = view.substring(pos+1);
               }
               setProperty(VIEW_NAME_INDEX, view);
           }
       }

       // Additional Validation
       if (getProperty(HOST_NAME_INDEX) != null) {
           int pos = getProperty(HOST_NAME_INDEX).indexOf(".");
           if (pos >= 0) {
              setProperty(SHORT_HOST_NAME_INDEX,  getProperty(HOST_NAME_INDEX).substring(0, pos));
           }
       }
       if (getProperty(SHORT_HOST_NAME_INDEX) != null) {
           setProperty(UC_HOST_NAME_INDEX,  getProperty(SHORT_HOST_NAME_INDEX).toUpperCase());
       }
       if (LABEL_ID != null) {
          setProperty(FARM_JOBID_INDEX,  LABEL_ID);
       }
       if (OECPERF_SID != null) {
          setProperty(OECPERF_SID_INDEX, OECPERF_SID);
       }
       if (ORA_SID != null) {
          setProperty(ORA_SID_INDEX, ORA_SID);
       }
       if (RCU_DB_SID != null) {
          setProperty(RCU_DB_SID_INDEX, RCU_DB_SID);
       }
       if (LDAPOID_VIEW != null) {
          setProperty(LDAPOID_VIEW_INDEX, LDAPOID_VIEW);
       }

       if (getProperty(VIEW_LABEL_INDEX) != null) {
           int pos = getProperty(VIEW_LABEL_INDEX).lastIndexOf("_");
           if (pos >= 0) {
              setProperty(LABEL_DATE_INDEX,  getProperty(VIEW_LABEL_INDEX).substring(pos + 1));
           }
       }
       if (getProperty(VIEW_NAME_INDEX) != null) {
           int pos = getProperty(VIEW_NAME_INDEX).indexOf("_");
           if (pos >= 0) {
              setProperty(USER_NAME_INDEX,  getProperty(VIEW_NAME_INDEX).substring(0, pos));
           }
       }
   }
   private String[] INFO_MATCHES = new String[ INFOS.length ];

   private String getProperty(int index) {
       return INFO_MATCHES[index];
   }
   private void setProperty(int index, String property) {
       if (property == null || property.equals("") || getProperty(index) != null) {
          return;
       }
       INFO_MATCHES[index] = property;
   }


   public void readFile() {
       readFileWithDelta(null);
   }

   public void readFileWithDelta(DeltaDiff otherDelta) {
       Pattern[] patterns = props.getPatterns();
       String[] replacements = props.getReplacements();
       String[] ignores = props.getIgnores();

       patternedLines = new Object[logFiles.length];

       for (int logFileIndex=0; logFileIndex<logFiles.length; logFileIndex++) {
           String file = logFiles[logFileIndex];
           List<String> patLines = new ArrayList<String>();
           patternedLines[logFileIndex] = patLines;

           LineNumberReader lnr = null;
           try {
               String line = null;
               int pos = 0;
               lnr = createLineNumberReader(file);
               while ( (line=lnr.readLine()) != null ) {
                   if (line.length() > LONGEST_LINE_LENGTH) {
                       line = "[LINE SUPPRESSED: longer than " + (LONGEST_LINE_LENGTH / 1024) + "k]";
                   }
                   boolean ignore = false;
                   if (!VERBOSE) {
                       for (int i=0; i < ignores.length; i++) {
                           if (line.indexOf(ignores[i]) >= 0) {
                               ignore = true;
                               break;
                           }
                       }
                   }
                   if (!ignore) {
                       // Using matches for extracted information
                       for (int i=0; i < INFOS.length; i++) {
                           if (INFO_MATCHES[i] != null) {
                               while ( (pos = line.indexOf(INFO_MATCHES[i])) >= 0 ) {
                                   line = line.substring(0,pos) + INFOS[i][1] + line.substring(pos+INFO_MATCHES[i].length());
                               }
                           }
                       }
        
                       // Using regexp matches
                       for (int i=0; i < patterns.length; i++) {
                           Matcher m = patterns[i].matcher(line);
                           if (m.find()) {
                               // if (DEBUG) { System.out.println("LINE    " + line); System.out.println("MATCHES " + PATTERNS[i][0]); }
                               StringBuffer newLine = new StringBuffer(line.substring(0, m.start()));
                               pos = m.end();
                               newLine.append(replacements[i]);
                               while (m.find()) {
                                   newLine.append(line.substring(pos, m.start()));
                                   pos = m.end();
                                   newLine.append(replacements[i]);
                               }
                               newLine.append(line.substring(pos));
                               line = newLine.toString();
                               // if (DEBUG) { System.out.println("=====>  " + line); }
                           }
                       }
                       addLine(logFileIndex, line, otherDelta);
                   } else {
                       addLine(logFileIndex, null, otherDelta);
                   }
               }
    
               // If we have a baseline diff file, then all lines that match this
               // baseline should be suppressed.
               normalizeDiffFile(otherDelta);
    
               contentRead = true;
               if (otherDelta != null) {
                   if (!otherDelta.contentRead) {
                       System.out.println("SEVERE: error processing " + file + ": the difference file for " + otherDelta.fileArgument 
                                          + " has not yet been read.");
                       System.exit(1);
                   } else {
                       contentNormalized = true;
                   }
                   baseline = otherDelta;
               }
           } catch (IOException ioe) {
               System.out.println("SEVERE: unable to read " + file + ": " + ioe);
               System.exit(1);
           } finally {
               if (lnr != null) {
                   try { lnr.close();
                   } catch (IOException ioe) {
                       // skip
                   }
               }
           }
       }
   }
   private Object[] patternedLines = null;  // actual type: List<String>[]
   private Set<String> linePatterns = new HashSet<String>();
   private boolean contentRead = false;
   private boolean contentNormalized = false;
   private DeltaDiff baseline;

   private void addLine(int index, String line, DeltaDiff otherDelta) {
       List<String> patLines = (List<String>) patternedLines[index];
       if (line == null) {
           patLines.add(SUPPRESSED);
       } else {
           String canonical = canonicalize(line);
           linePatterns.add(canonical);
           if (otherDelta != null && otherDelta.linePatterns.contains(canonical)) {
               patLines.add(SUPPRESSED);
           } else {
               patLines.add(line);
           }
       }
   }

   private void normalizeDiffFile(DeltaDiff otherDelta) {
       if (otherDelta == null) {
           return;
       } else if (otherDelta.contentNormalized) {
           System.out.println("SEVERE: error processing " + fileArgument + ": the difference file "
                              + otherDelta.fileArgument + " has already be compared to another file!");
           System.exit(1);
       };
       for (int logFileIndex=0; logFileIndex < otherDelta.logFiles.length; logFileIndex++) {
           List<String> otherPatterns = (List<String>) otherDelta.patternedLines[logFileIndex];
           for (int i=0; i<otherPatterns.size(); i++) {
               if (linePatterns.contains(canonicalize(otherPatterns.get(i)))) {
                   otherPatterns.set(i, SUPPRESSED);
               }
           }
       }
   }

   private String canonicalize(String line) {
       line = line.trim();
       Matcher m = multiWhiteSpace.matcher(line);
       if (m.find()) {
           StringBuffer newLine = new StringBuffer(line.substring(0, m.start()));
           int pos = m.end();
           newLine.append(" ");
           while (m.find()) {
               newLine.append(line.substring(pos, m.start()));
               pos = m.end();
               newLine.append(" ");
           }
           newLine.append(line.substring(pos));
           line = newLine.toString();
       }
       return line;
   }
   private static Pattern multiWhiteSpace = Pattern.compile("\\s\\s\\s*");


   public void writeDelta(String html) {
       if (html == null) {
           useHtml = false;
           out = System.out;
       } else {
           useHtml = true;
           try {
               out = new PrintStream(new FileOutputStream(new File(html)));
           } catch (IOException ioe) {
               explain("Unable to write HTML report to " + html + " - error: " + ioe);
           }
       }
            
       Set<String> seenPatterns = new HashSet<String>();

       populateNamesWithDelta(baseline);
       baseline.populateNamesWithDelta(this);
       
       // Header Information
       String title = "Log analysis of: " + shortFileName + " compared to baseline: " + baseline.shortFileName;
       if (useHtml) {
            out.println("<HTML><HEADER><TITLE>" + title + "</TITLE><HEADER>");
            out.println("<script language=\"JavaScript\" src=\"/jscript/mktree.js\"></script><link rel=\"stylesheet\" href=\"/jscript/mktree.css\">");
            out.println("<BODY><H1>" + title + "</H1>");
       } else {
           out.println("*** Deltas in " + title + " ***");
       }


       // Assignments
       if (useHtml) {
            out.println("<H3>Replacement Settings</H3>");
            out.println("<TABLE>");
            out.println("<TR><TH>Symbolic</TH><TH>Logfile value</TH><TH>Baseline value</TH></TR>");
       } else {
           out.println("Replacement Settings: ");
       }
       for (int i=0; i<INFO_MATCHES.length; i++) {
           if (INFO_MATCHES[i] != null) {
              String valueOther = (baseline.INFO_MATCHES[i]==null) ? "" : baseline.INFO_MATCHES[i];
              if (useHtml) {
                  out.println("<TR><TD>" + toHtml(INFOS[i][1]) + "</TD><TD>" + toHtml(INFO_MATCHES[i]) + "</TD><TD>" + toHtml(valueOther) + "</TD></TR>");
              } else {
                  out.println("    " + INFOS[i][1] + " = " + INFO_MATCHES[i]  + " (baseline: " + valueOther + ")");
              }
           }
       }
       if (useHtml) {
           out.println("</TABLE>");
       } else {
           out.println();
       }
       

       // Comparison index
       if (useHtml) {
            out.println("<H3>Log Comparisons</H3>");
            out.println("<TABLE>");
            out.println("<TR><TH></TH><TH>Baseline</TH><TH>Comparison Log</TH></TR>");
            if (fileUrl != null || baseline.fileUrl != null) {
                out.print("<TR><TD>Triage page</TD><TD>");
                if (baseline.fileUrl!=null) {
                    out.print(baseline.fileUrl);
                } 
                out.print("</TD><TD>");
                if (fileUrl!=null) {
                    out.print(fileUrl);
                } 
                out.println("</TD></TR>");
            }
            for (int i=0; i< logFileUrls.length; i++) {
                out.print("<TR><TD>");
                if (logFileUrls.length == 1) {
                   out.print("<A href=\"#comp0\">Comparison</A>");
                } else {
                   out.print("<A href=\"#comp" + i + "\">Comparison #" + (i+1) + "</A>");
                }
                out.print("</TD><TD>");
                out.print(baseline.logFileUrls[i]);
                out.print("</TD><TD>");
                out.print(logFileUrls[i]);
                out.print("</TD></TR>");
            }
            out.println("</TABLE>");
       } else {
          // out.println("Replacement Settings: ");
       }
       
       int totalCount = 0;
       outputCount = 0;

       // Comparisons performed
       for (int logFileIndex=0; logFileIndex < logFiles.length; logFileIndex++) {
           List<String> patLines = (List<String>)patternedLines[logFileIndex];

           title = "Comparing " + logFileUrls[logFileIndex] + " to baseline " + baseline.logFileUrls[logFileIndex]; 
           if (useHtml) {
               out.println("<H3><A name=\"comp" + logFileIndex + "\">" + title + "</A></H3>");
               out.println("<PRE>");
           } else {
               out.println();
           }
    
           lastLine = 0;
    
           int count = 0;
           String lastAdded = null;
    
           LineNumberReader lnr = null;
           try {
               String line = null;
               lnr = createLineNumberReader(logFiles[logFileIndex]);
               while ( (line=lnr.readLine()) != null ) {
                   if (line.length() > LONGEST_LINE_LENGTH) {
                       line = "[LINE SUPPRESSED: longer than " + (LONGEST_LINE_LENGTH / 1024) + "k]";
                   }
                   String pat = patLines.get(count);
                   if (pat != SUPPRESSED && !seenPatterns.contains(pat)) {
                       showLine(logFileIndex, count+1, line, true);
                       if (!VERBOSE) {
                           seenPatterns.add(pat);
                           lastAdded = pat;
                       }
                   } else {
                       boolean show = false;
                       for (int i = count - contextLength; i <= count + contextLength; i++) {
                            if (i < 0 || i >= patLines.size()) {
                                // must skip check
                            } else if (patLines.get(i) != SUPPRESSED
                                       && (patLines.get(i) == lastAdded
                                           || !seenPatterns.contains(patLines.get(i)))) {
                                show = true;
                                break;
                            }
                       }
                       if (show) {
                          showLine(logFileIndex, count+1, line, false);
                       }
                   }
                   count ++;
               }
           } catch (IOException ioe) {
               System.out.println("SEVERE: unable to read " + logFiles[logFileIndex] + ": " + ioe);
               // System.exit(1);
               if (useHtml) {
                   out.println("<large><b>SEVERE: unable to read " + logFiles[logFileIndex] + ": " + ioe+"</large></b>");
               }
           } finally {
               if (lnr != null) {
                   try { lnr.close();
                   } catch (IOException ioe) {
                       // skip
                   }
               }
    
               if (lastLine < count) {
                   if (useHtml) {
                       out.println("<HR>");
                   } else {
                       out.println("...");
                   }
               }
    
     
               if (useHtml) {
                   out.println("</PRE>\n");
               }
               totalCount += count;
            }
        }

        /* String percent = "" + (100.0 * outputCount / count);
           int pos = percent.indexOf(".");
           if (pos >= 0) {
              percent = percent.substring(0, pos + 5);
           } */
        String reported = "Reported " + outputCount + " lines out of " + totalCount + "."; 
        if (useHtml) {
            out.println("<P>");
            out.println(reported + "<br>");
            out.println("&nbsp; <br>");
            out.println("&nbsp; <br>");
            out.println("<small>This report was created with the triaging tool using the <code>tri -baselog</code> option. ");
            out.println("It can be found in the J2EE label or at /net/stjpg/public/ws/bin/triage</small>");
        } else {
            out.println(reported);
        }
    
        // Close file if opened to write
        if (useHtml) {
            out.println("</BODY></HTML>");
            out.flush();
            out.close();
            out = System.out;
            useHtml = false;
        }
    }
    private boolean useHtml = false;
    private PrintStream out = System.out;

    private static String getBasename(String s) {
        int pos = s.lastIndexOf("/");
        return (pos > 0) ? s.substring(pos+1) : s;
    }

    private static String toHtml(String s) {
        StringBuffer sb = new StringBuffer();
        for (int i=0; i<s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '<') {
                sb.append("&lt;");
            } else if (ch == '>') {
                sb.append("&gt;");
            } else if (ch == '&') {
                sb.append("&amp;");
            } else if (ch == '\t') {
                sb.append("<b>\\t</b>");
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }

    private void showLine(int fileIndex, int lineNo, String text, boolean isDelta) {
        if (lastLine != lineNo - 1) {
           if (useHtml) {
               out.println("<HR>");
           } else {
               out.println("...");
           }
        }
        String number = "0000000" + lineNo; 
        number = number.substring(number.length() - numDigits);

        if (useHtml) {
            out.print("<SUP>" + number + " </SUP>");
            if (isDelta) {
                out.print("! ");
            } else {
                out.print("  ");
            }
            if (isDelta) {
                out.print(FONT_START_RED);
            }
            out.print(toHtml(text));
            if (isDelta) {
                out.print(FONT_END);
            }
            out.println();
        } else {
            out.print(number);
            if (isDelta) {
                out.print(" ! ");
            } else {
                out.print("   ");
            }
            out.println(text);
        }

        if (DEBUG && isDelta) {
           List<String> patLines = (List<String>) patternedLines[fileIndex];
           if (!patLines.get(lineNo - 1).equals(text)) {
               if (useHtml) {
                   out.println(toHtml(">>>>>  " + patLines.get(lineNo - 1)));
               } else {
                   out.println(">>>>>>  " + patLines.get(lineNo - 1));
               }
           }
        }
        lastLine = lineNo;
        outputCount ++;
   }
   private int lastLine = 0;
   private int outputCount = 0;


   private static void explain(String message) {
        if (message != null) {
            System.out.println("SEVERE: " + message);
        }
        Messages.explainBaseDelta(null);

        if (message == null) {
            System.exit(0);
        } else {
            System.exit(1);
        }
   }

   public static void main(String[] args) {
       List<String> fileNames = new ArrayList<String>();

       String htmlFile  = null;

       if (args.length == 0) {
           explain(null);
       }
       for (int i=0; i<args.length; i++) {
           if (args[i].equalsIgnoreCase("-v")
                      || args[i].equalsIgnoreCase("-verbose")) {
               VERBOSE = true;
           } else if (args[i].equalsIgnoreCase("-d")
                      || args[i].equalsIgnoreCase("-debug")) {
               DEBUG = true;
           } else if (args[i].equalsIgnoreCase("-c")
                      || args[i].equalsIgnoreCase("-context")) {
               i++;
               if (i >= args.length) {
                   explain("Missing numeric argument for -context option.");
               }
               int num = -1;
               try {
                   num = Integer.parseInt(args[i]);
               } catch (Exception e) { 
                   // skip
               }
               if (num < 0 || 100 <= num ) {
                   explain("Invalid numeric argument for -context option: " + args[i]);
               } 
               contextLength = num;
           } else if (args[i].equalsIgnoreCase("-h")
                     || args[i].equalsIgnoreCase("-html")) {
               i++;
               if (i >= args.length) {
                   explain("Missing html file argument for -html option.");
               }
               htmlFile = args[i];
           } else if (args[i].equalsIgnoreCase("-r")
                     || args[i].equalsIgnoreCase("-rules")) {
               i++;
               if (i >= args.length) {
                   explain("Missing rule file argument for -rules option.");
               }
               ruleFile = args[i];
           } else if (args[i].equalsIgnoreCase("-pr")
                     || args[i].equalsIgnoreCase("-prepend-rules")) {
               i++;
               if (i >= args.length) {
                   explain("Missing rule file argument for -prepend-rules option.");
               }
               prependRuleFile = args[i];
           } else if (args[i].startsWith("-")) {
               explain("Unknown option: " + args[i]);
           } else {
               fileNames.add(args[i]);
               if (fileNames.size() > 2) {
                   explain("Illegal file name argument: " + args[i] + ". Must supply two files: base log and comparison log.");
               }
           }
       }

       // Special case: user just wants to generate a rule file template or list an existing rule file.
       if (fileNames.size() == 0 
           && (ruleFile != null || prependRuleFile !=null)) {
           boolean haveRuleFile        = ruleFile!=null        && (new File(ruleFile)).exists();
           boolean havePrependRuleFile = prependRuleFile!=null && (new File(prependRuleFile)).exists();

           if ( !haveRuleFile && prependRuleFile == null ) {
               new DeltaProperties(ruleFile, null);
           } else if ( haveRuleFile || havePrependRuleFile ) {
               System.out.println("INFO: content of rules:");   
               System.out.println(new DeltaProperties(ruleFile, prependRuleFile));
           } else {
               explain("Must specify either -rules <new-rules-file> to generate a rule template, or use: "
                       + " -rules <existing-rules-file> and/or -prepend-rules <existing-rules-file> to modify (and print) rules in use.");
           }
           System.exit(0);
       }
/***
       
       // Special case: user just wants to generate a rule file template or list an existing rul file.
       if (fileNames.size() == 0  && ruleFile != null) {
           if ((new File(ruleFile)).exists()) {
               System.out.println("INFO: content of rule file:");
               System.out.println(new DeltaProperties(ruleFile));
           } else {
               new DeltaProperties(ruleFile);
           }
           System.exit(0);
       }
***/

       if (fileNames.size() != 2) {
           explain("Must supply two files: base log and comparison log.");
       }

       DeltaDiff d1 = new DeltaDiff(fileNames.get(0));
       DeltaDiff d2 = new DeltaDiff(fileNames.get(1));
       d1.readFile();
       d2.readFileWithDelta(d1);
       d2.writeDelta(htmlFile);
   } 

}
