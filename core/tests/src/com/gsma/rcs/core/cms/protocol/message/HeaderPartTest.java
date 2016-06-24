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
 ******************************************************************************/

package com.gsma.rcs.core.cms.protocol.message;

import com.gsma.rcs.core.cms.Constants;

import android.test.AndroidTestCase;

public class HeaderPartTest extends AndroidTestCase {

    public static StringBuilder headerContent = new StringBuilder();
    static {
        headerContent.append("From: +33643202148").append(Constants.CRLF);
        headerContent.append("To: +33643209850").append(Constants.CRLF);
        headerContent.append("Date: ven., 30 10 2015 16:26:16.0 +0100").append(Constants.CRLF);
        headerContent.append("Conversation-ID: 1446218776000").append(Constants.CRLF);
        headerContent.append("Contribution-ID: 1446218776000").append(Constants.CRLF);
        headerContent.append("Message-ID: 103015261610014200000").append(Constants.CRLF);
        headerContent.append("IMDN-Message-ID: 1446218776000").append(Constants.CRLF);
        headerContent.append("Message-Direction: received").append(Constants.CRLF);
        headerContent.append("Message-Context: \"multimedia-message\"").append(Constants.CRLF);
        headerContent.append("Content-Type: message/cpim").append(Constants.CRLF);
    }

    public void test() {
        HeaderPart headers = new HeaderPart();
        headers.addHeader(Constants.HEADER_FROM, "+33643202148");
        headers.addHeader(Constants.HEADER_TO, "+33643209850");
        headers.addHeader(Constants.HEADER_DATE, "ven., 30 10 2015 16:26:16.0 +0100");
        headers.addHeader(Constants.HEADER_CONVERSATION_ID, "1446218776000");
        headers.addHeader(Constants.HEADER_CONTRIBUTION_ID, "1446218776000");
        headers.addHeader(Constants.HEADER_MESSAGE_ID, "103015261610014200000");
        headers.addHeader(Constants.HEADER_IMDN_MESSAGE_ID, "1446218776000");
        headers.addHeader(Constants.HEADER_DIRECTION, "received");
        headers.addHeader(Constants.HEADER_MESSAGE_CONTEXT, "\"multimedia-message\"");
        headers.addHeader(Constants.HEADER_CONTENT_TYPE, "message/cpim");
        assertEquals(headerContent.toString(), headers.toString());
    }
}
