package oracle.util.triage;

import java.io.File;
import java.io.FilenameFilter;


/**
 This class implements a diff analysis for MATs tests.
 */
public class Mats extends ResultsDir implements Constants {

    // public static String resultsDirectory = "/net/stdmlina41/linux_results/";
    // public static String resultsDirectory = "/net/stansx03/linux_results/";
    public static String resultsDirectory = "/net/stdmlina42.us.oracle.com/farm_results/";

    private Mats(TestDescriptors tds, SuiteDescriptors sds, File dir, String test, String soaLabel, String jobId) {
    super(dir, test);
        this.setTestDescriptors(tds);
        this.setSuiteDescriptors(sds);
        this.setSuiteDesignation("MATS");
        this.setLabel(soaLabel);
        this.setJobId(jobId);
    }

    public String getLabel() {
        return label;
    } 

    public void setLabel(String label) {
        this.label = label;
    } 

    public String getJobId() {
        return jobId;
    } 

    public void setJobId(String jobId) {
        this.jobId = jobId;
    } 

    private String label;
    private String jobId;

    public static Mats newMats(TestDescriptors tds, SuiteDescriptors sds, String soaLabel, String dteJob) {
        Mats res = null;

        String testDirName = resultsDirectory + soaLabel;
        final String dottedDteJob = dteJob + ".";

        // try {
        File testDir = new File(testDirName);
        if (!testDir.exists()
            || !testDir.isDirectory() ) {
            System.out.println("SEVERE: Error triaging test run for " + soaLabel + " with DTE JobID " + dteJob + ": "
                           + " directory " + testDir + " does not exist or is not a directory.");
            return res;
        }

        FilenameFilter jobIdSuite = new FilenameFilter() {
            public boolean accept(File f, String s) {
                return s.startsWith(dottedDteJob);
            }
        };
          
        File[] suites = testDir.listFiles(jobIdSuite);

        if (suites != null && suites.length > 1) {
            System.out.print("SEVERE: Error triaging test run for " + soaLabel + " with DTE JobID " + dteJob + ": "
                         + " multiple directories " + testDirName + "/" + dottedDteJob + "* found: ");
            for (int i = 0; i < suites.length; i++) {
                System.out.print(suites[i].getName());
                if (i < suites.length - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println(". This should not occur.");
            return res;

        } else if (suites == null || suites.length == 0) {
            System.out.println("SEVERE: Error triaging test run for " + soaLabel + " with DTE JobID " + dteJob + ": "
                           + " directory " + testDirName + "/" + dottedDteJob + "* not found.");
            return res;

        } else if (!suites[0].isDirectory()) {
            System.out.println("SEVERE: Error triaging test run for " + soaLabel + " with DTE JobID " + dteJob + ": "
                           + suites[0] + " is not a directory. - Should not occur.");
            return res;
        }

        String testName = suites[0].getName().substring(dottedDteJob.length());
  
        res = new Mats(tds, sds, suites[0], testName, soaLabel, dteJob);
        return res;
    }

}
