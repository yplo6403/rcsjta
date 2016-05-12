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

import android.text.TextUtils;

import com.gsma.rcs.core.ParseFailureException;
import com.gsma.rcs.core.cms.Constants;
import com.gsma.rcs.core.cms.event.exception.CmsSyncException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMessageNotSupportedException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.core.cms.protocol.message.cpmsession.CpmSessionDocument;
import com.gsma.rcs.core.cms.protocol.message.cpmsession.CpmSessionParser;
import com.gsma.rcs.core.cms.utils.DateUtils;
import com.gsma.rcs.imaplib.imap.Header;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.RcsService.Direction;
import com.gsma.services.rcs.RcsService;
import com.gsma.services.rcs.contact.ContactId;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class ImapCpmSessionMessage extends ImapMessage {

    private static final String SESSION_TYPE_GROUP = "Group";

    private final String mChatId;
    private final RcsService.Direction mDirection;
    private final long mTimestamp;
    private final String mSubject;
    private List<ContactId> mParticipants;

    /**
     * Constructor
     * 
     * @param settings the RCS settings accessor
     * @param rawMessage the IMAP raw message
     * @throws CmsSyncException
     */
    public ImapCpmSessionMessage(RcsSettings settings,
            com.gsma.rcs.imaplib.imap.ImapMessage rawMessage) throws CmsSyncException {
        super(rawMessage);
        String direction = getHeader(Constants.HEADER_DIRECTION);
        if (direction == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_DIRECTION
                    + " IMAP header is missing");
        }
        if (Constants.DIRECTION_SENT.equals(direction)) {
            mDirection = Direction.OUTGOING;
        } else {
            mDirection = Direction.INCOMING;
        }
        String date = getHeader(Constants.HEADER_DATE);
        if (date == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_DATE
                    + " IMAP header is missing");
        }
        mTimestamp = DateUtils.parseDate(date, DateUtils.CMS_IMAP_DATE_FORMAT);
        try {
            mChatId = getHeader(Constants.HEADER_CONTRIBUTION_ID);
            if (mChatId == null) {
                throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID
                        + " IMAP header is missing");
            }
            mSubject = getHeader(Constants.HEADER_SUBJECT);
            String xml = getBodyPart().getPayload();
            if (!TextUtils.isEmpty(xml)) {
                CpmSessionParser parser = new CpmSessionParser(new InputSource(
                        new ByteArrayInputStream(xml.getBytes())));
                CpmSessionDocument document = parser.parse().getCpmSessionDocument();
                String sessionType = document.getSessionType();
                if (!SESSION_TYPE_GROUP.equals(sessionType)) {
                    throw new CmsSyncMessageNotSupportedException(
                            "This type of cpm session is not supported : " + sessionType);
                }
                mParticipants = document.getParticipants();
                mParticipants.remove(settings.getUserProfileImsUserName());
            }
        } catch (ParserConfigurationException | SAXException | ParseFailureException e) {
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

    public String getSubject() {
        return mSubject;
    }

    public List<ContactId> getParticipants() {
        return mParticipants;
    }

    public long getTimestamp() {
        return mTimestamp;
    }

    public String getChatId() {
        return mChatId;
    }

    public Direction getDirection() {
        return mDirection;
    }
}
