package oracle.util.triage;

import java.util.Map;

/**
 This class implements a diff analysis for MATs tests.
 */
public class TWork extends Dir implements Constants {

    private TWork(String work) throws Exception {
     super(work, T_WORK);
        if (getDirectory() == null) {
            throw new Exception("SEVERE: $T_WORK environment variable is not defined.");
        }
    }

    public static TWork newTWork() {
        TWork tw = null;
        Map<String, String> map = System.getenv();
        try {
            tw = new TWork(map.get(T_WORK));
        } catch (Exception e) {
            System.out.println(e);
        }
        return tw;
    }
}
