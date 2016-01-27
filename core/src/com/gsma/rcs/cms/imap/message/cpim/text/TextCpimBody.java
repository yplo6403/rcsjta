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

package com.gsma.rcs.cms.imap.message.cpim.text;

import com.gsma.rcs.cms.Constants;
import com.gsma.rcs.cms.imap.message.cpim.CpimBody;

import com.sonymobile.rcs.imap.Header;

public class TextCpimBody extends CpimBody {

    private String mContent;

    public TextCpimBody(String contentType, String content) {
        super(contentType);
        mContent = content;
    }

    public TextCpimBody() {
        super();
    }

    @Override
    public void parseBody(String body) {
        String[] parts = body.split(Constants.CRLFCRLF, 2);
        if (2 == parts.length) {
            for (Header header : Header.parseHeaders(parts[0]).values()) {
                mHeaders.addHeader(header.getKey(), header.getValue());
            }
            mContent = parts[1];
        }
    }

    @Override
    public String toPayload() {
        StringBuilder sb = new StringBuilder();
        sb.append(mHeaders);
        sb.append(Constants.CRLF);
        sb.append(mContent);
        return sb.toString();
    }

    public String getContent() {
        return mContent;
    }
}
