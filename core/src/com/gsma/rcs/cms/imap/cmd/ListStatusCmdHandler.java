
package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.ImapFolder;

import com.gsma.rcs.utils.StringUtils;
import com.sonymobile.rcs.imap.Part;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class ListStatusCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_LIST_STATUS;
    private static final String sPattern = "^STATUS (.*) \\(MESSAGES ([0-9]+) UIDNEXT ([0-9]+) UIDVALIDITY ([0-9]+) HIGHESTMODSEQ ([0-9]+)\\)$";

    private static final int sExpectedValues = 5;

    final Map<String, Map<String, String>> mData = new HashMap<String, Map<String, String>>();

    @Override
    public String buildCommand(Object... params) {
        return sCommand;
    }

    @Override
    public boolean handleLine(String oneLine) {
        return false;
    }

    @Override
    public void handleLines(List<String> lines) {

        for (String line : lines) {
            line = line.substring(2).trim();

            String[] values = extractCounterValuesFromLine(sPattern, line);

            if (values == null || values.length != sExpectedValues) {
                continue;
            }

            values[0] = StringUtils.removeQuotes(values[0]);

            Map<String, String> data = new HashMap<String, String>();
            data.put(Constants.METADATA_MESSAGES, values[1]);
            data.put(Constants.METADATA_UIDNEXT, values[2]);
            data.put(Constants.METADATA_UIDVALIDITY, values[3]);
            data.put(Constants.METADATA_HIGHESTMODSEQ, values[4]);
            mData.put(values[0], data);
        }
    }

    @Override
    public void handlePart(Part part) {
        // TODO Auto-generated method stub

    }

    @Override
    public List<ImapFolder> getResult() {
        List<ImapFolder> folders = new ArrayList<ImapFolder>();
        Iterator<Entry<String, Map<String, String>>> iter1 = mData.entrySet().iterator();
        while (iter1.hasNext()) {
            Entry<String, Map<String, String>> entry1 = iter1.next();
            String folderName = entry1.getKey();
            Map<String, String> counters = entry1.getValue();
            folders.add(new ImapFolder(folderName, counters));
        }
        return folders;
    }
}
