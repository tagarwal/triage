package oracle.util.triage;

import java.io.*;

/** Representation of a build.jtl runtime log segment.
 */

public class BuildJTL extends DatedFile implements Constants {

    public BuildJTL(VirtualFile root) {
        super(root);
    }

    static void readJTLFiles(VirtualFile f) {
        BuildJTL jtl = new BuildJTL(f);
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new BufferedReader(new InputStreamReader(f.getInputStream())));
            String line = null;
            boolean startSeen = false;
            boolean endSeen = false;
            while ( (line = lnr.readLine()) != null) {
                if (!startSeen 
                    && (line.startsWith("*** Start ") 
                        || line.startsWith("** Start ")
                        || line.startsWith("* Start "))
                    && line.indexOf("@") > 0) {
                    String time = line.substring(line.indexOf("@") + 1);
                    if (time.indexOf(" by ") > 0) {
                       time = time.substring(0, time.indexOf(" by "));
                    }
                    if (time.indexOf(" as ") > 0) {
                       time = time.substring(0, time.indexOf(" as "));
                    }
                    jtl.setStartTime(time);
                    startSeen = true;
                } else if (startSeen 
                           && (line.startsWith("*** Finish ")
                               || line.startsWith("** Finish ")
                               || line.startsWith("* Finish "))
                           && line.indexOf("@") > 0) {
                    String time = line.substring(line.indexOf("@") + 1);
                    if (time.indexOf(" total time") > 0) {
                       time = time.substring(0, time.indexOf(" total time"));
                    }
                    jtl.setEndTime(time);
                    endSeen = true;
                }
            }
            lnr.close();
            if (endSeen) {
                theUniverse.addChild(jtl);
                // jtl= new BuildJTL(f);
            }
         } catch (Exception e) {
            e.printStackTrace();
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (IOException ioe) {
                    // ignore closing resource
                }
            }
         }
    }

}
