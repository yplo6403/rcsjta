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

package com.gsma.rcs.core.cms.protocol.message.cpmsession;

import com.gsma.rcs.core.ParseFailureException;

import android.test.AndroidTestCase;

import junit.framework.Assert;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;

import javax.xml.parsers.ParserConfigurationException;

public class CpmSessionParserTest extends AndroidTestCase {

    private static final String CRLF = "\r\n";

    public void test() throws ParseFailureException, SAXException, ParserConfigurationException {
        InputSource source = new InputSource(
                new ByteArrayInputStream(
                        ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
                                + CRLF
                                + "<session>"
                                + "<session-type>Group</session-type>"
                                + "<sdp>o=- 3664184448 3664184448 IN IP4 sip.imsnsn.fr</sdp>"
                                + "<invited-participants>tel:+33642639381;tel:+33643209850</invited-participants>" + "</session>")
                                .getBytes()));
        CpmSessionParser parser = new CpmSessionParser(source);
        parser.parse();
        CpmSessionDocument doc = parser.getCpmSessionDocument();
        Assert.assertEquals("Group", doc.getSessionType());
        Assert.assertEquals(2, doc.getParticipants().size());
        Assert.assertEquals("+33642639381", doc.getParticipants().get(0).toString());
        Assert.assertEquals("+33643209850", doc.getParticipants().get(1).toString());
    }
}
