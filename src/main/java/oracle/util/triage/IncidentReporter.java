/* $Header: j2ee/itools/src/triage/src/IncidentReporter.java /main/1 2011/02/04 06:44:39 gcook Exp $ */

/* Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved. */

/*
   DESCRIPTION
    <short description of component this file declares/defines>

   PRIVATE CLASSES
    <list of private classes defined - with one-line descriptions>

   NOTES
    <other useful comments, qualifications, etc.>

   MODIFIED    (MM/DD/YY)
    gcook       02/02/11 - Creation
 */
package oracle.util.triage;

import java.io.BufferedReader;                        
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileOutputStream;
import java.io.InputStreamReader;                        
import java.io.IOException;
import java.io.PrintStream;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeSet;

/**
 * IncidentReporter analyses a test results area producing a report of 
 * incidents that have been found.
 *
 *  @version $Header: IncidentReporter.java 02-feb-2011.02:31:22 gcook    Exp $
 *  @author  gcook
 *  @since   release specific (what release of product did this appear i
 */
public class IncidentReporter 
  implements Constants 
{

  /**
   * Process all incidents in the given directory structure.
   *
   * @param vd virtual directory to process
   */
  public IncidentReporter(VirtualDir vd) 
  {
    try 
    {
      if (DEBUG) 
      {
        System.out.println("IncidentReporter reading from virtual dir: " + 
                           vd.getPath() + "-" + vd.getName());
      }

      Iterator<VirtualFile> iter = vd.getFiles(README_FILES);

      while (iter.hasNext()) 
      {
        VirtualFile vf = iter.next();
        if (DEBUG) 
        {
          System.out.println("IncidentReporter reading from virtual file: " + 
                             vf.getUrl());
        }

        // extract the ADR Base and ADR Home locations from the full path 
        StringTokenizer st = new StringTokenizer(vf.getPath(), File.separator);
        int tokenCount = st.countTokens();
        
        if (tokenCount > 6)
        {
          int count = 0;
          int maxADRBaseTokens = tokenCount -6;
          int maxADRHomeTokens = tokenCount -2;          
          String adrBase = "";
          String adrHome = "";          

          while (st.hasMoreTokens())
          {
            count++;
            if (count < maxADRBaseTokens)
            {
              adrBase += File.separator + st.nextToken();
            }
            else if (count != maxADRHomeTokens)
            {
              adrHome += st.nextToken();
              
              if (count != (maxADRHomeTokens - 1))
              {
                adrHome += File.separator;
              }
            }
            else
            { 
              break;
            }
          }

          if (adrBase.length() > 0 && adrHome.length() > 0)
          {          
            ADRInstance adrInstance = m_adrInstances.get(adrBase+adrHome);
            if (adrInstance == null)
            {
              adrInstance = new ADRInstance(adrBase, adrHome);
              m_adrInstances.put(adrBase+adrHome, adrInstance);
            }
            
            // parse the incident details from the readme.txt
            Incident inc = parseIncidentDetails(vf);
            
            if (inc != null)
            {
              adrInstance.addIncident(inc);
              m_incidentCount++;
            }
            
          }
        } // we have enough path tokens to determine the ADR Base and ADR Home          
      } // each file
    } 
    catch (IOException ioe) 
    {
      System.out.println(
        "SEVERE: IncidentReporter issue reading readme files from directory " + 
        vd.getName() + ": " + ioe);
    }
  }

  /**
   * Uses the  given readme.txt file to extract incident details.
   *
   * @param readme readme file to parse
   * @return the Incident details if they can be parsed from the readme file, 
   * otherwise null
   */
  private static Incident parseIncidentDetails(VirtualFile readme)
  {
    BufferedReader br = null;
    Incident inc = null;
    
    try
    {
      String id = null;
      String createTime = null;
      String problemKey = null;
      String line = null;
      ArrayList<String> dumps = new ArrayList<String>();
      dumps.add(readme.getName());
      
      br = new BufferedReader(new InputStreamReader(readme.getInputStream())); 
      
      while ((line = br.readLine()) != null)
      {
        if (line.startsWith(INCIDENT_ID))
        {
          id = line.substring(INCIDENT_ID.length());
        }
        else if (line.startsWith(CREATE_TIME))
        {
          createTime = line.substring(CREATE_TIME.length());        
        }
        else if (line.startsWith(PROBLEM_KEY))
        {
          problemKey = line.substring(PROBLEM_KEY.length());        
        }        
        else if (line.startsWith(DUMP_FILES))
        {
          dumps.add(line.substring(DUMP_FILES.length()));  
        }
      }

      if (id != null && createTime != null && problemKey != null)
      {
        inc = new Incident(id, createTime, problemKey, dumps, readme.getUrl());
      }
    }
    catch (IOException e)
    {
      System.out.println(
        "SEVERE: IncidentReporter unable to process readme.txt file: " +
        readme.getPath() + " : " + e);
    }
    finally
    {
      if (br != null)
      {
        try
        {
          br.close();
        }
        catch (IOException e)
        {
          // ignore
        }
      }
    }
    
    return inc;
  }
  
  String toHtml() 
  {
    StringBuffer sb = new StringBuffer();
    sb.append("<html><head><title>Incident Report</title><head>"); 
    sb.append(NL);
    sb.append(HEADER_INCLUDES);
    sb.append("<head>"); 
    sb.append(NL);
    sb.append("<body><h2>Incident Report</h2>"); 
    sb.append(NL);
    sb.append("<b>Total Incidents: </b>");
    sb.append("" + m_incidentCount);
    sb.append("<p><hr><br>");
 
    for (ADRInstance adrInstance : m_adrInstances.values())
    {
      if (adrInstance.getIncidents().size() == 0)
      {
        continue;
      }

      sb.append("<b>ADR Base: </b>");
      sb.append(adrInstance.getADRBase());
      sb.append("<br>");      
      sb.append("<b>ADR Home: </b>");
      sb.append(adrInstance.getADRHome());      
      sb.append("<br><b>Incidents:</b><p>");      
      
      if (adrInstance.getIncidents().size() > 0)
      {
        sb.append("<table><tr><th>Id</th><th>Time</th><th>Problem Key</th><th>Dumps</th></tr>");
        
        for (Incident inc : adrInstance.getIncidents())
        {
          String baseUrl = inc.getReadmeUrl();
          baseUrl = baseUrl.replace("readme.txt","");
          
          sb.append("<tr><td>");
          sb.append(Util.makeDetailLink(inc.getReadmeUrl(),inc.getId().toString()));
          sb.append("</td>");
          sb.append("<td>");
          sb.append(inc.getCreateTime());
          sb.append("</td>");
          sb.append("<td>");
          sb.append(inc.getProblemKey());
          sb.append("<td>");
 
          for (String dumpFile : inc.getDumpFiles())
          {
            sb.append(Util.makeDetailLink(baseUrl + dumpFile , dumpFile));
            sb.append("<br>");
          }          
          
          sb.append("</td></tr>");          
        }
        
        sb.append("</table><p>");
        sb.append(NL);
      }
      else
      {
        sb.append("No incidents found<p>");
      }
    }
        
    sb.append("</body></html>");
 
    return sb.toString();
  }

  /**
   * Gets the number of incidents found.
   *
   * @return the number of incidents found.
   */
  int getIncidentCount()
  {
    return m_incidentCount;
  }

  /**
   * Analyses the given location producing a report of incidents in the 
   * environment.
   *
   * @param location directory to analyse
   * @param target name and path of the report file to write to
   * @param return number of incidents found
   */
  static int createIncidentReport(String location, String target) 
  {
    int incCount = 0;
    PrintStream ps = null;
    try 
    {
      IncidentReporter mr = new IncidentReporter(
                                  VirtualDir.create(new File(location)));
      if (DEBUG)
      {
        System.out.println("INFO: IncidentReporter generating html output");
      }
      ps = new PrintStream(new FileOutputStream(new File(target)));
      ps.println(mr.toHtml());
      incCount = mr.getIncidentCount();
    } 
    catch (IOException ioe) 
    {
      System.out.println(
        "SEVERE: IncidentReporter: problem creating incident report for " + 
        location + " in " + target + ": " + ioe);
    } 
    finally 
    {
      if (ps != null) 
      {
        ps.close();
      }
    }

    return incCount;
  }

  public static void main(String[] args) 
    throws Exception 
  {
    for (String s : args) 
    {
      IncidentReporter fb = 
        new IncidentReporter(VirtualDir.create(new File(s)));
      System.out.println(fb.toHtml());
    }
  }

  /**
   * ADRInstance represents a single ADR Home within an ADR Base.
   */ 
  private static class ADRInstance
  {
    ADRInstance(String adrBase, String adrHome)
    {
      m_adrBase = adrBase;
      m_adrHome = adrHome;
    }
    
    String getADRBase()
    {
      return m_adrBase;
    }
    
    String getADRHome()
    {
      return m_adrHome;
    }
    
    void addIncident(Incident inc)
    {
      m_incidents.add(inc);
    }
    
    TreeSet<Incident> getIncidents()
    {
      return m_incidents;
    }
    
    private String m_adrBase;
    private String m_adrHome;
    
    // incidents are sorted by incident id
    private TreeSet<Incident> m_incidents = 
      new TreeSet<Incident>( 
        new Comparator<Incident>()
          {
            public int compare(Incident o1, Incident o2)
            {
              return o1.getId().compareTo(o2.getId());
            }           
          }
       );
  }

  /**
   * Incident represents a single occurrence of an incident
   */
  private static class Incident
  {
  
    Incident(String id, 
             String createTime,
             String problemKey,
             List<String> dumpFiles,
             String readmeUrl)
    {
      m_id = Integer.valueOf(id);
      m_createTime = createTime;
      m_problemKey = problemKey;
      m_dumpFiles = dumpFiles;
      m_readmeUrl = readmeUrl;
    }
    
    Integer getId()
    {
      return m_id;
    }

    String getCreateTime()
    {
      return m_createTime;
    }

    String getProblemKey()
    {
      return m_problemKey;
    }
    
    String getReadmeUrl()
    {
      return m_readmeUrl;
    }
    
    List<String> getDumpFiles()
    {
      return m_dumpFiles;
    }
    
    private Integer m_id;
    private String m_createTime;
    private String m_problemKey;
    private List<String> m_dumpFiles;    
    private String m_readmeUrl;    
  }
       
  /* filter to find all incident readme.txt files */
  private FilenameFilter README_FILES = new FilenameFilter() 
    {
       public boolean accept(File f, String s) 
       {
         int pos = s.lastIndexOf("/");
         String name = (pos >= 0) ? s.substring(pos + 1) : s;
         String path = (pos > 0)  ? s.substring(0,pos) : "";

         if (name.equalsIgnoreCase("readme.txt")) 
         {
           return true;
         }
         return false;
       }
     };
     
  private static final boolean DEBUG = false;
  private static final String INCIDENT_ID = "Incident Id: ";
  private static final String CREATE_TIME = "Create Time: ";
  private static final String PROBLEM_KEY = "Problem Key: ";  
  private static final String DUMP_FILES = "Dump Files: ";    
  private Map<String, ADRInstance> m_adrInstances = new
    HashMap<String, ADRInstance>();
  private int m_incidentCount = 0;
} 
