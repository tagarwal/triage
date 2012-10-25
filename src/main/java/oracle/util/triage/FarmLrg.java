package oracle.util.triage;

import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.BufferedReader;

/**
 This obtains information on a Farm job.
 */


public class FarmLrg implements Constants {

    private static final String LABEL_INTEGRATION_LOG_DIR = "/net/stbbo03/scratch/aime/log";


    static FarmLrg newFarmLrg(String label) {
        File dir = new File(LABEL_INTEGRATION_LOG_DIR, label);
        if (!dir.exists() || !dir.isDirectory()) {
            dir = new File(LABEL_INTEGRATION_LOG_DIR, DEFAULT_LABEL_PREFIX + "_" + label);
            if (!dir.exists() || !dir.isDirectory()) {
                System.out.println("SEVERE: cannot find log directory for label " + label +
                                   " under " + LABEL_INTEGRATION_LOG_DIR);
                return null;
            }
        }
        File farmLog = null;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.getName().startsWith("farmsubmit.") && f.getName().endsWith(".log")) {
                    farmLog = f;
                }
            }
        }
        if (farmLog == null) {
            System.out.println("SEVERE: cannot find farmsubmit.<...>.log file in " + dir);
            return null;
        }
        return new FarmLrg(farmLog);
    }

    private FarmLrg(File log) {
        LineNumberReader lnr = null;
        try {
            lnr = new LineNumberReader(new BufferedReader(new FileReader(log)));
            String line = null;
            int pos = 0;
            int count = 0;
            while ((line = lnr.readLine()) != null) {
               if ((pos = line.indexOf("Job # is ")) > 0) {
                   String jobStr = line.substring(pos + "Job # is ".length()).trim();
                   int jobId = Integer.parseInt(jobStr);
                   count++;
                   setFarmId(count, jobId);
               } 
            }
        } catch (Exception e) {
            System.out.println("SEVERE: error reading farmsubmit log " + log + ": " + e);
        } finally {
            if (lnr != null) {
                try {
                    lnr.close();
                } catch (Exception e) {
                    // ignore
                }
            }
        }
    }

    private void setFarmId(int count, int id) {
        if (count == 1) {
            lrgFarmId = id;
        } else if (count == 2) {
            srgFarmId = id;
        } else { // (count==3)
            ctsFarmId = id;
        }
    }

    public int getSrgFarmId() {
        return srgFarmId;
    }

    public int getLrgFarmId() {
        return lrgFarmId;
    }

    public int getCtsFarmId() {
        return ctsFarmId;
    }

    private int srgFarmId = 0;
    private int lrgFarmId = 0;
    private int ctsFarmId = 0;

}
