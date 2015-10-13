
package com.gsma.rcs.cms.imap.cmd;

import com.sonymobile.rcs.imap.Part;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class CmdHandler {

    /**
     * IMAP command definition
     */
    public enum CommandType {
        LIST_STATUS, SELECT_CONDSTORE, FETCH_FLAGS, FETCH_HEADERS, FETCH_MESSAGES_BODY
    }

    /**
     * Return a IMAP command handler
     * 
     * @param command
     * @param capabilities
     * @param params
     * @return CmdHandler
     */
    public static CmdHandler getHandler(CommandType command, List<String> capabilities,
            Object... params) {
        CmdHandler handler = null;
        switch (command) {
            case LIST_STATUS:
                handler = new ListStatusCmdHandler();
                break;
            case SELECT_CONDSTORE:
                handler = new SelectCondstoreCmdHandler();
                break;
            case FETCH_FLAGS:
                handler = new FetchFlagCmdHandler((String)params[0]);
                break;
            case FETCH_HEADERS:
                handler = new FetchHeaderCmdHandler();
                break;
            case FETCH_MESSAGES_BODY:
                handler = new FetchMessageCmdHandler();
                break;
        }
        return handler.checkCapabilities(capabilities) ? handler : null;
    }

    /**
     * @param params
     * @return String
     */
    public abstract String buildCommand(Object... params);

    /**
     * @param oneLine
     * @return boolean
     */
    public abstract boolean handleLine(String oneLine);

    /**
     * @param lines
     */
    public abstract void handleLines(List<String> lines);

    /**
     * @param part
     */
    public abstract void handlePart(Part part);

    /**
     * @return result
     */
    public abstract Object getResult();

    protected boolean checkCapabilities(List<String> capabilities) {
        return true;
    }

    protected String[] extractCounterValuesFromLine(String pattern, String line) {

        Matcher matcher = Pattern.compile(pattern).matcher(line);
        if (!matcher.find()) {
            return null;
        }

        String[] values = new String[matcher.groupCount()];
        for (int i = 0; i < matcher.groupCount(); i++) {
            values[i] = matcher.group(i + 1);
        }
        return values;
    }
}
