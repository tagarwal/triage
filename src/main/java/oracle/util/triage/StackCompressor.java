package oracle.util.triage;

import java.util.Set;
import java.util.HashSet;


import java.io.File;
import java.io.FileReader;
import java.io.LineNumberReader;
import java.io.IOException;
import java.io.StringReader;

public class StackCompressor implements Constants {

    
    public static String compress(String s) {
       Set<String> hs = new HashSet<String>();

       LineNumberReader lnr = null;
       StringBuffer sb = new StringBuffer();
       String line = null;
       int elisionCount = 0;
       int delta = 0;
       boolean inJavaCompilation = false;

       if (s == null) {
           return null;
       }

       init();


       try {
           lnr = new LineNumberReader(new StringReader(s));
           while ((line = lnr.readLine()) != null) {

               // Sorry, but we do not care about verbose Java compilation
               if (line.startsWith("    [javac] [")) {
                   // why do they do that??

                   // dropped silently...
               } else {
                   if (line.equals(BEGIN_JAVA_COMPILATION)) {
                       inJavaCompilation = true;
                   }
    
                   if (!inJavaCompilation
                       && (hs.contains(line) 
                           || (line.startsWith("Caused by: ")
                               && hs.contains(line.substring(0,"Caused by: ".length()))))
                       && (delta = getElision(line)) == 0) {
                      elisionCount++;
                   } else if (toBeElided(line)) {
                       elisionCount ++;
                   } else if ( (delta = getElision(line)) > 0) {
                       elisionCount += delta;
                   } else {
                       if (elisionCount > 0) {
                           String el =  "\t... " + elisionCount + " more\n";
                           sb.append(el);
                           elisionCount = 0;
                       } 
                       sb.append(line);
                       sb.append("\n");
    
                       hs.add(line);
                       if (line.startsWith("Caused by: ")) {
                           hs.add(line.substring(0,"Caused by: ".length()));
                       }
                   }
    
                   if (line.equals(END_JAVA_COMPILATION)) {
                       inJavaCompilation = false;
                   }
               }
           }
           // do not display final elision
           // if (elisionCount > 0) {
           //     sb.append("\t... " + elisionCount + " more\n");
           //     elisionCount = 0;
           //} 

       } catch (IOException ioe) {
           // Ignore
       } finally {
           try {
               if (lnr != null) {
                   lnr.close();
               }
           } catch (IOException _ioe) {
               // ignore
           }
       }
       return sb.toString();
    }


    private static boolean toBeElided(String s) {
        for (int i = 0; i < PREFIXES.length; i++) {
             if (s.startsWith(PREFIXES[i])) {
                 return true;
             }
        }
        return false;
    }

    private static int getElision(String s) {
        if (s.startsWith("\t... ")
            && s.endsWith(" more")) {
            s = s.substring("\t... ".length());
            s = s.substring(0, s.length() - " more".length());
            try {
                return Integer.parseInt(s);
            } catch (Exception e) {
                return 0;
            }
        } else {
            return 0;
        }
    }

    private static String[] PREFIXES = null;

    private static void init() {
        if (PREFIXES == null) {
            PREFIXES = new String[TRACE_PREFIXES_TO_ELIDE.length];
            for (int i = 0; i < TRACE_PREFIXES_TO_ELIDE.length; i++) {
                 PREFIXES[i] = "\tat " + TRACE_PREFIXES_TO_ELIDE[i];
            }
        }
    }


    public static void main(String[] args) throws IOException {
        for (int i = 0; i < args.length; i++) {
             System.out.println("PROCESSING: " + args[i]);
             LineNumberReader lnr = new LineNumberReader(new FileReader(new File(args[i])));
             StringBuffer sb = new StringBuffer();
             String line = null;
             while ((line = lnr.readLine()) != null) {
                    sb.append(line); sb.append("\n");
             }
             lnr.close();
             int ini = sb.length();
             int fin = compress(sb.toString()).length();
             System.out.println("DONE PROCESSING: " + args[i] + " compressed " + ini + " to " + fin);
         }
    }

}
