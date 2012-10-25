package oracle.util.triage;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.io.*;

/** Representation of a build.jtl runtime log segment.
 */

public class DatedLog extends DatedFile implements Constants {

    public DatedLog(VirtualFile root) {
        super(root);
    }

    public String getFullName() {
        return /* getDir() + "/" + */ getName();
    }

    private int startLine = -1;
    private int endLine = -1;
    private String url = null;

    public String getUrl() {
        return (url == null)
               ? super.getUrl()
               : url;
    }

    private static final boolean DEBUG = false;

    static void readDatedLog(VirtualFile f) {
        DatedLog dl = new DatedLog(f);

        /* 
        if (dl.getName().endsWith(".log")) {
            dl.setEndTime(f.lastModified());
            dl.setStartTime(f.lastModified());
        } else if (dl.getName().endsWith(".farm.out")) {
            dl.setStartTime(Long.MIN_VALUE);
            dl.setEndTime(Long.MAX_VALUE);
        } else
            // for all other kinds of files...
        */
        {
            dl.setEndTime(f.lastModified());
            dl.setStartTime(f.lastModified());
        }

        Date d = Util.getTimestamp(f.getName());
        if (d != null && d.getTime() < dl.getStartTime()) {
           dl.setStartTime(d.getTime());
        }

        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(f.getInputStream())));
   
            String line = null;
            List<String> openTask = new ArrayList<String>();
            List<Long> startTask = new ArrayList<Long>();
            List<Integer> startLines = new ArrayList<Integer>();

            while ( (line = lnr.readLine()) != null) {
                String trim = line.trim();
                boolean started = false;
                boolean finished = false;

                ////////////////////////////////////////////////////////////////
                //// SHELL SCRIPT LOG FILE METRICS                          //// 
                ////////////////////////////////////////////////////////////////
                if ((finished = trim.startsWith("Finished task \""))
                    || trim.startsWith("Starting task \"") ) {
                    trim = (finished)
                           ? trim.substring("Finished task \"".length())
                           : trim.substring("Starting task \"".length());

                    int pos = trim.indexOf("\"");
                    String taskName = trim.substring(0,pos);
                    trim = trim.substring(pos + 1);

                    pos = trim.indexOf(", at ");
                    if (pos > 0) {
                        String time = trim.substring(pos + ", at Ddd ".length());
                        pos = time.indexOf(",");
                        if (pos > 0) {
                            time = time.substring(0,pos);
                        }
                        long timestamp = Util.parseTime2(time).getTime();

                        if (timestamp < dl.getStartTime()) {
                            dl.setStartTime(timestamp);
                        } else if (dl.getEndTime() < timestamp) { 
                            dl.setEndTime(timestamp);
                        }
                       
                        if (finished) {
                           for (int i = openTask.size() - 1; i >= 0; i--) {
                                if (taskName.equals(openTask.get(i))) {
                                    dl.addChild(new DatedFile(dl.getName() + "#" + taskName,
                                                              startTask.get(i).longValue(),
                                                              timestamp));
                                    for (int j = openTask.size() - 1; j >= i; j--) {
                                         openTask.remove(j);
                                         startTask.remove(j);
                                    }
                                    break;
                                }
                            }
                        } else {
                            openTask.add(taskName);
                            startTask.add(new Long(timestamp));
                        }
                    } else {
                        System.out.println("!!!!! Timestamp not found in " + trim + "!!!");
                    }

                ////////////////////////////////////////////////////////////////
                //// ANT BUILD FILE METRICS                                 //// 
                ////   - this uses mainly output from orajtstinit            //// 
                ////////////////////////////////////////////////////////////////
                } else if (trim.indexOf("@") > 0 
                           && trim.indexOf(" UTC ") > 0
                           && ( trim.indexOf("* Finish ") > 0
                                || trim.indexOf("* Start ") > 0 ) ) {

                    int pos = trim.indexOf(" @");
                    String taskName = trim.substring(0,pos);
                    String time = trim.substring(pos + " @".length());

                    finished = taskName.indexOf("* Finish ") >= 0;
                    if (finished) {
                        pos = taskName.indexOf("* Finish ");
                        taskName = taskName.substring(pos + "* Finish ".length()).trim();
                        if (DEBUG) {
                            System.out.println(">>>> DETECTED END: " + taskName + " <<<<<");
                        }
                    } else {
                        pos = taskName.indexOf("* Start ");
                        taskName = taskName.substring(pos + "* Start ".length()).trim();
                        if (DEBUG) {
                            System.out.println(">>>> DETECTED START: " + taskName + " <<<<<");
                        }
                    }

                    pos = time.indexOf(" UTC");
                    time = time.substring(0,pos + " UTC".length());

                    long timestamp = Util.parseTime1(time).getTime();

                    if (timestamp < dl.getStartTime()) {
                        dl.setStartTime(timestamp);
                    } else if (dl.getEndTime() < timestamp) { 
                        dl.setEndTime(timestamp);
                    }
                       
                    if (finished) {
                        if (openTask.size() > 0) {
                            if (DEBUG) {
                                System.out.println(">>>> ADDING CHILD " + openTask.get(openTask.size() - 1) + " <<<<<");
                            }
                            dl.addChild(new DatedFile(openTask.get(openTask.size() - 1),
                                                      startTask.get(startTask.size() - 1).longValue(),
                                                      timestamp,
                                                      f.getUrl(),
                                                      startLines.get(startLines.size() - 1).intValue(),
                                                      lnr.getLineNumber()));
                            openTask.remove(openTask.size() - 1);
                            startTask.remove(startTask.size() - 1);
                            startLines.remove(startLines.size() - 1);
                        }
                     } else {
                        openTask.add(taskName);
                        startTask.add(new Long(timestamp));
                        startLines.add(new Integer(lnr.getLineNumber()));
                     }


                ////////////////////////////////////////////////////////////////
                //// ANT BUILD FILE METRICS                                 //// 
                ////     TODO: Metrics may also happen with deeper nesting! ////
                ////////////////////////////////////////////////////////////////
                } else if ((finished = trim.startsWith("*** EVENT_ENDTIME: "))
                            || trim.startsWith("*** EVENT_STARTTIME: ")
                            || (finished = trim.startsWith("[echo] *** EVENT_ENDTIME: "))
                            || trim.startsWith("[echo] *** EVENT_STARTTIME: ")
                            || (finished = trim.indexOf("*** END-METRIC ") >= 0)
                            || (trim.indexOf("*** BEGIN-METRIC ") >= 0)
                           ) {
                    String origTrim = trim;
                    boolean isAnt = trim.startsWith("[echo] ");
                    boolean isMetric = trim.indexOf("-METRIC ") > 0;


                    String taskName = null;
                    if (finished) {
                        if (isMetric) {
                            int pos = trim.indexOf("*** END-METRIC ");
                            taskName = trim.substring(pos + "*** END-METRIC ".length());
                        } else {
                            int pos = trim.indexOf("*** EVENT_ENDTIME: ");
                            taskName = trim.substring(pos + "*** EVENT_ENDTIME: ".length());
                        }
                    } else {
                        if (isMetric) {
                            int pos = trim.indexOf("*** BEGIN-METRIC ");
                            taskName = trim.substring(pos + "*** BEGIN-METRIC ".length());
                        } else {
                            int pos = trim.indexOf("*** EVENT_STARTTIME: ");
                            taskName = trim.substring(pos + "*** EVENT_STARTTIME: ".length());
                        }
                    }

                    int pos = taskName.indexOf(" at ");
                    String time = taskName.substring(pos + " at ".length());
                    taskName = taskName.substring(0,pos);
                    pos = time.indexOf(" ***");
                    if (pos > 0) {
                        time = time.substring(0,pos);
                    }

                    if (isMetric) {
                        if ((pos = time.lastIndexOf(".")) > 0) {
                            time = time.substring(0, pos + 4);
                        } else if (time.endsWith(" UTC")) {
                            time = time.substring(0, time.length() - " UTC".length()) + ".000" ;
                        }
                    }

                    long timestamp = (isMetric)
                                      ? Util.parseTime8(time + " " + FARM_TIMEZONE).getTime()
                                      : ((isAnt)
                                          ? Util.parseTime7(time).getTime()
                                          : Util.parseTime1(time).getTime());

                    if (DEBUG) {
                        System.out.println("origTrim=" + origTrim);
                        System.out.println("   trim=" + trim);
                        System.out.println("   finished=" + finished);
                        System.out.println("   taskName=" + taskName);
                        System.out.println("   time=" + time);
                        System.out.println("   timestamp=" + (new Date(timestamp)));
                    }

                    if (timestamp < dl.getStartTime()) {
                        dl.setStartTime(timestamp);
                    } else if (dl.getEndTime() < timestamp) { 
                        dl.setEndTime(timestamp);
                    }
                       
                    if (finished) {
                        if (openTask.size() > 0) {
                            if (DEBUG) {
                                System.out.println("$$$$$ ADDING: " + openTask.get(openTask.size() - 1) + ": " +
                                               f.getUrl() + " range: " + startLines.get(startLines.size() - 1) +
                                               "-" + lnr.getLineNumber()); 
                            }
                            dl.addChild(new DatedFile(openTask.get(openTask.size() - 1),
                                                      startTask.get(startTask.size() - 1).longValue(),
                                                      timestamp,
                                                      f.getUrl(),
                                                      startLines.get(startLines.size() - 1).intValue(),
                                                      lnr.getLineNumber()));
                            openTask.remove(openTask.size() - 1);
                            startTask.remove(startTask.size() - 1);
                            startLines.remove(startLines.size() - 1);
                        } else { 
                            if (isMetric) {
                              System.out.println("Issue adding a metric!!!");
                            }
                         }
                     } else {
                        openTask.add(taskName);
                        startTask.add(new Long(timestamp));
                        startLines.add(new Integer(lnr.getLineNumber()));
                     }


                ////////////////////////////////////////////////////////////////
                //// DIAGNOSTIC LOG EVENTS                                  //// 
                ////////////////////////////////////////////////////////////////
                } else if ((trim.startsWith("[20")
                             && trim.indexOf("+00:00] [") > 0
                             && Util.isImportantEvent(trim))
                           || (trim.startsWith("####<")
                               && trim.indexOf("> <") > 0
                               && Util.isImportantWLSEvent(trim))
                          ) {
                     DiagnosticEvent de = trim.startsWith("[20")
                                          ? DiagnosticEvent.newDiagnosticEvent(trim, f, lnr)
                                          : DiagnosticWLSEvent.newDiagnosticEvent(trim, f, lnr);
                     if (de != null) {
                         if (de.getStartTime() < dl.getStartTime()) {
                             dl.setStartTime(de.getStartTime());
                         }
                         if (dl.getEndTime() < de.getEndTime()) { 
                             dl.setEndTime(de.getEndTime());
                         }
                         dl.addChild(de);
                     }
                }
            } 
            lnr.close();
      } catch (IOException ioe) {
          System.out.println("SEVERE: Error in reading " + f);
          ioe.printStackTrace();
          if (lnr != null) {
              try {
                  lnr.close();
              } catch (Exception e) {
                  // ignore
              }
          }
      }

      // if (dl.getName().endsWith(".farm.out")) {
      //    theUniverse.addChildren(dl.getChildren());
      //} else {
          theUniverse.addChild(dl);
      //}

    }

}
