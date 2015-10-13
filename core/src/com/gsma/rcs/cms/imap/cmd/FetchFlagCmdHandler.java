
package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.sync.strategy.FlagChange;

import com.sonymobile.rcs.imap.Flag;
import com.sonymobile.rcs.imap.Part;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class FetchFlagCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_FETCH_FLAGS;
    private static final String sPattern = "\\(UID ([0-9]+) FLAGS \\((.*)\\) MODSEQ \\(([0-9]+)\\)\\)$";

    private static final int sExpectedValues = 3;

    final Map<Integer, Map<String, String>> mData = new HashMap<Integer, Map<String, String>>();

    private String mFolderName;
    
    public FetchFlagCmdHandler(String folderName){
        mFolderName = folderName;    
    }
    
    @Override
    public String buildCommand(Object... params) {
        return String.format(sCommand, params);
    }

    @Override
    public boolean handleLine(String oneLine) {

        String[] values = extractCounterValuesFromLine(sPattern, oneLine);

        if (values == null || values.length != sExpectedValues) {
            return false;
        }

        Map<String, String> data = new HashMap<String, String>();
        Integer uid = Integer.valueOf(values[0]);
        data.put(Constants.METADATA_UID, values[0]);
        data.put(Constants.METADATA_FLAGS, values[1]);
        data.put(Constants.METADATA_MODSEQ, values[2]);
        mData.put(uid, data);
        return true;
    }

    @Override
    public void handleLines(List<String> lines) {
    }

    @Override
    public void handlePart(Part part) {
    }

    @Override
    public Set<FlagChange> getResult() {
        Set<FlagChange> flagChanges = new HashSet<FlagChange>();
        Iterator<Entry<Integer, Map<String, String>>> iter = mData.entrySet().iterator();
        while (iter.hasNext()) {
            Entry<Integer, Map<String, String>> entry = iter.next();
            Integer uid = entry.getKey();
            Map<String, String> data = entry.getValue();

            Set<Flag> flags = CmdUtils.parseFlags(data.get(Constants.METADATA_FLAGS));
            flagChanges.add(new FlagChange(mFolderName, uid, flags));
        }
        return flagChanges;
    }

}
