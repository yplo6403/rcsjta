/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2010-2016 Orange.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ******************************************************************************/

package com.gsma.rcs.cms.toolkit.operations;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.provider.imap.MessageData.MessageType;

import com.sonymobile.rcs.imap.Flag;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Message {

    private String mFolder;
    private String mContent;
    private Set<Flag> mFlags = new HashSet<Flag>();
    private Map<String, String> mHeaders = new HashMap<String, String>();

    public Message(String folder, String content, String[] flags, Map<String, String> headers) {
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
