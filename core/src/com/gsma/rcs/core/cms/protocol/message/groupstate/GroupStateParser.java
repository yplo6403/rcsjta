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

package com.gsma.rcs.core.cms.protocol.message.groupstate;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.utils.ContactUtil;
import com.gsma.rcs.utils.logger.Logger;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class GroupStateParser extends DefaultHandler {

    private String mTimestamp;
    private String mLastFocusSessionId;
    private List<ContactId> mParticipants = new ArrayList<>();
    private final InputSource mInputSource;

    private static final Logger sLogger = Logger.getLogger(GroupStateParser.class.getName());

    /**
     * Constructor
     *
     * @param inputSource Input source
     */
    public GroupStateParser(InputSource inputSource) {
        mInputSource = inputSource;
    }

    /**
     * Parse the imdn parser
     * 
     * @return ImdnParser
     * @throws ParserConfigurationException
     * @throws SAXException
     * @throws ParseFailureException
     */
    public GroupStateParser parse() throws ParserConfigurationException, SAXException,
            ParseFailureException {
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser parser = factory.newSAXParser();
            parser.parse(mInputSource, this);
            return this;

        } catch (IOException e) {
            throw new ParseFailureException("Failed to parse input source!", e);
        }
    }

    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        if (GroupStateDocument.GROUP_STATE_ELEMENT.equals(localName)) {
            mLastFocusSessionId = attr.getValue(GroupStateDocument.LASTFOCUSSESSIONID_ATTR);
            mTimestamp = attr.getValue(GroupStateDocument.TIMESTAMP_ATTR);

        } else if (GroupStateDocument.PARTICIPANT_ELEMENT.equals(localName)) {
            String participant = attr.getValue(GroupStateDocument.COMM_ADDR_ATTR);
            ContactUtil.PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromUri(participant);
            if (phoneNumber == null) {
                if (sLogger.isActivated()) {
                    sLogger.error("Invalid participant " + participant);
                }
            } else {
                mParticipants.add(ContactUtil.createContactIdFromValidatedData(phoneNumber));
            }
        }
    }

    public void warning(SAXParseException exception) {
        if (sLogger.isActivated()) {
            sLogger.error("Warning: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }
    }

    public void error(SAXParseException exception) {
        if (sLogger.isActivated()) {
            sLogger.error("Error: line " + exception.getLineNumber() + ": " + exception.getMessage());
        }
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        if (sLogger.isActivated()) {
            sLogger.error("Fatal: line " + exception.getLineNumber() + ": " + exception.getMessage());
        }
        throw exception;
    }

    public GroupStateDocument getGroupStateDocument() {
        if (mTimestamp == null || mLastFocusSessionId == null) {
            return null;
        }
        return new GroupStateDocument(mLastFocusSessionId, mTimestamp, mParticipants);
    }
}
