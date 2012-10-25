package oracle.util.triage;

public class GuidToMailIdMapper {

    public static void main(String[] args) {
        if (args.length == 0) {
            Messages.explainMailMap("SEVERE: Missing input. Please specify a list of GUIDS, mailIds to map to the full mailIDs\n");
            System.exit(1);
        }

        String inputList = "";
        String separator = ","; //By default, comma separated

        for (int i = 0; i < args.length; i++) {
            args[i].trim();
            if (!args[i].equals("")) {
                if (args[i].equalsIgnoreCase("-space") || args[i].equalsIgnoreCase("-s")) {
                    separator = " ";
                } else if (args[i].equalsIgnoreCase("-comma") || args[i].equalsIgnoreCase("-c")) {
                    separator = ",";
                } else if (args[i].charAt(0) == '-') {
                    Messages.explainMailMap("SEVERE: Unknown option: " + args[i] +
                                            ". Please specify from available options\n");
                    System.exit(1);
                } else {
                    if (inputList.equals("")) {
                        inputList += args[i];
                    } else {
                        inputList += "," + args[i];
                    }
                }
            }
        }

        //System.out.println(inputList);
        if (!inputList.equals("")) {
            String output = MailIdFormatter.getFormattedMailIds(inputList, separator);
            System.out.println(output);
            if (output.startsWith("SEVERE") || output.startsWith("ERROR"))
                System.exit(1);
            else
                System.exit(0);
        } else {
            System.out.println("SEVERE: Empty Input. Please specify a list of arguments");
            System.exit(1);
        }
    }
}
