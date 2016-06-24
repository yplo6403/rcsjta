/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package com.gsma.rcs.imaplib.imap;

import static com.gsma.rcs.imaplib.imap.ImapUtil.CRLF;
import static com.gsma.rcs.imaplib.imap.ImapUtil.CRLFCRLF;
import static com.gsma.rcs.imaplib.imap.ImapUtil.HEADER_CONTENT_TYPE;
import static com.gsma.rcs.imaplib.imap.ImapUtil.decodeBase64;
import static com.gsma.rcs.imaplib.imap.ImapUtil.encodeBase64;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

public class Part implements IPart {

    private Map<String, Header> mHeaders = new HashMap<>();

    private String mContent;

    private static final Logger sLogger = Logger.getLogger(Part.class.getName());

    public Part() {
    }

    public Part(String contentType, String dataAsString) {
        mContent = dataAsString;// encodeBase64(data);
        setContentType(contentType);
    }

    public Part(String payload) {
        fromPayload(payload);
    }

    @Override
    public void fromPayload(String payload) {
        int sepa = payload.indexOf(CRLFCRLF);
        if (sepa != -1) {
            String headerString = payload.substring(0, sepa); // extract and unfold
            mHeaders = Header.parseHeaders(headerString);
            mContent = payload.substring(sepa + CRLFCRLF.length());
        } else {
            if (payload.indexOf(':') != -1) {
                mHeaders = Header.parseHeaders(payload);
                mContent = "";
            } else {
                sLogger.warning("Payload with no header : " + payload);
                mContent = payload;
            }
        }
    }

    public void setContent(byte[] contentAsBytes) {
        mContent = encodeBase64(contentAsBytes);
    }

    public void setContent(String content) {
        mContent = content;
    }

    public String getContent() {
        return mContent;
    }

    public String getContentType() {
        Header h = mHeaders.get(HEADER_CONTENT_TYPE);
        if (h == null) {
            return null;
        }
        return h.getValue();
    }

    public void setContentType(String contentType) {
        mHeaders.put(HEADER_CONTENT_TYPE, new Header(HEADER_CONTENT_TYPE, contentType));
    }

    public void setHeader(String key, String value) {
        mHeaders.put(key, new Header(key, value));
    }

    public String getHeader(String key) {
        if (mHeaders.containsKey(key)) {
            return mHeaders.get(key).getValue();
        }
        return null;
    }

    @Override
    public String toPayload() {
        Set<String> keys = mHeaders.keySet();
        StringBuilder sb = new StringBuilder();
        for (String k : keys) {
            sb.append(k);
            sb.append(':');
            sb.append(' ');
            sb.append(mHeaders.get(k).getValue());
            sb.append(CRLF);
        }
        sb.append(CRLF);
        sb.append(mContent);
        // sb.append(CRLF);
        return sb.toString();
    }

    /**
     * http://tools.ietf.org/html/rfc2046#section-5.1.3
     */
    public List<Part> getMultiParts() {
        String boundary = mHeaders.get(HEADER_CONTENT_TYPE).getValueAttribute("boundary");
        String[] ptexts = mContent.split("--" + boundary);
        List<Part> parts = new ArrayList<>();
        for (int i = 1; i < ptexts.length; i++) {
            String t = ptexts[i];
            if (t.equals("--"))
                break; // the end
            parts.add(new Part(t));
        }
        return parts;
    }

    public byte[] getContentAsBytes() {
        return decodeBase64(mContent.getBytes());
    }

    public void setDate(Date date) {
        Header h = Header.createHeader("Date");
        h.setValueAsDate(date);
        mHeaders.put("Date", h);
    }

    public Date getDate() {
        try {
            return mHeaders.get("Date").getValueAsDate();
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, Header> getHeaders() {
        return mHeaders;
    }

}
