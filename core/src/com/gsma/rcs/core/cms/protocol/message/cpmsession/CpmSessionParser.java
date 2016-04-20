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

package com.gsma.rcs.core.cms.protocol.message.cpmsession;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.utils.CmsUtils;
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

public class CpmSessionParser extends DefaultHandler {

    private String mSessionType;
    private List<ContactId> mParticipants = new ArrayList<>();

    private final InputSource mInputSource;
    private StringBuffer accumulator;

    /**
     * The logger
     */
    private Logger logger = Logger.getLogger(this.getClass().getName());

    /**
     * Constructor
     *
     * @param inputSource Input source
     */
    public CpmSessionParser(InputSource inputSource) {
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
    public CpmSessionParser parse() throws ParserConfigurationException, SAXException,
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

    @Override
    public void characters(char buffer[], int start, int length) {
        accumulator.append(buffer, start, length);
    }

    @Override
    public void startDocument() {
        accumulator = new StringBuffer();
    }

    @Override
    public void startElement(String namespaceURL, String localName, String qname, Attributes attr) {
        accumulator.setLength(0);
    }

    @Override
    public void endElement(String namespaceURL, String localName, String qname) {
        if (CpmSessionDocument.SESSION_TYPE.equals(localName)) {
            mSessionType = accumulator.toString();

        } else if (CpmSessionDocument.INVITED_PARTICIPANTS.equals(localName)) {
            for (String participant : accumulator.toString().split(";")) {
                ContactUtil.PhoneNumber phoneNumber = ContactUtil.getValidPhoneNumberFromUri(participant);
                if (phoneNumber == null) {
                    if (logger.isActivated()) {
                        logger.error("Invalid participant " + participant);
                    }
                } else {
                    mParticipants.add(ContactUtil.createContactIdFromValidatedData(phoneNumber));
                }
            }
        }
    }

    public void warning(SAXParseException exception) {
        if (logger.isActivated()) {
            logger.error("Warning: line " + exception.getLineNumber() + ": "
                    + exception.getMessage());
        }
    }

    public void error(SAXParseException exception) {
        if (logger.isActivated()) {
            logger.error("Error: line " + exception.getLineNumber() + ": " + exception.getMessage());
        }
    }

    public void fatalError(SAXParseException exception) throws SAXException {
        if (logger.isActivated()) {
            logger.error("Fatal: line " + exception.getLineNumber() + ": " + exception.getMessage());
        }
        throw exception;
    }

    public CpmSessionDocument getCpmSessionDocument() {
        return new CpmSessionDocument(mSessionType, mParticipants);
    }
}
