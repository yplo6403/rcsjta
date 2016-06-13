/*
 * Copyright (C) 2015 Sony Mobile Communications Inc.
 * Copyright (C) 2010-2016 Orange.
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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Key, value pair holder with parsing utility methods.
 */
public class Header {

    private String mKey;

    private String mValue;

    private Header(String key) {
        mKey = key.trim();
    }

    protected Header(String key, String rawValue) {
        mKey = key.trim();
        mValue = rawValue;
    }

    public Date getValueAsDate() throws ParseException {
        // Thu, 13 Feb 1989 23:32 -0330
        // EEE, dd MMM yyyy HH:mm:ss Z
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        String normalized = normalizeSpace(mValue);
        return sdf.parse(normalized);
    }

    public void setValueAsDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US);
        mValue = sdf.format(date);
    }

    public String getKey() {
        return mKey;
    }

    public String getValue() {
        return mValue;
    }

    public static String normalizeSpace(String string) {
        return string.replaceAll("\\s+", " ").trim();
    }

    public static String cleanComments(String string) {
        if (string.indexOf('(') == -1)
            return string;
        boolean ignore = false;
        boolean escape = false;
        char[] arr = string.toCharArray();
        StringBuilder sb = new StringBuilder();
        for (char anArr : arr) {
            if (anArr == '\\') {
                escape = true;
                continue;
            } else if (anArr == '(' && !escape) {
                ignore = true;
            } else if (anArr == ')' && !escape) {
                ignore = false;
            } else if (!ignore) {
                sb.append(anArr);
            }
            escape = false;
        }
        return sb.toString();
    }

    public static Header createHeader(String headerSpec) {
        headerSpec = cleanComments(headerSpec);
        int i = headerSpec.indexOf(':');
        if (i != -1) {
            String key = headerSpec.substring(0, i).trim();
            String value = headerSpec.substring(i + 2);
            return new Header(key, value);
        } else {
            return new Header(headerSpec);
        }
    }

    public static Map<String, Header> parseHeaders(String headerString) {
        Map<String, Header> headers = new HashMap<>();
        // unfold
        headerString = headerString.replace(CRLF + " ", "");
        String[] headersArray = headerString.split(CRLF);
        for (String spec : headersArray) {
            Header header = createHeader(spec);
            headers.put(header.getKey(), header);
        }
        return headers;
    }

    public String getValueAttribute(String attr) {
        String a = attr + "=";
        String[] split = mValue.split(";");
        for (String s : split) {
            s = s.trim();
            if (s.startsWith(a)) {
                char[] arr = s.substring(a.length()).toCharArray();
                if (arr[0] == '"')
                    arr[0] = ' ';
                if (arr[arr.length - 1] == '"')
                    arr[arr.length - 1] = ' ';
                return new String(arr).trim();
            }
        }
        return null;
    }

    public String getMimeType() {
        String[] split = mValue.split(";");
        return split[0];
    }

}
