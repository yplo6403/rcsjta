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
import com.gsma.rcs.core.cms.event.exception.CmsSyncMissingHeaderException;
import com.gsma.rcs.core.cms.event.exception.CmsSyncXmlFormatException;
import com.gsma.rcs.core.cms.protocol.message.groupstate.GroupStateDocument;
import com.gsma.rcs.core.cms.protocol.message.groupstate.GroupStateParser;
import com.gsma.rcs.provider.settings.RcsSettings;
import com.gsma.services.rcs.contact.ContactId;

import android.text.TextUtils;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.xml.parsers.ParserConfigurationException;

public class ImapGroupStateMessage extends ImapMessage {

    private final String mChatId;
    private final RcsSettings mRcsSettings;
    private final String mMessageId;
    private String mRejoinId;
    private List<ContactId> mParticipants;

    /**
     * Constructor
     * 
     * @param settings the RCS settings accessor
     * @param rawMessage the IMAP raw message
     * @throws CmsSyncMissingHeaderException
     */
    public ImapGroupStateMessage(RcsSettings settings,
            com.gsma.rcs.imaplib.imap.ImapMessage rawMessage) throws CmsSyncMissingHeaderException {
        super(rawMessage);
        mChatId = getHeader(Constants.HEADER_CONTRIBUTION_ID);
        if (mChatId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_CONTRIBUTION_ID
                    + " IMAP header is missing");
        }
        mMessageId = getHeader(Constants.HEADER_IMDN_MESSAGE_ID);
        if (mMessageId == null) {
            throw new CmsSyncMissingHeaderException(Constants.HEADER_IMDN_MESSAGE_ID
                    + " IMAP header is missing");
        }
        mRcsSettings = settings;
    }

    @Override
    public void parseBody() throws CmsSyncXmlFormatException {
        String content = mRawMessage.getBody().getContent();
        if (TextUtils.isEmpty(content)) {
            throw new CmsSyncXmlFormatException("Cannot parse Group State: IMAP body is missing!");
        }
        setBodyPart(new BodyPart(content));
        try {
            GroupStateParser parser = new GroupStateParser(new InputSource(
                    new ByteArrayInputStream(content.getBytes())));
            GroupStateDocument document = parser.parse().getGroupStateDocument();
            mRejoinId = document.getLastFocusSessionId();
            mParticipants = document.getParticipants();
            mParticipants.remove(mRcsSettings.getUserProfileImsUserName());
            if (mParticipants == null || mParticipants.isEmpty()) {
                throw new CmsSyncXmlFormatException("Invalid Group State: " + content);
            }
        } catch (ParserConfigurationException | SAXException | ParseFailureException e) {
            throw new CmsSyncXmlFormatException(e);
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

    public String getMessageId() {
        return mMessageId;
    }

}
