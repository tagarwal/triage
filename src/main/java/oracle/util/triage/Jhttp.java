package oracle.util.triage;

import java.net.*;
import java.io.*;
import java.util.*;
import java.util.zip.*;

public class Jhttp extends Thread implements Constants {

  private static final boolean DEBUG = false;

  Socket theConnection;
  
  public Jhttp(Socket s) {
    theConnection = s;
  }

  private static boolean isResource(String s) {
     if (!s.startsWith("/jscript/")) {
         return false;
     }
     for (int i = 0; i < HTML_RESOURCES.length; i++) {
         if (s.equals(HTML_RESOURCES[i])) {
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

  static boolean isZipFile(String s) {
      return s.endsWith("_htm.zip")
          || s.endsWith("_html.zip");
  }

  public static void main(String[] args) {

    if (args.length != 2) {
        Messages.explainHttpd("SEVERE: Usage: -httpd <directory> <port>  - for example: -httpd . 8080\n");
        return;
    }

    int thePort = -1;
    ServerSocket ss;

    // get the Document root
    try {
      JhttpFile.setDocroot(args[0]);
    } catch (Exception e) {
      Messages.explain("Usage: -httpd <directory> <port>");
    }
    
    // set the port to listen on
    try {
      thePort = Integer.parseInt(args[1]);
      if (thePort < 0 || thePort > 65535) {
          Messages.explain("Must be a port: " + args[1]);
      }
    }  
    catch (Exception e) {
      Messages.explain("Must be a port: " + args[1]);
    }  
                
    try {
      ss = new ServerSocket(thePort);
      System.out.println("Accepting connections on port " 
        + ss.getLocalPort());
      System.out.println("Document Root:" + JhttpFile.getDocroot());
      while (true) {
        Jhttp j = new Jhttp(ss.accept());
            j.start();
      }
    }
    catch (IOException e) {
      Messages.explain("Server aborted prematurely: " + e);
    }
  
  }

  private static long EXPIRATION_TIME = 60 * 60 * 24 * 7; // Resources expire in 7 days

  public void run() {
  
    String method;
    String ct;
    String version = "";
    File theFile = null;
    long timestamp = System.currentTimeMillis();
    Date now = new Date();
    try {
      DataInputStream is = new DataInputStream(theConnection.getInputStream());
      String get = is.readLine();
      String ucGet = null;
      if (DEBUG) {
          System.out.println("REQUEST: " + get);
      }

      if (get != null) {
          PrintStream os = new PrintStream(theConnection.getOutputStream());
          String agent = null;
          StringBuffer sout = new StringBuffer();
    
          // Date has format:   Day Mmm dd hh:mm:ss yyyy
          StringTokenizer dt = new StringTokenizer( (new Date()).toString() );
          String[] da = new String[6];
          int i = 0;
          while (dt.hasMoreElements())
          { da[i] = (String) dt.nextElement(); i++; }
    
          sout.append(theConnection.getInetAddress());
          sout.append(" - - [");
          sout.append(da[2]);
          sout.append("/");
          sout.append(da[1]);
          sout.append("/");
          sout.append(da[4]);
          sout.append(":");
          sout.append(da[3]);
          sout.append("] \"");
          sout.append(get.trim());
          sout.append("\" ");

          StringTokenizer st = new StringTokenizer(get);
          method = st.nextToken();
          if (method.equals("GET")) {
              String file = st.nextToken();
    
              JhttpFile jf = new JhttpFile(file);

              if (st.hasMoreTokens()) {
                  version = st.nextToken();
              }

              // loop through the rest of the input lines 
              while ((get = is.readLine()) != null) {
                  if (DEBUG) {
                      System.out.println("       > " + get);
                  }
                  get = get.trim();
                  ucGet = get.toUpperCase();
                  if (ucGet.startsWith("USER-AGENT: ")) {
                      agent = get.substring(12);
                  } else if (ucGet.startsWith("ACCEPT-ENCODING: ")) {
                      if (ucGet.indexOf("GZIP") > 0) {
                          jf.hasAcceptsGzip(true);
                      }
                  }
                  if (get.equals("")) break;        
              }
    
                  byte[] theData = jf.getByteArray();

                  if (theData != null) {
                      if (version.startsWith("HTTP/")) {  // send a MIME header
                          // os.print("HTTP/1.0 200 OK\r\n"); 
                          os.print("HTTP/1.1 200 OK\r\n");
                             if (DEBUG) { sout.append("HTTP/1.1 200 OK\n"); }
                          if (jf.getFileDate() != null) {
                              os.print("Date: " + jf.getFileDate() + "\r\n");
                              os.print("Last-Modified: " + jf.getFileDate() + "\r\n");
                              if (DEBUG) {
                                  sout.append("Date: " + jf.getFileDate() + "\n");
                                  sout.append("Last-Modified: " + jf.getFileDate() + "\n");
                              }
                          } else {
                              os.print("Date: " + now + "\r\n");
                              if (DEBUG) { sout.append("Date: " + now + "\n"); }
                          }
                          if (jf.getExpires() != null) {
                              os.print("Expires: " + jf.getExpires() + "\r\n");
                              if (DEBUG) { sout.append("Expires: " + jf.getExpires() + "\n"); }
                          }
                          os.print("Server: Jhttp 1.0\r\n");
                             if (DEBUG) { sout.append("Server: Jhttp 1.0\n"); }
                          os.print("Connection: close\r\n");
                             if (DEBUG) { sout.append("Connection: close\n"); }
                          if (jf.isGzipped()) {
                              os.print("Content-encoding: gzip\r\n");
                              if (DEBUG) { sout.append("Content-encoding: gzip\n"); }
                          }
                          os.print("Content-length: " + jf.getSize() + "\r\n");
                              if (DEBUG) { sout.append("Content-length: " + jf.getSize() + "\n"); }
                          os.print("Content-type: " + jf.getContentType() + "\r\n\r\n");
                              if (DEBUG) { sout.append("Content-type: " + jf.getContentType() + "\n"); }
                      }
                    
                      // send the file (or a portion of it)
                      os.write(theData, jf.getOffset(), jf.getSize());
                      os.close();
                      sout.append("200 ");
                      sout.append(jf.getSize());
                 } else {
                    if (version.startsWith("HTTP/")) {  // send a MIME header
                        os.print("HTTP/1.0 404 File Not Found\r\n");
                        // os.print("HTTP/1.1 404 File Not Found\r\n");
                        os.print("Date: " + now + "\r\n");
                        os.print("Server: Jhttp 1.0\r\n");
                        os.print("Content-type: text/html" + "\r\n\r\n");
                    } 
                    os.println("<HTML><HEAD><TITLE>File Not Found</TITLE></HEAD>");
                    os.println("<BODY><H1>HTTP Error 404: File Not Found</H1></BODY></HTML>");
                    os.close();
                    sout.append("404 File Not Found");
                }
       } else {  // method does not equal "GET"
              if (version.startsWith("HTTP/")) {  // send a MIME header
                os.print("HTTP/1.0 501 Not Implemented\r\n");
                os.print("Date: " + now + "\r\n");
                os.print("Server: Jhttp 1.0\r\n");
                os.print("Content-type: text/html" + "\r\n\r\n"); 
              }       
              os.println("<HTML><HEAD><TITLE>Not Implemented</TITLE></HEAD>");
              os.println("<BODY><H1>HTTP Error 501: Not Implemented</H1></BODY></HTML>");
              os.close();
              sout.append("501 Not Implemented");
       }
       if (agent != null) {
              sout.append(" ");
              sout.append(agent);
       }
       sout.append(" ");
       sout.append( (System.currentTimeMillis() - timestamp) );
       sout.append("mS");
       System.out.println(sout.toString());
    }
  } catch (IOException ioe) {
    System.out.println("WARNING: error when serving request: " + ioe);
  }
    try {
        theConnection.close();
    } catch (IOException e) {
        System.out.println("WARNING: error when closing connection: " + e);
    }
  }

}
