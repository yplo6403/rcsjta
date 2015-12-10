
package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;

import com.sonymobile.rcs.imap.Part;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SelectCondstoreCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_SELECT_CONDSTORE;
    private static final String sPattern = "OK \\[(UIDVALIDITY|UIDNEXT|HIGHESTMODSEQ) ([0-9]+)\\]";

    private static final int sExpectedValues = 2;

    private String mFolderName;

    final Map<String, Map<String, String>> mData = new HashMap<String, Map<String, String>>();

    @Override
    public String buildCommand(Object... params) {
        mFolderName = String.valueOf(params[0]);
        return String.format(sCommand, params);
    }

    @Override
    public boolean handleLine(String oneLine) {
        return false;
    }

    @Override
    public void handleLines(List<String> lines) {

        Map<String, String> data = new HashMap<String, String>();
        for (String line : lines) {
            line = line.substring(2).trim();

            String[] values = extractCounterValuesFromLine(sPattern, line);

            if (values == null || values.length != sExpectedValues) {
                continue;
            }
            data.put(values[0], values[1]);
        }
        mData.put(mFolderName, data);
    }

    @Override
    public boolean checkCapabilities(List<String> capabilities) {
        return capabilities.contains(Constants.CAPA_CONDSTORE);
    }

    @Override
    public void handlePart(Part part) {
    }

    @Override
    public Object getResult() {
        return null;
    }
}
