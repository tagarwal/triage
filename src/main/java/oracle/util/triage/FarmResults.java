package oracle.util.triage;

import java.util.Date;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.io.*;

/** Representation of a build.jtl runtime log segment.
 */

public class FarmResults extends DatedFile implements Constants {

    public FarmResults(VirtualFile root) {
        super(root);
    }

    public String getSuiteName() {
        return suiteName;
    }
    public int getSuccCount() {
        return succCount;
    }

    public int getDiffCount() {
        return diffCount;
    }

    public String[] getDiffs() {
        String[] res = new String[diffs.size()];
        for (int i=0; i<diffs.size(); i++) {
             res[i] = diffs.get(i);
        }
        return res;
    }

    private String suiteName;
    private int succCount;
    private int diffCount;
    private List<String> diffs = new ArrayList<String>();

    static void readFarmResults(VirtualFile f, Date submitTime) {
        FarmResults fr = new FarmResults(f);
        fr.setEndTime(f.lastModified());
        fr.setStartTime(f.lastModified());

        System.out.println("FARM initial: "+(new Date(fr.getStartTime()))+" - "+(new Date(fr.getEndTime())));

        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(f.getInputStream())));
   
            String line=null;
            List<String> openTask = new ArrayList<String>();
            List<Long> startTask = new ArrayList<Long>();

            while ( (line=lnr.readLine()) != null) {
                String trim = line.trim();
                if (trim.startsWith("<lrgname>")) {
                    String nam = Util.chop(trim, "<lrgname>", "</lrgname>");
                    if (nam!=null) {
                        fr.suiteName = nam;
                    }
                } else if (trim.startsWith("<succnt>")) {
                    String count = Util.chop(trim, "<succnt>", "</succnt>");
                    if (count!=null) {
                        fr.succCount = Integer.parseInt(count);
                    }
                } else if (trim.startsWith("<diffcnt count=\"")) {
                    String count = Util.chop(trim, "<diffcnt count=\"", "\">");
                    if (count!=null) {
                        fr.diffCount = Integer.parseInt(count);
                    }
                } else if (trim.startsWith("<difflog name=\"")) {
                    String nam = Util.chop(trim, "<difflog name=\"", "\"");
                    if (nam!=null) {
                        fr.diffs.add(nam);
                    }
                } else if (trim.startsWith("<starttime>")) {
                    String time = Util.chop(trim, "<starttime>", "</starttime>");
                    System.out.println("FARM START: '"+time+"'");
                    if (time!=null) {
                       fr.setStartTime(Util.parseTime5(time + " " +FARM_TIMEZONE).getTime());
                    }
                } else if (trim.startsWith("<endtime>")) {
                    String time = Util.chop(trim, "<endtime>", "</endtime>");
                    System.out.println("FARM END: '"+time+"'");
                    if (time!=null) {
                       fr.setEndTime(Util.parseTime5(time + " " +FARM_TIMEZONE).getTime());
                    }
                }
            } 
            lnr.close();
        } catch (IOException ioe) {
            System.out.println("SEVERE: Error in reading "+f);
            ioe.printStackTrace();
            if (lnr!=null) {
               try {
                   lnr.close();
               } catch (Exception e) {
                   // ignore
               }
            }
        }

        theUniverse.addChild(fr);

        if (submitTime != null && submitTime.getTime() < fr.getStartTime() - 1000L) {
            DatedFile df = new DatedFile("farm_queue&setup", submitTime.getTime(), fr.getStartTime());
            theUniverse.addChild(df);
        }
        if (fr.getEndTime() < f.lastModified() - 1000L) {
            DatedFile df = new DatedFile("farm_clean&report", fr.getEndTime(), f.lastModified());
            theUniverse.addChild(df);
        }
    }
}
