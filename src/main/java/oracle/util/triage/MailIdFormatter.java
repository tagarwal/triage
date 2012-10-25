package oracle.util.triage;

import java.util.ArrayList;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

public class MailIdFormatter {

    public static String getFormattedMailIds(String inputList, String separator) {

        try {

            String ldap_url = "ldap://gmldap.oraclecorp.com";
            String attribute = "mail";
            String email = "";
            String query = "";
            String[] inputTokens = inputList.split(",");

            ArrayList<String> guids = new ArrayList<String>();
            ArrayList<String> propMails = new ArrayList<String>();
            StringBuffer output = new StringBuffer();

            for (String input : inputTokens) {
                input.trim();
                if (!input.equals("")) {
                    String split[] = input.split("@", 2);
                    if (!split[0].contains(".") && !split[0].contains("_") && split[0].length() <= 8) {
                        guids.add(split[0]);
                    } else {
                        propMails.add(split[0] + "@oracle.com");
                        //output.append(split[0] + "@oracle.com" + separator);
                    }
                }
            }

            query = "(|";
            for (String guid : guids) {
                query = query + "(uid=" + guid + ")";
            }
            for (String propMail : propMails) {
                query = query + "(mail=" + propMail + ")";
            }
            query = query + ")";

            //System.out.println("Query:" + query);

            Hashtable env = new Hashtable();
            env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
            env.put(Context.PROVIDER_URL, ldap_url);
            DirContext context = new InitialDirContext(env);

            SearchControls ctrl = new SearchControls();
            ctrl.setSearchScope(SearchControls.SUBTREE_SCOPE);

            NamingEnumeration enumeration = context.search("", query, ctrl);

            while (enumeration.hasMore()) {
                SearchResult result = (SearchResult)enumeration.next();
                //System.out.println(result.toString());

                javax.naming.directory.Attributes attribs = result.getAttributes();
                NamingEnumeration values = ((BasicAttribute)attribs.get(attribute)).getAll();
                while (values.hasMore()) {
                    if (output.length() > 0) {
                        output.append(separator);
                    }
                    output.append(values.next().toString());
                }
            }
            email = output.toString();            
            return email;

        } catch (Exception ex) {
            return "SEVERE: " + ex.getMessage();
        }

    }
}
