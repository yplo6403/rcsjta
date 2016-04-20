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

package com.gsma.rcs.core.cms.protocol.message.groupstate;

import com.gsma.rcs.core.ParseFailureException;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.ParserConfigurationException;

/**
 * Created by VGZL8743 on 14/01/2016.
 */
public class GroupStateParserTest extends AndroidTestCase {

    private static final String CRLF = "\r\n";

    public void test() throws ParseFailureException, SAXException, ParserConfigurationException {

        InputSource source = new InputSource(new ByteArrayInputStream(
                ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + CRLF + "<groupstate" + CRLF
                        + "timestamp=\"2012-06-13T16:39:57-05:00\"" + CRLF
                        + "lastfocussessionid=\"sip:da9274453@company.com\"" + CRLF
                        + "group-type=\"Closed\">" + CRLF
                        + "<participant name=\"bob\" comm-addr=\"tel:+16135551210\"/>" + CRLF
                        + "<participant name=\"alice\" comm-addr=\"tel:+16135551211\"/>" + CRLF
                        + "<participant name=\"donald\" comm-addr=\"tel:+16135551212\"/>" + CRLF
                        + "</groupstate>" + CRLF).getBytes()));
        GroupStateParser parser = new GroupStateParser(source);
        parser.parse();
        GroupStateDocument doc = parser.getGroupStateDocument();
        Assert.assertEquals("2012-06-13T16:39:57-05:00", doc.getTimestamp());
        Assert.assertEquals("sip:da9274453@company.com", doc.getLastfocussessionid());
        Assert.assertEquals(3, doc.getParticipants().size());
        Assert.assertEquals("+16135551210", doc.getParticipants().get(0).toString());
        Assert.assertEquals("+16135551211", doc.getParticipants().get(1).toString());
        Assert.assertEquals("+16135551212", doc.getParticipants().get(2).toString());
    }
}
