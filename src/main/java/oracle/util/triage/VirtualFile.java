package oracle.util.triage;

import java.net.*;
import java.io.*;
import java.util.*;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarEntry;

public class VirtualFile implements Constants {

  public VirtualFile(VirtualDir base, File file) {
      this(base, file, null);
  }

  public VirtualFile(VirtualDir base, File file, String nameToUse) {
      this.base = base;
      this.file = file;
      this.nameToUse = nameToUse;
      type = FILE_TYPE;
      initCheck();
  }

  public VirtualFile(VirtualDir base, TarInputStream tis, TarEntry entry)
  {
    this.base = base;
    is = tis;
    tarEntry = entry;
    type = TAR_TYPE;
    initCheck();
  }

  public VirtualFile(VirtualDir base, ZipFile zipFile, ZipEntry zipEntry)
  {
    this.base = base;
    this.zipFile = zipFile;
    this.zipEntry = zipEntry;
    type = ZIP_TYPE;
    initCheck();
  }


  public static void setNullElimination(boolean eliminate) {
      eliminateNulls = eliminate;
  }

  public static boolean getNullElimination() {
      return eliminateNulls;
  }
  private static boolean eliminateNulls = true;
 
 
 
  private File file;
  private String nameToUse;
  private TarEntry tarEntry;
  private VirtualDir base;
  private InputStream is;
  private ZipFile zipFile;
  private ZipEntry zipEntry;
  private int type;
  private String theCheck;
  private Long explicitLength;

  private void initCheck() {
      if ( (type == FILE_TYPE || type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) 
           && length() > MAX_INPUT_STREAM_SIZE ) {

          theCheck = "Original file size is " + length() + " (" + Util.makeFileSize(length()) + ") " +
                     " - only showing the initial " + Util.makeFileSize(MAX_INPUT_STREAM_SIZE);

          if (Main.isFull()) {
              String name = getFullName();
              int pos = name.indexOf("::");
              if (pos > 0) {
                  name = name.substring(0,pos) + "/" + name.substring(pos + "::".length());
              }

              if (!bigFiles.contains(name)) {
                  System.out.println("WARNING: " + name + ": " + theCheck);
                  bigFiles.add(name);
              }
          }

          explicitLength = new Long(MAX_INPUT_STREAM_SIZE);
      }
      base.register(this);
  }

  private static List<String> bigFiles = new ArrayList();

  public String getTheCheck() {
      return theCheck;
  }

  public void setLimitInputStream(boolean limit) {
      limitInputStream = limit;
  }
  private boolean limitInputStream = true;

  public String getFullName() {
      if (type == FILE_TYPE) {
          return (nameToUse == null) ? file.toString()
                                     : nameToUse;
      } else if (type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) {
          return base.getBaseFile().toString() + "::" + tarEntry.getName();
      } else if (type == ZIP_TYPE) {
          return base.getBaseFile().toString() + "::" + zipEntry.getName();
      } else {
          throw new UnsupportedOperationException("Unimplemented: getFullName()");
      }
  }

  public String getCanonicalName() {
      if (type == FILE_TYPE) {
          return getFullName();
      } else if (type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) {
          return base.getBaseFile().getParent().toString() + "/" + tarEntry.getName();
      } else if (type == ZIP_TYPE) {
          return base.getBaseFile().getParent().toString() + "/" + zipEntry.getName();
      } else {
          throw new UnsupportedOperationException("Unimplemented: getFullName()");
      }
  }

  public String getName() {
      if (type == FILE_TYPE) {
          if (nameToUse == null) {
              return file.getName();
          } else {
              int pos = nameToUse.lastIndexOf("/");
              return (pos < 0) ? nameToUse : nameToUse.substring(pos + 1);
          }
      } else if (type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) {
          String name = tarEntry.getName();
          int pos = name.lastIndexOf("/");
          if (pos > 0) {
              name = name.substring(pos + 1);
          }
          return name;
      } else if (type == ZIP_TYPE) {
          String name = zipEntry.getName();
          int pos = name.lastIndexOf("/");
          if (pos >= 0) {
              name = name.substring(pos + 1);
          }
          return name;
      } else {
          throw new UnsupportedOperationException("Unimplemented: getName()");
      }
  }

  public String getUrl() {
      return getFullName();
  }

  public String getPath() {
      if (type == FILE_TYPE) {
          if (nameToUse == null) {
              return file.getPath();
          } else {
              int pos = nameToUse.lastIndexOf("/");
              return (pos < 0) ? "" : nameToUse.substring(0, pos);
          }
      } else if (type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) {
          // return base.getName() + "::" + tarEntry.getName();
          String name = tarEntry.getName();
          int pos = name.lastIndexOf("/");
          return (pos > 0) ? name.substring(0,pos) : "";
      } else if (type == ZIP_TYPE) {
          // return base.getName() + "::" + tarEntry.getName();
          String name = zipEntry.getName();
          int pos = name.lastIndexOf("/");
          return (pos > 0) ? name.substring(0,pos) : "";
      } else {
          throw new UnsupportedOperationException("Unimplemented: getPath()");
      }
  }

  public String getParent() {
      if (type == FILE_TYPE) {
          if (nameToUse == null) {
              return file.getParent();
          } else {
              return getPath();
          }
      } else if (type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) {
          // return base.getName() + "::" + name;
          String name = tarEntry.getName();
          int pos = name.lastIndexOf("/");
          return (pos > 0) ? name.substring(0,pos) : "";
      } else if (type == ZIP_TYPE) {
          // return base.getName() + "::" + name;
          String name = zipEntry.getName();
          int pos = name.lastIndexOf("/");
          return (pos > 0) ? name.substring(0,pos) : "";
      } else {
          throw new UnsupportedOperationException("Unimplemented: getParent()");
      }
  }

  public long lastModified() {
      if (type == FILE_TYPE) {
          return file.lastModified();
      } else if (type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) {
          return tarEntry.getModTime().getTime();
      } else if (type == ZIP_TYPE) {
          return zipEntry.getTime();
      } else {
          throw new UnsupportedOperationException("Unimplemented: lastModified()");
      }
  }

  public long length() {
      if (explicitLength != null) {
          return explicitLength.longValue();
      } else if (type == FILE_TYPE) {
          return file.length();
      } else if (type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) {
          return tarEntry.getSize();
      } else if (type == ZIP_TYPE) {
          return zipEntry.getSize();
      } else {
          throw new UnsupportedOperationException("Unimplemented: length()");
      }
  }


  public InputStream getInputStream() throws IOException {
      if (type == FILE_TYPE) {
          if (is == null) {
              is = new BufferedInputStream(new FileInputStream(file), BUFF_LENGTH);
          }
          if (limitInputStream) {
              is = new LimitInputStream(is, MAX_INPUT_STREAM_SIZE);
          }
      } else if (type == TAR_TYPE || type == COMPRESSED_TAR_TYPE) {
          return (limitInputStream)
                  ? new LimitInputStream(is, MAX_INPUT_STREAM_SIZE)
                   : is;
      } else if (type == ZIP_TYPE) {
          int theSize = (int) ( (limitInputStream
                                 && zipEntry.getSize() > MAX_INPUT_STREAM_SIZE)
                                   ? MAX_INPUT_STREAM_SIZE
                                   : zipEntry.getSize());
          byte[] theData = new byte[theSize];
          int idx = 0;
          
          InputStream zis = zipFile.getInputStream(zipEntry);
          int lastRead = 0;
          while (lastRead >= 0 && (theData.length - idx) > 0) {
             lastRead = zis.read(theData, idx, theData.length - idx);
             if (lastRead >= 0) {
                idx += lastRead;
             }
          }
          zis.close();

          if (eliminateNulls) {
              // Cut off any NULL characters at the end of the content read.
              int origIdx = idx;
              for (int i=idx; 
                   0 < i 
                   &&  (theData[i-1] == 0                                       // eliminate ... \0
                        || (1 < i && theData[i-1] == 10 && theData[i-2] == 0)   // eliminate ... \0\n
                       ) ;
                   i--) {
                   idx = i-1;
              }
              if (idx != origIdx) {
                  explicitLength = new Long(idx);
                  System.out.println(getFullName() + ": removed " + (origIdx - idx) + " null trailing bytes and reset length to " + explicitLength);
              }
          }

          // Only return those byteas that we did read.
          is = new ByteArrayInputStream(theData, 0, idx);
      } else {
          // return null;
          is = null;
      }
      return is;
  }

  public void close() throws IOException {
      if (type == FILE_TYPE 
          || type == ZIP_TYPE) {
          if (is != null) {
              try {
                   is.close();
              } catch (IOException ioe) {
                   // ignore
              }
          }
      } else if (type == TAR_TYPE ||  type == COMPRESSED_TAR_TYPE) {
          // Nothing to do
      } else {
      }
  }

}
