package oracle.util.triage;

import java.io.*;
import java.util.regex.*;
import java.util.*;
import java.net.*;

public class DeltaProperties {


   private static final String RULES_RESOURCE = "baselog.properties";

   private static final String DEFINE                = "DEFINE";
   private static final String PATTERN_LITERAL       = "DEFINE_QUOTE";

   private static final String BEGIN_IGNORE_LITERALS = "BEGIN_IGNORE_LITERALS";
   private static final String END_IGNORE_LITERALS   = "END_IGNORE_LITERALS";
   private static final String BEGIN_MAP_PATTERNS    = "BEGIN_MAP_PATTERNS";
   private static final String END_MAP_PATTERNS      = "END_MAP_PATTERNS";
   private static final String BEGIN_MAP_HTML_TO_LOG = "BEGIN_MAP_HTML_TO_LOG";
   private static final String END_MAP_HTML_TO_LOG   = "END_MAP_HTML_TO_LOG";

   private static final String[] LINE_COMMENT = new String[] { "#", "//", "--" };
   private static final String[]   STRING_SIGNATURE     = new String[]{};
   private static final String[][] STRINGARRY_SIGNATURE = new String[][]{};
   private static final Pattern[]  PATTERN_SIGNATURE    = new Pattern[]{};


   public String[] getIgnores() {
      if (IGNORES == null) {
          IGNORES = ignores.toArray(STRING_SIGNATURE);
      }
      return IGNORES;
   }
   private String[] IGNORES = null;
   private List<String> ignores = new ArrayList<String>();


   public Pattern[] getPatterns() {
       if (PATTERNS == null) {
           PATTERNS = patterns.toArray(PATTERN_SIGNATURE);
       }
       return PATTERNS;
   }
   private Pattern[] PATTERNS = null;
   private List<Pattern> patterns = new ArrayList<Pattern>();
   private List<String> patternSrcs = new ArrayList<String>();

   public String[] getReplacements() {
      if (REPLACEMENTS == null) {
          REPLACEMENTS = replacements.toArray(STRING_SIGNATURE);
      }
       return REPLACEMENTS;
   }
   private String[] REPLACEMENTS = null;
   private List<String> replacements = new ArrayList<String>();

   public Pattern[] getHtmlToLogPatterns() {
       if (HTML_TO_LOG_PATTERNS == null) {
           HTML_TO_LOG_PATTERNS = htmlToLogPatterns.toArray(PATTERN_SIGNATURE);
       }
       return HTML_TO_LOG_PATTERNS;
   }
   public String[][] getHtmlToLogReplacements() {
       if (HTML_TO_LOG_REPLACEMENTS == null) {
           HTML_TO_LOG_REPLACEMENTS = htmlToLogReplacements.toArray(STRINGARRY_SIGNATURE);
       }
       return HTML_TO_LOG_REPLACEMENTS;
   }
   private Pattern[]  HTML_TO_LOG_PATTERNS;
   private String[][] HTML_TO_LOG_REPLACEMENTS;
   private List<Pattern> htmlToLogPatterns       = new ArrayList<Pattern>();
   private List<String[]>  htmlToLogReplacements = new ArrayList<String[]>();

   private void addPatternDefinition(int line, String replacement, String pattern) {
       replacement = applyDefinitions(replacement);
       pattern = applyDefinitions(pattern);

       replacements.add(replacement);
       patternSrcs.add(pattern);
       try {
           patterns.add(Pattern.compile(pattern));
       } catch (PatternSyntaxException e) {
           throw new IllegalArgumentException("Error:" + fileName + ":" + line + ": compilation of Java pattern " + pattern +
                                              " has an issue: " + e.getMessage());
       }
   }

   private void addMapHtmlToLog(int line, String pattern, String[] replacementNames) {
       for (int i=0; i<replacementNames.length; i++) {
           replacementNames[i] = applyDefinitions(replacementNames[i]);
       }
       htmlToLogReplacements.add(replacementNames);
       pattern = applyDefinitions(pattern);

       StringBuffer src = new StringBuffer();
       src.append("   " + Token.quote(pattern) + " ==> ");
       for (int i=0; i<replacementNames.length; i++) {
            src.append(Token.quote(replacementNames[i]));
            if (i < replacementNames.length - 1) {
                src.append("\n      + ");
            }
       }
       htmlToLogMapSrcs.add(src + " ;");

       try {
           htmlToLogPatterns.add(Pattern.compile(pattern));
       } catch (PatternSyntaxException e) {
           throw new IllegalArgumentException("Error:" + fileName + ":" + line + ": compilation of Java pattern " + pattern +
                                              " has an issue: " + e.getMessage());
       }
   }
   private List<String> htmlToLogMapSrcs = new ArrayList<String>();


   private String applyDefinitions(String s) {
      int pos = 0;
      for (String var : definitions.keySet()) {
          while ((pos = s.indexOf(var)) >= 0) {
              s = s.substring(0, pos) + definitions.get(var) + s.substring(pos + var.length());
          }
      }
      return s;
   }
   private void addDefinition(String var, String value) {
       definitions.put("^" + var + "^", applyDefinitions(value));
   }
   private HashMap<String,String> definitions = new HashMap<String,String>();

   public String toString() {
       StringBuffer sb = new StringBuffer();
       sb.append(BEGIN_MAP_HTML_TO_LOG);
       sb.append("\n");
       for (String s : htmlToLogMapSrcs) {
            sb.append("    ");
            sb.append(s);
            sb.append("\n");
       }
       sb.append(END_MAP_HTML_TO_LOG);
       sb.append("\n");
       sb.append(BEGIN_IGNORE_LITERALS);
       sb.append("\n");
       for (String s : ignores) {
            sb.append("    ");
            sb.append(Token.quote(s));
            sb.append("\n");
       }
       sb.append(END_IGNORE_LITERALS);
       sb.append("\n");
       sb.append(BEGIN_MAP_PATTERNS);
       sb.append("\n");
       for (int i=0; i<patternSrcs.size(); i++) {
            sb.append("    ");
            sb.append(Token.quote(replacements.get(i)));
            sb.append(" ::= ");
            sb.append(Token.quote(patternSrcs.get(i)));
            sb.append(" ;\n");
       }
       sb.append(END_MAP_PATTERNS);
       return sb.toString();
   }

   public DeltaProperties(String rulefile, String prependRulefile) {
      try {
          if (prependRulefile != null) {
              fileName = rulefile;
              File prependRules = new File(prependRulefile);
              if (!prependRules.exists()) {
                  throw new IllegalArgumentException("the -prepend-rules file " + prependRulefile + " must exist.");
              };
              readRules(new FileInputStream(prependRules));
          }

          if (rulefile != null) {
              fileName = rulefile;
              File rules = new File(rulefile);
              if (!rules.exists()) {
                  LineNumberReader lnr = new LineNumberReader(new InputStreamReader(getRulesResource()));
                  Writer ow = new FileWriter(rules);
                  String line = null;
                  while ((line = lnr.readLine()) != null) {
                      ow.write(line);
                      ow.write("\n");
                  }
                  ow.close();
                  lnr.close();
                  System.out.println("INFO: wrote rules template to " + rulefile + ". Now adjust this file as necessary.");
                  System.exit(0);
              } else {
                  readRules(new FileInputStream(rules));
              }
          } else {
              fileName = RULES_RESOURCE + " (classpath resource)";
              readRules(getRulesResource());
          }
      } catch (IOException ioe) {
          System.out.println("SEVERE: Error occured when reading rule set from " + fileName + ": " + ioe);
          System.exit(1);
      } catch (IllegalArgumentException iae) {
          System.out.println("SEVERE: " + iae.getMessage());
          System.exit(1);
      }
   }
   private String fileName = null;


   private void readRules(InputStream is) throws IOException {
       LineNumberReader lnr = new LineNumberReader(new InputStreamReader(is));
       Tokenizer tr = new Tokenizer(lnr, this.fileName);
       Token tok = null;
 
       while ((tok = tr.next()) != null) {
           if (tok.getKind() == Token.IDENTIFIER
               && tok.getText().equals(BEGIN_IGNORE_LITERALS)) {
               while ((tok = tr.next()) != null
                      && tok.getKind() == Token.LITERAL) {
                    ignores.add(applyDefinitions(tok.getText()));
               }
               match(tok, Token.IDENTIFIER, END_IGNORE_LITERALS, null);
           } else if (tok.getKind() == Token.IDENTIFIER
                      && tok.getText().equals(DEFINE)) {

               Token variable = tr.next();
               match(variable, Token.IDENTIFIER, null, "a variable name");

               Token value = tr.next();
               match(value, Token.LITERAL, null, "a literal defining the value of a variable");

               addDefinition(variable.getText(), value.getText());
           } else if (tok.getKind() == Token.IDENTIFIER
                      && tok.getText().equals(PATTERN_LITERAL)) {

               Token variable = tr.next();
               match(variable, Token.IDENTIFIER, null, "a variable name");

               Token value = tr.next();
               match(value, Token.LITERAL, null, "a literal whose value is the quoted literal pattern, i.e. Pattern(quote(...))");

               addDefinition(variable.getText(), Pattern.quote(value.getText()));
           } else if (tok.getKind() == Token.IDENTIFIER
               && tok.getText().equals(BEGIN_MAP_PATTERNS)) {

               while ((tok = tr.next()) != null
                      && tok.getKind() == Token.LITERAL) {
                      parsePatternDefinition(tr, tok);
               }
               match(tok, Token.IDENTIFIER, END_MAP_PATTERNS, null);

           } else if (tok.getKind() == Token.IDENTIFIER
               && tok.getText().equals(BEGIN_MAP_HTML_TO_LOG)) {

               while ((tok = tr.next()) != null
                      && tok.getKind() == Token.LITERAL) {
                      parseMapHtmlToLog(tr, tok);
               }
               match(tok, Token.IDENTIFIER, END_MAP_HTML_TO_LOG, null);
 
           } else {
               throw new IllegalArgumentException(fileName + ":" + tok.getLine() + " saw " + tok.getValue() +
                                                  " when expecting one of " + 
                                                    DEFINE + ", " +
                                                    PATTERN_LITERAL + ", " +
                                                    BEGIN_IGNORE_LITERALS + ", " +
                                                    BEGIN_MAP_PATTERNS + ",  or " +
                                                    BEGIN_MAP_HTML_TO_LOG);
           }
       }
   }

   private void parsePatternDefinition(Tokenizer tr, Token lhs) throws IOException {
       Token nxt = tr.next();
       match(nxt, Token.ASSIGN, null, null);

       Token def = tr.next();
       match(def, Token.LITERAL, null, "a literal defining a Java pattern to match");
       
       addPatternDefinition(def.getLine(), lhs.getText(), def.getText());

       while ((nxt = tr.next()) != null && nxt.getKind() != Token.SEMI) {
            match(nxt, Token.CHOICE, null, null);

            def = tr.next();
            match(def, Token.LITERAL, null, "a literal defining a Java pattern to match");
            addPatternDefinition(def.getLine(), lhs.getText(), def.getText());
       }
       match(nxt, Token.SEMI, null, null);
   }

   private void parseMapHtmlToLog(Tokenizer tr, Token lhs) throws IOException {
       List<String> rhs = new ArrayList<String>();
       Token nxt = tr.next();
       match(nxt, Token.ARROW, null, null);

       Token def = tr.next();
       match(def, Token.LITERAL, null, "a literal defining a URL naming pattern to generate");
       rhs.add(def.getText());

       while ((nxt = tr.next()) != null && nxt.getKind() != Token.SEMI) {
            match(nxt, Token.PLUS, null, null);

            def = tr.next();
            match(def, Token.LITERAL, null, "a literal defining a URL naming pattern to generate");
            rhs.add(def.getText());
       }
       match(nxt, Token.SEMI, null, null);
       
       addMapHtmlToLog(lhs.getLine(), lhs.getText(), rhs.toArray(STRING_SIGNATURE));
   }


   private void match(Token tok, int kind, String text, String expectedMessage) {
       if (tok == null) {
           Token messageToken = new Token(kind, text, 0);
           throw new IllegalArgumentException(fileName + ": reached end-of-file when expecting " + 
                                              ((expectedMessage == null) ? messageToken : expectedMessage));
       } else if (tok.getKind() == kind) {
           if (text == null || text.equals(tok.getText())) {
              // a match
           } else {
              // no match
              Token messageToken = new Token(kind, text, tok.getLine());
              throw new IllegalArgumentException(fileName + ":" + tok.getLine() + ": saw " + tok + " when expecting " + 
                                                 ((expectedMessage == null) ? messageToken : expectedMessage));
           }
       } else {
           // no match
           Token messageToken = new Token(kind, text, tok.getLine());
           throw new IllegalArgumentException(fileName + ":" + tok.getLine() + ": saw " + tok + " when expecting " + 
                                              ((expectedMessage == null) ? messageToken : expectedMessage));
       }
   }


   public static class Token {

       public static final int IDENTIFIER = 1;
       public static final int LITERAL = 2;
       public static final int ASSIGN = 3;
       public static final int ARROW = 4;
       public static final int PLUS = 5;
       public static final int CHOICE = 6;
       public static final int SEMI = 7;

       public Token(int kind, String text, int line) {
           this(kind, line);
           this.text = text;
       }

       public Token(int kind, int line) {
           this.kind = kind;
           this.line = line;
       }

       private int line;
       private int kind;
       private String text;

       public int getLine() {
           return line;
       }
       public int getKind() {
           return kind;
       }
       public String getText() {
           return text;
       }
       public String getValue() {
           if (kind == ASSIGN) {
               return "::=";
           } else if (kind == ARROW) {
               return "==>";
           } else if (kind == CHOICE) {
               return "|";
           } else if (kind == PLUS) {
               return "+";
           } else if (kind == SEMI) {
               return ";";
           } else {
               return text;
           }
       }

       private String quote() {
           return quote(text);
       }

       public static String quote(String s) {
           StringBuffer sb = new StringBuffer();
           sb.append("\"");
           for (int i=0; i<s.length(); i++) {
               char ch = s.charAt(i);
               if (ch == '\t') {
                   sb.append("\\t");
               } else if (ch == '\\') {
                   sb.append("\\\\");
               } else if (ch == '"') {
                   sb.append("\\\"");
               } else {
                   sb.append(ch);
               }
           }
           sb.append("\"");
           return sb.toString();
       }

       public String toString() {
           if (getKind() == LITERAL) {
               return quote();
           } else {
               return getValue();
           }
       }
   }

   public static class Tokenizer {
      public Tokenizer(LineNumberReader lnr, String fileName) {
          this.lnr = lnr;
          this.fileName = fileName;
      }
      private LineNumberReader lnr = null;
      private String line = "";
      private String fileName = "";
      private boolean isClosed = false;

      public Token next() throws IOException {
          if (line == null) {
              return null;
          } else {
              if (line.equals("") || isLineComment(line)) {
                  while ((line = lnr.readLine()) != null) {
                       line = line.trim();
                       if (!line.equals("")
                           && !isLineComment(line)) {
                           break;
                       } 
                  }
              }
              if (line == null) {
                  if (!isClosed) {
                      isClosed = true;
                      lnr.close();
                  }
                  return null;
              } else if (line.startsWith("::=")) {
                  line = line.substring(3).trim();
                  return new Token(Token.ASSIGN, lnr.getLineNumber());
              } else if (line.startsWith("==>")) {
                  line = line.substring(3).trim();
                  return new Token(Token.ARROW, lnr.getLineNumber());
              } else if (line.startsWith("|")) {
                  line = line.substring(1).trim();
                  return new Token(Token.CHOICE, lnr.getLineNumber());
              } else if (line.startsWith("+")) {
                  line = line.substring(1).trim();
                  return new Token(Token.PLUS, lnr.getLineNumber());
              } else if (line.startsWith(";")) {
                  line = line.substring(1).trim();
                  return new Token(Token.SEMI, lnr.getLineNumber());
              } else if (!line.startsWith("\"")) {
                  int pos1 = line.indexOf(" ");
                  int pos2 = line.indexOf(" ");
                  int pos =  (pos1 < 0) ? pos2
                          : ((pos2 < 0) ? pos1
                          : ((pos1 < pos2) ? pos1 : pos2));
                  if (pos < 0) {
                     String tok = line;
                     line = "";
                     return new Token(Token.IDENTIFIER, tok, lnr.getLineNumber());
                  } else {
                     String tok = line.substring(0,pos);
                     line = line.substring(pos+1).trim();
                     return new Token(Token.IDENTIFIER, tok, lnr.getLineNumber());
                  }
              } else if (line.startsWith("\"")) {
                  StringBuffer sb = new StringBuffer();
                  for (int i=1; i<line.length(); i++) {
                      char ch = line.charAt(i);
                      if (ch == '"') {
                          line = line.substring(i+1).trim();
                          return new Token(Token.LITERAL, sb.toString(), lnr.getLineNumber());
                      } else if (ch == '\\') {
                          if (i+1 >= line.length()) {
                             throw new IllegalArgumentException("Error: " + fileName + ":" + lnr.getLineNumber() + ": missing quoted symbol '\\'");
                          }
                          i++;
                          char ch2 = line.charAt(i);
                          if (ch2 == 't') {
                            sb.append('\t');
                          } else if (ch2 == '\\') {
                            sb.append('\\');
                          } else if (ch2 == '"') {
                            sb.append('"');
                          } else {
                             throw new IllegalArgumentException("Error: " + fileName + ":" + lnr.getLineNumber() + ": \\" + ch2 + " is not permitted. "
                                                                + "May only quote '\"' or '\\'.");
                          }
                      } else {
                          sb.append(ch);
                      }
                  }
                  throw new IllegalArgumentException("Error: " + fileName + ":" + lnr.getLineNumber() + ": Literal is missing closing '\"'.");
              } else {
                  throw new IllegalArgumentException("Error: " + fileName + ":" + lnr.getLineNumber() + ": unrecognized symbol: '" + line + "'");
              }
          }
      }

      private boolean isLineComment(String line) {
          for (int i=0; i<LINE_COMMENT.length; i++) {
               if (line.startsWith(LINE_COMMENT[i])) {
                   return true;
               }
          }
          return false;
      }
   }

   private InputStream getRulesResource() {
       InputStream is = getClass().getResourceAsStream(RULES_RESOURCE);
       if (is == null) {
           System.out.println("FATAL: unable to access resource " + RULES_RESOURCE + " from " + this.getClass() + ". Aborting.");
           System.exit(1);
       }
       return is;
   }


   public static void main(String[] args) {
       System.out.println(new DeltaProperties(null, null));
   } 

}
