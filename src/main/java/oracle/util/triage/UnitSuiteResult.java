package oracle.util.triage;

import java.io.*;

/** Representation of a TEST-*.xml suite segment.
 */

public class UnitSuiteResult extends DatedFile implements Constants {

    public UnitSuiteResult(UnitTestSuite uts, long startTime) {
        super( (uts.getName() == null) ? "** unit suite result**" : uts.getName(), startTime, startTime + Util.millis(uts.getTime()));
 
        float time = 0.0f;
        UnitTestResults utr = new UnitTestResults(uts.getTests(),
                                                  getStartTime(),
                                                  getStartTime() + Util.millis(uts.getTime()));
        addChild(utr);
    }

    public boolean isTest() {
        return true;
    }

    public float getTestTime() {
        return getTime();
    } 

}
