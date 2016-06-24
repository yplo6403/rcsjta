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

package com.gsma.rcs.core.cms.protocol.message;

import com.gsma.rcs.core.cms.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HeaderPart {

    private class Header {

        private final String mKey;
        private final String mValue;

        public Header(String key, String value) {
            mKey = key;
            mValue = value;
        }

        public String toString() {
            return mKey + Constants.HEADER_SEP + mValue + Constants.CRLF;
        }
    }

    private final Map<String, Header> mHeadersMap;
    private final List<Header> mHeadersList;

    public HeaderPart() {
        mHeadersMap = new HashMap<>();
        mHeadersList = new ArrayList<>();
    }

    public void addHeader(String key, String value) {
        Header header = new Header(key, value);
        mHeadersList.add(header);
        mHeadersMap.put(key, header);
    }

    public String getHeaderValue(String headerName) {
        Header header = mHeadersMap.get(headerName.toLowerCase());
        return header == null ? null : header.mValue;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Header header : mHeadersList) {
            sb.append(header);
        }
        return sb.toString();
    }
}
