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

package com.gsma.rcs.core.cms.protocol.message;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.core.cms.protocol.message.groupstate.GroupStateDocument;
import com.gsma.rcs.core.cms.protocol.message.groupstate.GroupStateParser;
import com.gsma.rcs.core.cms.utils.CmsUtils;
import com.gsma.services.rcs.contact.ContactId;

import com.gsma.rcs.imaplib.imap.Header;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class ImapGroupStateMessage extends ImapMessage {

    private final String mChatId;
    private String mRejoinId;
    private List<ContactId> mParticipants;

    public ImapGroupStateMessage(com.gsma.rcs.imaplib.imap.ImapMessage rawMessage)
            throws CmsSyncException {
        super(rawMessage);

        String from = getHeader(Constants.HEADER_FROM);
        if (from == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_FROM
                    + " IMAP header is missing");
        }

        String to = getHeader(Constants.HEADER_TO);
        if (from == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_TO + " IMAP header is missing");
        }

        String direction = getHeader(Constants.HEADER_DIRECTION);
        if (direction == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_DIRECTION
                    + " IMAP header is missing");
        }

        // remove my number from participants list
        ContactId myNumber;
        if (Constants.DIRECTION_SENT.equals(direction)) {
            myNumber = CmsUtils.headerToContact(from);
        } else {
            myNumber = CmsUtils.headerToContact(to);
        }

        try {
            mChatId = getHeader(Constants.HEADER_CONTRIBUTION_ID);
            if (mChatId == null) {
                throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID
                        + " IMAP header is missing");
            }

            String xml = getBodyPart().getPayload();
            if (!xml.isEmpty()) {
                GroupStateParser parser = new GroupStateParser(new InputSource(
                        new ByteArrayInputStream(xml.toString().getBytes())));
                GroupStateDocument document = parser.parse().getGroupStateDocument();
                mRejoinId = document.getLastfocussessionid();
                mParticipants = document.getParticipants();
                mParticipants.remove(myNumber);
            }

        } catch (ParserConfigurationException | SAXException | ParseFailureException e) {
            e.printStackTrace();
            throw new CmsSyncXmlFormatException(e);
        }
    }

    @Override
    public void parsePayload(String payload) {
        String[] parts = payload.split(Constants.CRLFCRLF, 2);
        if (2 == parts.length) {
            for (Header header : Header.parseHeaders(parts[0]).values()) {
                addHeader(header.getKey().toLowerCase(), header.getValue());
            }
            setBodyPart(new BodyPart(parts[1]));
        }
    }

    public String getRejoinId() {
        return mRejoinId;
    }

    public List<ContactId> getParticipants() {
        return mParticipants;
    }

    public String getChatId() {
        return mChatId;
    }

}
