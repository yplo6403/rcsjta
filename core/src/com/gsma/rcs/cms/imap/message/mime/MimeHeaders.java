/*******************************************************************************
 * Software Name : RCS IMS Stack
 *
 * Copyright (C) 2015 France Telecom S.A.
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

package com.gsma.rcs.cms.imap.message.mime;

import com.gsma.rcs.cms.Constants;

import java.util.ArrayList;
import java.util.List;

public class MimeHeaders {

    class Header{
        private String mKey;
        private String mValue;
        public Header (String key, String value){
            mKey = key;
            mValue = value;
        }
        public String toString(){
            return new StringBuilder(mKey).append(Constants.HEADER_SEP).append(mValue).append(Constants.CRLF).toString();
        }
    }

    List<Header> mHeaders;

    public MimeHeaders(){
        mHeaders = new ArrayList<>();
    }

    public void addHeader(String key, String value){
        mHeaders.add(new Header(key, value));
    }

    public void addHeader(Header header){
        mHeaders.add(header);
    }

    public String toString(){
        StringBuilder sb = new StringBuilder();
        for(Header header : mHeaders){
            sb.append(header);
        }
        return sb.toString();
    }
}
