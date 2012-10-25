package oracle.util.triage;

import java.util.Iterator;

import java.io.IOException;
import java.io.File;
import java.io.InputStream;
import java.io.FileOutputStream;

import java.util.jar.JarOutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipException;

/**
 This class implements a diff analysis for MATs tests.
 */


public class Farm extends Dir implements Constants {

    protected Farm(String dir, String title) throws Exception {
        super(dir, title);
    }

    Farm(String dir) throws Exception {
         this(dir, "FARM");
    }

    private static boolean DEBUG = false;

    public static void createJarFile(String workdirName) throws IOException {

/*****************
        String baseDirectory = null;
        boolean secondAttempt = false;

        File workdir = null;
        File jtwork = new File(workdirName + "/" + "jtwork");

        File workdir2 = new File(workdirName + "/" + WORKDIR_FILE);
        File workdir3 = new File(workdirName + "/" + workdirName.substring(workdirName.lastIndexOf("/")+1) + WORKDIR_SUFFIX);

        if (jtwork.exists() && jtwork.isDirectory()) {
            if (Main.getForceTarGz() && (workdir2.exists() || workdir3.exists())) {
                workdir = workdir2;
            } else {
                // we're set - remember that this is the base directory
                workdir = new File(workdirName);
                baseDirectory = workdir.toString();
            }
        } else {
           workdir = workdir2;
        }

        if (!workdir.exists()) {
            workdir = workdir3;
        }
        if (!workdir.exists() || !workdir.canRead()) {
            System.out.println("WARNING: Unable to access archived work area at "+workdir +
                               ( (secondAttempt) ? " or " + workdir2 : ""));
            return;
        }
*******************/
        String baseDirectory = workdirName;
        File workdir = new File(workdirName);

        
        File zipFile = new File(Main.getArchiveFile());
        JarOutputStream zos = new JarOutputStream(new FileOutputStream(zipFile));

        VirtualDir vd = VirtualDir.create(workdir);
        Iterator<VirtualFile> files = vd.getFiles(TRIAGE_FILES);
        int fileCount = 0;
        while (files.hasNext()) {
            VirtualFile vf = files.next();
            String parent = vf.getParent();
            if (baseDirectory != null && parent.startsWith(baseDirectory)) {
                parent = parent.substring(baseDirectory.length());
            }
            if (parent.startsWith("/")) {
                parent = parent.substring(1);
            } else if (parent.startsWith("./")) {
                parent = parent.substring(2);
            }
            // System.out.println("ADDING: Virtual: "+ parent + "/" + vf.getName());

            JarEntry ze = new JarEntry( parent + "/" + vf.getName());
            ze.setTime(vf.lastModified());

            // If a warning was emitted with respect to file size,
            // we will append that warning and account for it in
            // this entry's size.
            String check = (vf.getTheCheck() == null)
                            ? ""
                            : "\n" + vf.getTheCheck() + "\n";
            long newLength = vf.length() + check.length();
            ze.setSize(newLength);
            byte[] content = new byte[(int)vf.length()];
            InputStream is = vf.getInputStream();
            is.read(content);
            is.close();

            try {
                zos.putNextEntry(ze);
                zos.write(content, 0, content.length);
                // Now write the warning, if any
                if (!check.equals("")) {
                    for (int i = 0; i < check.length(); i++) {
                        zos.write(check.charAt(i));
                    }
                }
                zos.closeEntry();
                fileCount++;
            } catch (ZipException zexn) {
                if (zexn.getMessage().indexOf("duplicate entry:") >= 0) {
                    // suppress duplicate entry warnings
                    if (DEBUG) {
                        System.out.println("INFO: " + zexn);
                    }
                } else {
                    System.out.println("SEVERE: error adding zip entry " +  parent + "/" + vf.getName() + ": " + zexn);
                }
            }
        }

        if (fileCount == 0) {
            JarEntry ze = new JarEntry("Empty_JAR_file");
            ze.setTime(System.currentTimeMillis());
            ze.setSize(0);
            zos.putNextEntry(ze);
            zos.write(new byte[]{}, 0, 0);
            zos.closeEntry();
        }
        zos.close();

        Main.setArchive(new JarFile(new File(Main.getArchiveFile())));

        Main.setTestLogicFiles(vd.getTestLogicFiles());
    }

/*
    static FilenameFilter TRIAGE_FILES = new FilenameFilter() {


            public boolean accept(File f, String s) {
                 int pos = s.lastIndexOf("/");
                 String name = (pos>=0) ? s.substring(pos+1) : s;
                 String path = (pos>0)  ? s.substring(0,pos) : "";

                 if (name.endsWith(XML_SUFFIX)
                     && (name.startsWith("TEST-") 
                         || name.startsWith("TESTS-")
                         || name.startsWith("build")
                         || name.indexOf("build") > 0)) {
                     return true;
                 } else if (name.endsWith(JTS_SUFFIX)
                            || name.endsWith(JTL_SUFFIX)) {
                     return true;
                 } else if (Util.isLogFile(name)) {
                     return true;
                 } else if (name.endsWith(".suc")
                            || name.endsWith(".dif")
                            || name.endsWith(".tlg")) {
                     return true;
                 } else if (name.endsWith(".farm.out")
                            || name.endsWith(".history.log")) {
                     return true;
                 }
                
                 return false;
             }
     };
  */

}
