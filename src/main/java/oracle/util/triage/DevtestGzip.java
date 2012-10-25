package oracle.util.triage;

import junit.framework.TestSuite;
import junit.framework.TestCase;

import java.io.*;


public class DevtestGzip extends TestCase {

    public DevtestGzip(String name) {
        super(name);
    }

    public static TestSuite suite() {
        return new TestSuite(DevtestGzip.class);
    }

    public void testFile() throws Exception {
        String tf = "testFile.txt";
        String brownFox = "The brown fox jumped over the lazy dog.\n";
        brownFox = brownFox + brownFox + brownFox + brownFox;
        brownFox = brownFox + brownFox + brownFox + brownFox;
        brownFox = brownFox + brownFox + brownFox + brownFox;
        brownFox = brownFox + brownFox + brownFox + brownFox;
        byte[] buf = stringToByteArray(brownFox);

        makeGzipfile(tf, buf);
        checkGzipfile(tf + ".gz", buf);

        (new File(tf)).delete();
        (new File(tf + ".gz")).delete();
    }

    public void testEmptyFile() throws Exception {
        String tf = "testFile2.txt";
        String brownFox = null;
        byte[] buf = stringToByteArray(brownFox);

        makeGzipfile(tf, buf);
        checkGzipfile(tf + ".gz", buf);

        (new File(tf)).delete();
        (new File(tf + ".gz")).delete();
    }

    private void makeGzipfile(String fileName, byte[] content) throws Exception {
        FileOutputStream fos = new FileOutputStream(fileName);
        fos.write(content);
        fos.close();
        Gzip.gzip(new File(fileName));
    }

    private void checkGzipfile(String fileName, byte[] content) throws Exception {
        byte[] unzipped = Gzip.gunzip(new File(fileName));
        assertEquals("Returned size differs from expected size after decompression", content.length, unzipped.length);
        for (int i = 0; i < content.length; i++) {
            assertEquals("Byte at position " + i + " differs after decompression", content[i], unzipped[i]);
        }
    }
  
    private static byte[] stringToByteArray(String s) {
        if (s == null) {
            return new byte[]{};
        } else {
            byte[] res = new byte[s.length()];
            for (int i = 0; i < s.length(); i++) {
                res[i] = (byte) s.charAt(i);
            }
            return res;
        }
    }
}
