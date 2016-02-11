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

    class Header {
        private String mKey;
        private String mValue;

        public Header(String key, String value) {
            mKey = key;
            mValue = value;
        }

        public String toString() {
            return new StringBuilder(mKey).append(Constants.HEADER_SEP).append(mValue)
                    .append(Constants.CRLF).toString();
        }
    }

    Map<String, Header> mHeadersMap;
    List<Header> mHeadersList;

    public HeaderPart() {
        mHeadersMap = new HashMap<>();
        mHeadersList = new ArrayList<>();
    }

    public void addHeader(String key, String value) {
        addHeader(new Header(key, value));
    }

    public void addHeader(Header header) {
        mHeadersList.add(header);
        mHeadersMap.put(header.mKey, header);
    }

    public String getHeaderValue(String headerName) {
        Header header = mHeadersMap.get(headerName.toLowerCase());
        return (header == null ? null : header.mValue);
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Header header : mHeadersList) {
            sb.append(header);
        }
        return sb.toString();
    }
}
