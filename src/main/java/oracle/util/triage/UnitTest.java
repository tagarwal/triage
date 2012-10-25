package oracle.util.triage;


public class UnitTest implements Constants {

    public UnitTest() {
    }

    public void setName(String n) {
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setClassname(String cn) {
        classname = cn;
    }

    public String getClassname() {
        return classname;
    }

    public String getFullName() {
        return getClassname() + "::" + getName();
    }

    public void setTime(String t) {
        if (t == null) {
            time = 0.0f;
        } else {
            time = Float.parseFloat(t);
        }
    }

    public float getTime() {
        return time;
    }

    public void setStartEndTime(long start, long end) {
        startTime = start;
        time = (end - startTime) / 1000.0f;
    }
    private long startTime = 0l;

    public void setSuccess(boolean b) {
        success = b;
    }

    public void setShortMessage(String sm) {
        shortMessage = StackCompressor.compress(Util.unHTML(sm));
        if (shortMessage != null) {
            int pos = shortMessage.indexOf("; nested exception is:");
            if (pos > 0) {
                shortMessage = shortMessage.substring(0, pos);
            }

            shortMessage = Util.rightTrim(shortMessage);
        }
    }

    public String getShortMessage() {
        if (mergeCount > 0) {
            return shortMessage + 
                   "... and " + mergeCount + " more";
        } else {
            return shortMessage;
        }
    }

    public void setMessage(String m) {
        message = StackCompressor.compress(Util.unHTML(m));
    }

    public String getMessage() {
        return message;
    }

    public void setType(String t) {
        type = t;
    }

    public String getType() {
        return type;
    }

    public boolean isDiff() {
        return success == false;
    }

    public boolean mergableWith(UnitTest t) {
        return t.success == success
            && ( isMergeable()
                 && t.shortMessage != null
                 && t.shortMessage.equals(shortMessage) ); 
    }

    public void mergeWith(UnitTest t) {
         message = message + 
                   "... also " + t.getName() + "\n";
         mergeCount++;
         if (startTime > 0l && t.startTime > 0l) {
             setStartEndTime(startTime, t.startTime + (long)(t.getTime() * 1000.0f));
         } else {
             time += t.time;
         }
    }
    private int mergeCount = 0;

    private boolean isMergeable() {
        return shortMessage != null
            && type != null
            && type.equals("SKIP");
    }

    public String toString(int level, boolean diffOnly) {
        if (diffOnly && !isDiff() && !Main.showTimings()) {
            return null;
        }

        StringBuffer sb = new StringBuffer();

        boolean showDetail = INFO < level;
        boolean showFullDetail = (isDiff() && showDetail)
                                 || (!diffOnly && FINER < level);

        if (showDetail) {
            sb.append(getFullName());
        } else {
            sb.append(getName());
        }

        if (Main.showTimings()) {
            sb.append(" ");
            sb.append(getTime());
            sb.append(" secs");
        }

        if (showDetail) {
            if (getShortMessage() != null) {
                if (showFullDetail) {
                    sb.append(NL);
                    sb.append("SYNOPSIS: ");
                } else { 
                    if (getType() != null) {
                        sb.append(" - ");
                    }
                }
                if (getType() != null) {
                    sb.append(getType());
                    sb.append(": ");
                }
                sb.append(getShortMessage());
            }
        } else if (getType() != null) {
            sb.append(" - ");
            sb.append(getType());
        }

        if (showFullDetail) {
            if (getMessage() != null) {
                sb.append(NL);
                sb.append("DETAIL:   ");
                sb.append(NL);
                sb.append(getMessage());
                sb.append("---------------------------------------------------------------------------");
            }
        }

        return sb.toString();
    }


    public String toHtml(int level, boolean diffOnly) {
        if (diffOnly && !isDiff() && !Main.showTimings()) {
            return null;
        }

        StringBuffer sb = new StringBuffer();
        sb.append(Util.makeLi(getFullName() +
                              ( (getType() != null) ? (" - " + getType()) : "") ));
        if (getShortMessage() != null) {
            sb.append(Util.makeLi("<b>Synopsis:</b><br>" + NL + Util.makeHtml(true,"code",getShortMessage()) + NL));
        }
        if (getMessage() != null && !getMessage().equals("")) {
            sb.append(
                Util.makeLi("<b>Detail:</b><br>" + NL +
                   Util.makeUl(
                     Util.makeLi(Util.makeHtml(true,"code", getMessage()))
            )   )  );
        }
        return Util.makeUl(sb.toString());
    }

    private boolean success;
    private String name;
    private String classname;
    private String message;
    private String shortMessage;
    private String type;
    private float time;
}
