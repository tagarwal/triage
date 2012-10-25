package oracle.util.triage;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Gzip implements Constants {

  private static final int     BUF_SIZE = 8192;
  private static final boolean DEBUG = true;

   /**
    Gzip the input file to a file with the same file name with .gz appended and
    remove the original file if the gzipping was successful.
    **/
  public static boolean gzip(File infile) {
  
        File outfile = new File (infile.getParent(), infile.getName() + ".gz");
        GZIPOutputStream gos = null;
        boolean success = true;
    
        try {
            gos = new GZIPOutputStream 
                      (new BufferedOutputStream
                          (new FileOutputStream(outfile)));
    
            byte[] buf = new byte[BUF_SIZE];
            int len = 0;
    
            BufferedInputStream bis =
                new BufferedInputStream(new FileInputStream(infile), BUF_SIZE);
    
            while ((len = bis.read(buf, 0, BUF_SIZE)) != -1) {
                gos.write(buf, 0, len);
            }
            bis.close ();
        } catch (IOException ioe) {
            success = false;
            System.out.println("SEVERE: error gzipping " + infile + ": " + ioe);
        } finally {
           if (gos != null) {
               try {
                   gos.close ();
               } catch (IOException ioe) {
                   success = false;
               }
           } else {
               success = false;
           }
        }
        if (success) {
            infile.delete();
            return true;
        }
        return false;
    }


    /**
     Gunzip the input file into a byte array and return that.
     Returns null when the gunzipping did not work.
     **/
    public static byte[] gunzip(File infile) {
        GZIPInputStream gis = null;
        try {
            gis = new GZIPInputStream
                      (new BufferedInputStream
                           (new FileInputStream(infile)));
     
            ByteArrayOutputStream bos = new ByteArrayOutputStream(BUF_SIZE);
    
            byte[] buf = new byte[BUF_SIZE];
            int len = 0;
    
            while ((len = gis.read(buf, 0, BUF_SIZE)) != -1) {
                bos.write(buf, 0, len);
            }
            bos.close();
            return bos.toByteArray();
        } catch (IOException ioe) {
            System.out.println("SEVERE: unable to gunzip " + infile + ": " + ioe); 
            return null;
        } finally {
            if (gis != null) {
                try {
                    gis.close();
                } catch (IOException ioe) {
                    // ignore;
                }
            }
        }
    }
}
