package oracle.util.triage;

import java.net.*;
import java.io.*;
import java.util.*;
import java.lang.reflect.*;

import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.GZIPInputStream;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarEntry;

public class VirtualDir implements Constants {

  public static VirtualDir create(File f) {
      return VirtualDir.create(f, true);
  }

  public static VirtualDir create(File f, boolean farmProcessing) {
      String name = f.getName();
      if (f.isDirectory()) {

          File farmResults = new File(f.toString() + "/" + f.getName() + XML_SUFFIX);
          // /net/stansx04/farm_txn_results/SOA_MAIN_GENERIC_T4159567/4087046.MATS_JRF_FULL/4087046.MATS_JRF_FULL.tar.gz
          // /net/stansx04/farm_txn_results/SOA_MAIN_GENERIC_T4159567/4087046.MATS_JRF_FULL/4087046.MATS_JRF_FULL.work.tar.gz

          if (farmProcessing) {
              File compile = new File(farmResults.getParentFile().getParentFile().toString() + "/build/compile.log");

              if (farmResults.exists() || compile.exists()) {
                 System.out.println("INFO: This is a farm job. Results at: " + ((farmResults.exists()) ? farmResults : compile));
                 File workFile    = new File(f.toString() + "/" + WORKDIR_FILE);
                 File installFile = null;
                 if (!workFile.exists()) {
                     workFile = new File(f.toString() + "/" + f.getName() + ".work.tar.gz");
                     if (!workFile.exists()) {
                         workFile = null;
                         System.out.println("INFO: farm job has no work archive.");
                     } else {
                         System.out.println("INFO: DTE job with work archive: " + workFile);
                     }
                     installFile = new File(f.toString() + "/" + f.getName() + ".tar.gz");
                     if (!installFile.exists()) {
                         installFile = null;
                     } else {
                         System.out.println("INFO: DTE job with install archive: " + workFile);
                     }
                 } else {
                     System.out.println("INFO: farm run with work archive: " + workFile);
    
                 }
                 if (!farmResults.exists()) {
                     System.out.println("INFO: farm run has build issue.");
                 }
    
                 VirtualDir vd = new VirtualDir(f, FARM_TYPE);
                 vd.wrkFile  = workFile;
                 vd.instFile = installFile;
                 vd.farmXml   = farmResults;

                 if (compile.exists()) {
                     vd.compileFile   = compile;
                 }

                 return vd;
    
              } else {
                 return new VirtualDir(f, FILE_TYPE);
              }
          } else {
              return new VirtualDir(f, FILE_TYPE);
          }
      } else if (name.endsWith(".jar") || name.endsWith(".zip")) {
          return new VirtualDir(f, ZIP_TYPE);
      } else if (name.endsWith(".tar")) {
          return new VirtualDir(f, TAR_TYPE);
      } else if (name.endsWith(".tar.gz") || name.endsWith(".tgz")) {
          return new VirtualDir(f, COMPRESSED_TAR_TYPE);
      } else {
          throw new IllegalArgumentException(f + " must either be a directory, or " +
                                             "an archive, such as jtwork.jar or " + WORKDIR_FILE);
      }
  }

  private VirtualDir(File f, int typ) {
      baseFile = f;
      type = typ;
  }

  public static boolean isVirtualDir(File f) {
      return f.isDirectory()
          || f.getName().endsWith(".tar")
          || f.getName().endsWith(".tar.gz")
          || f.getName().equals(WORKDIR_FILE)
          || f.getName().equals("jtwork.jar")
          // || f.getName().equals("tmp.jar");
          || f.getName().endsWith(".jar")
          ;
  }

  public String getName() {
      return baseFile.getName();
  }

  public String getPath() {
      return baseFile.getPath();
  }

  public File getBaseFile() {
      return baseFile;
  }

  public int getType() {
      return type;
  }

  public File getFarmXml() {
      return farmXml;
  }

  public boolean isFarm() {
      return getType() == FARM_TYPE;
  } 

  public File getWorkdirFile() {
      return wrkFile;
  }

  public File getInstallFile() {
      return instFile;
  }

  public File getCompileFile() {
      return compileFile;
  }

  public List<String> getTestLogicFiles() {
      return testLogicFiles;
  }

  void register(VirtualFile vf) {
      if (vf.getName().equals("testlogic.html")) {
         String p = vf.getPath();
         String n = vf.getName();
         testLogicFiles.add( vf.getPath() + "/" + vf.getName() );
      }
  }

  private int type;
  private File baseFile;
  private File wrkFile;
  private File instFile;
  private File compileFile;
  private File farmXml;
  private List<String> testLogicFiles = new ArrayList<String>();

    
  public Iterator<VirtualFile> getFiles() throws IOException {
      return getFiles(null);
  }

  public Iterator<VirtualFile> getFiles(FilenameFilter filter) throws IOException {
      if (getType() == FILE_TYPE) {
          return new FileIterator(this, getBaseFile(), filter);
      } else if (getType() == FARM_TYPE) {
          // return new FarmIterator(this, getBaseFile(), getWorkdirFile(), filter);

          int sizeCount = 2 + ((getInstallFile()==null) ? 0 : 1) + ((getCompileFile()==null) ? 0 : 1);
          
          Iterator<VirtualFile>[] iterSequence = 
                 (Iterator<VirtualFile>[]) Array.newInstance(Iterator.class, sizeCount);
          File[] excludes = null;

          int idx = 0;
          iterSequence[idx++] = new FileIterator(this, getBaseFile(), filter);
          if (getCompileFile() != null) {
              iterSequence[idx++] = new SingleFileIterator(this, getCompileFile(), "/compile.log");
          }
          iterSequence[idx++] = new TarIterator(this, getWorkdirFile(), filter, true);
          if (getInstallFile() != null) {
              iterSequence[idx++] = new TarIterator(this, getInstallFile(), filter, true);
              excludes = new File[]{  getWorkdirFile(), getInstallFile(), };
          } else {
              excludes = new File[]{  getWorkdirFile(), getInstallFile(), };
          }

          return new SequenceIterator(iterSequence, excludes, filter);
      } else if (getType() == ZIP_TYPE) {
          return new ZipIterator(this, getBaseFile(), filter);
      } else if (getType() == TAR_TYPE) {
          return new TarIterator(this, getBaseFile(), filter, false);
      } else if (getType() == COMPRESSED_TAR_TYPE) {
          return new TarIterator(this, getBaseFile(), filter, true);
      } else {
          throw new IllegalArgumentException("Unknown file type: " + getType());
      }
  }

  private static class FileIterator implements Iterator<VirtualFile> {
      private FileIterator(VirtualDir vd, File f) {
          this(vd, f, null);
      }
      private VirtualDir virtualDir;

      private FileIterator(VirtualDir vd, File f, FilenameFilter filt) {
         virtualDir = vd;
         filter = filt;
         queue = new ArrayList<File>();
         if (f.isDirectory()
             || filter == null 
             || filter.accept(f, f.getName())) {
             queue.add(f);
         }
      }
      private FilenameFilter filter;
      private List<File> queue;

      public boolean hasNext() {
          updateQueue();
          return queue.size() > 0; 
      }

      public VirtualFile next() {
          if (hasNext()) {
              VirtualFile vf = new VirtualFile(virtualDir, queue.get(queue.size() - 1));
              queue.remove(queue.size() - 1);
              return vf;
          } else {
              return null;
          }
      }

      private void updateQueue() {
          // Invariant: at all times the queue contains
          //    <dir_1> ... <dir_n> <match-file_1> ... <match-file_n>

          while (queue.size() > 0) {
              File f = queue.get(queue.size() - 1);
              if (!f.isDirectory()) {
                  return;
              } 

              // Have: f == queue.get(queue.size()-1)
              //  and  f.isDirectory()

              queue.remove(queue.size() - 1);
              File[] files = f.listFiles();

              if (files != null) {
                  // First add all subdirectories
                  for (File file : files) {
                       if (file.isDirectory()
                           // && !file.getName().equals(".") && !file.getName().equals("..")
                           && !file.getName().startsWith(".")) {
                           queue.add(file);
                       }
                  }
    
                  // Next add all matching files
                  for (File file : files) {
                       if (!file.isDirectory()
                           && (filter == null
                               || filter.accept(file, file.getName()))) {
                           queue.add(file);
                       }
                  }
              }
          }
      }

      public void remove() {
          throw new UnsupportedOperationException("FileIterator::remove() not supported");
      }
  }

  private static class SingleFileIterator implements Iterator<VirtualFile> {
      private SingleFileIterator(VirtualDir vd, File f) {
          this(vd, f, null);
      }

      private SingleFileIterator(VirtualDir vd, File f, String nameToUse) {
          virtualDir = vd;
          theFile = f;
          this.nameToUse = nameToUse;
      }
      private VirtualDir virtualDir;
      private File theFile;
      private String nameToUse;

      public VirtualFile next() {
          if (hasNext()) {
              VirtualFile vf = new VirtualFile(virtualDir, theFile, nameToUse);
              theFile = null;
              return vf;
          } else {
              return null;
          }
      }

      public boolean hasNext() {
          return theFile != null;
      }

      public void remove() {
          throw new UnsupportedOperationException("FileIterator::remove() not supported");
      }
  }

  private static class TarIterator implements Iterator<VirtualFile> {
      // A version of TarInputStream that ignores close
      private static class MyTarInputStream extends TarInputStream {
         MyTarInputStream(InputStream is) {
             super(is);
         }

         public void close() throws IOException {
             close(false);
         }

         public void close(boolean force) throws IOException {
             if (force) {
                  super.close();
             }
         }
      }
    
      private TarIterator(VirtualDir vd, File base, boolean compression) throws IOException {
          this(vd, base, null, compression);
      }

      private TarIterator(VirtualDir vd, File base, FilenameFilter filt, boolean compression) 
          throws java.io.IOException { 
           virtualDir = vd;
           tarBase = base;
           filter = filt;
           tarInputStream = null;

           if (base == null || !base.exists()) {
               nextEntry = null;
           } else {
               tarInputStream = (compression)
                                ? new MyTarInputStream(new GZIPInputStream(new FileInputStream(tarBase)))
                                : new MyTarInputStream(new FileInputStream(tarBase));
               nextEntry = tarInputStream.getNextEntry();
               hasNext();
           }
      }
      private MyTarInputStream tarInputStream;
      private TarEntry nextEntry;
      private boolean nextEntryInUse = false;
      private FilenameFilter filter;
      private File tarBase;
      private VirtualDir virtualDir;
      
      public boolean hasNext() {
          try {
              if (tarInputStream == null) {
                  return false;
              }

              if (nextEntryInUse) {
                  nextEntryInUse = false;
                  nextEntry = tarInputStream.getNextEntry();
              }
              while (nextEntry != null && !match(nextEntry)) {
                  nextEntry = tarInputStream.getNextEntry();
              }
              if (nextEntry == null) {
                  tarInputStream.close(true);
              }
              return nextEntry != null;
          } catch (IOException ioe) {
              ioe.printStackTrace();

              System.out.println("SEVERE: exception " + ioe + " reading TAR file from " + tarBase);
              nextEntry = null;
              nextEntryInUse = false;
              return false;
          }
      }

      public VirtualFile next() {
          if (hasNext()) {
              TarEntry res = nextEntry;
              nextEntryInUse = true;
              return new VirtualFile(virtualDir, tarInputStream, res);
          } else {
              return null;
          }
      }

      private boolean match(TarEntry entry) {
          if (entry == null) {
              return false;
          }
          if (filter == null) {
              return true;
          }
          int pos = entry.getName().lastIndexOf("/");
          return (pos > 0) ? filter.accept(null, entry.getName().substring(pos + 1))
                         : filter.accept(null, entry.getName());
      }

      public void remove() {
          throw new UnsupportedOperationException("Tar::remove() not supported");
      }

  }

  private static class SequenceIterator implements Iterator<VirtualFile> {
      private SequenceIterator(Iterator<VirtualFile>[] sequence, File[] excludes) throws IOException {
          this(sequence, excludes, null);
      }

      private SequenceIterator(Iterator<VirtualFile>[] sequence, File[] excludes, FilenameFilter filt) 
          throws java.io.IOException { 
           outerFilter = filt;
           this.excludes = excludes;

           iterIndex = 0;
           iterSequence = sequence;
      }

      private FilenameFilter filter = 
          new FilenameFilter() {
               public boolean accept(File f, String s) {
                    if (excludes != null) {
                        for (File excl : excludes) {
                            if (s.equals(excl.getName())) {
                                return false;
                            }
                        }
                    }
                    if (outerFilter == null) {
                        return true;
                    }
                    return (outerFilter.accept(f, s));
                }
           };
      private FilenameFilter outerFilter;
      private File[] excludes = null;
      private int iterIndex;
      private Iterator<VirtualFile>[] iterSequence = null;
      // (Iterator<VirtualFile>[]) Array.newInstance(Iterator.class, 2);

      public boolean hasNext() {
          while (iterIndex < iterSequence.length) {
              if (iterSequence[iterIndex].hasNext()) {
                  return true;
              } else {
                  iterIndex++;
              }
          }
          return false;
      }

      public VirtualFile next() {
          if (hasNext()) {
              return iterSequence[iterIndex].next();
          } else {
              return null;
          }
      }

      public void remove() {
          throw new UnsupportedOperationException("SequenceIterator::remove() not supported");
      }
  }

  private static class ZipIterator implements Iterator<VirtualFile> {
      private ZipIterator(VirtualDir vd, File base) 
          throws IOException, ZipException {
          this(vd, base, null);
      }
      private ZipIterator(VirtualDir vd, File base, FilenameFilter filt)
          throws IOException, ZipException {
          filter = filt;
          virtualDir = vd;
          baseFile = base;
          zipFile = new ZipFile(base);
          zipEntries = zipFile.entries();
      }

      private FilenameFilter filter;
      private VirtualDir virtualDir;
      private ZipFile zipFile;
      private Enumeration zipEntries;
      private File baseFile;
      private ZipEntry nextEntry = null;

      public boolean hasNext() {
         while (nextEntry == null && zipEntries.hasMoreElements()) {
              ZipEntry ze = (ZipEntry) zipEntries.nextElement();
              if (filter == null
                  || filter.accept(new File(ze.getName()), ze.getName())) {
                  nextEntry = ze;
              }
         }
         return (nextEntry != null);
      }

      public VirtualFile next() {
          if (hasNext()) {
              VirtualFile vf = new VirtualFile(virtualDir, zipFile, nextEntry);
              nextEntry = null;
              return vf;
          } else {
              return null;
          }
      }

      public void remove() {
          throw new UnsupportedOperationException("ZipIterator::remove() not supported");
      }
  }

  public static void main(String[] args) throws Exception {
     File[] files = new File[args.length];
     for (int i = 0; i < args.length; i++) {
         File file = new File(args[i]);
         System.out.println("*** Listing of " + args[i] + " items. ***");
         VirtualDir vd = VirtualDir.create(file);
         Iterator<VirtualFile> it = vd.getFiles();
         while (it.hasNext()) {
             VirtualFile vf = it.next();
             // System.out.println(vf.getPath() + " --- " + vf.getName());
             System.out.println(vf.getCanonicalName() + "      ==> " + vf.getPath() + " --- " + vf.getName());
         }
     }
  }
}
