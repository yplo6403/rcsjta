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

package com.gsma.rcs.core.cms.protocol.message.cpim;

import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.protocol.message.BodyPart;
import com.gsma.rcs.core.cms.protocol.message.HeaderPart;
import com.gsma.rcs.imaplib.imap.Header;

public class CpimMessage extends BodyPart {

    protected final HeaderPart mHeaders;
    protected final CpimBody mBody;

    public CpimMessage(HeaderPart headers, CpimBody body) {
        super();
        mHeaders = headers;
        mBody = body;
    }

    public void parsePayload(String payload) {
        String[] parts = payload.split(Constants.CRLFCRLF, 2);
        if (2 == parts.length) {
            for (Header header : Header.parseHeaders(parts[0]).values()) {
                mHeaders.addHeader(header.getKey().toLowerCase(), header.getValue());
            }
            mBody.parseBody(parts[1]);
        }
    }

    @Override
    public String getPayload() {
        StringBuilder sb = new StringBuilder();
        sb.append(mHeaders);
        sb.append(Constants.CRLF);
        sb.append(mBody.toPayload());
        return sb.toString();
    }

    public CpimBody getBody() {
        return mBody;
    }

    public String getHeader(String headerName) {
        return mHeaders.getHeaderValue(headerName);
    }

    public String getContentType() {
        return mBody.getContentType();
    }

    /*
     * public ContactId getContact(String headerName){ String raw =
     * mHeaders.getHeaderValue(headerName); if(raw.startsWith("<") && raw.endsWith(">")){ } }
     */

}
