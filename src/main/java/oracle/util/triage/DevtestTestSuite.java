package oracle.util.triage;

import junit.framework.TestSuite;

/**
 * Tests for the annotations.
 *
 * @author <a href="ekkehard.rohwedder@oracle.com">Ekkehard Rohwedder</a>
 */
public class DevtestTestSuite {


    public static TestSuite suite() {
        TestSuite testSuite = new TestSuite();
        testSuite.addTest( DevtestGzip.suite() );
        testSuite.addTest( DevtestJhttp.suite() );
        return testSuite;
    }

    /**
     * Runs the test suite using the textual runner.
     */
    public static void main(String[] args) {
        junit.textui.TestRunner.run(suite());
    }
}

