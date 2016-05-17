/*******************************************************************************
 * Software Name : RCS IMS Stack
 * <p/>
 * Copyright (C) 2010 France Telecom S.A.
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/

package com.gsma.rcs.chat;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnDocument;
import com.gsma.rcs.core.ims.service.im.chat.imdn.ImdnParser;
import com.gsma.rcs.utils.DateUtils;
import com.gsma.rcs.utils.logger.Logger;

import android.test.AndroidTestCase;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;

public class ImdnParserTest extends AndroidTestCase {
    private static Logger sLogger = Logger.getLogger(ImdnParserTest.class.getName());

    // @formatter:off
    private static final String sXmlDisplayNotification = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">\n"
            + "<message-id>34jk324j</message-id>\n"
            + "<datetime>2008-04-04T12:16:49-05:00</datetime>\n"
            + "<display-notification><status><displayed/></status></display-notification>\n"
            + "</imdn>";

    private static final String sXmlDeliveryNotification = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<imdn xmlns=\"urn:ietf:params:xml:ns:imdn\">\n"
            + "<message-id>554671c403f544208924fc29aee3e6eb</message-id>\n"
            + "<datetime>2016-05-13T14:23:23+02:00</datetime>\n"
            + "<delivery-notification><status><delivered/></status></delivery-notification>\n"
            + "</imdn>";

    // @formatter:on

    public void testParseImdnDocumentDisplayNofitfication() throws SAXException,
            ParserConfigurationException, IOException, ParseFailureException {
        ImdnParser parser = new ImdnParser(new InputSource(new ByteArrayInputStream(
                sXmlDisplayNotification.getBytes())));
        parser.parse();
        ImdnDocument imdnDoc = parser.getImdnDocument();
        assertEquals("34jk324j", imdnDoc.getMsgId());
        assertEquals("displayed", imdnDoc.getStatus().toString());
        assertEquals("display-notification", imdnDoc.getNotificationType());
        assertEquals(DateUtils.decodeDate("2008-04-04T12:16:49-05:00"), imdnDoc.getDateTime());
    }

    public void testParseImdnDocumentDeliveryNofitfication() throws SAXException,
            ParserConfigurationException, IOException, ParseFailureException {
        ImdnParser parser = new ImdnParser(new InputSource(new ByteArrayInputStream(
                sXmlDeliveryNotification.getBytes())));
        parser.parse();
        ImdnDocument imdnDoc = parser.getImdnDocument();
        assertEquals("554671c403f544208924fc29aee3e6eb", imdnDoc.getMsgId());
        assertEquals("delivered", imdnDoc.getStatus().toString());
        assertEquals("delivery-notification", imdnDoc.getNotificationType());
        assertEquals(DateUtils.decodeDate("2016-05-13T14:23:23+02:00"), imdnDoc.getDateTime());
    }
}
