package oracle.util.triage;
                        

import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashMap;
import java.io.*;

/** Representation of a build.jts runtime log segment.
 */

public class FileBrowser implements Constants {

    private static final boolean DEBUG = false;

    public FileBrowser(VirtualDir dir) throws IOException {
        Iterator<VirtualFile> iter = dir.getFiles(TRIAGE_FILES);

        while (iter.hasNext()) {
            VirtualFile vf = iter.next();
            vf.setLimitInputStream(false);
            if (DEBUG) System.out.println("INFO: creating file entry for " + vf.getName() + " length " + Util.makeFileSize(vf.length()));
            FileEntry fe = new FileEntry(vf);
            if (DEBUG) System.out.println("INFO: got entry for " + vf.getName());
            addFileEntry(fe);
        }
    }

    public void traverse(StringBuffer sb) {
        if (DEBUG) System.out.println("INFO: starting file traversal.");
        traverse(sb, 0, rootDir);
    }

    private void traverse(StringBuffer sb, int indent, String currentDir) {
        List<String> list = dirs.get(currentDir);

        sortFiles(list);

        for (String s : list) {
            if (s.endsWith("/")) {
                if (isEmptyDir(currentDir + "/" + s.substring(0,s.length() - 1)) ) {
                    // list.remove(s);
                    // System.out.println("Should remove: "+currentDir+"/"+s);
                } else {
                    sb.append(getIndent(indent) + "<li>" + s); // Util.makeColor(GRAY_COLOR, s) );
                    sb.append("<ul>\n");
                    traverse(sb, indent + 1, currentDir + "/" + s.substring(0,s.length() - 1));
                    sb.append("</ul>\n");
                    sb.append("</li>\n");
                }
            } else { 
                FileEntry fe = files.get(currentDir + "/" + s);
                if (fe.length() != 0l
                    || fe.getName().endsWith(".suc")
                    || fe.getName().endsWith(".dif")) {
                    sb.append(getIndent(indent) + "<li>" + fe.toHtml() + "</li>\n" );
                }
            }
        }
    }

    private boolean isEmptyDir(String dir) {
        List<String> list = dirs.get(dir);
        for (String s : list) {
             if (!s.endsWith("/")) {
                 return false;
             } else if (!isEmptyDir(dir + "/" + s.substring(0,s.length() - 1)) ) {
                 return false;
             }
        }
        return true;
    }

    private static void sortFiles(List<String> list) {
        int lastIndex = list.size() - 1;
        String tmp = null;
        boolean sorted = false;
        while (!sorted) {
            sorted = true;
            for (int i = 0; i < lastIndex; i++) {
                 if ( isSmaller(list.get(i + 1), list.get(i)) ) {
                     tmp = list.get(i);
                     list.set(i, list.get(i + 1));
                     list.set(i + 1, tmp);
                     sorted = false;
                 }
             }
        };
    }

    private static boolean isSmaller(String f1, String f2) {
         if (f1.endsWith("/")) {
             if (f2.endsWith("/")) {
                 return f1.compareTo(f2) < 0;
             } else {
                 return true;
             }
         } else if (f2.endsWith("/")) {
             return false;
         } else {
             return f1.compareTo(f2) < 0;
         }
    }

    private static String getIndent(int ind) {
        return PREFIX.substring(0,ind);
    }
    // private static final String PREFIX = "--------------------------------------------------------";
    private static final String PREFIX = "                                                        ";

    private void addFileEntry(FileEntry fe) {
        if (DEBUG) System.out.println("Adding file entry: " + fe.getName() + " under " + fe.getParent());
        List<String> siblings = dirs.get(fe.getParent());
        if (siblings == null) {
            String parentDir = fe.getParent();
            siblings = new ArrayList<String>();
            addDirEntry(parentDir);
        }
        siblings.add(fe.getName());
        files.put(fe.getParent() + "/" + fe.getName(), fe);
    }

    private void addDirEntry(String dir) {
        if (DEBUG) System.out.println("Adding dir entry: " + dir);
        List<String> list = dirs.get(dir);
        if (list == null) {
             list = new ArrayList<String>();
             dirs.put(dir,list);  
             int pos = 0;
             if ((pos = dir.lastIndexOf("/")) > 0) {
                 String parentDir = dir.substring(0,pos);
                 String dirName = dir.substring(pos + 1) + "/";
                 List<String> siblings = dirs.get(parentDir);
                 if (siblings == null) {
                     addDirEntry(parentDir);
                     siblings = dirs.get(parentDir);
                 }
                 boolean found = false;
                 for (int i = 0; i < siblings.size(); i++) {
                    if (dirName.equals(siblings.get(i))) {
                        found = true;
                        break;
                    }
                 }
                 if (!found) {
                    if (DEBUG) System.out.println(" -> Adding name " + dirName + " for dir entry: " + dir);
                    siblings.add(dirName);
                 }
             } else {
                if (rootDir == null) {
                    rootDir = dir;
                    if (DEBUG) System.out.println(" -> Adding root dir " + rootDir);
                } else if (!rootDir.equals(dir)) {
                    System.out.println("SEVERE: cannot set file browsing root to " + dir + " - it was already set to " + rootDir);
                }
            }
        }
    }
                    

    private HashMap<String, List<String>> dirs = new HashMap<String, List<String>>();
    private HashMap<String, FileEntry> files = new HashMap<String, FileEntry>();
    private String rootDir = null;
   
    public class FileEntry {
        public FileEntry(VirtualFile vf) {
            this.name = vf.getName();
            this.parent = vf.getParent();
            this.url = vf.getUrl();
            this.check = vf.getTheCheck();
            if (parent == null
                || parent.equals("")
                || parent.equals(".")
                || parent.equals("./")
                || parent.equals("/")) {
                parent = ".";
            } else if (parent.startsWith("/")) {
                parent = "." + parent;
            } else {
                parent = "./" + parent;
            }
            this.lastModified = vf.lastModified();
            this.length = vf.length();

            if (length() > 0l) {
                int nextIdx = 0;
                String[] cirBuf = new String[2 * MAX_HINT_NUM_LINES];
                String lastLine = null;
                LineNumberReader lnr = null;
                try {
                    lnr = new LineNumberReader(new InputStreamReader(vf.getInputStream()));
                    String line = null;
                    while ( (line = lnr.readLine()) != null ) {
                        cirBuf[nextIdx] = line;
                        lastLine = line;
                        nextIdx++;
                        if (nextIdx >= cirBuf.length) {
                            nextIdx = 0;
                        }
                    }
                } catch (Exception e) {
                    System.out.println("WARNING: unable to read from " + vf);
                } finally {
                    if (lnr != null) {
                        try {
                            lnr.close();
                        } catch (IOException ioe) {
                            // ignore
                        }
                    }
                }

                // If the file size exceeds the maximum permitted, then the
                // last line contains the original warning about exceeding the
                // limit. Get that line!
                if (length() > MAX_INPUT_STREAM_SIZE
                    && check == null) {
                    check = lastLine;
                }

                // Now put together the hint detail text from the last couple of lines.
                String det = "";
                int numCharacters = 0;
                for (int i = 0; i < cirBuf.length; i++) {
                    nextIdx--;
                    if (nextIdx < 0) {
                        nextIdx = cirBuf.length - 1;
                    }
                    if (cirBuf[nextIdx] == null) {
                         break;
                    } else {
                        if (i == 0
                            || numCharacters + cirBuf[nextIdx].length() + 1 < MAX_HINT_CHARACTERS) {
                            det = cirBuf[nextIdx] + "\n" + det;
                            numCharacters += cirBuf[nextIdx].length() + 1;
                        } else if (i > 0) {
                            break;
                        }
                    }
                }
                if (length() > MAX_HINT_CHARACTERS) {
                    det = "...\n" + det;
                }
                this.detail = det;
            }
        }
        private String name;
        private String parent;
        private long lastModified;
        private long length;
        private String url;
        private String detail;
        private String check;

        public long lastModified() {
            return this.lastModified;
        }

        public long length() {
            return this.length;
        }

        public String getParent() {
            return this.parent;
        }

        public String getName() {
            return this.name;
        }

        public String getUrl() {
            return this.url;
        }

        public String getDisplayName() {
            return (getName().startsWith("/"))
                   ? getName().substring(1)
                   : getName();
        }

        public String toHtml() {
            int icon = -1;

            if (length() > 0) {
                icon = TEXT_ICON;
            }
            if (length() > MAX_HINT_CHARACTERS) {
                icon = ENDTEXT_ICON;
            }

            if (getDisplayName().endsWith(".suc")) {
                icon = CHECK_ICON;
            } else if (getDisplayName().endsWith(".dif")) {
                icon = RED_X_ICON;
            } else if (check != null) {
                icon = TRIANGLE_ICON;
            }
    
            String prefix = (icon < 0)
                              ? ""
                              : ((detail == null)
                                    ? Util.makeIcon(icon)
                                    : Util.makeHint(detail, icon))
                                 + Util.makeColor(GRAY_COLOR, Util.toTimestamp(lastModified())) + " ";

            String link = (length() == 0l) 
                          ?  Util.makeColor(GRAY_COLOR, getDisplayName())
                          :  Util.makeDetailLink(getUrl(), getDisplayName());

            String suffix = (length() > MAX_HINT_CHARACTERS)
                               ? ((length() < MIN_BIG_FILE)
                                   ? Util.makeColor(GRAY_COLOR, " [" + Util.makeFileSize(length()) + "]")
                                   : " [" + Util.makeFileSize(length()) + "]")
                               : "";

            String theCheck = (check == null) ? ""
                                            : " " + Util.makeColor(RED_COLOR, check);

            return prefix + link + suffix + theCheck;
        }
    }

    public String toHtml() {
        StringBuffer sb = new StringBuffer();
        sb.append("<HTML><HEAD><TITLE>Test Run Log Files</TITLE></HEAD>"); sb.append(NL);
        sb.append(HEADER_INCLUDES);

        sb.append("<BODY><H1>Test Run Log Files</H1>"); sb.append(NL);
/*
        sb.append("<H2>Summary</H2>"); sb.append(NL);
        sb.append(AreaDescriptors.getHtmlStats());
        sb.append(NL);
*/

        sb.append("<H2>Files</H2>"); sb.append(NL);
        sb.append("<a href=\"#\" onclick=\"expandTree('tree1'); return false;\">Expand all</a> - ");
        sb.append("<a href=\"#\" onclick=\"collapseTree('tree1'); return false;\">collapse all</a>");
        sb.append(NL);

        sb.append("<ul class=\"mktree\" id=\"tree1\">"); sb.append(NL);
        traverse(sb);
        sb.append("</ul>"); sb.append(NL);

        sb.append("</BODY></HTML>"); sb.append(NL);
        return sb.toString();
    }


    static void createFileBrowser(String location, String target) {
        PrintStream ps = null;
        try {
            FileBrowser fb = new FileBrowser(VirtualDir.create(new File(location)));
            if (DEBUG) System.out.println("INFO: generating html output for file browser");
            ps = new PrintStream(new FileOutputStream(new File(target)));
            ps.println(fb.toHtml());
        } catch (IOException ioe) {
            System.out.println("SEVERE: problem creating file browser for " + location + " in " + target + ": " + ioe);
        } finally {
            if (ps != null) {
                ps.close();
            }
        }
    }

    public static void main(String[] args) throws Exception {
        for (String s : args) {
            FileBrowser fb = new FileBrowser(VirtualDir.create(new File(s)));
            System.out.println(fb.toHtml());
        }
    }

}
