package oracle.util.triage;

import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.*;


public class DevtestJhttp extends TestCase {

    public DevtestJhttp(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return new TestSuite(DevtestJhttp.class);
    }


    private static final File TEMP_DIR = new File("tmp");
    private static File LOG_FILE = new File(TEMP_DIR, "test.log");
    private static String LOG_FILE_URL = "/test.log";
    private static String[] LOG_FILE_CONTENT = new String[] {
        "", 
        "1 Line 1\n" ,
        "\n" ,
        "3 the third line\r\n" ,
        "4 #four\r\n" ,
        "5 the fifth\r" ,
        "6 the sizth\r" ,
        "\r" ,
        "8 number eigth\n" ,
        "\r\n" ,
        "TEN\n" ,
        "ELEVEN" ,
    };
    private static File EMPTY_FILE = new File(TEMP_DIR, "empty.log");
    private static String EMPTY_FILE_URL = "/empty.log";
    private static String EMPTY_FILE_CONTENT = "";

    private static void makeFile(File f, String[] content) throws Exception {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < content.length; i++) {
            sb.append(content[i]);
        };
        makeFile(f, sb.toString());
    }
    
    private static void makeFile(File f, String content) throws Exception {
        FileOutputStream fos = new FileOutputStream(f);

        for (int i = 0; i < content.length(); i++) {
            fos.write((byte) content.charAt(i));
        }
        fos.close();
    }

    private void init() throws Exception {
       if (!initialized) {
           TEMP_DIR.mkdir();
           makeFile(LOG_FILE, LOG_FILE_CONTENT);
           makeFile(EMPTY_FILE, EMPTY_FILE_CONTENT);

           JhttpFile.setDocroot(TEMP_DIR.toString());
       };
       initialized = true;
    }
    private boolean initialized = false;

    private void checkRange(String range, String expected) throws Exception {
        JhttpFile jf = new JhttpFile(LOG_FILE_URL + "?rg=" + range);
        byte[] res = jf.getByteArray();
        StringBuffer sb = new StringBuffer();
        int size = jf.getSize(); 
        int offset = jf.getOffset(); 
        for (int i = offset; i < offset + size; i++) {
             sb.append((char) res[i]);
        }
        String message = "Checked " + LOG_FILE_URL + "?rg=" + range + " with expected '" + expected + "', actual '" + sb.toString() + "'";
        assertEquals(message + " - different lengths", expected.length(), size);
        for (int i = offset; i < offset + size; i++) {
           int idx = i - offset;
           assertEquals(message + " - different content at index " + idx, (byte) expected.charAt(idx), res[i]);
        }
    }

    private String getLogFileRange(int start, int end) {
        StringBuffer sb = new StringBuffer();
        for (int i = start; i <= end; i++) {
            if (i < LOG_FILE_CONTENT.length) {
                sb.append(LOG_FILE_CONTENT[i]);
            }
        }
        return sb.toString();
    }

    public void testRangeIndividualLines() throws Exception {
        init();
        for (int i = 1; i < LOG_FILE_CONTENT.length; i++) {
             checkRange("" + i, LOG_FILE_CONTENT[i]);
        }
    }

    public void testRangeMultipleLines() throws Exception {
        init();
        for (int i = 1; i <= LOG_FILE_CONTENT.length; i++) {
            checkRange("" + i + "-" + (i + 3), getLogFileRange(i, i + 3));
        }
    }
}
