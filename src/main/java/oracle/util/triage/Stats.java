package oracle.util.triage;

import java.util.List;
import java.util.ArrayList;

public class Stats implements Constants {

    public Stats() {
        maxSlowCount = Main.getSlowest();
        totalTime = 0.0f;
        totalCount = 0;
        fastest = 0.0f;
        slowest = 0.0f;
        count = 0;
    }
    private int maxSlowCount = 0;
    private int totalCount = 0;
    private int count = 0;
    private float totalTime = 0.0f;
    private float fastest = 0.0f;
    private float slowest = 0.0f;
    private List<UnitTest> tests = new ArrayList<UnitTest>();


    void addSuite(UnitTestSuite uts) {
        numSuites++;
        if (maxSlowCount > 0) {
            for (UnitTest ut : uts.getTests()) {
                addTest(ut);
            }
        }
    }

    public boolean isSingleUnitTestSuite() {
        return numSuites==1;
    }
    int numSuites = 0;


    private void addTest(UnitTest ut) {
        totalCount++;
        float uTime = ut.getTime();

        if (uTime <= 0.0f) {
            return;
        }

        totalTime += uTime;

        if (count == 0) {
            fastest = uTime;
            slowest = uTime;
            tests.add(ut);
            count ++;
        } else if (count < maxSlowCount) {
            if (uTime < fastest) {
                fastest = uTime;
            } else if (slowest < uTime) {
                slowest = uTime;
            }
            tests.add(ut);
            count ++;
        } else if (fastest < uTime) {
            float newFastest = uTime;
            boolean inserted = false;

            if (slowest < newFastest) {
                slowest = newFastest;
            }

            for (int i=0; i<tests.size(); i++) {
                 float time = tests.get(i).getTime();
                 if (!inserted && time == fastest) {
                     inserted = true;
                     tests.set(i, ut);
                 } else if (time < newFastest) {
                     newFastest = time;
                 }
            }
            fastest = newFastest;
        }
    }


    public String toString() {
        StringBuffer sb = new StringBuffer();
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int i=0; i<tests.size()-1; i++) {
                 if (tests.get(i).getTime() < tests.get(i+1).getTime()) {           
                     UnitTest tmp = tests.get(i);
                     tests.set(i, tests.get(i+1));
                     tests.set(i+1, tmp);
                     sorted = false;
                  }
            }
        }

        int shownCount = 0;
        float shownTime = 0.0f;
       
        for (int i=0; i < tests.size(); i++) {
             UnitTest ut = tests.get(i);
             if ( MIN_TEST_TIME <= ut.getTime()
                  && (1.0f * (i+1)) / (1.0f * totalCount) <= MAX_TEST_PERCENTAGE ) {
                 shownCount++; shownTime += ut.getTime();
                 sb.append( "#" + (i+1) + " " + ut.getFullName() + ": " + ut.getTime() + " secs");
                 sb.append(NL);
             }
        }
        if (shownCount > 0) {
           return shownCount + " tests (of " + totalCount + ") account for " 
                  + (100.0f * shownTime / totalTime) + "% of the time" + NL +
                  sb.toString();
        } else {
           return null;
        }
    }
}
