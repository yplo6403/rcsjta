
package com.gsma.rcs.cms.toolkit.operations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;

import com.sonymobile.rcs.imap.Flag;

public class Message {

    private String mFolder;
    private String mContent;
    private Set<Flag> mFlags = new HashSet<Flag>();
    private Map<String, String> mHeaders = new HashMap<String, String>();

    public Message(String folder, String content, String[] flags,
            Map<String, String> headers) {
        mFolder = folder;
        mContent = content;
        for (String f : flags) {
            mFlags.add(Flag.valueOf(f));
        }
        mHeaders = headers;
    }
    
    /**
     * @param folder
     * @param content
     * @param flags
     * @param type
     * @param headers
     */
    public Message(String folder, String content, String[] flags, MessageType type,
            Map<String, String> headers) {
        mFolder = folder;
        mContent = content;
        for (String f : flags) {
            mFlags.add(Flag.valueOf(f));
        }
        mHeaders = headers;

        // add header for each type of message
        switch (type) {
            case SMS:
                mHeaders.put(Constants.HEADER_MESSAGE_CONTEXT, Constants.PAGER_MESSAGE);
                mHeaders.put(Constants.HEADER_MESSAGE_CORRELATOR, content);
                break;
            case MMS:
                break;
        }
    }

    /**
     * @param params
     */
    public Message(String[] params) {
        mFolder = params[0].trim();
        mContent = params[1].trim();
        for (String f : params[2].trim().split("\\|")) {
            mFlags.add(Flag.valueOf(f));
        }
        for (int i = 3; i < params.length; i++) {
            String[] f = params[i].split(":");
            mHeaders.put(f[0], f[1]);
        }
    }

    public String getFolder() {
        return mFolder;
    }

    public String getHeader(String name) {
        return mHeaders.get(name);
    }

    public String getContent() {
        return mContent;
    }

    public Set<Flag> getFlag() {
        return mFlags;
    }

    public Map<String, String> getHeaders() {
        return mHeaders;
    }
}
