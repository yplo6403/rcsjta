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

package com.gsma.rcs.cms.imap.message.groupstate;

import com.gsma.services.rcs.contact.ContactUtil;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.xml.sax.InputSource;

import java.io.ByteArrayInputStream;

/**
 * Created by VGZL8743 on 14/01/2016.
 */
public class GroupStateParserTest extends AndroidTestCase {

    private final String CRLF = "\r\n";

    public void test() {

        try {
            ContactUtil.getInstance(getContext());
            StringBuffer xml = new StringBuffer()
                    .append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(CRLF)
                    .append("<groupstate").append(CRLF)
                    .append("timestamp=\"2012-06-13T16:39:57-05:00\"").append(CRLF)
                    .append("lastfocussessionid=\"sip:da9274453@company.com\"").append(CRLF)
                    .append("group-type=\"Closed\">").append(CRLF)
                    .append("<participant name=\"bob\" comm-addr=\"tel:+16135551210\"/>")
                    .append(CRLF)
                    .append("<participant name=\"alice\" comm-addr=\"tel:+16135551211\"/>")
                    .append(CRLF)
                    .append("<participant name=\"donald\" comm-addr=\"tel:+16135551212\"/>")
                    .append(CRLF).append("</groupstate>").append(CRLF);

            InputSource source = new InputSource(
                    new ByteArrayInputStream(xml.toString().getBytes()));
            GroupStateParser parser = new GroupStateParser(source);
            parser.parse();
            GroupStateDocument doc = parser.getGroupStateDocument();
            Assert.assertEquals("2012-06-13T16:39:57-05:00", doc.getTimestamp());
            Assert.assertEquals("sip:da9274453@company.com", doc.getLastfocussessionid());
            Assert.assertEquals(3, doc.getParticipants().size());
            Assert.assertEquals("+16135551210", doc.getParticipants().get(0).toString());
            Assert.assertEquals("+16135551211", doc.getParticipants().get(1).toString());
            Assert.assertEquals("+16135551212", doc.getParticipants().get(2).toString());
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail();
        }
    }
}
