
package com.gsma.rcs.cms.imap.cmd;

import com.gsma.rcs.cms.Constants;

import com.sonymobile.rcs.imap.ImapMessage;
import com.sonymobile.rcs.imap.ImapMessageMetadata;
import com.sonymobile.rcs.imap.Part;

import android.annotation.SuppressLint;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FetchMessageCmdHandler extends CmdHandler {

    static final String sCommand = Constants.CMD_FETCH_MESSAGE;

    private static final String sFlagPattern = "^\\* [0-9]+ FETCH \\(UID ([0-9]+) RFC822.SIZE ([0-9]+) FLAGS \\((.*)\\) MODSEQ \\(([0-9]+)\\).*$";

    private static final int sExpectedValuesForFlag = 4;

    @Override
    public String buildCommand(Object... params) {
        return String.format(sCommand, params);
    }

    @SuppressLint("UseSparseArrays")
    final Map<String, String> mData = new HashMap<String, String>();

    private Integer mUid;
    private Part mPart;
    
    @Override
    public boolean handleLine(String oneLine) {

        String[] values = extractCounterValuesFromLine(sFlagPattern, oneLine);
        if (values != null && values.length == sExpectedValuesForFlag) {            
            mUid = Integer.parseInt(values[0]);
            mData.put(Constants.METADATA_UID, values[0]);
            mData.put(Constants.METADATA_SIZE, values[1]);
            mData.put(Constants.METADATA_FLAGS, values[2]);
            mData.put(Constants.METADATA_MODSEQ, values[3]);
            return true;
        }
        return false;
    }

    @Override
    public void handleLines(List<String> lines) {
    }

    @Override
    public void handlePart(Part part) {
        mPart = part;
    }

    @Override
    public ImapMessage getResult() {        
        ImapMessageMetadata metadata = new ImapMessageMetadata(mUid);
        CmdUtils.fillFlags(metadata.getFlags(), mData.get(Constants.METADATA_FLAGS));
        return new ImapMessage(Integer.valueOf(mUid), metadata, mPart);
    }

}
