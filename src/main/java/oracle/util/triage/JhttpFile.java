package oracle.util.triage;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class JhttpFile implements Constants {

    private static final String INDEX_FILE = "index.html";
    private static final int MAX_RESOURCE_SIZE = 50000; // max size of a resource file (in bytes)
    private static long RESOURCE_EXPIRATION_TIME = 60 * 60 * 24 * 7; // Resources expire in 7 days
  
    public JhttpFile(String fName) {
        fileName = fName;

        // Compute range, if any
        checkRange();
        checkResource();
  
        if (fileName.endsWith("/")) {
            fileName += INDEX_FILE;
        }
        // String fileDate = "Tue, 01 Jan 2008 12:00:00 GMT";
 
        if (isResource()) {
            // nothing else to do
        } else {
            if (isZipElement(fileName)) {
                int pos = fileName.indexOf("::");
                zipFileElement = fileName.substring(pos + "::".length());
                fileName = fileName.substring(0,pos);
            } else if (lastZipFilePrefix != null && fileName.startsWith(lastZipFilePrefix)) {
                zipFileElement = lastZipDir + fileName.substring(lastZipFilePrefix.length() - 1);
                fileName = lastZipFile;
            } else if (Jhttp.isZipFile(fileName)) {
                zipFileElement = fileName.substring(0,fileName.length() - 4);
                int pos = zipFileElement.lastIndexOf("/");
                if (pos > 0) {
                    zipFileElement = zipFileElement.substring(pos);
                }
                pos = zipFileElement.lastIndexOf("_"); 
                zipFileElement = zipFileElement.substring(0,pos) + "." + zipFileElement.substring(pos + 1);

                // now start mapping future requests as follows:
                //    foo/bar_files/bas
                //    --------------      <-- lastZipFilePrefix
                // to
                //    foo/bar_htm.zip::/bar_files/bas
                //    ---------------     <-- lastZipFile
                //                     --------- <-- lastZipDir
                // NOTE: this will not work properly in a multi-threaded / multi-client environment
                lastZipFile = fileName;
                pos = fileName.lastIndexOf("_"); 
                lastZipFilePrefix = fileName.substring(0,pos) + "_files";
                pos = lastZipFilePrefix.lastIndexOf("/");
                lastZipDir = lastZipFilePrefix.substring(pos);
                lastZipFilePrefix = lastZipFilePrefix + "/";
            }
    
            if (fileName.startsWith("/adedescribetrans")) {
                String option = (fileName.startsWith("/adedescribetrans/"))
                                ? "-short " : "";;

                String txn = fileName.substring("/adedescribetrans".length());
                int pos = txn.indexOf("/");
                if (pos >= 0) txn = txn.substring(pos + 1);

                String delims="/ \t'\"\\<>&`%";
                for (int i=0; i<delims.length(); i++) {
                    pos = txn.indexOf(delims.substring(i,i+1));
                    if (pos>=0) {
                       txn = txn.substring(0, pos);
                    }
                }

                fileName = "/tmp/adedescribetrans.txt";
                String[] command = new String[] { "sh", "-c",
                                                  "ade describetrans " + option + txn + " >& " + fileName };
                try {
                   System.out.println("INFO: executing command: " + command[2]);
                   ProcessBuilder pb = new ProcessBuilder(command);
                   pb.directory(new File("/tmp"));
                   Process p = pb.start();
                   BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
                   String line = null;
                   while ((line = in.readLine()) != null) {
                       System.out.println("STDOUT: " + line);
                   }
                   in.close();
                   p.waitFor();
                } catch (Exception e) {
                   System.out.println("ERROR: while executing command: " + e);
                }
            } else if (!isAbsolute(fileName)) {
                fileName = docroot + "/" + fileName.substring(1,fileName.length());
            }

            theFile = new File(unescape(fileName));

            File tmpFile = null;
            if (zipFileElement == null 
                && (tmpFile = new File(fileName + ".gz")).exists()) {
                theFile = tmpFile;
                gzipped = true;
            } 

            if (!theFile.exists()) {
                String undup = removeDupSegments(fileName);
                if (undup!=null) {
                    if ( (tmpFile = new File(undup)).exists() ) {
                       System.out.println("INFO: remapped " + fileName + " to " + undup);
                       theFile = tmpFile;
                    } else if ( (tmpFile = new File(undup + ".gz")).exists() ) {
                       System.out.println("INFO: remapped " + fileName + " to " + undup + ".gz");
                       theFile = tmpFile;
                       gzipped = true;
                    }  
                }
            }

                   
            fileDate = (new Date(theFile.lastModified())).toString();
        }
    }

    public String getFileDate() {
        return fileDate;
    }
    public String getExpires() {
        return expires;
    }


    // Remove the first immediate duplication of a path segment: 
    //      bar/foo/bar/bar/bar/baz/baz
    //                  ^^^
    //  ==> bar/foo/bar/bar/baz/baz
    //  Return the new path and null if nothing was removed.

    private static String removeDupSegments(String path) {
        StringBuffer sb = new StringBuffer();
        int idx = 0;  // search from here
        int pos = 0;
        String lastSegment = "";   // last path segment seen
        boolean addSlash = false;  // add slash after each segment, except the first one
        while ((pos = path.indexOf("/", idx)) >= 0) {
           String segment = path.substring(idx, pos);
           // System.out.println("  lastSegment=\"" + lastSegment + "\" segment=\"" + segment + "\" idx=" + idx + " pos="+pos);
           if (!lastSegment.equals("") && lastSegment.equals(segment)) {
              // convert  foo/bar/bar/baz  to  foo/bar/baz  and return
              sb.append(lastSegment);
              sb.append("/");
              sb.append(path.substring(pos+1));
              return sb.toString();
           }
           sb.append(lastSegment);
           if (addSlash) {
               sb.append("/");
           }
           lastSegment = segment;
           addSlash = true;
           idx=pos+1;           // search beyond the slash that we found
        }
        return null;  // we could not remove a duplicate path segment
    }


    private String zipFileElement = null;
    private String fileDate = null;
    private String expires = null;
    private boolean gzipped = false;  // Do we have gzipped content representation?

    private static String lastZipFile = null;
    private static String lastZipFilePrefix = null;
    private static String lastZipDir = null;

    /**
     Does the client for this file accept gzipped content?
     **/
    public void hasAcceptsGzip(boolean acceptGzip) {
        this.acceptGzip = acceptGzip;
    }
    private boolean acceptGzip = false;

    public boolean isGzipped() {
        return gzipped           // this file was gzipped in the first place
               && acceptGzip     // the browser clientaccepts gzipped files
               && !hasRange()    // and we will not return a line range for this file
               ;
    }
    

    public byte[] getByteArray() {
      byte[] theData = new byte[]{};
      try {
        if (isResource()) {
            return getResource();
        } else if (theFile == null) {
            theOffset = 0;
            theSize = theData.length;
            return theData;
        } else {
              // Read bytes into output buffer
              if (zipFileElement != null) {
                  zipFileElement = unescape(zipFileElement);
                  ZipFile zipFile = new ZipFile(theFile);
                  ZipEntry zipEntry = zipFile.getEntry(zipFileElement);

                  if (zipEntry == null && zipFileElement.startsWith("/")) {
                      zipEntry = zipFile.getEntry(zipFileElement.substring(1));
                  }
                  if (zipEntry == null) {
                      System.out.println("SEVERE: Error retrieving " + zipFileElement + ": entry not found in " + theFile + "!");
                      theOffset = 0;
                      theSize = 0;
                      return null;
                  }

                  theData = new byte[(int) zipEntry.getSize()];
                  int idx = 0;
                  int lastRead = 0;
                  InputStream zis = zipFile.getInputStream(zipEntry);
                  while (lastRead >= 0 && (theData.length - idx) > 0) {
                     lastRead = zis.read(theData, idx, theData.length - idx);
                     if (lastRead >= 0) {
                         idx += lastRead;
                     }
                  }
                  zis.close();
              } else {
                  if (gzipped && !isGzipped()) {
                      // need to expand the gzipped file
                      theData = Gzip.gunzip(theFile);
                  } else {
                      // read the file straight in (zipped or not)
                      FileInputStream fis = new FileInputStream(theFile);
                      theData = new byte[(int) theFile.length()];

                      // TODO: to check the number of bytes read here
                      fis.read(theData);
                      fis.close();
                  }
              }
    
              // Extract a range of lines for output - if necessary
              int offset = 0;
              int len = theData.length;
              if (hasRange()) {
                  len = 0;
                  offset = detectLines(theData, offset, startRange - 1);
                  if (offset < 0) {
                      offset = 0;
                  } else {
                      int nextOffset = detectLines(theData, offset, endRange - startRange + 1);
                      if (nextOffset < 0) {
                          nextOffset = theData.length;
                      }
                      len = nextOffset - offset;
                      if (len < 0) {
                          len = 0;
                      }
                  }
              }

              theOffset = offset;
              theSize = len;
              return theData;
         }
      } catch (IOException ioe) {
          System.out.println("SEVERE: Error retrieving " + fileName + ": " + ioe);
          theOffset = 0;
          theSize = 0;
          return null;
      }
    }

    public int getOffset() {
        return theOffset;
    }
    private int theOffset = 0;

    public int getSize() {
        return theSize;
    }
    private int theSize = 0;


    public static void setDocroot(String root) {
        docroot = new File(root);
        if (!docroot.exists()
            || !docroot.isDirectory()) {
            Messages.explain("Must be a directory: " + root);
        }
    }
    public static File getDocroot() {
        return docroot;
    }
    private static File docroot;
  
    
    private boolean isResource() {
        return resource;
    }
    private boolean resource = false;
    
    private void checkResource() {
       resource = false;
       if (fileName.startsWith("/jscript/")) {
           for (int i = 0; i < HTML_RESOURCES.length; i++) {
               if (fileName.equals(HTML_RESOURCES[i])) {
                   resource = true;
                   return;
               }
           }
       }
    }

    private byte[] getResource() throws IOException {
        byte[] buf = new byte[MAX_RESOURCE_SIZE];
        expires = Util.toDateTime((new Date().getTime()) + 1000 * RESOURCE_EXPIRATION_TIME);
        fileDate = "Fri, 01 Jan 2010 12:00:00 GMT";
        InputStream ins = JhttpFile.class.getResourceAsStream(fileName);

        if (ins == null) {
            System.out.println("SEVERE: resource " + fileName + " not found!");
            theOffset = 0;
            theSize = 0;
            return null;
        } else {
            int by = 0;
            int count;
            for (count = 0; count < buf.length && by >= 0;) { 
                 by = ins.read();
                 if (by >= 0) {
                     buf[count] = (byte) by;
                     count++;
                 }
            }
            ins.close();
            theOffset = 0;
            theSize = count;
            return buf;
        }
    }
 
    private static boolean isAbsolute(String s) {
       for (int i = 0; i < ABSOLUTE_DIR_PREFIXES.length; i++) {
            if (s.startsWith(ABSOLUTE_DIR_PREFIXES[i])) {
                return true;
            }
       }
       return false;
    }
  
    private static boolean isZipElement(String s) {
       for (int i = 0; i < ZIP_INFIXES.length; i++) {
            if (s.indexOf(ZIP_INFIXES[i]) > 0) {
                return true;
            }
       }
       return false;
    }


    public String getFileName() {
        return fileName;
    }
    private String fileName;

    private File theFile = null;
    byte[]  theData = null;

    /** Detect numLines lines in the byte buffer starting from index offset.
        Return the buffer index position _after_ the detected lines.
        Return -1 if we get to or past the end of the buffer.
     **/
    private static int detectLines(byte[] buf, int offs, int numLines) {
       byte cr = (byte)'\r';  byte lf = (byte)'\n';
  
       byte b = ' ';
       for (int i = 1; i <= numLines; i++) {
           while (offs < buf.length
                  && (b = buf[offs]) != cr
                  && b != lf) {
               offs++;
           }
  
           // Detect a crlf sequence
           if (b == cr
               && offs + 1 < buf.length
               && buf[offs + 1] == lf) {
               offs++;
           }
  
           // Move to next character
           offs++;
  
           if (offs >= buf.length) {
               return -1;
           }
       }
       return offs;
    }


    private static int readNatural(String s) {
        if (s == null || s.trim().equals("")) {
            return -1;
        }
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return -1;
        }
    }
  
    public String getContentType() {

        String name = (zipFileElement != null)
                       ? zipFileElement.toLowerCase()
                       : fileName.toLowerCase();

        int pos = name.indexOf("#");
        if (pos > 0) {
            name = name.substring(0, pos);
        }

        if (name.endsWith(".html")
            || name.endsWith(".htm")
            || name.endsWith(".shtml")) {
            return "text/html";
        } else if (name.endsWith(".gif")) {
            return "image/gif";
        } else if (name.endsWith(".class")) {
            return "application/octet-stream";
        } else if (name.endsWith(".jpg")
                   || name.endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (name.endsWith(".pdf")) {
            return "application/pdf";
        } else {
            // name.endsWith(".txt")
            // || name.endsWith(".java")
            // || name.endsWith(".suc")
            // || name.endsWith(".dif")
            // || name.endsWith(".log")
            // etc. etc.
            return "text/plain";
        }
    }

    private boolean hasRange() {
        return startRange >= 1;
    }

    private int startRange = -1;
    private int endRange = -1;

    private void checkRange() {
        if (fileName.indexOf("?rg=") > 0) {
            int pos = fileName.indexOf("?rg=");
            String range = fileName.substring(pos + "?rg=".length());
            fileName = fileName.substring(0,pos);
            pos = range.indexOf("-");
            if (pos >= 0) {
                startRange = readNatural(range.substring(0,pos));
                if (startRange < 0) {  // support: ?rg=-1234
                    startRange = 1;
                }
                endRange = readNatural(range.substring(pos + 1));
                if (endRange < 0) {  // support: ?rg=1234-
                    endRange = Integer.MAX_VALUE;
                }
             } else {
                startRange = readNatural(range);
                endRange = startRange;
             }
        }
    } 


    private static String unescape(String s) {
        if (s.indexOf("%20") >= 0) {
            return s.replaceAll("%20"," ");
        } else {
            return s;
        }
    }


    public static void main(String[] args) {
        for (int i=0; i<args.length; i++) {
            System.out.println(args[i] + " =undup=> " + removeDupSegments(args[i]));
        }
    }

}
