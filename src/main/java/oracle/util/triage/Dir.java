package oracle.util.triage;

/**
 This class implements a diff analysis for MATs tests.
 */


public class Dir extends ResultsDir implements Constants {

    protected Dir(String dir, String title) throws Exception {
     super(dir, title);
        directory = dir;
    }

    Dir(String dir) throws Exception {
     this(dir, "DIRECTORY");
    }

    public String getDirectory() {
        return directory;
    }
    private String directory;

    public void triageResults() {
        try {
            super.triageResults();        
        } catch (Exception e) {
            System.out.println("SEVERE: error while triaging from " + getDirectory() + ": " + e);
            e.printStackTrace();
        }
    }

}
